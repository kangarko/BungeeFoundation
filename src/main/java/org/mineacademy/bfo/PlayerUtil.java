package org.mineacademy.bfo;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.packet.Title;
import net.md_5.bungee.protocol.packet.Title.Action;

/**
 * Utility class related to players
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	/**
	 * Send a tablist to the player with a colorized header and a footer.
	 *
	 * @param player
	 * @param header
	 * @param footer
	 */
	public static void sendTablist(ProxiedPlayer player, String header, String footer) {
		player.setTabHeader(Common.colorize(header), Common.colorize(footer));
	}

	/**
	 * Send a title to the player with a colorized title and subtitle displayed
	 * for 4 seconds.
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(ProxiedPlayer player, String title, String subtitle) {
		sendTitle(player, title, subtitle, 20, 3 * 20, 20);
	}

	/**
	 * Send a title to the player with a colorized title and subtitle
	 * and the stay time (all in ticks) (20 ticks = 1 second)
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 */
	public static void sendTitle(ProxiedPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		ProxyServer.getInstance()
				.createTitle()
				.reset()
				.title(Common.colorize(title))
				.subTitle(Common.colorize(subtitle))
				.fadeIn(fadeIn)
				.fadeOut(fadeOut)
				.stay(stay)
				.send(player);
	}

	/**
	 * Send an action bar to the player with a colorized text.
	 *
	 * @param player
	 * @param title
	 */
	public static void sendActionBar(ProxiedPlayer player, String title) {
		final Title packet = new Title();

		packet.setAction(Action.ACTIONBAR);
		packet.setText(ComponentSerializer.toString(Common.colorize(title)));

		player.unsafe().sendPacket(packet);
	}
}
