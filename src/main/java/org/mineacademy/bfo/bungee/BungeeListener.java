package org.mineacademy.bfo.bungee;

import java.util.HashSet;
import java.util.Set;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.message.IncomingMessage;
import org.mineacademy.bfo.bungee.message.OutgoingMessage;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Represents a BungeeCord listener using a {@link BungeeChannel} channel
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
	 * Temporary variable storing the senders connection
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private ServerConnection sender;

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
	 * Create a new bungee listener for the {@link SimplePlugin#getBungeeChannel()}
	 *
	 * @param channel
	 */
	protected BungeeListener() {
		for (final BungeeListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	/**
	 * Called automatically when you receive a plugin message from Bungeecord,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param action
	 * @param message
	 */
	public abstract void onMessageReceived(ServerConnection sender, IncomingMessage message);

	/**
	 * Creates a new outgoing message for the given action using the sender connection
	 * and this listeners channel
	 *
	 * @param action
	 * @return
	 */
	protected final OutgoingMessage createOutgoingMessage(BungeeAction action) {
		return new OutgoingMessage(sender.getInfo().getName(), action);
	}

	/**
	 * Shortcut for {@link ProxyServer#getInstance()}
	 *
	 * @return
	 */
	protected final ProxyServer getProxy() {
		return ProxyServer.getInstance();
	}

	/**
	 * A proxy for {@link SimplePlugin#getBungeeCord()#getChannel()}
	 *
	 * @return
	 */
	public final String getChannel() {
		return SimplePlugin.getBungee().getChannel();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BungeeListener && ((BungeeListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * Distributes received plugin message accross all {@link BungeeListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final class BungeePluginMessageListener implements Listener {

		private static boolean registered = false;

		public BungeePluginMessageListener() {
			Valid.checkBoolean(!registered, "Already registered!");

			registered = true;
		}

		/**
		 * Handle the received message automatically if it matches our tag
		 *
		 * @param even
		 */
		@EventHandler
		public void onPluginMessage(PluginMessageEvent event) {
			final String tag = event.getTag();
			final Connection sender = event.getSender();
			final Connection receiver = event.getReceiver();

			if (!(sender instanceof ServerConnection))
				return;

			for (final BungeeListener listener : registeredListeners)
				if (tag.equals(listener.getChannel())) {
					listener.sender = (ServerConnection) sender;
					listener.receiver = receiver;
					listener.data = event.getData();

					final IncomingMessage message = new IncomingMessage(listener.data);

					Debugger.debug("bungee", "Channel " + message.getChannel() + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
					listener.onMessageReceived(listener.sender, message);
				}
		}
	}
}
