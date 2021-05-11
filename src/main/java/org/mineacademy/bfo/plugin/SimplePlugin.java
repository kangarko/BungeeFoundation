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
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeListener;
import org.mineacademy.bfo.bungee.SimpleBungee;
import org.mineacademy.bfo.command.SimpleCommand;
import org.mineacademy.bfo.conversation.ConversationManager;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.model.JavaScriptExecutor;
import org.mineacademy.bfo.settings.SimpleLocalization;
import org.mineacademy.bfo.settings.SimpleSettings;
import org.mineacademy.bfo.settings.YamlStaticConfig;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Represents a basic Java plugin using enhanced library functionality
 */
@Getter
public abstract class SimplePlugin extends Plugin implements Listener {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	@Getter
	private static volatile boolean reloading = false;

	/**
	 * The instance of this plugin
	 */
	private static volatile SimplePlugin instance;

	/**
	 * Returns the instance of {@link SimplePlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link SimplePlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @param <T>
	 * @return this instance
	 */
	public static SimplePlugin getInstance() {
		Objects.requireNonNull(instance, "Cannot get a new instance! Have you reloaded?");

		return instance;
	}

	/**
	 * Shortcut for getDescription().getVersion()
	 *
	 * @return plugin's version
	 */
	public static final String getVersion() {
		return getInstance().getDescription().getVersion();
	}

	/**
	 * Shortcut for getName()
	 *
	 * @return plugin's name
	 */
	public static final String getNamed() {
		return hasInstance() ? getInstance().getDescription().getName() : "No instance yet";
	}

	/**
	 * Shortcut for getFile()
	 *
	 * @return plugin's jar file
	 */
	public static final File getSource() {
		return getInstance().getFile();
	}

	/**
	 * Shortcut for getDataFolder()
	 *
	 * @return plugins' data folder in plugins/
	 */
	public static final File getData() {
		return getInstance().getDataFolder();
	}

	/**
	 * Return the bungee suite or null if not set
	 *
	 * @return
	 */
	public static final SimpleBungee getBungee() {
		return getInstance().getBungeeCord();
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

	/**
	 * Method to disable the plugin.
	 */
	public static void disablePlugin() {

		// Unregister commands and listener
		getInstance().getProxy().getPluginManager().unregisterCommands(getInstance());
		getInstance().getProxy().getPluginManager().unregisterListeners(getInstance());

		// Cancel scheduler
		try {
			getInstance().getProxy().getScheduler().cancel(getInstance());
			getInstance().getExecutorService().shutdownNow();

		} catch (final Throwable throwable) {
			throwable.printStackTrace();
		}

		getInstance().onDisable();
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * For your convenience, event listeners and timed tasks may be set here to stop/unregister
	 * them automatically on reload
	 */
	private final Reloadables reloadables = new Reloadables();

	/**
	 * Is this plugin enabled? Checked for after {@link #onPluginPreStart()}
	 */
	public boolean isEnabled = true;

	/**
	 * An internal flag to indicate whether we are calling the {@link #onReloadablesStart()}
	 * block. We register things using {@link #reloadables} during this block
	 */
	private boolean startingReloadables = false;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onLoad() {

		// Set the instance
		instance = this;

		// Call parent
		onPluginLoad();
	}

	@Override
	public final void onEnable() {

		// Check if Foundation is correctly moved
		checkShading();

		// Boot up JavaScript
		JavaScriptExecutor.run("");

		if (!isEnabled)
			return;

		// --------------------------------------------
		// Call the main pre start method
		// --------------------------------------------
		onPluginPreStart();
		// --------------------------------------------

		// Return if plugin pre start indicated a fatal problem
		if (!isEnabled)
			return;

		if (getStartupLogo() != null)
			Common.log(getStartupLogo());

		try {
			// Load our main static settings classes
			if (getSettings() != null) {
				YamlStaticConfig.load(getSettings());

				Valid.checkBoolean(SimpleSettings.isSettingsCalled() != null && SimpleLocalization.isLocalizationCalled() != null, "Developer forgot to call Settings or Localization");
			}

			// Load bungee suite
			Common.registerEvents(new BungeeListener.BungeeListenerImpl());
			final SimpleBungee bungee = getBungeeCord();

			if (bungee != null) {
				getProxy().registerChannel(bungee.getChannel());

				Debugger.debug("bungee", "Registered BungeeCord listener for " + bungee.getChannel());
			}

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------
			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

			onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!isEnabled)
				return;

			// Register our listeners
			registerEvents(this); // For convenience
			registerEvents(new ConversationManager());

		} catch (final Throwable t) {
			displayError0(t);
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
	private final class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(getDescription().getName())) {
				ProxyServer.getInstance().getLogger().severe(Common.consoleLine());
				ProxyServer.getInstance().getLogger().severe("We have a class path problem in the BungeeFoundation library");
				ProxyServer.getInstance().getLogger().severe("preventing " + getDescription().getName() + " from loading correctly!");
				ProxyServer.getInstance().getLogger().severe("");
				ProxyServer.getInstance().getLogger().severe("This is likely caused by two plugins having the");
				ProxyServer.getInstance().getLogger().severe("same Foundation library paths - make sure you");
				ProxyServer.getInstance().getLogger().severe("relocale the package! If you are testing using");
				ProxyServer.getInstance().getLogger().severe("Ant, only test one plugin at the time.");
				ProxyServer.getInstance().getLogger().severe("");
				ProxyServer.getInstance().getLogger().severe("Possible cause: " + SimplePlugin.getNamed());
				ProxyServer.getInstance().getLogger().severe("Foundation package: " + SimplePlugin.class.getPackage().getName());
				ProxyServer.getInstance().getLogger().severe(Common.consoleLine());

				isEnabled = false;
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
				"&c  _   _                       _ ",
				"&c  | | | | ___   ___  _ __  ___| |",
				"&c  | |_| |/ _ \\ / _ \\| '_ \\/ __| |",
				"&c  |  _  | (_) | (_) | |_) \\__ \\_|",
				"&4  |_| |_|\\___/ \\___/| .__/|___(_)",
				"&4                    |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + getDescription().getName() + " v" + getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + getProxy().getVersion() + " & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		String error = "Unable to get the error message, search above.";
		if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
			error = throwable.getMessage();

		Common.log(" &cError: " + error);
		Common.log("&4!-----------------------------------------------------!");

		isEnabled = false;
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		// If the early startup was interrupted, do not call shutdown methods
		if (!isEnabled)
			return;

		try {
			onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		unregisterReloadables();
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
	 * Called before we start loading the plugin, but after {@link #onPluginLoad()}
	 */
	protected void onPluginPreStart() {
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
		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + getDescription().getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {
			unregisterReloadables();

			onPluginPreReload();

			if (getSettings() != null)
				YamlStaticConfig.load(getSettings());

			onPluginReload();

			startingReloadables = true;
			onReloadablesStart();
			startingReloadables = false;

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + getDescription().getName() + " " + getVersion());

		} finally {
			Common.log(Common.consoleLineSmooth());
			reloading = false;
		}
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		getProxy().getScheduler().cancel(this);
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events if the condition is met
	 *
	 * @param listener
	 * @param condition
	 */
	protected final void registerEventsIf(final Listener listener, final boolean condition) {
		if (condition)
			if (startingReloadables)
				reloadables.registerEvents(listener);
			else
				registerEvents(listener);
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	protected final void registerEvents(final Listener listener) {
		if (startingReloadables)
			reloadables.registerEvents(listener);
		else
			getProxy().getPluginManager().registerListener(this, listener);
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(final Command command) {
		getProxy().getPluginManager().registerCommand(this, command);
	}

	/**
	 * Convenience shortcut for calling the register method in {@link SimpleCommand}
	 *
	 * @param command
	 */
	protected final void registerCommand(final SimpleCommand command) {
		command.register();
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * Get the BungeeCord message channel, return null for none
	 *
	 * @return
	 */
	public abstract SimpleBungee getBungeeCord();

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * Return your main setting classes extending {@link YamlStaticConfig}.
	 *
	 * TIP: Extend {@link SimpleSettings} and {@link SimpleLocalization}
	 *
	 * @return
	 */
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return null;
	}

	/**
	 * Get the year of foundation displayed in {@link #getMainCommand()}
	 *
	 * @return -1 by default, or the founded year
	 */
	public int getFoundedYear() {
		return -1;
	}

	// ----------------------------------------------------------------------------------------
	// Prevention
	// ----------------------------------------------------------------------------------------

	/**
	 * @deprecated DO NOT USE
	 * Use Common#log instead
	 */
	@Deprecated
	@Override
	public Logger getLogger() {
		return super.getLogger();
	}

}
