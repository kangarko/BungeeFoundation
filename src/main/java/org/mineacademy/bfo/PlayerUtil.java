package org.mineacademy.bfo;

import org.mineacademy.bfo.model.Variables;
import org.mineacademy.bfo.remain.Remain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
		Remain.sendTablist(player, header, footer);
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
		Remain.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
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
		Remain.sendActionBar(player, title);
	}
}
