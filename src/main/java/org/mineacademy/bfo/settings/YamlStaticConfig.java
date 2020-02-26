package org.mineacademy.bfo.settings;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.util.ClassWrapper;
import de.leonhard.storage.util.Valid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Replacer;
import org.mineacademy.bfo.plugin.SimplePlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class YamlStaticConfig {
	private static Yaml internalYaml;

	protected void beforeLoad() {
	}

	protected String[] getHeader() {
		return null;
	}

	/**
	 * Set the config using cfg(Yaml yaml)
	 * Must be used
	 */
	public abstract Yaml getConfigInstance();

	// ----------------------------------------------------------------------------------------------------
	// Methods for using our yaml
	// ----------------------------------------------------------------------------------------------------

	public static void pathPrefix(@NonNull String pathPrefix) {
		cfg().setPathPrefix(pathPrefix);
	}

	public static String pathPrefix() {
		return cfg().getPathPrefix();
	}

	public static Object get(@NonNull final String key) {
		return cfg().get(key);
	}

	public static boolean contains(@NonNull final String key) {
		return internalYaml.contains(key);
	}

	protected static <T> T getOrDefault(@NonNull final String key, final T def) {
		return cfg().getOrDefault(key, def);
	}

	protected static <T> T getOrError(@NonNull String key, @NonNull final Class<T> type) {
		if (!contains(key)) {
			throw new FoException("Your config lacks '" + type.getSimpleName() + "' at '" + key + "'");
		}
		return ClassWrapper.getFromDef(get(key), type);
	}

	protected static Object getObject(@NonNull String key) {
		return cfg().get(key);
	}


	protected static void set(String path, Object value) {
		cfg().set(path, value);
	}

	protected static boolean isSet(String path) {
		return cfg().contains(path);
	}

	protected static String getFileName() {
		return cfg().getName();
	}

	// -----------------------------------------------------------------------------------------------------
	// Config manipulators
	// -----------------------------------------------------------------------------------------------------

	protected static StrictList<String> getCommandList(String path) {
		return new StrictList<>(getStringList(path));
	}

	protected static List<String> getStringList(String path) {
		return cfg().getStringList(path);
	}

	protected static boolean getBoolean(String path) {
		return cfg().getBoolean(path);
	}

	protected static String getString(String path) {
		return cfg().getString(path);
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
		return cfg().getInt(path);
	}

	protected static final double getDouble(String path) {
		return cfg().getDouble(path);
	}


	// ----------------------------------------------------------------------------------------------------
	// Setting up our internal config & load our data via reflection.
	// ----------------------------------------------------------------------------------------------------

	public static Yaml cfg() {
		Valid.notNull(internalYaml, "Please set the Yaml using cfg() to set your Config-Instance first");
		return internalYaml;
	}


	@SneakyThrows
	public static void loadAll(@NonNull final List<Class<? extends YamlStaticConfig>> classes) {
		for (final Class<? extends YamlStaticConfig> aClass : classes) {
			final YamlStaticConfig config = aClass.newInstance();
			config.load();
		}
	}

	public void load() {
		internalYaml = getConfigInstance();
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
		for (final Method m : clazz.getDeclaredMethods()) {

			if (!SimplePlugin.getInstance().isEnabled()) // Disable if plugin got shutdown for an error
				return;

			final int mod = m.getModifiers();

			if (m.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0,
						"Method '" + m.getName() + "' in " + clazz + " must be 'private static void init()'");

				m.setAccessible(true);
				m.invoke(null);
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
		for (final Field f : clazz.getDeclaredFields()) {
			f.setAccessible(true);

			if (Modifier.isPublic(f.getModifiers()))
				Valid.checkBoolean(!f.getType().isPrimitive(), "Field '" + f.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;
			try {
				result = f.get(null);
			} catch (final NullPointerException ex) {
			}
			Valid.notNull(result, "Null " + f.getType().getSimpleName() + " field '" + f.getName() + "' in " + clazz);
		}
	}
}
