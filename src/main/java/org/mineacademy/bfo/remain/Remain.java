package org.mineacademy.bfo.remain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.ReflectionUtil.ReflectionException;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Variables;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.gson.Gson;

import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.DefinedPacket;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
public final class Remain {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	/**
	 * The Adventure platform
	 */
	private static BungeeAudiences adventure;

	// ----------------------------------------------------------------------------------------------------
	// Flags below
	// ----------------------------------------------------------------------------------------------------
	/**
	 * The internal private section path data class
	 */
	private static Class<?> sectionPathDataClass = null;

	// Singleton
	private Remain() {
	}

	/**
	 * Initialize all fields and methods automatically when we set the plugin
	 */
	static {

		try {
			sectionPathDataClass = ReflectionUtil.lookupClass("org.bukkit.configuration.SectionPathData");

		} catch (final ReflectionException ex) {
			// unsupported
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Advanced: Sends a packet to the player
	 *
	 * @param player the player
	 * @param packet the packet
	 */
	public static void sendPacket(final ProxiedPlayer player, final DefinedPacket packet) {
		player.unsafe().sendPacket(packet);
	}

	// ----------------------------------------------------------------------------------------------------
	// Compatibility methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The server getter, used to change for Redis compatibility
	 */
	@Setter
	private static Supplier<Collection<ServerInfo>> serverGetter = () -> ProxyServer.getInstance().getServers().values();

	/**
	 * Returns all servers
	 *
	 * @return
	 */
	public static Collection<ServerInfo> getServers() {
		return serverGetter.get();
	}

	/**
	 * Returns all online players
	 *
	 * @return the online players
	 */
	public static Collection<ProxiedPlayer> getOnlinePlayers() {
		final Collection<ProxiedPlayer> players = new ArrayList<>();

		for (final ServerInfo serverInfo : getServers())
			players.addAll(serverInfo.getPlayers());

		return players;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param name
	 * @return
	 */
	public static ProxiedPlayer getPlayer(String name) {
		for (final ProxiedPlayer player : getOnlinePlayers())
			if (player.getName().equalsIgnoreCase(name))
				return player;

		return null;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param uuid
	 * @return
	 */
	public static ProxiedPlayer getPlayer(UUID uuid) {
		for (final ProxiedPlayer player : getOnlinePlayers())
			if (player.getUniqueId().equals(uuid))
				return player;

		return null;
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
	 * Converts a base component to json
	 *
	 * @param base
	 * @return
	 */
	public static String convertBaseComponentToJson(BaseComponent... base) {
		return convertAdventureToJson(convertBaseComponentToAdventure(base));
	}

	/**
	 * Converts a base component to Adventure component
	 *
	 * @param base
	 * @return
	 */
	public static Component convertBaseComponentToAdventure(BaseComponent... base) {
		return BungeeComponentSerializer.get().deserialize(base);
	}

	/**
	 * Converts a component to JSON
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToJson(Component component) {
		return GsonComponentSerializer.gson().serialize(component);
	}

	/**
	 * Serializes the component into legacy text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToLegacy(Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	/**
	 * Serializes the component into plain text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToPlain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	/**
	 * Converts a json string to Adventure component
	 *
	 * @param json
	 * @return
	 */
	public static Component convertJsonToAdventure(String json) {
		return GsonComponentSerializer.gson().deserialize(json);
	}

	/**
	 *
	 * @param componentJson
	 * @return
	 */
	public static String convertJsonToLegacy(String componentJson) {
		return convertAdventureToLegacy(convertJsonToAdventure(componentJson));
	}

	public static BaseComponent[] convertJsonToBaseComponent(String json) {
		final Component adventure = GsonComponentSerializer.gson().deserialize(json);
		final BaseComponent[] bungee = BungeeComponentSerializer.get().serialize(adventure);

		return bungee;
	}

	/**
	 * Creates a new adventure component from legacy text with {@link CompChatColor#COLOR_CHAR} colors replaced
	 *
	 * @param legacyText
	 * @return
	 */
	public static Component convertLegacyToAdventure(String legacyText) {
		return LegacyComponentSerializer.legacySection().deserialize(legacyText);
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 *
	 * @param message
	 * @return
	 */
	public static String convertLegacyToJson(final String message) {
		return GsonComponentSerializer.gson().serialize(convertLegacyToAdventure(message));
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final CommandSender sender, final String json) {
		sendJson(getAdventure().sender(sender), json);
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final Audience sender, final String json) {
		try {
			sender.sendMessage(convertJsonToAdventure(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			throw new RuntimeException("Malformed JSON when sending message to " + sender + " with JSON: " + json, t);
		}
	}

	/**
	 * Sends a message to the player
	 *
	 * @param sender
	 * @param component
	 */
	public static void tell(CommandSender sender, Component component) {
		tell(getAdventure().sender(sender), component);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 */
	public static void tell(Audience sender, Component component) {
		tell(sender, component, true);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 * @param skipEmpty
	 */
	public static void tell(@NonNull CommandSender sender, Component component, boolean skipEmpty) {
		tell(getAdventure().sender(sender), component, skipEmpty);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 * @param skipEmpty
	 */
	public static void tell(@NonNull Audience sender, Component component, boolean skipEmpty) {
		if (Remain.convertAdventureToPlain(component).trim().isEmpty() && skipEmpty)
			return;

		sender.sendMessage(component);
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
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final ProxiedPlayer player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		getAdventure().player(player).showTitle(Title.title(
				convertLegacyToAdventure(Variables.replace(title, player)),
				convertLegacyToAdventure(Variables.replace(subtitle, player)),
				Times.times(Duration.ofMillis(fadeIn * 50), Duration.ofMillis(stay * 50), Duration.ofMillis(fadeOut * 50))));
	}

	/**
	 * Resets the title that is being displayed to the player (1.8+)
	 *
	 * @param player the player
	 */
	public static void resetTitle(final ProxiedPlayer player) {
		getAdventure().player(player).clearTitle();
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
		getAdventure().player(player).sendPlayerListHeaderAndFooter(
				convertLegacyToAdventure(Variables.replace(header, player)),
				convertLegacyToAdventure(Variables.replace(footer, player)));

	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param player the player
	 * @param text   the text
	 */
	public static void sendActionBar(final ProxiedPlayer player, final String text) {
		getAdventure().player(player).sendActionBar(convertLegacyToAdventure(Variables.replace(text, player)));
	}

	/**
	 * Shows a boss bar that is then hidden after the given period
	 *
	 * @param player
	 * @param message
	 * @param secondsToShow
	 */
	public static void sendBossbarTimed(ProxiedPlayer player, String message, int secondsToShow) {
		final BossBar bar = sendBossbar(player, message, 1.0F);

		Common.runLaterAsync(secondsToShow * 20, (Runnable) () -> removeBossBar(player, bar));
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent
	 *
	 * @return
	 */
	public static BossBar sendBossbar(final ProxiedPlayer player, final String message, final float percent) {
		return sendBossbar(player, message, percent, null, null);
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent from 0.0 to 1.0
	 * @param color
	 * @param overlay
	 *
	 * return
	 * @return
	 */
	public static BossBar sendBossbar(final ProxiedPlayer player, final String message, final float percent, final BossBar.Color color, final BossBar.Overlay overlay) {
		final BossBar bar = BossBar.bossBar(convertLegacyToAdventure(Variables.replace(message, player)), percent, color, overlay);
		getAdventure().player(player).showBossBar(bar);

		return bar;
	}

	/**
	 * Attempts to remove a boss bar from player.
	 *
	 * @param player
	 * @param bar
	 */
	public static void removeBossBar(final ProxiedPlayer player, BossBar bar) {
		getAdventure().player(player).hideBossBar(bar);
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

	/**
	 * Converts the given object that may be a SectionPathData for MC 1.18 back into its root data
	 *
	 * @param objectOrSectionPathData
	 * @return
	 *
	 * @deprecated legacy code, will be removed
	 */
	@Deprecated
	public static Object getRootOfSectionPathData(Object objectOrSectionPathData) {
		if (objectOrSectionPathData != null && objectOrSectionPathData.getClass() == sectionPathDataClass)
			objectOrSectionPathData = ReflectionUtil.invoke("getData", objectOrSectionPathData);

		return objectOrSectionPathData;
	}

	/**
	 * Return true if the given object is a memory section
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isMemorySection(Object obj) {
		return obj != null && sectionPathDataClass == obj.getClass();
	}

	/*
	 * Return the Adventure platform
	 */
	private static BungeeAudiences getAdventure() {
		if (adventure == null)
			adventure = BungeeAudiences.create(SimplePlugin.getInstance());

		return adventure;
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