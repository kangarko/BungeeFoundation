package org.mineacademy.bfo.constants;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.TimeUtil;
import org.mineacademy.bfo.plugin.SimplePlugin;

import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

/**
 * Stores constants for this plugin
 */
@UtilityClass
@FieldDefaults(makeFinal = true)
public class FoConstants {

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class File {

		/**
		 * The name of our settings file
		 */
		public String SETTINGS = "settings.yml";

		/**
		 * The error.log file created automatically to log errors to
		 */
		public String ERRORS = "error.log";

		/**
		 * The debug.log file to log debug messages to
		 */
		public String DEBUG = "debug.log";

		/**
		 * The data.db file (uses YAML) for saving various data
		 */
		public String DATA = "data.db";

		/**
		 * Files related to the ChatControl plugin
		 */
		public class ChatControl {

			/**
			 * The command-spy.log file in logs/ folder
			 */
			public String COMMAND_SPY = "logs/command-spy.log";

			/**
			 * The chat log file in logs/ folder
			 */
			public String CHAT_LOG = "logs/chat.log";

			/**
			 * The admin log in log/s folder
			 */
			public String ADMIN_CHAT = "logs/admin-chat.log";

			/**
			 * The bungee chat log file in logs/ folder
			 */
			public String BUNGEE_CHAT = "logs/bungee-chat.log";

			/**
			 * The rules log file in logs/ folder
			 */
			public String RULES_LOG = "logs/rules.log";

			/**
			 * The console log file in logs/ folder
			 */
			public String CONSOLE_LOG = "logs/console.log";

			/**
			 * The file logging channels joins and leaves in logs/ folder
			 */
			public String CHANNEL_JOINS = "logs/channel-joins.log";
		}
	}

	@UtilityClass
	@FieldDefaults(makeFinal = true)
	public class Header {

		/**
		 * The header for data.db file
		 */
		public String[] DATA_FILE = new String[] {
				"",
				"This file stores various data you create via the plugin.",
				"",
				" ** THE FILE IS MACHINE GENERATED. PLEASE DO NOT EDIT **",
				""
		};

		/**
		 * The header that is put into the file that has been automatically
		 * updated and comments were lost
		 */
		public String[] UPDATED_FILE = new String[] {
				Common.configLine(),
				"",
				" Your file has been automatically updated at " + TimeUtil.getFormattedDate(),
				" to " + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion(),
				"",
				" Unfortunatelly, due to how Bukkit saves all .yml files, it was not possible",
				" preserve the documentation comments in your file. We apologize.",
				"",
				" If you'd like to view the default file, you can either:",
				" a) Open the " + SimplePlugin.getSource().getName() + " with a WinRar or similar",
				" b) or, visit: https://github.com/kangarko/" + SimplePlugin.getNamed() + "/wiki",
				"",
				Common.configLine(),
				""
		};
	}

}
