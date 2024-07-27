package org.mineacademy.bfo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Utility class for managing players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Kicks the player on the main thread with a colorized message
	 *
	 * @param player
	 * @param message
	 *
	 */
	public static void kick(final ProxiedPlayer player, final String... message) {
		player.disconnect(Common.colorize(message));
	}
}
