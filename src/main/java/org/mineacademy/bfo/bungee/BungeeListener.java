package org.mineacademy.bfo.bungee;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.message.IncomingMessage;
import org.mineacademy.bfo.bungee.message.OutgoingMessage;
import org.mineacademy.bfo.debug.Debugger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Represents a BungeeCord listener using a bungee channel
 * on which you can listen to receiving messages
 *
 * This class is also a Listener for Bukkit events for your convenience
 */
public abstract class BungeeListener implements Listener {

	/**
	 * Holds registered bungee listeners
	 */
	private static final Set<BungeeListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final BungeeMessageType[] actions;

	/**
	 * Temporary variable storing the senders connection
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private Server sender;

	/**
	 * Temporary variable storing the receiver
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private Connection receiver;

	/**
	 * Temporary variable for reading data
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private byte[] data;

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected BungeeListener(@NonNull String channel, Class<? extends BungeeMessageType> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final BungeeListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static BungeeMessageType[] toActions(@NonNull Class<? extends BungeeMessageType> actionEnum) {
		Valid.checkBoolean(actionEnum != BungeeMessageType.class, "When creating BungeeListener put your own class that extend BungeeMessageType there, not BungeeMessageType class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "BungeeListener expects BungeeMessageType to be an enum, given: " + actionEnum);

		try {
			return (BungeeMessageType[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

			return null;
		}
	}

	/**
	 * Called automatically when you receive a plugin message from Bungeecord,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param sender
	 * @param message
	 */
	public abstract void onMessageReceived(Connection sender, IncomingMessage message);

	/**
	 * Creates a new outgoing message for the given action using the sender connection
	 * and this listeners channel
	 *
	 * @param senderUid
	 * @param messageType
	 * @return
	 */
	protected final OutgoingMessage createOutgoingMessage(UUID senderUid, BungeeMessageType messageType) {
		return new OutgoingMessage(senderUid, this.sender.getInfo().getName(), messageType);
	}

	/**
	 * Shortcut for {@link ProxyServer#getInstance()}
	 *
	 * @return
	 */
	protected final ProxyServer getProxy() {
		return ProxyServer.getInstance();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BungeeListener && ((BungeeListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public static void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * Distributes received plugin message across all {@link BungeeListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final class BungeeListenerImpl implements Listener {

		private static boolean registered = false;

		public BungeeListenerImpl() {
			Valid.checkBoolean(!registered, "Already registered!");

			registered = true;
		}

		/**
		 * Handle the received message automatically if it matches our tag
		 *
		 * @param event
		 */
		@EventHandler
		public void onPluginMessage(PluginMessageEvent event) {

			final Connection sender = event.getSender();
			final Connection receiver = event.getReceiver();

			final String channelName = event.getTag();

			if (!(sender instanceof Server))
				return;

			boolean handled = false;

			for (final BungeeListener listener : registeredListeners)
				if (channelName.equals(listener.getChannel())) {
					final IncomingMessage message = new IncomingMessage(listener, event.getData());

					listener.sender = (Server) sender;
					listener.receiver = receiver;
					listener.data = event.getData();

					Debugger.debug("bungee", "Channel " + message.getChannel() + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
					listener.onMessageReceived(listener.sender, message);

					handled = true;
				}

			if (handled)
				event.setCancelled(true);
		}
	}
}
