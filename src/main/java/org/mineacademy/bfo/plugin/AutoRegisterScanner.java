package org.mineacademy.bfo.plugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.annotation.AutoRegister;
import org.mineacademy.bfo.command.SimpleCommand;
import org.mineacademy.bfo.command.SimpleCommandGroup;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.SimpleExpansion;
import org.mineacademy.bfo.model.Tuple;
import org.mineacademy.bfo.model.Variables;
import org.mineacademy.bfo.proxy.ProxyListener;
import org.mineacademy.bfo.remain.Remain;
import org.mineacademy.bfo.settings.SimpleLocalization;
import org.mineacademy.bfo.settings.SimpleSettings;
import org.mineacademy.bfo.settings.YamlConfig;
import org.mineacademy.bfo.settings.YamlStaticConfig;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Utilizes \@AutoRegister annotation to add auto registration support for commands, events and much more.
 */
final class AutoRegisterScanner {

	/**
	 * Prevents overriding {@link ProxyListener} in case of having multiple
	 */
	private static boolean proxyListenerRegistered = false;

	/**
	 * Automatically register the main command group if there is only one in the code
	 */
	private static List<SimpleCommandGroup> registeredCommandGroups = new ArrayList<>();

	/**
	 * Scans your plugin and if your class implements {@link Listener}
	 * and has "instance" method to be a singleton, your events are registered there automatically
	 * <p>
	 * If not, we only call the instance constructor in case there is any underlying registration going on
	 */
	public static void scanAndRegister() {

		// Reset
		proxyListenerRegistered = false;
		registeredCommandGroups.clear();

		// Find all plugin classes that can be autoregistered
		final List<Class<?>> classes = findValidClasses();

		// Register settings early to be used later
		registerSettings(classes);

		for (final Class<?> clazz : classes)
			try {

				// Prevent beginner programmer mistake of forgetting to implement listener
				try {
					for (final Method method : clazz.getMethods())
						if (method.isAnnotationPresent(EventHandler.class))
							Valid.checkBoolean(Listener.class.isAssignableFrom(clazz), "Detected @EventHandler in " + clazz + ", make this class 'implements Listener' before using events there");

				} catch (final Error err) {
					// Ignore, likely caused by missing plugins
				}

				// Handled above
				if (YamlStaticConfig.class.isAssignableFrom(clazz))
					continue;

				// Auto register classes
				final AutoRegister autoRegister = clazz.getAnnotation(AutoRegister.class);

				// Require our annotation to be used, or support legacy classes from Foundation 5
				if (autoRegister != null
						|| ProxyListener.class.isAssignableFrom(clazz)
						|| SimpleExpansion.class.isAssignableFrom(clazz)) {

					Valid.checkBoolean(Modifier.isFinal(clazz.getModifiers()), "Please make " + clazz + " final for it to be registered automatically (or via @AutoRegister)");

					try {
						autoRegister(clazz, autoRegister == null || !autoRegister.hideIncompatibilityWarnings());

					} catch (final NoClassDefFoundError | NoSuchFieldError ex) {
						Common.warning("Failed to auto register " + clazz + " due to it requesting missing fields/classes: " + ex.getMessage());

					} catch (final Throwable t) {
						final String error = Common.getOrEmpty(t.getMessage());

						if (t instanceof NoClassDefFoundError && error.contains("org/bukkit/entity")) {
							Common.warning("**** WARNING ****");

							if (error.contains("DragonFireball"))
								Common.warning("Your Minecraft version does not have DragonFireball class, we suggest replacing it with a Fireball instead in: " + clazz);
							else
								Common.warning("Your Minecraft version does not have " + error + " class you call in: " + clazz);
						} else
							Common.error(t, "Failed to auto register class " + clazz);
					}
				}

			} catch (final Throwable t) {

				// Ignore exception in other class we loaded
				if (t instanceof VerifyError)
					continue;

				Common.error(t, "Failed to scan class '" + clazz + "' using Foundation!");
			}

		// Register command groups later
		registerCommandGroup(classes);
	}

	/*
	 * Registers settings and localization classes, either automatically if
	 * a class is detected, or forced if settings/localization files are found
	 */
	private static void registerSettings(List<Class<?>> classes) {
		final List<Class<?>> staticSettingsFound = new ArrayList<>();
		final List<Class<?>> staticLocalizations = new ArrayList<>();

		for (final Class<?> clazz : classes) {
			boolean load = false;

			if (clazz == SimpleLocalization.class || clazz == SimpleSettings.class || clazz == YamlStaticConfig.class)
				continue;

			if (SimpleSettings.class.isAssignableFrom(clazz)) {
				staticSettingsFound.add(clazz);

				load = true;
			}

			if (SimpleLocalization.class.isAssignableFrom(clazz)) {
				staticLocalizations.add(clazz);

				load = true;
			}

			if (load || !load && YamlStaticConfig.class.isAssignableFrom(clazz))
				YamlStaticConfig.load((Class<? extends YamlStaticConfig>) clazz);
		}

		boolean staticSettingsFileExist = false;
		boolean staticLocalizationFileExist = false;

		try (final JarFile jarFile = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry type = it.nextElement();
				final String name = type.getName();

				if (name.matches("settings\\.yml"))
					staticSettingsFileExist = true;

				else if (name.matches("localization\\/messages\\_(.*)\\.yml"))
					staticLocalizationFileExist = true;
			}
		} catch (final IOException ex) {
		}

		Valid.checkBoolean(staticSettingsFound.size() < 2, "Cannot have more than one class extend SimpleSettings: " + staticSettingsFound);
		Valid.checkBoolean(staticLocalizations.size() < 2, "Cannot have more than one class extend SimpleLocalization: " + staticLocalizations);

		if (staticSettingsFound.isEmpty() && staticSettingsFileExist)
			YamlStaticConfig.load(SimpleSettings.class);

		if (staticLocalizations.isEmpty() && staticLocalizationFileExist)
			YamlStaticConfig.load(SimpleLocalization.class);
	}

	/*
	 * Registers command groups, automatically assuming the main command group from the main command label
	 */
	private static void registerCommandGroup(List<Class<?>> classes) {
		boolean mainCommandGroupFound = false;

		for (final SimpleCommandGroup group : registeredCommandGroups) {

			// Register if main command or there is only one command group, then assume main
			if (group.getLabel().equals(SimpleSettings.MAIN_COMMAND_ALIASES.first()) || registeredCommandGroups.size() == 1) {
				Valid.checkBoolean(!mainCommandGroupFound, "Found 2 or more command groups that do not specify label in their constructor."
						+ " (We can only automatically use one of such groups as the main one using Command_Aliases as command label(s)"
						+ " from settings.yml but not more.");

				SimplePlugin.getInstance().setMainCommand(group);
				mainCommandGroupFound = true;
			}

			SimplePlugin.getInstance().registerCommands(group);
		}
	}

	/*
	 * Automatically registers the given class, printing console warnings
	 */
	private static void autoRegister(Class<?> clazz, boolean printWarnings) {

		final SimplePlugin plugin = SimplePlugin.getInstance();
		final Tuple<FindInstance, Object> tuple = findInstance(clazz);

		final FindInstance mode = tuple.getKey();
		final Object instance = tuple.getValue();

		boolean eventsRegistered = false;

		if (ProxyListener.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

			if (!proxyListenerRegistered) {
				proxyListenerRegistered = true;

				plugin.setProxy((ProxyListener) instance);
			}

			plugin.registerEvents((ProxyListener) instance);

			eventsRegistered = true;
		}

		else if (SimpleCommand.class.isAssignableFrom(clazz))
			plugin.registerCommand((SimpleCommand) instance);
		else if (SimpleCommandGroup.class.isAssignableFrom(clazz)) {
			final SimpleCommandGroup group = (SimpleCommandGroup) instance;

			// Special case, do it at the end
			registeredCommandGroups.add(group);
		}

		else if (SimpleExpansion.class.isAssignableFrom(clazz)) {
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

			Variables.addExpansion((SimpleExpansion) instance);
		}

		else if (YamlConfig.class.isAssignableFrom(clazz)) {

			// Automatically called onLoadFinish when getting instance
			enforceModeFor(clazz, mode, FindInstance.SINGLETON);

			if (SimplePlugin.isReloading()) {
				((YamlConfig) instance).save();
				((YamlConfig) instance).reload();
			}
		}

		else if (instance instanceof Listener) {
			// Pass-through to register events later
		}

		else
			throw new FoException("@AutoRegister cannot be used on " + clazz);

		// Register events if needed
		if (!eventsRegistered && instance instanceof Listener)
			plugin.registerEvents((Listener) instance);
	}

	/*
	 * Compiles valid classes from our plugin that can be autoregistered
	 */
	private static List<Class<?>> findValidClasses() {
		final List<Class<?>> classes = new ArrayList<>();

		// Ignore anonymous inner classes
		final Pattern anonymousClassPattern = Pattern.compile("\\w+\\$[0-9]$");

		try (final JarFile file = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
				final JarEntry jar = entry.nextElement();
				final String name = jar.getName().replace("/", ".");

				// Ignore files such as settings.yml
				if (!name.endsWith(".class"))
					continue;

				final String className = name.substring(0, name.length() - 6);
				Class<?> clazz = null;

				// Look up the Java class, silently ignore if failing
				try {
					clazz = SimplePlugin.class.getClassLoader().loadClass(className);

				} catch (final VerifyError | NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError error) {
					continue;
				}

				// Ignore abstract or anonymous classes
				if (!Modifier.isAbstract(clazz.getModifiers()) && !anonymousClassPattern.matcher(className).find())
					classes.add(clazz);
			}

		} catch (final Throwable t) {
			Remain.sneaky(t);
		}

		return classes;
	}

	/*
	 * Tries to return instance of the given class, either by returning its singleon
	 * or creating a new instance from constructor if valid
	 */
	private static Tuple<FindInstance, Object> findInstance(Class<?> clazz) {
		final Constructor<?>[] constructors = clazz.getDeclaredConstructors();

		Object instance = null;
		FindInstance mode = null;

		// Strictly limit the class to one no args constructor
		if (constructors.length == 1) {
			final Constructor<?> constructor = constructors[0];

			if (constructor.getParameterCount() == 0) {
				final int modifiers = constructor.getModifiers();

				// Case 1: Public constructor
				if (Modifier.isPublic(modifiers)) {
					instance = ReflectionUtil.instantiate(constructor);
					mode = FindInstance.NEW_FROM_CONSTRUCTOR;
				}

				// Case 2: Singleton
				else if (Modifier.isPrivate(modifiers)) {
					Field instanceField = null;

					for (final Field field : clazz.getDeclaredFields()) {
						final int fieldMods = field.getModifiers();

						if (Modifier.isPrivate(fieldMods) && Modifier.isStatic(fieldMods) && (Modifier.isFinal(fieldMods) || Modifier.isVolatile(fieldMods)))
							instanceField = field;
					}

					if (instanceField != null) {
						instance = ReflectionUtil.getFieldContent(instanceField, (Object) null);
						mode = FindInstance.SINGLETON;
					}
				}
			}

		}

		Valid.checkNotNull(instance, "Your class " + clazz + " using @AutoRegister must EITHER have 1) one public no arguments constructor,"
				+ " OR 2) one private no arguments constructor plus a 'private static final " + clazz.getSimpleName() + " instance' instance field.");

		return new Tuple<>(mode, instance);
	}

	/*
	 * Checks if the way the given class can be made a new instance of, correspond with the required way
	 */
	private static void enforceModeFor(Class<?> clazz, FindInstance actual, FindInstance required) {
		Valid.checkBoolean(required == actual, clazz + " using @AutoRegister must have " + (required == FindInstance.NEW_FROM_CONSTRUCTOR ? "a single public no args constructor"
				: "one private no args constructor plus a 'private static final " + clazz.getSimpleName() + " instance' field to be a singleton'"));
	}

	/*
	 * How a new instance can be made to autoregister
	 */
	enum FindInstance {
		NEW_FROM_CONSTRUCTOR,
		SINGLETON
	}
}
