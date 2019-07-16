package org.mineacademy.bfo;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * A generic utility class
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

	/**
	 * Send a colorized message to the player.
	 *
	 * @param sender
	 * @param message
	 */
	public static void tell(CommandSender sender, String message) {
		sender.sendMessage(colorize(message));
	}

	/**
	 * Replaces & characters with colors and wraps the text as a valid BaseComponent array.
	 *
	 * @param message
	 * @return
	 */
	public static BaseComponent[] colorize(String message) {
		return TextComponent.fromLegacyText(colorizeLegacy(message));
	}

	/**
	 * Replaces & characters with colors.
	 *
	 * @param message
	 * @return
	 */
	public static String colorizeLegacy(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}
}
