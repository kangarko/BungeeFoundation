package org.mineacademy.bfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Variables;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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

	public static boolean callEvent(Event event) {
		ProxyServer.getInstance().getPluginManager().callEvent(event);
		return !(event instanceof Cancellable) || !((Cancellable) event).isCancelled();
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
		for (int i = 0; i < messages.length; i++)
			messages[i] = messages[i].replace(what, byWhat);

		return messages;
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
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(T value, T def) {
		Valid.checkNotNull(def, "The default value must not be null!");

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
}
