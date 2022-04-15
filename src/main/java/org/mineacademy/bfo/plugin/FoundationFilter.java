package org.mineacademy.bfo.plugin;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import lombok.AccessLevel;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Represents the console filtering module
 */
final class FoundationFilter {

	/**
	 * The messages we should filter, plugin authors can customize this in {@link SimplePlugin}
	 */
	@Setter(value = AccessLevel.PACKAGE)
	private static List<String> MESSAGES_TO_FILTER = new ArrayList<>();

	/**
	 * Start filtering the console
	 */
	public static void inject() {

		// Set filter for System out
		System.setOut(new FilterSystem());

		// Set filter for Bukkit
		final FilterLegacy filter = new FilterLegacy();

		for (final Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins())
			plugin.getLogger().setFilter(filter);

		ProxyServer.getInstance().getLogger().setFilter(filter);
	}

	/*
	 * Return true if the message is filtered
	 */
	static boolean isFiltered(String message) {
		if (message == null || message.isEmpty())
			return false;

		// Replace & color codes
		for (final ChatColor color : ChatColor.values()) {
			message = message.replace("&" + color.toString().charAt(0), "");
			message = message.replace(color.toString(), "");
		}

		// Log4j2 exploit
		if (message.contains("${jndi:ldap:"))
			return true;

		// Filter a warning since we've already patched this with NashornPlus extension
		if (message.equals("Warning: Nashorn engine is planned to be removed from a future JDK release"))
			return true;

		// One less spammy message for server owners
		if (message.endsWith("which is not a depend, softdepend or loadbefore of this plugin."))
			return true;

		message = message.toLowerCase();

		// Only filter this after plugin has been fully enabled
		if (SimplePlugin.hasInstance() && SimplePlugin.getInstance().getMainCommand() != null) {

			// Filter inbuilt Foundation or ChatControl commands
			if (message.contains("issued server command: /" + SimplePlugin.getInstance().getMainCommand().getLabel() + " internal") || message.contains("issued server command: /#flp"))
				return true;

			// Filter user-defined commands
			if (SimplePlugin.hasInstance())
				for (String filter : SimplePlugin.getInstance().getConsoleFilter()) {
					filter = filter.toLowerCase();

					if (message.startsWith(filter) || message.contains(filter))
						return true;
				}
		}

		return false;
	}
}

/**
 * The old Bukkit filter
 */
class FilterLegacy implements java.util.logging.Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		final String message = record.getMessage();

		return !FoundationFilter.isFiltered(message);
	}
}

/**
 * The System out filter
 */
class FilterSystem extends PrintStream {

	FilterSystem() {
		super(System.out);
	}

	@Override
	public void println(Object x) {
		if (x != null && !FoundationFilter.isFiltered(x.toString()))
			super.println(x);
	}

	@Override
	public void println(String x) {
		if (x != null && !FoundationFilter.isFiltered(x))
			super.println(x);
	}
}
