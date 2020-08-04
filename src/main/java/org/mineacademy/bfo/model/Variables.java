package org.mineacademy.bfo.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.TimeUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.collection.expiringmap.ExpiringMap;
import org.mineacademy.bfo.plugin.SimplePlugin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find simple {} placeholders
	 */
	protected static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	// ------------------------------------------------------------------------------------------------------------
	// Changing variables for loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final Map<String, Function<CommandSender, String>> customVariables = new HashMap<>();

	/**
	 * Player, Their Cached Variables
	 */
	private static final StrictMap<String, Map<String, String>> cache = new StrictMap<>();

	/**
	 * Player, Original Message, Translated Message
	 */
	private static final Map<String, Map<String, String>> fastCache = makeNewFastCache();

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * As a developer you can add or remove custom variables. Return an unmodifiable
	 * set of all added custom variables
	 *
	 * @return
	 */
	public static Set<String> getVariables() {
		return Collections.unmodifiableSet(customVariables.keySet());
	}

	/**
	 * Register a new variable. The variable will be found inside {} block so if you give the variable
	 * name player_health it will be {player_health}. The function takes in a command sender (can be player)
	 * and outputs the variable value.
	 *
	 * Please keep in mind we replace your variables AFTER PlaceholderAPI and Javascript variables
	 *
	 * @param variable
	 * @param replacer
	 */
	public static void addVariable(String variable, Function<CommandSender, String> replacer) {
		customVariables.put(variable, replacer);
	}

	/**
	 * Removes an existing variable, only put the name here without brackets, e.g. player_name not {player_name}
	 * This fails when the variables does not exist
	 *
	 * @param variable
	 */
	public static void removeVariable(String variable) {
		customVariables.remove(variable);
	}

	/**
	 * Checks if the given variable exist. Warning: only put the name here without brackets,
	 * e.g. player_name not {player_name}
	 *
	 * @param variable
	 * @return
	 */
	public static boolean hasVariable(String variable) {
		return customVariables.containsKey(variable);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, CommandSender sender) {
		Valid.checkNotNull(sender, "Sender cannot be null!");

		if (message == null || message.isEmpty())
			return "";

		final String original = message;

		{
			// Already cached ? Return.
			final Map<String, String> cached = fastCache.get(sender.getName());

			if (cached != null && cached.containsKey(original))
				return cached.get(original);
		}

		// Default
		message = replaceVariables0(sender, message);

		// Support the & color system
		message = Common.colorize(message);

		{
			final Map<String, String> map = fastCache.get(sender.getName());

			if (map != null)
				map.put(original, message);
			else
				fastCache.put(sender.getName(), Common.newHashMap(original, message));
		}

		return message;
	}

	/**
	 * Replaces our hardcoded variables in the message, using a cache for better performance
	 *
	 * @param sender
	 * @param message
	 * @return
	 */
	private static String replaceVariables0(CommandSender sender, String message) {
		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);
		final ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;

		while (matcher.find()) {
			final String variable = matcher.group(1);

			final boolean isSenderCached = cache.contains(sender.getName());
			boolean makeCache = true;

			String value = null;

			// Player is cached
			if (isSenderCached) {
				final Map<String, String> senderCache = cache.get(sender.getName());
				final String storedVariable = senderCache.get(variable);

				// This specific variable is cached
				if (storedVariable != null) {
					value = storedVariable;
					makeCache = false;
				}
			}

			if (makeCache) {
				value = replaceVariable0(variable, player, sender);

				if (value != null) {
					final Map<String, String> speciCache = cache.getOrPut(sender.getName(), makeNewCache());

					speciCache.put(variable, value);
				}
			}

			if (value != null)
				message = message.replace("{" + variable + "}", Common.colorize(value));
		}

		return message;
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 *
	 * Also, if the variable ends with +, we insert a space after it if it is not empty
	 *
	 * @param variable
	 * @param player
	 * @param console
	 * @return
	 */
	private static String replaceVariable0(String variable, ProxiedPlayer player, CommandSender console) {
		final boolean insertSpace = variable.endsWith("+");

		if (insertSpace)
			variable = variable.substring(0, variable.length() - 1); // Remove the + symbol

		final String found = lookupVariable0(player, console, variable);

		return found == null ? null : found + (insertSpace && !found.isEmpty() ? " " : "");
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 *
	 * @param player
	 * @param console
	 * @param variable
	 * @return
	 */
	private static String lookupVariable0(ProxiedPlayer player, CommandSender console, String variable) {
		{ // Replace custom variables
			final Function<CommandSender, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			case "plugin_name":
				return SimplePlugin.getNamed();
			case "plugin_version":
				return SimplePlugin.getVersion();

			case "timestamp":
				return TimeUtil.getFormattedDate();

			case "player":
				return player == null ? console.getName() : player.getName();
			case "player_display_name":
				return player == null ? console.getName() : player.getDisplayName();
			case "player_server_name":
				return player == null ? "" : player.getServer().getInfo().getName();
			case "player_address":
				return player == null ? "" : formatIp0(player);
		}

		return null;
	}

	/**
	 * Formats the {pl_address} variable for the player
	 *
	 * @param player
	 * @return
	 */
	private static String formatIp0(ProxiedPlayer player) {
		try {
			return player.getAddress().toString().split("\\:")[0];
		} catch (final Throwable t) {
			return player.getAddress() != null ? player.getAddress().toString() : "";
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Cache making
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new expiring map with 10 millisecond expiration
	 *
	 * @return
	 */
	private static Map<String, Map<String, String>> makeNewFastCache() {
		return ExpiringMap.builder()
				.maxSize(300)
				.expiration(10, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Create a new expiring map with 1 second expiration, used to cache player-related
	 * variables that are called 10x after each other to save performance
	 *
	 * @return
	 */
	private static Map<String, String> makeNewCache() {
		return ExpiringMap.builder()
				.maxSize(300)
				.expiration(1, TimeUnit.SECONDS)
				.build();
	}
}