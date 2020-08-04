package org.mineacademy.bfo.settings;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.constants.FoConstants;
import org.mineacademy.bfo.plugin.SimplePlugin;

import de.leonhard.storage.Config;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.DataType;

/**
 * A simple implementation of a typical main plugin settings where each key can
 * be accessed in a static way from anywhere.
 * <p>
 * Typically we use this class for settings.yml main plugin config.
 */
@SuppressWarnings("unused")
public abstract class SimpleSettings extends YamlStaticConfig {

	public static List<String> DEBUG_SECTIONS = new ArrayList<>();
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";
	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------
	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 * <p>
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;
	public static List<String> MAIN_COMMAND_ALIASES = new ArrayList<>();
	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class
	 * to load and manage your locale file. Typically the file path is:
	 * localization/messages_PREFIX.yml with this prefix below.
	 * <p>
	 * Typically: Locale: en
	 * <p>
	 * // ONLY MANDATORY IF YOU USE SIMPLELOCALIZATION //
	 */
	public static String LOCALE_PREFIX = "en";

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------
	/**
	 * The server name used in {server_name} variable or BungeeCord, if your plugin
	 * supports either of those.
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
	public static Boolean NOTIFY_UPDATES = false;
	/**
	 * Should we enable inbuilt advertisements? ** We found out that users really
	 * hate this feature, you may want not to use this completelly ** ** If you want
	 * to broadcast important messages regardless of this feature just implement
	 * your ** ** own Runnable that checks for a YAML file on your external server
	 * on plugin load. **
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Notify_Promotions: true
	 * <p>
	 * // NOT MANDATORY //
	 */
	public static Boolean NOTIFY_PROMOTIONS = true;
	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 */
	protected static Integer VERSION;
	private static boolean settingsClassCalled;

	/**
	 * Load the values -- this method is called automatically by reflection in the
	 * {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		pathPrefix(null);

		if (isSet("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		// Colorizing default-prefix
		PLUGIN_PREFIX = PLUGIN_PREFIX.replace("&", "§");

		if (isSet("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				System.out.println("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSet("Debug"))
			DEBUG_SECTIONS = new ArrayList<>(getStringList("Debug"));

		if (isSet("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		if (isSet("Server_Name"))
			SERVER_NAME = colorize(getString("Server_Name"));

		if (isSet("Notify_Promotions"))
			NOTIFY_PROMOTIONS = getBoolean("Notify_Promotions");

		if (isSet("Serialization"))
			SECRET_KEY = getString("Serialization");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load localization
			// We always have localizations
			final boolean keySet = isSet("Locale");

			if (SimpleLocalization.isLocalizationCalled())
				Valid.checkBoolean(isSet("Locale"), "You need to set the key: 'Locale' in your config as your plugin uses localization");

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		{ // Load main command alias

			final boolean keySet = isSet("Command_Aliases");

			MAIN_COMMAND_ALIASES = keySet ? getStringList("Command_Aliases") : MAIN_COMMAND_ALIASES;
		}

		settingsClassCalled = true;
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
	 * Reset the flag indicating that the class has been loaded, used in reloading.
	 */
	public static void resetSettingsCall() {
		settingsClassCalled = false;
	}

	@Override
	protected final Config getConfigInstance() {
		return LightningBuilder.fromPath("settings.yml", SimplePlugin.getData().getAbsolutePath()).addInputStreamFromResource("settings.yml").setConfigSettings(ConfigSettings.PRESERVE_COMMENTS).setDataType(DataType.SORTED).createConfig().addDefaultsFromInputStream();
	}

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return FoConstants.File.SETTINGS;
	}

	/**
	 * Set and update the config version automatically, however the {@link #VERSION}
	 * will contain the older version used in the file on the disk so you can use it
	 * for comparing in the init() methods
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
	 * Any changes here must also be made to the "Version" key in your settings
	 * file.
	 *
	 * @return
	 */
	protected abstract int getConfigVersion();
}
