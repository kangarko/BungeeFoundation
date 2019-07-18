package org.mineacademy.bfo;

import org.mineacademy.bfo.model.Variables;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param player
	 * @param header
	 * @param footer
	 */
	public static void sendTablist(ProxiedPlayer player, String header, String footer) {
		header = Variables.replace(header, player);
		footer = Variables.replace(footer, player);

		player.setTabHeader(Common.toComponent(header), Common.toComponent(footer));
	}

	/**
	 * Send a title to the player with a colorized title and subtitle displayed
	 * for 4 seconds.
	 *
	 * Variables from {@link Variables} are replaced.
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
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 * @param fadeIn
	 * @param stay
	 * @param fadeOut
	 */
	public static void sendTitle(ProxiedPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		title = Variables.replace(title, player);
		subtitle = Variables.replace(subtitle, player);

		ProxyServer.getInstance()
				.createTitle()
				.reset()
				.title(Common.toComponent(title))
				.subTitle(Common.toComponent(subtitle))
				.fadeIn(fadeIn)
				.fadeOut(fadeOut)
				.stay(stay)
				.send(player);
	}

	/**
	 * Send an action bar to the player with a colorized text.
	 *
	 * Variables from {@link Variables} are replaced.
	 *
	 * @param player
	 * @param title
	 */
	public static void sendActionBar(ProxiedPlayer player, String title) {
		final Title packet = new Title();

		packet.setAction(Action.ACTIONBAR);
		packet.setText(Common.toJson(Variables.replace(title, player)));

		player.unsafe().sendPacket(packet);
	}
}
