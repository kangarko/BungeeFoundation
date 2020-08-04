package org.mineacademy.bfo.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.constants.FoConstants.Header;
import org.mineacademy.bfo.model.Replacer;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.util.ClassWrapper;
import jline.internal.Nullable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Platform independent version of the YamlStaticConfig
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class YamlStaticConfig {

	private static Yaml TEMPORARY_INSTANCE;

	public static void pathPrefix(final String pathPrefix) {
		temporaryInstance().setPathPrefix(pathPrefix);
	}

	public static String colorize(@Nullable final String input) {
		if (input == null)
			return "";

		return input.replace("§", "&");
	}

	public static String pathPrefix() {
		return temporaryInstance().getPathPrefix();
	}

	public static void set(final String path, final Object value) {
		temporaryInstance().set(path, value);
		//    temporaryInstance().setHeader(FoConstants.Header.UPDATED_FILE);
	}

	// ----------------------------------------------------------------------------------------------------
	// Methods for using our yaml
	// ----------------------------------------------------------------------------------------------------

	protected static boolean contains(@NonNull final String key) {
		return TEMPORARY_INSTANCE.contains(key);
	}

	protected static boolean isSet(final String path) {
		return temporaryInstance().contains(path);
	}

	// ----------------------------------------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------------------------------------

	protected static String getFileName() {
		return temporaryInstance().getName();
	}

	protected static Object get(@NonNull final String key) {
		return temporaryInstance().get(key);
	}

	/**
	 * Return a replacer for localizable messages
	 *
	 * @param path
	 * @return
	 */
	protected static Replacer getReplacer(final String path) {
		final Object raw = get(path);

		Valid.checkBoolean(raw != null, "Your config lacks a Replacer at '" + path + "'");

		if (raw instanceof List<?>)
			return Replacer.of((List<String>) raw);
		return Replacer.of(raw.toString());
	}

	protected static Object getObject(@NonNull final String key) {
		return temporaryInstance().get(key);
	}

	protected static <T> T getOrDefault(@NonNull final String key, final T def) {
		return temporaryInstance().getOrDefault(key, def);
	}

	protected static <T> T getOrSetDefault(@NonNull final String key, final T def) {
		final T result = temporaryInstance().getOrSetDefault(key, def);
		temporaryInstance().setHeader(Header.UPDATED_FILE);
		return result;
	}

	// ----------------------------------------------------------------------------------------------------
	// Primitive getters used in our other methods
	// ----------------------------------------------------------------------------------------------------

	protected static <T> T getOrError(@NonNull final String key, @NonNull final Class<T> type) {
		final val raw = get(key);

		Valid.checkBoolean(raw != null, "Your config lacks '" + type.getSimpleName() + "' at '" + key + "'");

		return ClassWrapper.getFromDef(get(key), type);
	}

	protected static List<String> getStringList(final String path) {
		return temporaryInstance().getStringList(path);
	}

	protected static boolean getBoolean(final String path) {
		return getOrError(path, boolean.class);
	}

	// ----------------------------------------------------------------------------------------------------
	// More advanced setters
	// ----------------------------------------------------------------------------------------------------

	protected static String getString(final String path) {
		return colorize(getOrError(path, String.class));
	}

	protected static int getInteger(final String path) {
		return getOrError(path, int.class);
	}

	protected static double getDouble(final String path) {
		return getOrError(path, double.class);
	}

	protected static Yaml temporaryInstance() {
		Valid.checkNotNull(TEMPORARY_INSTANCE, "Temporary instance is null - Make sure to set your temporaryInstance.");
		return TEMPORARY_INSTANCE;
	}

	@SafeVarargs
	public static void loadAll(final Class<? extends YamlStaticConfig>... classes) {
		loadAll(Arrays.asList(classes));
	}

	@SneakyThrows
	public static void loadAll(@NonNull final List<Class<? extends YamlStaticConfig>> classes) {
		// Loading Settings
		for (final Class<? extends YamlStaticConfig> aClass : classes) {
			final YamlStaticConfig config = aClass.newInstance();
			if (!(config instanceof SimpleSettings))
				continue;
			config.load();
		}

		// Loading Localizations
		for (final Class<? extends YamlStaticConfig> aClass : classes) {
			final YamlStaticConfig config = aClass.newInstance();
			if (config instanceof SimpleSettings)
				continue;
			config.load();
		}
	}

	protected void beforeLoad() {
	}

	// ----------------------------------------------------------------------------------------------------
	// Setting up our internal config & load our data via reflection.
	// ----------------------------------------------------------------------------------------------------

	protected String[] getHeader() {
		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	// Loading our config
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Set the config using cfg(Yaml yaml) Must be used
	 */
	protected abstract de.leonhard.storage.Yaml getConfigInstance();

	public final void load() {
		TEMPORARY_INSTANCE = getConfigInstance();
		beforeLoad();
		loadViaReflection();
	}

	/**
	 * Loads the class via reflection, scanning for "private static void init()"
	 * methods to run
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

		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Invoke all "private static void init()" methods in the class and its
	 * subclasses
	 *
	 * @param clazz
	 * @throws Exception
	 */
	private void invokeAll(final Class<?> clazz) throws Exception {
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
	private void invokeMethodsIn(final Class<?> clazz) throws Exception {
		for (final Method method : clazz.getDeclaredMethods()) {

			final int mod = method.getModifiers();

			if (method.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && method.getReturnType() == Void.TYPE && method.getParameterTypes().length == 0, "Method '" + method.getName() + "' in " + clazz + " must be 'private static void init()'");

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
	private void checkFields(final Class<?> clazz) throws Exception {
		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (Modifier.isPublic(field.getModifiers()))
				Valid.checkBoolean(!field.getType().isPrimitive(), "Field '" + field.getName() + "' in " + clazz + " must not be primitive!");

			try {
				field.get(null);
			} catch (final NullPointerException ex) {
			}
		}
	}
}
