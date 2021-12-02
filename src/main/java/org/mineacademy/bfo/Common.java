package org.mineacademy.bfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.exception.RegexTimeoutException;
import org.mineacademy.bfo.model.Variables;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.settings.SimpleSettings;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.config.Configuration;

/**
 * A generic utility class
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

	/**
	 * The console command sender
	 */
	private final static CommandSender consoleSender = ProxyServer.getInstance().getConsole();

	/**
	 * Send a colorized message to the player.
	 * <p>
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(CommandSender sender, Collection<String> messages) {
		tell(sender, messages.toArray(new String[messages.size()]));
	}

	/**
	 * Send a colorized message to the player.
	 * <p>
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param sender
	 * @param message
	 */
	public static void tell(CommandSender sender, String... messages) {
		Valid.checkNotNull(sender, "Sender cannot be null!");

		for (String message : messages)
			if (message != null) {
				message = Variables.replace(message, sender);

				sender.sendMessage(toComponent(message));
			}
	}

	/**
	 * A dummy helper method adding "&cWarning: &f" to the given message
	 * and logging it.
	 *
	 * @param message
	 */
	public static void warning(String message) {
		log("&cWarning: &f" + message);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 * <p>
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param messages
	 */
	public static void log(String... messages) {
		for (String message : messages) {
			if (message.isEmpty() || message.equals("none"))
				continue;

			message = message.replace("\n", "\n&r");
			message = Variables.replace(message, consoleSender);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					consoleSender.sendMessage(toLegacyText(stripped));

			} else
				consoleSender.sendMessage(message.trim());
		}
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 * <p>
	 * Used when an error occurs, can also disable the plugin
	 *
	 * @param messages
	 */
	public static void logFramed(String... messages) {
		logFramed(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 *
	 * @param messages
	 */
	public static void logFramed(boolean disablePlugin, String... messages) {
		if (messages != null && !Valid.isNullOrEmpty(messages)) {
			log("&7" + consoleLine());
			for (final String msg : messages)
				log(" &c" + msg);
		}

		if (disablePlugin) {
			SimplePlugin.disablePlugin();

			log("&7",
					"&7The plugin is now disabled.");
		}

		if (messages != null && !Valid.isNullOrEmpty(messages))
			log("&7" + consoleLine());
	}

	/**
	 * Convert a message into a TextComponent replacing & letters with legacy color
	 * codes
	 *
	 * @param message
	 * @return
	 */
	public static BaseComponent[] toComponentColorized(String message) {
		return toComponent(colorize(message));
	}

	/**
	 * Convert a message into a TextComponent
	 *
	 * @param message
	 * @return
	 */
	public static BaseComponent[] toComponent(String message) {
		return TextComponent.fromLegacyText(message);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Register events for the given listener in BungeeFoundation
	 *
	 * @param listener
	 */
	public static void registerEvents(Listener listener) {
		ProxyServer.getInstance().getPluginManager().registerListener(SimplePlugin.getInstance(), listener);
	}

	/**
	 * Dispatches the event and returns if it was not canceled
	 *
	 * @param event
	 * @return
	 */
	public static boolean callEvent(Event event) {
		ProxyServer.getInstance().getPluginManager().callEvent(event);

		return !(event instanceof Cancellable) || !((Cancellable) event).isCancelled();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Running commands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the given command (without /) as the console, replacing {player} with sender
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchCommand(final CommandSender playerReplacement, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		command = command.startsWith("/") ? command.substring(1) : command;
		command = command.replace("{player}", playerReplacement == null ? "" : playerReplacement.getName());

		// Workaround for JSON in tellraw getting HEX colors replaced
		if (!command.startsWith("tellraw"))
			command = colorize(command);

		ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
	}

	/**
	 * Runs the given command (without /) as if the sender would type it, replacing {player} with his name
	 *
	 * @param playerSender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final ProxiedPlayer playerSender, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		command = command.startsWith("/") ? command.substring(1) : command;
		command = command.replace("{player}", playerSender == null ? "" : playerSender.getName());

		// Workaround for JSON in tellraw getting HEX colors replaced
		if (!command.startsWith("tellraw"))
			command = colorize(command);

		ProxyServer.getInstance().getPluginManager().dispatchCommand(playerSender, command);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Colors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces & colors for every string in the list A new list is created only
	 * containing non-null list values
	 *
	 * @param list
	 * @return
	 */
	public static List<String> colorize(final List<String> list) {
		final List<String> copy = new ArrayList<>();
		copy.addAll(list);

		for (int i = 0; i < copy.size(); i++) {
			final String message = copy.get(i);

			if (message != null)
				copy.set(i, colorize(message));
		}

		return copy;
	}

	/**
	 * Replace the & letter with the {@link ChatColor.COLOR_CHAR} in the message.
	 *
	 * @param messages the messages to replace color codes with '&'
	 * @return the colored message
	 */
	public static String colorize(String... messages) {
		return colorize(String.join("\n", messages));
	}

	/**
	 * Replaces & characters with colors.
	 *
	 * @param message
	 * @return
	 */
	public static String colorize(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	/**
	 * Remove all {@link ChatColor#COLOR_CHAR} as well as & letter colors from the message
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {
		return message == null ? "" : message.replaceAll("(" + ChatColor.COLOR_CHAR + "|&)([0-9a-fk-or])", "");
	}

	/**
	 * Replaces the {@link ChatColor#COLOR_CHAR} colors with & letters
	 *
	 * @param message
	 * @return
	 */
	public static String revertColorizing(final String message) {
		return message.replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Errors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param t
	 * @param messages
	 */
	public static void error(Throwable t, String... messages) {
		if (!(t instanceof FoException))
			Debugger.saveError(t, messages);

		Debugger.printStackTrace(t);
		logFramed(replaceErrorVariable(t, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 * <p>
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static void throwError(Throwable throwable, String... messages) {
		if (throwable.getCause() != null)
			throwable = throwable.getCause();

		if (messages != null)
			logFramed(replaceErrorVariable(throwable, messages));

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		throw Common.<RuntimeException>superSneaky(throwable);
	}

	private static <T extends Throwable> T superSneaky(Throwable t) throws T {
		throw (T) t;
	}

	/**
	 * Replace the %error variable with a smart error info, see above
	 *
	 * @param throwable
	 * @param msgs
	 * @return
	 */
	private static String[] replaceErrorVariable(Throwable throwable, String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++)
			msgs[i] = msgs[i].replace("%error", throwableName + throwableMessage);

		return msgs;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces string by a substitute for each element in the array
	 *
	 * @param what
	 * @param byWhat
	 * @param messages
	 * @return
	 */
	public static String[] replace(String what, String byWhat, String... messages) {
		final String[] copy = new String[messages.length];

		for (int i = 0; i < messages.length; i++)
			copy[i] = messages[i].replace(what, byWhat);

		return copy;
	}

	/**
	 * Replaces string by a substitute for each element in the list
	 *
	 * @param what
	 * @param byWhat
	 * @param messages
	 * @return
	 */
	public static List<String> replace(String what, String byWhat, List<String> messages) {
		messages = new ArrayList<>(messages);

		for (int i = 0; i < messages.size(); i++)
			messages.set(i, messages.get(i).replace(what, byWhat));

		return messages;
	}

	/**
	 * REplaces all nulls with an empty string
	 *
	 * @param list
	 * @return
	 */
	public static String[] replaceNuls(String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> List<T> removeNulsAndEmpties(T[] array) {
		return array != null ? removeNulsAndEmpties(Arrays.asList(array)) : new ArrayList<>();
	}

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> removeNulsAndEmpties(List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			final T key = list.get(i);

			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);
		}

		return copy;
	}

	/**
	 * Get an array from the given object. If the object
	 * is a list, return its array, otherwise return an array only containing the
	 * object as the first element
	 *
	 * @param obj
	 * @return
	 */
	public static String[] getListOrString(Object obj) {
		if (obj instanceof List) {
			final List<String> cast = (List<String>) obj;

			return toArray(cast);
		}

		return new String[] { obj.toString() };
	}

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static String getOrEmpty(String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * PSA: If values are strings, we return default if the value is empty or equals to "none"
	 *
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	/**
	 * Return the default value if the given string is null, "" or equals to "none"
	 *
	 * @param input
	 * @param def
	 * @return
	 */
	public static String getOrSupply(String input, String def) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? def : input;
	}

	/**
	 * Converts a list of string into a string array
	 *
	 * @param array
	 * @return
	 */
	public static String[] toArray(Collection<String> array) {
		return array.toArray(new String[array.size()]);
	}

	/**
	 * Converts a string array into a list of strings
	 *
	 * @param array
	 * @return
	 */
	public static ArrayList<String> toList(String... array) {
		return new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Converts {@link Iterable} to {@link List}
	 *
	 * @param it the iterable
	 * @return the new list
	 */
	public static <T> List<T> toList(Iterable<T> it) {
		final List<T> list = new ArrayList<>();
		it.forEach(el -> list.add(el));

		return list;
	}

	/**
	 * Reverses elements in the array
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T[] reverse(T[] array) {
		if (array == null)
			return null;

		int i = 0;
		int j = array.length - 1;

		while (j > i) {
			final T tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}

		return array;
	}

	/**
	 * Create a new hashset
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> Set<T> newSet(final T... keys) {
		return new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * Create a new array list that is mutable
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		Collections.addAll(list, keys);

		return list;
	}

	/**
	 * Lowercases all items in the array
	 *
	 * @param list
	 * @return
	 */
	public static String[] toLowerCase(String... list) {
		for (int i = 0; i < list.length; i++)
			list[i] = list[i].toLowerCase();

		return list;
	}

	/**
	 * Return a new hashmap having the given first key and value pair
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static <A, B> Map<A, B> newHashMap(A firstKey, B firstValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Regular expressions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given regex matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final String regex, final String message) {
		return regExMatch(compilePattern(regex), message);
	}

	/**
	 * Returns true if the given pattern matches the given message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final Pattern regex, final String message) {
		return regExMatch(compileMatcher(regex, message));
	}

	/**
	 * Returns true if the given matcher matches. We also evaluate
	 * how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param matcher
	 * @return
	 */
	public static boolean regExMatch(final Matcher matcher) {
		Valid.checkNotNull(matcher, "Cannot call regExMatch on null matcher");

		try {
			return matcher.find();

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, matcher.pattern());

			return false;
		}
	}

	/**
	 * Compiles a matches for the given pattern and message. Colors are stripped.
	 * <p>
	 * We also evaluate how long the evaluation took and stop it in case it takes too long,
	 * see {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(@NonNull final Pattern pattern, final String message) {

		try {
			String strippedMessage = stripColors(message);
			strippedMessage = ChatUtil.replaceDiacritic(strippedMessage);

			return pattern.matcher(strippedMessage);

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, pattern);

			return null;
		}
	}

	/**
	 * Compiles a matcher for the given regex and message
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(final String regex, final String message) {
		return compileMatcher(compilePattern(regex), message);
	}

	/**
	 * Compiles a pattern from the given regex, stripping colors and making
	 * it case insensitive
	 *
	 * @param regex
	 * @return
	 */
	public static Pattern compilePattern(String regex) {
		Pattern pattern = null;

		regex = stripColors(regex);
		regex = ChatUtil.replaceDiacritic(regex);

		try {

			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		} catch (final PatternSyntaxException ex) {
			throwError(ex,
					"Your regular expression is malformed!",
					"Expression: '" + regex + "'",
					"",
					"IF YOU CREATED IT YOURSELF, we unfortunately",
					"can't provide support for custom expressions.",
					"Use online services like regex101.com to put your",
					"expression there (without '') and discover where",
					"the syntax error lays and how to fix it.");

			return null;
		}

		return pattern;
	}

	/**
	 * A special call handling regex timeout exception, do not use
	 *
	 * @param ex
	 * @param pattern
	 */
	public static void handleRegexTimeoutException(RegexTimeoutException ex, Pattern pattern) {

		Common.error(ex,
				"A regular expression took too long to process, and was",
				"stopped to prevent freezing your server.",
				" ",
				"Limit " + SimpleSettings.REGEX_TIMEOUT + "ms ",
				"Expression: '" + (pattern == null ? "unknown" : pattern.pattern()) + "'",
				"Evaluated message: '" + ex.getCheckedMessage() + "'",
				" ",
				"IF YOU CREATED THAT RULE YOURSELF, we unfortunately",
				"can't provide support for custom expressions.",
				" ",
				"Sometimes, all you need doing is increasing timeout",
				"limit in your settings.yml",
				" ",
				"Use services like regex101.com to test and fix it.",
				"Put the expression without '' and the message there.",
				"Ensure to turn flags 'insensitive' and 'unicode' on",
				"on there when testing: https://i.imgur.com/PRR5Rfn.png");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Calculates the similarity (a double within 0 and 1) between two strings.
	 */
	public static double similarityPercentage(String first, String second) {
		if (first.isEmpty() && second.isEmpty())
			return 1D;

		String longer = first, shorter = second;

		// Longer should always have greater length
		if (first.length() < second.length()) {
			longer = second;

			shorter = first;
		}

		final int longerLength = longer.length();

		// Return 0 if both strings are zero length
		return longerLength == 0 ? 0 : (longerLength - editDistance(longer, shorter)) / (double) longerLength;

	}

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	private static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		final int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
				if (i == 0)
					costs[j] = j;
				else if (j > 0) {
					int newValue = costs[j - 1];
					if (s1.charAt(i - 1) != s2.charAt(j - 1))
						newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
					costs[j - 1] = lastValue;
					lastValue = newValue;
				}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Joins an array of lists together into one big list
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	public static <T> List<T> joinArrays(Collection<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Collection<T> array : arrays)
			all.addAll(array);

		return all;
	}

	/**
	 * Join a strict list array into one big list
	 *
	 * @param <T>
	 * @param lists
	 * @return
	 */
	public static <T> StrictList<T> join(StrictList<T>... lists) {
		final StrictList<T> joined = new StrictList<>();

		for (final StrictList<T> list : lists)
			joined.addAll(list);

		return joined;
	}

	/**
	 * Join a strict list array into one big list
	 *
	 * @param <T>
	 * @param lists
	 * @return
	 */
	public static <T extends Enum<T>> StrictList<String> join(Enum<T>[] enumeration) {
		final StrictList<String> joined = new StrictList<>();

		for (final Enum<T> constant : enumeration)
			joined.add(constant.toString());

		return joined;
	}

	/**
	 * Joins an array together using spaces from the given start index
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(int startIndex, String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * Join an array together using spaces using the given range
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(int startIndex, int stopIndex, String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * Join an array together using the given deliminer
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static String joinRange(int start, int stop, String[] array, String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtil.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * Joins an array of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(T[] array, String delimiter, Stringer<T> stringer) {
		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(T[] array) {
		return array == null ? "null" : joinToString(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String joinToString(Iterable<T> array) {
		return array == null ? "null" : joinToString(array, ", ");
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static <T> String joinToString(Iterable<T> array, String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : object.toString());
	}

	/**
	 * A convenience method for converting array of command senders into array of their names
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T extends CommandSender> String joinPlayers(Iterable<T> array) {
		return join(array, ", ", (Stringer<T>) CommandSender::getName);
	}

	/**
	 * Joins a list of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(Iterable<T> array, String delimiter, Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * A simple interface from converting objects into strings
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * Convert the given object into a string
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

	/**
	 * Converts json string into legacy colored text
	 *
	 * @param json
	 * @return
	 */
	public static String toLegacyText(String json) {
		String text = "";

		for (final BaseComponent comp : ComponentSerializer.parse(json))
			text += comp.toLegacyText();

		return text;
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 */
	public static String toJson(String message) {
		return toJson(TextComponent.fromLegacyText(message));
	}

	/**
	 * Converts base components into json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(BaseComponent... comps) {
		String json;

		try {
			json = ComponentSerializer.toString(comps);

		} catch (final Throwable t) {
			json = new Gson().toJson(new TextComponent(comps).toLegacyText());
		}

		return json;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static <OLD, NEW> List<NEW> convert(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final List<NEW> copy = new ArrayList<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Converts a set having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static <OLD, NEW> Set<NEW> convertSet(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final Set<NEW> copy = new HashSet<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter
	 * @return the new list
	 */
	public static <OLD, NEW> StrictList<NEW> convertStrict(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final StrictList<NEW> copy = new StrictList<>();

		for (final OLD old : list)
			copy.add(converter.convert(old));

		return copy;
	}

	/**
	 * Attempts to convert the given map into another map
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> Map<NEW_KEY, NEW_VALUE> convert(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final Map<NEW_KEY, NEW_VALUE> newMap = new HashMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Attempts to convert the given map into another map
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> StrictMap<NEW_KEY, NEW_VALUE> convertStrict(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final StrictMap<NEW_KEY, NEW_VALUE> newMap = new StrictMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Attempts to convert the gfiven map into a list
	 *
	 * @param <LIST_KEY>
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param map
	 * @param converter
	 * @return
	 */
	public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictList<LIST_KEY> convertToList(final Map<OLD_KEY, OLD_VALUE> map, final MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
		final StrictList<LIST_KEY> list = new StrictList<>();

		for (final Entry<OLD_KEY, OLD_VALUE> e : map.entrySet())
			list.add(converter.convert(e.getKey(), e.getValue()));

		return list;
	}

	/**
	 * Attempts to convert an array into a different type
	 *
	 * @param <OLD_TYPE>
	 * @param <NEW_TYPE>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static <OLD_TYPE, NEW_TYPE> List<NEW_TYPE> convert(final OLD_TYPE[] oldArray, final TypeConverter<OLD_TYPE, NEW_TYPE> converter) {
		final List<NEW_TYPE> newList = new ArrayList<>();

		for (final OLD_TYPE old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

	/**
	 * A simple interface to convert between types
	 *
	 * @param <Old> the initial type to convert from
	 * @param <New> the final type to convert to
	 */
	public interface TypeConverter<Old, New> {

		/**
		 * Convert a type given from A to B
		 *
		 * @param value the old value type
		 * @return the new value type
		 */
		New convert(Old value);
	}

	/**
	 * Convenience class for converting map to a list
	 *
	 * @param <O>
	 * @param <K>
	 * @param <V>
	 */
	public interface MapToListConverter<O, K, V> {

		/**
		 * Converts the given map key-value pair into a new type stored in a list
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, V value);
	}

	/**
	 * Convenience class for converting between maps
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface MapToMapConverter<A, B, C, D> {

		/**
		 * Converts the old key type to a new type
		 *
		 * @param key
		 * @return
		 */
		C convertKey(A key);

		/**
		 * Converts the old value into a new value type
		 *
		 * @param value
		 * @return
		 */
		D convertValue(B value);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Schedule a task to be run at a later time
	 *
	 * @param delayTicks
	 * @param runnable
	 */
	public static void runLater(int delayTicks, Runnable runnable) {
		ProxyServer.getInstance().getScheduler().schedule(SimplePlugin.getInstance(), runnable, delayTicks * 50, TimeUnit.MILLISECONDS);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a long ------ console line
	 *
	 * @return
	 */
	public static String consoleLine() {
		return "!-----------------------------------------------------!";
	}

	/**
	 * Returns a long ______ console line
	 *
	 * @return
	 */
	public static String consoleLineSmooth() {
		return "______________________________________________________________";
	}

	/**
	 * Returns a long -------- chat line
	 *
	 * @return
	 */
	public static String chatLine() {
		return "*----------------------------------------------------*";
	}

	/**
	 * Returns a long ----------- chat line with strike color
	 *
	 * @return
	 */
	public static String chatLineSmooth() {
		return ChatColor.STRIKETHROUGH + "――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――";
	}

	/**
	 * Returns a very long -------- config line
	 *
	 * @return
	 */
	public static String configLine() {
		return "-------------------------------------------------------------------------------------------";
	}

	/**
	 * Returns a |------------| scoreboard line with given dashes amount
	 *
	 * @param length
	 * @return
	 */
	public static String scoreboardLine(int length) {
		String fill = "";

		for (int i = 0; i < length; i++)
			fill += "-";

		return "&m|" + fill + "|";
	}

	/**
	 * Limits length to 60 chars.
	 * <p>
	 * If JSON, unpacks it and display [json] prefix.
	 */
	public static String formatStringHover(String msg) {
		String finalText = msg;

		if (msg.startsWith("[JSON]")) {
			final String stripped = msg.replaceFirst("\\[JSON\\]", "").trim();

			if (!stripped.isEmpty())
				finalText = "&8[&6json&8] &r" + toLegacyText(stripped);
		}

		return finalText.length() <= 60 ? finalText : finalText.substring(0, 60) + "...";
	}

	/**
	 * If the count is 0 or over 1, adds an "s" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String plural(long count, String ofWhat) {
		return count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("s") ? "s" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "es" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralEs(long count, String ofWhat) {
		return count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("es") ? "es" : "");
	}

	/**
	 * If the count is 0 or over 1, adds an "ies" to the given string
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralIes(long count, String ofWhat) {
		return count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
	}

	/**
	 * Prepends the given string with either "a" or "an" (does a dummy syllable check)
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated only a dummy syllable check, e.g. returns a hour
	 */
	@Deprecated
	public static String article(String ofWhat) {
		Valid.checkBoolean(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		return (syllables.contains(ofWhat.toLowerCase().trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
	}

	/**
	 * Generates a bar indicating progress. Example:
	 * <p>
	 * ##-----
	 * ###----
	 * ####---
	 *
	 * @param min            the min progress
	 * @param minChar
	 * @param max            the max prograss
	 * @param maxChar
	 * @param delimiterColor
	 * @return
	 */
	public static String fancyBar(int min, char minChar, int max, char maxChar, ChatColor delimiterColor) {
		String formatted = "";

		for (int i = 0; i < min; i++)
			formatted += minChar;

		formatted += delimiterColor;

		for (int i = 0; i < max - min; i++)
			formatted += maxChar;

		return formatted;
	}

	/**
	 * Resolves the inner Map in a Bukkit's {@link Configuration}
	 *
	 * @param mapOrSection
	 * @return
	 */
	public static Map<String, Object> getMapFromSection(@NonNull final Object mapOrSection) {
		final Map<String, Object> map = mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection : mapOrSection instanceof Configuration ? ReflectionUtil.getFieldContent(mapOrSection, "self") : null;
		Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

		return map;
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

		return Integer.parseInt(version);
	}
}
