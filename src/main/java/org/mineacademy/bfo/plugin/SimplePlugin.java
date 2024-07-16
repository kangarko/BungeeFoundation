/**
 * (c) 2013 - 2019 - All rights reserved.
 * <p>
 * Do not share, copy, reproduce or sell any part of this library
 * unless you have written permission from MineAcademy.org.
 * All infringements will be prosecuted.
 * <p>
 * If you are the personal owner of the MineAcademy.org End User License
 * then you may use it for your own use in plugins but not for any other purpose.
 */
package org.mineacademy.bfo.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.annotation.AutoRegister;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.command.SimpleCommand;
import org.mineacademy.bfo.command.SimpleCommandGroup;
import org.mineacademy.bfo.command.SimpleSubCommand;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.library.BungeeLibraryManager;
import org.mineacademy.bfo.library.Library;
import org.mineacademy.bfo.library.LibraryManager;
import org.mineacademy.bfo.metrics.Metrics;
import org.mineacademy.bfo.model.JavaScriptExecutor;
import org.mineacademy.bfo.proxy.ProxyListener;
import org.mineacademy.bfo.proxy.message.OutgoingMessage;
import org.mineacademy.bfo.remain.Remain;
import org.mineacademy.bfo.settings.FileConfig;
import org.mineacademy.bfo.settings.Lang;
import org.mineacademy.bfo.settings.SimpleLocalization;
import org.mineacademy.bfo.settings.SimpleSettings;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Represents a basic Java plugin using enhanced library functionality,
 * implementing a listener for easy use
 */
public abstract class SimplePlugin extends Plugin implements Listener {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * The instance of this plugin
	 */
	private static SimplePlugin instance;

	/**
	 * Shortcut for getDescription().getVersion()
	 */
	@Getter
	private static String version;

	/**
	 * Shortcut for getName()
	 */
	@Getter
	private static String named;

	/**
	 * Shortcut for getFile()
	 */
	@Getter
	private static File source;

	/**
	 * Shortcut for getDataFolder()
	 */
	@Getter
	private static File data;

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	@Getter
	private static boolean reloading = false;

	/**
	 * Returns the instance of {@link SimplePlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link SimplePlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static SimplePlugin getInstance() {
		return instance;
	}

	/**
	 * Get if the instance that is used across the library has been set. Normally it
	 * is always set, except for testing.
	 *
	 * @return if the instance has been set.
	 */
	public static final boolean hasInstance() {
		return instance != null;
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * The default proxy listener, see {@link #setProxy(ProxyListener)}
	 */
	private ProxyListener proxyListener;

	/**
	 * A temporary main command to be set in {@link #setMainCommand(SimpleCommandGroup)}
	 * automatically by us.
	 */
	private SimpleCommandGroup mainCommand;

	/**
	 * Shortcut to discover if the plugin was disabled (only used internally)
	 */
	private boolean enabled = true;

	/**
	 * The library manager
	 */
	private LibraryManager libraryManager;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {

		instance = this;

		// Cache results for best performance
		version = instance.getDescription().getVersion();
		named = instance.getDataFolder().getName();
		source = instance.getFile();
		data = instance.getDataFolder();

		if (getJavaVersion() >= 11)
			this.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.4");

		this.loadLibrary("net.kyori", "adventure-api", "4.17.0");
		this.loadLibrary("net.kyori", "adventure-text-serializer-plain", "4.17.0");
		this.loadLibrary("net.kyori", "adventure-platform-bungeecord", "4.3.3");

		// Call parent
		this.onPluginLoad();
	}

	@Override
	public final void onEnable() {

		// Check if Foundation is correctly moved
		this.checkShading();

		// Before all, check if necessary libraries and the minimum required MC version
		if (!this.enabled)
			return;

		// Print startup logo early before onPluginPreStart
		// Disable logging prefix if logo is set
		if (this.getStartupLogo() != null) {
			final String oldLogPrefix = Common.getLogPrefix();

			Common.setLogPrefix("");
			Common.log(this.getStartupLogo());
			Common.setLogPrefix(oldLogPrefix);
		}

		// Return if plugin pre start indicated a fatal problem
		if (!this.enabled)
			return;

		try {

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------

			Common.registerEvents(new ProxyListener.ProxyListenerImpl());

			if (!this.getProxy().getChannels().contains(ProxyListener.DEFAULT_CHANNEL))
				this.getProxy().registerChannel(ProxyListener.DEFAULT_CHANNEL);

			// Hide plugin name before console messages
			final String oldLogPrefix = Common.getLogPrefix();
			Common.setLogPrefix("");

			try {
				AutoRegisterScanner.scanAndRegister();

			} catch (final Throwable t) {
				Remain.sneaky(t);

				return;
			}

			this.onReloadablesStart();
			this.onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!this.enabled)
				return;

			// Register our listeners
			this.registerEvents(this);

			// Prepare Nashorn engine
			JavaScriptExecutor.run("");

			// Finish off by starting metrics (currently bStats)
			if (this.getMetricsPluginId() != -1)
				new Metrics(this, this.getMetricsPluginId());

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runAsync(() -> Common.setLogPrefix(oldLogPrefix));

		} catch (final Throwable t) {
			this.displayError0(t);
		}
	}

	/**
	 * A dirty way of checking if Foundation has been shaded correctly
	 */
	private void checkShading() {
		try {
			throw new ShadingException();
		} catch (final Throwable t) {
		}
	}

	/**
	 * The exception enabling us to check if for some reason {@link SimplePlugin}'s instance
	 * does not match this class' instance, which is most likely caused by wrong repackaging
	 * or no repackaging at all (two plugins using Foundation must both have different packages
	 * for their own Foundation version).
	 * <p>
	 * Or, this is caused by a PlugMan, and we have no mercy for that.
	 */
	private class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(SimplePlugin.this.getDescription().getName())) {
				SimplePlugin.this.getLogger().severe(Common.consoleLine());
				SimplePlugin.this.getLogger().severe("We have a class path problem in the Foundation library");
				SimplePlugin.this.getLogger().severe("preventing " + SimplePlugin.this.getDescription().getName() + " from loading correctly!");
				SimplePlugin.this.getLogger().severe("");
				SimplePlugin.this.getLogger().severe("This is likely caused by two plugins having the");
				SimplePlugin.this.getLogger().severe("same Foundation library paths - make sure you");
				SimplePlugin.this.getLogger().severe("relocale the package! If you are testing using");
				SimplePlugin.this.getLogger().severe("Ant, only test one plugin at the time.");
				SimplePlugin.this.getLogger().severe("");
				SimplePlugin.this.getLogger().severe("Possible cause: " + SimplePlugin.getNamed());
				SimplePlugin.this.getLogger().severe("Foundation package: " + SimplePlugin.class.getPackage().getName());
				SimplePlugin.this.getLogger().severe(Common.consoleLine());

				throw new FoException("Shading exception, see above for details.");
			}
		}
	}

	/**
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	protected final void displayError0(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + this.getDescription().getName() + " v" + this.getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + ProxyServer.getInstance().getVersion() + " (" + ProxyServer.getInstance().getGameVersion() + ") & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		{
			while (throwable.getCause() != null)
				throwable = throwable.getCause();

			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		}
		Common.log("&4!-----------------------------------------------------!");

		this.enabled = false;
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		try {
			this.onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		this.unregisterReloadables();

		Objects.requireNonNull(instance, "Instance of " + this.getDataFolder().getName() + " already nulled!");
		instance = null;
	}

	// ----------------------------------------------------------------------------------------
	// Delegate methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Called before the plugin is started, see {@link JavaPlugin#onLoad()}
	 */
	protected void onPluginLoad() {
	}

	/**
	 * The main loading method, called when we are ready to load
	 */
	protected abstract void onPluginStart();

	/**
	 * The main method called when we are about to shut down
	 */
	protected void onPluginStop() {
	}

	/**
	 * Invoked before settings were reloaded.
	 */
	protected void onPluginPreReload() {
	}

	/**
	 * Invoked after settings were reloaded.
	 */
	protected void onPluginReload() {
	}

	/**
	 * Register your commands, events, tasks and files here.
	 * <p>
	 * This is invoked when you start the plugin, call /reload, or the {@link #reload()}
	 * method.
	 */
	protected void onReloadablesStart() {
	}

	// ----------------------------------------------------------------------------------------
	// Reload
	// ----------------------------------------------------------------------------------------

	/**
	 * Attempts to reload the plugin
	 */
	public final void reload() {
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + this.getDataFolder().getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {

			for (final Listener listener : this.listeners)
				ProxyServer.getInstance().getPluginManager().unregisterListener(listener);

			this.listeners.clear();

			this.unregisterReloadables();

			FileConfig.clearLoadedSections();

			this.onPluginPreReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.enabled)
				return;

			if (!this.getProxy().getChannels().contains(ProxyListener.DEFAULT_CHANNEL))
				this.getProxy().registerChannel(ProxyListener.DEFAULT_CHANNEL);

			// Register classes
			AutoRegisterScanner.scanAndRegister();

			Lang.reloadLang();
			Lang.loadPrefixes();

			this.onReloadablesStart();

			Common.log(Common.consoleLineSmooth());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getDataFolder().getName() + " " + getVersion());

		} finally {
			Common.setLogPrefix(oldLogPrefix);

			reloading = false;
		}
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		this.getProxy().getScheduler().cancel(this);
		this.mainCommand = null;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events in all classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends Listener> void registerAllEvents(final Class<T> extendingClass) {

		Valid.checkBoolean(!extendingClass.equals(Listener.class), "registerAllEvents does not support Listener.class due to conflicts, create your own middle class instead");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			for (final Constructor<?> con : pluginClass.getConstructors())
				if (con.getParameterCount() == 0) {
					final T instance = (T) ReflectionUtil.instantiate(con);

					this.registerEvents(instance);

					continue classLookup;
				}
		}
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	protected final void registerEvents(final Listener listener) {
		ProxyServer.getInstance().getPluginManager().registerListener(this, listener);
	}

	/**
	 * Convenience method for quickly registering all command classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends Command> void registerAllCommands(final Class<T> extendingClass) {
		Valid.checkBoolean(!extendingClass.equals(Command.class), "registerAllCommands does not support Command.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleCommand.class), "registerAllCommands does not support SimpleCommand.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleSubCommand.class), "registerAllCommands does not support SubCommand.class");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass))
				continue;

			try {
				for (final Constructor<?> con : pluginClass.getConstructors())
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						if (instance instanceof SimpleCommand)
							this.registerCommand(instance);

						else
							this.registerCommand(instance);

						continue classLookup;
					}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}
		}
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(final Command command) {
		if (command instanceof SimpleCommand)
			((SimpleCommand) command).register();

		else
			this.getProxy().getPluginManager().registerCommand(this, command);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final SimpleCommandGroup group) {
		group.register();
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * Return false if plugin was disabled during startup/reload
	 *
	 * @return
	 */
	public final boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * If you use \@AutoRegister on a command group that has a no args constructor,
	 * we use the label and aliases from {@link SimpleSettings#MAIN_COMMAND_ALIASES}
	 * and associate it here for the record.
	 *
	 * @return
	 */
	@Nullable
	public final SimpleCommandGroup getMainCommand() {
		return this.mainCommand;
	}

	/**
	 * @deprecated do not use, internal use only
	 * @param group
	 */
	@Deprecated
	public final void setMainCommand(SimpleCommandGroup group) {
		Valid.checkBoolean(this.mainCommand == null, "Main command has already been set to " + this.mainCommand);

		this.mainCommand = group;
	}

	/**
	 * Get the year of foundation displayed in our {@link SimpleCommandGroup} on help
	 *
	 * @return -1 by default, or the founded year
	 */
	public int getFoundedYear() {
		return -1;
	}

	/**
	 * If you want to use bStats.org metrics system,
	 * simply return the plugin ID (https://bstats.org/what-is-my-plugin-id)
	 * here and we will automatically start tracking it.
	 * <p>
	 * Defaults to -1 which means disabled
	 *
	 * @return
	 */
	public int getMetricsPluginId() {
		return -1;
	}

	/**
	 * Foundation automatically can filter console commands for you, including
	 * messages from other plugins or the server itself, preventing unnecessary console spam.
	 *
	 * You can return a list of messages that will be matched using "startsWith OR contains" method
	 * and will be filtered.
	 *
	 * @return
	 */
	public Set<String> getConsoleFilter() {
		return new HashSet<>();
	}

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 *
	 * @return time limit in milliseconds for processing regular expression
	 */
	public int getRegexTimeout() {
		throw new FoException("Must override getRegexTimeout()");
	}

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @return
	 */
	public boolean regexStripColors() {
		return true;
	}

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @return
	 */
	public boolean regexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @return
	 */
	public boolean regexUnicode() {
		return true;
	}

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true
	 *
	 * @return
	 */
	public boolean regexStripAccents() {
		return true;
	}

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in ChatUtil?
	 *
	 * @return defaults to true
	 */
	public boolean similarityStripAccents() {
		return true;
	}

	/**
	 * Returns the "default" proxy listener you use. This is used in {@link OutgoingMessage} when no channel is provided.
	 *
	 * @deprecated only returns the first listener, if you have multiple, do not use, order not guaranteed
	 * @return
	 */
	@Deprecated
	public final ProxyListener getProxyListener() {
		return this.proxyListener;
	}

	/**
	 * Sets a proxy listener to use in {@link OutgoingMessage} when no listener is provided
	 *
	 * @deprecated INTERNAL USE ONLY
	 * @param listener
	 */
	@Deprecated
	public final void setProxy(ProxyListener listener) {
		this.proxyListener = listener;
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 * <p>
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public void loadLibrary(String groupId, String artifactId, String version) {
		this.getLibraryManager().loadLibrary(Library.builder().groupId(groupId).artifactId(artifactId).version(version).resolveTransitiveDependencies(true).build());
	}

	/**
	 * Get the Libby library manager
	 *
	 * @return
	 */
	public final LibraryManager getLibraryManager() {
		if (this.libraryManager == null)
			this.libraryManager = new BungeeLibraryManager(this);

		return this.libraryManager;
	}

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}
}
