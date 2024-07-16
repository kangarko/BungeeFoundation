package org.mineacademy.bfo.proxy;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.proxy.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

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
 * Represents a proxy listener
 * on which you can listen to receiving messages
 */
public abstract class ProxyListener implements Listener {

	/**
	 * The default channel
	 */
	public static final String DEFAULT_CHANNEL = "BungeeCord";

	/**
	 * Holds registered listeners
	 */
	private static final Set<ProxyListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final ProxyMessage[] actions;

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
	 * Create a new instance
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected ProxyListener(@NonNull String channel, Class<? extends ProxyMessage> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final ProxyListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static ProxyMessage[] toActions(@NonNull Class<? extends ProxyMessage> actionEnum) {
		Valid.checkBoolean(actionEnum != ProxyMessage.class, "When creating a new proxy listener put your own class that extend ProxyMessage there, not ProxyMessage class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "Proxy listener expects ProxyMessage to be an enum, given: " + actionEnum);

		try {
			return (ProxyMessage[]) actionEnum.getMethod("values").invoke(null);

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
	 * Shortcut for {@link ProxyServer#getInstance()}
	 *
	 * @return
	 */
	protected final ProxyServer getProxy() {
		return ProxyServer.getInstance();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ProxyListener && ((ProxyListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * Distributes received plugin message across all {@link ProxyListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final class ProxyListenerImpl implements Listener {

		private static boolean registered = false;

		public ProxyListenerImpl() {
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
			synchronized (DEFAULT_CHANNEL) {
				final Connection sender = event.getSender();
				final Connection receiver = event.getReceiver();
				final byte[] data = event.getData();

				if (event.isCancelled())
					return;

				// Check if the message is for a server (ignore client messages)
				if (!event.getTag().equals(DEFAULT_CHANNEL))
					return;

				// Check if a player is not trying to send us a fake message
				if (!(sender instanceof Server))
					return;

				// Read the plugin message
				final ByteArrayInputStream stream = new ByteArrayInputStream(data);
				ByteArrayDataInput input;

				try {
					input = ByteStreams.newDataInput(stream);

				} catch (final Throwable t) {
					input = ByteStreams.newDataInput(data);
				}

				final String channelName = input.readUTF();

				boolean handled = false;

				for (final ProxyListener listener : registeredListeners)
					if (channelName.equals(listener.getChannel())) {

						final UUID senderUid = UUID.fromString(input.readUTF());
						final String serverName = input.readUTF();
						final String actionName = input.readUTF();

						final ProxyMessage action = ProxyMessage.getByName(listener, actionName);
						Valid.checkNotNull(action, "Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, ensure all servers were updated and start it again.");

						final IncomingMessage message = new IncomingMessage(listener, senderUid, serverName, action, data, input, stream);

						listener.sender = (Server) sender;
						listener.receiver = receiver;
						listener.data = data;

						listener.onMessageReceived(listener.sender, message);

						handled = true;
					}

				if (handled)
					event.setCancelled(true);
			}
		}
	}
}
