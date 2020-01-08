package org.mineacademy.bfo.settings;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.constants.FoConstants;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.debug.LagCatcher;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.plugin.SimplePlugin;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 * <p>
 * Typically we use this class for settings.yml main plugin config.
 */
// Use for settings.yml
@SuppressWarnings("unused")
public abstract class SimpleSettings extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 * <p>
	 * You can place this class to {@link SimplePlugin} to make
	 * it load automatically
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return FoConstants.File.SETTINGS;
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 */
	protected static Integer VERSION;

	/**
	 * Set and update the config version automatically, however the {@link #VERSION} will
	 * contain the older version used in the file on the disk so you can use
	 * it for comparing in the init() methods
	 * <p>
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected void beforeLoad() {
		// Load version first so we can use it later
		pathPrefix(null);

		if ((VERSION = getInteger("Version")) != getConfigVersion())
			set("Version", getConfigVersion());
	}

	/**
	 * Return the very latest config version
	 * <p>
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected abstract int getConfigVersion();

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 * <p>
	 * Typically this is left empty: Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * The plugin prefix in front of chat/console messages, added automatically unless
	 * disabled in {@link Common} and .
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to -1 to disable.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 * <p>
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)? See {@link SimplePlugin}
	 * <p>
	 * Typical values for ChatControl:
	 * <p>
	 * Command_Aliases: [chatcontrol, chc, cc]
	 * <p>
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin} //
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class to load and manage your
	 * locale file. Typically the file path is: localization/messages_PREFIX.yml with this prefix below.
	 * <p>
	 * Typically: Locale: en
	 * <p>
	 * // ONLY MANDATORY IF YOU USE SIMPLELOCALIZATION //
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * The server name used in {server_name} variable or BungeeCord, if your plugin supports either of those.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Server_Name: "My ChatControl Server"
	 * <p>
	 * // NOT MANDATORY //
	 */
	public static String SERVER_NAME = "Server";

	/**
	 * The server name identifier
	 * <p>
	 * Mandatory if using BungeeCord
	 */
	public static String BUNGEE_SERVER_NAME = "Server";

	/**
	 * Antipiracy stuff for our protected software, leave empty to Serialization: ""
	 * <p>
	 * // NOT MANDATORY //
	 */
	public static String SECRET_KEY = "";

	/**
	 * Should we enable inbuilt advertisements?
	 * ** We found out that users really hate this feature, you may want not to use this completelly **
	 * ** If you want to broadcast important messages regardless of this feature just implement your **
	 * ** own Runnable that checks for a YAML file on your external server on plugin load. **
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Notify_Promotions: true
	 * <p>
	 * // NOT MANDATORY //
	 */
	public static Boolean NOTIFY_PROMOTIONS = true;

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		pathPrefix(null);

		if (isSet("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		if (isSet("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				Common.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSet("Debug"))
			DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));

		if (isSet("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		if (isSet("Server_Name"))
			SERVER_NAME = Common.colorize(getString("Server_Name"));

		if (isSet("Notify_Promotions"))
			NOTIFY_PROMOTIONS = getBoolean("Notify_Promotions");

		if (isSet("Serialization"))
			SECRET_KEY = getString("Serialization");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load Bungee server name

			final boolean keySet = isSet("Bungee_Server_Name");

			if (SimplePlugin.getInstance().getBungeeCord() != null && !keySet)
				throw new FoException("Since you override getBungeeCord in your main plugin class you must set the 'Bungee_Server_Name' key in " + getFileName());

			BUNGEE_SERVER_NAME = keySet ? getString("Bungee_Server_Name") : BUNGEE_SERVER_NAME;

			if (SimplePlugin.getInstance().getBungeeCord() != null && BUNGEE_SERVER_NAME.equals("undefined")) {
				Common.logFramed(true,
						"Please change your Bungee_Server_Name in",
						"settings.yml to the exact name of this server",
						"as you have in config.yml of your BungeeCord.");
			}
		}

		{ // Load localization
			final boolean hasLocalization = hasLocalization();
			final boolean keySet = isSet("Locale");

			if (hasLocalization && !keySet)
				throw new FoException("Since you have your Localization class you must set the 'Locale' key in " + getFileName());

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		settingsClassCalled = true;
	}

	/**
	 * Inspect if some settings classes extend localization and make sure only one does, if any
	 *
	 * @return
	 */
	private static boolean hasLocalization() {
		final SimplePlugin plugin = SimplePlugin.getInstance();
		int localeClasses = 0;

		if (plugin.getSettings() != null)
			for (final Class<?> clazz : plugin.getSettings())
				if (SimpleLocalization.class.isAssignableFrom(clazz))
					localeClasses++;

		Valid.checkBoolean(localeClasses < 2, "You cannot have more than 1 class extend SimpleLocalization!");
		return localeClasses == 1;
	}


	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static Boolean isSettingsCalled() {
		return settingsClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static void resetSettingsCall() {
		settingsClassCalled = false;
	}
}
