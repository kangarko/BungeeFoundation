package org.mineacademy.bfo.settings;

import de.leonhard.storage.Config;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.util.ClassWrapper;
import de.leonhard.storage.util.Valid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.constants.FoConstants;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Replacer;
import org.mineacademy.bfo.plugin.SimplePlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class YamlStaticConfig {
	private static Yaml TEMPORARY_INSTANCE;

	protected void beforeLoad() {
	}

	protected String[] getHeader() {
		return null;
	}

	/**
	 * Set the config using cfg(Yaml yaml)
	 * Must be used
	 */
	protected abstract Yaml getConfigInstance();

	// ----------------------------------------------------------------------------------------------------
	// Methods for using our yaml
	// ----------------------------------------------------------------------------------------------------

	public static void pathPrefix(@NonNull String pathPrefix) {
		temporaryInstance().setPathPrefix(pathPrefix);
	}

	public static String pathPrefix() {
		return temporaryInstance().getPathPrefix();
	}

	// ----------------------------------------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------------------------------------

	protected static void set(String path, Object value) {
		temporaryInstance().set(path, value);
		temporaryInstance().setHeader(Arrays.asList(FoConstants.Header.UPDATED_FILE));
	}

	protected static boolean contains(@NonNull final String key) {
		return TEMPORARY_INSTANCE.contains(key);
	}

	protected static boolean isSet(String path) {
		return temporaryInstance().contains(path);
	}

	protected static String getFileName() {
		return temporaryInstance().getName();
	}

	// ----------------------------------------------------------------------------------------------------
	// Primitive getters used in our other methods
	// ----------------------------------------------------------------------------------------------------

	protected static Object get(@NonNull final String key) {
		return temporaryInstance().get(key);
	}

	protected static Object getObject(@NonNull String key) {
		return temporaryInstance().get(key);
	}

	protected static <T> T getOrDefault(@NonNull final String key, final T def) {
		return temporaryInstance().getOrDefault(key, def);
	}

	protected static <T> T getOrError(@NonNull String key, @NonNull final Class<T> type) {
		if (!contains(key)) {
			throw new FoException("Your config lacks '" + type.getSimpleName() + "' at '" + key + "'");
		}
		return ClassWrapper.getFromDef(get(key), type);
	}

	// ----------------------------------------------------------------------------------------------------
	// More advanced setters
	// ----------------------------------------------------------------------------------------------------

	protected static StrictList<String> getCommandList(String path) {
		return new StrictList<>(getStringList(path));
	}

	protected static List<String> getStringList(String path) {
		return temporaryInstance().getStringList(path);
	}

	protected static boolean getBoolean(String path) {
		return temporaryInstance().getBoolean(path);
	}

	protected static String getString(String path) {
		return temporaryInstance().getString(path);
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @return
	 */
	protected static Replacer getReplacer(String path) {
		return getReplacer(path, "");
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	protected static Replacer getReplacer(String path, String def) {
		final String message = getString(path);

		return Replacer.of(Common.getOrDefault(message, def));
	}

	protected static final int getInteger(String path) {
		return temporaryInstance().getInt(path);
	}

	protected static final double getDouble(String path) {
		return temporaryInstance().getDouble(path);
	}


	// ----------------------------------------------------------------------------------------------------
	// Setting up our internal config & load our data via reflection.
	// ----------------------------------------------------------------------------------------------------

	protected static Yaml temporaryInstance() {
		Valid.notNull(TEMPORARY_INSTANCE, "Temporary instance is null", "Make sure to set your temporaryInstance.");
		return TEMPORARY_INSTANCE;
	}

	// ----------------------------------------------------------------------------------------------------
	// Loading our config
	// ----------------------------------------------------------------------------------------------------

	@SneakyThrows
	public static void loadAll(@NonNull final List<Class<? extends YamlStaticConfig>> classes) {
		for (final Class<? extends YamlStaticConfig> aClass : classes) {
			final YamlStaticConfig config = aClass.newInstance();
			config.load();
		}
	}


	public void load() {
		TEMPORARY_INSTANCE = getConfigInstance();
		loadViaReflection();
	}

	/**
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	public final void loadViaReflection() {
		try {
			beforeLoad();

			// Parent class if applicable.
			if (YamlStaticConfig.class.isAssignableFrom(getClass().getSuperclass())) {
				final Class<?> superClass = getClass().getSuperclass();

				invokeAll(superClass);
			}

			// The class itself.
			invokeAll(getClass());

		} catch (Throwable t) {

		}
	}

	/**
	 * Invoke all "private static void init()" methods in the class and its subclasses
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private final void invokeAll(Class<?> clazz) throws Exception {
		invokeMethodsIn(clazz);

		// All sub-classes in superclass.
		for (final Class<?> subClazz : clazz.getDeclaredClasses()) {
			invokeMethodsIn(subClazz);

			// And classes in sub-classes in superclass.
			for (final Class<?> subSubClazz : subClazz.getDeclaredClasses())
				invokeMethodsIn(subSubClazz);
		}
	}

	/**
	 * Invoke all "private static void init()" methods in the class
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void invokeMethodsIn(Class<?> clazz) throws Exception {
		for (final Method method : clazz.getDeclaredMethods()) {

			if (!SimplePlugin.getInstance().isEnabled()) // Disable if plugin got shutdown for an error
				return;

			final int mod = method.getModifiers();

			if (method.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && method.getReturnType() == Void.TYPE && method.getParameterTypes().length == 0,
					"Method '" + method.getName() + "' in " + clazz + " must be 'private static void init()'");

				method.setAccessible(true);
				method.invoke(null);
			}
		}
		checkFields(clazz);
	}

	/**
	 * Safety check whether all fields have been set
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void checkFields(Class<?> clazz) throws Exception {
		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (Modifier.isPublic(field.getModifiers()))
				Valid.checkBoolean(!field.getType().isPrimitive(), "Field '" + field.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;
			try {
				result = field.get(null);
			} catch (final NullPointerException ex) {
			}
			Valid.notNull(result, "Null " + field.getType().getSimpleName() + " field '" + field.getName() + "' in " + clazz);
		}
	}
}
