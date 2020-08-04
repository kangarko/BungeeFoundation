package org.mineacademy.bfo.bungee;

import lombok.Getter;

/**
 * Proprietary implementation of BungeeAction for some of our
 * premium plugins handled by BungeeControl
 *
 * The BungeeCord protocol always begins with
 *
 * 1) The sender server name
 * 2) The {@link BungeeAction}
 *
 * and the rest is the actual data within this enum
 */
public enum FoBungeeAction implements BungeeAction {

	// ----------------------------------------------------------------------------------------------------
	// Stay on BungeeCord using BungeeControl plugin
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Execute a command on bungee
	 */
	BUNGEE_COMMAND("command"),

	/**
	 * Sends the player a simple colorized message
	 */
	TELL_PLAYER("player", "message"),

	// ----------------------------------------------------------------------------------------------------
	// Forward to Bukkit servers
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Mute the chat.
	 */
	CHAT_MUTE("message"),

	/**
	 * Wipe the chat.
	 */
	CHAT_CLEAR("message"),

	/**
	 * Broadcast a message in a channel.
	 */
	CHANNEL("sender", "channel_name", true /*hasMuteBypassPermission*/, "message"),

	/**
	 * Sends an announcement message to everyone, and plays a nice sound
	 */
	ANNOUNCEMENT("log message", "message"),

	/**
	 * Broadcast a json message to every player with permission.
	 */
	BROADCAST_JSON_WITH_PERMISSION("permission", "json_message"),

	/**
	 * Broadcast a json message to every player with permission as a sender.
	 */
	BROADCAST_JSON_WITH_PERMISSION_AS("sender", "sender_uuid", "permission", "json_message"),

	/**
	 * Forwards a spy message to players with permissions
	 */
	SPY("message"),

	/**
	 * Attempts to find a private message recipient, reports back to bungee
	 * whether we found him or not
	 */
	PM_LOOKUP("sender", "receiver", "senderRawMessage", "receiverMessage", true /*hasBypassPermission*/, true /*hasTogglePMBypassPermission*/, true /*hasPMVanishedPermission*/),

	/**
	 * This message is forwarded after PM_LOOKUP to the sender server
	 * to tell him that the recipient was not found
	 */
	PM_PLAYER_NOT_FOUND("sender", "playerThatIsNotFound"),

	/**
	 * Forwarded after PM_LOOKUP is done to deliver the private message
	 * to the recipient
	 */
	PM_PLAYER_FOUND("sender", "recipient", "message"),

	;

	/**
	 * Stores all valid values, the names of them are only used
	 * in the error message when the length of data does not match
	 */
	@Getter
	private final Class<?>[] content;

	/**
	 * Constructs a new bungee action
	 *
	 * @param validValues
	 */
	FoBungeeAction(final Object... validValues) {
		final Class<?>[] classes = new Class<?>[validValues.length];

		for (int i = 0; i < classes.length; i++) {
			final Object value = validValues[i];

			classes[i] = value.getClass();
		}

		content = classes;
	}
}