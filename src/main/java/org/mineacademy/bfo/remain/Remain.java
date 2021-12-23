package org.mineacademy.bfo.remain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Variables;

import com.google.gson.Gson;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.packet.Title;
import net.md_5.bungee.protocol.packet.Title.Action;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
public final class Remain {

	/**
	 * Pattern used to match encoded HEX colors &x&F&F&F&F&F&F
	 */
	private static final Pattern RGB_HEX_ENCODED_REGEX = Pattern.compile("(?i)(§x)((§[0-9A-F]){6})");

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	// Singleton
	private Remain() {
	}

	// ----------------------------------------------------------------------------------------------------
	// Various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Returns all online players
	 *
	 * @return the online players
	 */
	public static Collection<? extends ProxiedPlayer> getOnlinePlayers() {
		return ProxyServer.getInstance().getPlayers();
	}

	/**
	 * Converts json string into legacy colored text
	 *
	 * @param json
	 * @return
	 * @throws InteractiveTextFoundException
	 */
	public static String toLegacyText(final String json) throws InteractiveTextFoundException {
		return toLegacyText(json, true);
	}

	/**
	 * Converts chat message in JSON (IChatBaseComponent) to one lined old style
	 * message with color codes. e.g. {text:"Hello world",color="red"} converts to
	 * &cHello world
	 * @param json
	 *
	 * @param denyEvents if an exception should be thrown if hover/click event is
	 *                   found.
	 * @return
	 * @throws InteractiveTextFoundException if click/hover event are found. Such
	 *                                       events would be removed, and therefore
	 *                                       message containing them shall not be
	 *                                       unpacked
	 */
	public static String toLegacyText(final String json, final boolean denyEvents) throws InteractiveTextFoundException {
		final StringBuilder text = new StringBuilder();

		// Translate options does not want to work well with ChatControl
		if (json.contains("\"translate\""))
			return text.append("").toString();

		try {
			for (final BaseComponent comp : ComponentSerializer.parse(json)) {
				if ((comp.getHoverEvent() != null || comp.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text.append(comp.toLegacyText());
			}

		} catch (final Throwable throwable) {

			// Do not catch our own exception
			if (throwable instanceof InteractiveTextFoundException)
				throw throwable;
		}

		return text.toString();
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String toJson(final Collection<String> list) {
		return gson.toJson(list);
	}

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> fromJsonList(String json) {
		return gson.fromJson(json, List.class);
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 * @param message
	 * @return
	 */
	public static String toJson(final String message) {
		return toJson(TextComponent.fromLegacyText(message));
	}

	/**
	 * Converts base components into json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(final BaseComponent... comps) {
		String json;

		try {
			json = ComponentSerializer.toString(comps);

		} catch (final Throwable t) {
			json = new Gson().toJson(new TextComponent(comps).toLegacyText());
		}

		return json;
	}

	/**
	 * Converts json into base component array
	 *
	 * @param json
	 * @return
	 */
	public static BaseComponent[] toComponent(final String json) {

		try {
			return ComponentSerializer.parse(json);

		} catch (final Throwable t) {
			Common.throwError(t,
					"Failed to call toComponent!",
					"Json: " + json,
					"Error: %error%");

			return null;
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 * @param placeholders
	 */
	public static void sendJson(final CommandSender sender, final String json, final SerializedMap placeholders) {
		try {
			final BaseComponent[] components = ComponentSerializer.parse(json);

			replaceHexPlaceholders(Arrays.asList(components), placeholders);

			sendComponent(sender, components);

		} catch (final RuntimeException ex) {
			Common.error(ex, "Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json);
		}
	}

	/*
	 * A helper Method for MC 1.16+ to partially solve the issue of HEX colors in JSON
	 *
	 * BaseComponent does not support colors when in text, they must be set at the color level
	 */
	private static void replaceHexPlaceholders(final List<BaseComponent> components, final SerializedMap placeholders) {

		for (final BaseComponent component : components) {
			if (component instanceof TextComponent) {
				final TextComponent textComponent = (TextComponent) component;
				String text = textComponent.getText();

				for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
					String key = entry.getKey();
					String value = Common.simplify(entry.getValue());

					// Detect HEX in placeholder
					final Matcher match = RGB_HEX_ENCODED_REGEX.matcher(text);

					while (match.find()) {

						// Find the color
						final String color = "#" + match.group(2).replace(ChatColor.COLOR_CHAR + "", "");

						// Remove it from chat and bind it to TextComponent instead
						value = match.replaceAll("");
						textComponent.setColor(net.md_5.bungee.api.ChatColor.of(color));
					}

					key = key.charAt(0) != '{' ? "{" + key : key;
					key = key.charAt(key.length() - 1) != '}' ? key + "}" : key;

					text = text.replace(key, value);
					textComponent.setText(text);
				}
			}

			if (component.getExtra() != null)
				replaceHexPlaceholders(component.getExtra(), placeholders);

			if (component.getHoverEvent() != null)
				replaceHexPlaceholders(Arrays.asList(component.getHoverEvent().getValue()), placeholders);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final CommandSender sender, final String json) {
		try {
			sendComponent(sender, ComponentSerializer.parse(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			throw new RuntimeException("Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json, t);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param comps
	 */
	public static void sendComponent(final CommandSender sender, final BaseComponent... comps) {
		sender.sendMessage(comps);
	}

	/**
	 * Sends a title to the player (1.8+) for three seconds
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(final ProxiedPlayer player, final String title, final String subtitle) {
		sendTitle(player, 20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player (1.8+) Texts will be colorized.
	 *
	 * @param player   the player
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param titleText    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final ProxiedPlayer player, final int fadeIn, final int stay, final int fadeOut, final String titleText, final String subtitle) {
		final net.md_5.bungee.api.Title title = ProxyServer.getInstance().createTitle();

		title.fadeIn(fadeIn);
		title.stay(stay);
		title.fadeOut(fadeOut);
		title.title(TextComponent.fromLegacyText(Common.colorize(Variables.replace(titleText, player))));

		if (subtitle != null)
			title.title(TextComponent.fromLegacyText(Common.colorize(Variables.replace(subtitle, player))));

		title.send(player);
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null. (1.8+)
	 * Texts will be colorized.
	 *
	 * @param player the player
	 * @param header the header
	 * @param footer the footer
	 */
	public static void sendTablist(final ProxiedPlayer player, final String header, final String footer) {
		player.setTabHeader(
				TextComponent.fromLegacyText(Common.colorize(header)),
				TextComponent.fromLegacyText(Common.colorize(footer)));
	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param player the player
	 * @param text   the text
	 */
	public static void sendActionBar(final ProxiedPlayer player, final String text) {
		final Title packet = new Title();

		packet.setAction(Action.ACTIONBAR);
		packet.setText(Remain.toJson(Variables.replace(text, player)));

		player.unsafe().sendPacket(packet);
	}

	/**
	 * Return the player ping
	 *
	 * @deprecated use {@link ProxiedPlayer#getPing()}
	 * @param player
	 * @return
	 */
	@Deprecated
	public static int getPing(ProxiedPlayer player) {
		return player.getPing();
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrow.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
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

	// ------------------------ Utility ------------------------

	/**
	 * Thrown when message contains hover or click events which would otherwise got
	 * removed.
	 * <p>
	 * Such message is not checked.
	 */
	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}

/**
 * A wrapper for Spigot
 */
class SneakyThrow {

	public static void sneaky(final Throwable t) {
		throw SneakyThrow.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}