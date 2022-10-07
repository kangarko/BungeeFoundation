package org.mineacademy.bfo.bungee.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeListener;
import org.mineacademy.bfo.bungee.BungeeMessageType;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link BungeeMessageType} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * The pending queue to write the message
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Construct a new outgoing packet with null UUID and empty server name
	 *
	 * @param action
	 */
	public OutgoingMessage(BungeeMessageType action) {
		this(UUID.fromString("00000000-0000-0000-0000-000000000000"), "", action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param senderUid
	 * @param server
	 * @param action
	 */
	public OutgoingMessage(UUID senderUid, String server, BungeeMessageType action) {
		this(SimplePlugin.getInstance().getBungeeCord(), server, senderUid, action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param server
	 * @param senderUid
	 * @param action
	 */
	public OutgoingMessage(BungeeListener listener, String server, UUID senderUid, BungeeMessageType action) {
		super(listener);

		this.setSenderUid(senderUid.toString());
		this.setServerName(server);
		this.setAction(action);

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		this.queue.add(listener.getChannel());
		this.queue.add(senderUid);
		this.queue.add(this.getServerName());
		this.queue.add(this.getAction().name());
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map.toJson(), String.class);
	}

	/**
	 * Write the given strings into the message
	 *
	 * @param messages
	 */
	public void writeString(String... messages) {
		for (final String message : messages)
			this.write(message, String.class);
	}

	/**
	 * Write a boolean into the message
	 *
	 * @param bool
	 */
	public void writeBoolean(boolean bool) {
		this.write(bool, Boolean.class);
	}

	/**
	 * Write a byte into the message
	 *
	 * @param number
	 */
	public void writeByte(byte number) {
		this.write(number, Byte.class);
	}

	/**
	 * Write a double into the message
	 *
	 * @param number
	 */
	public void writeDouble(double number) {
		this.write(number, Double.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeFloat(float number) {
		this.write(number, Float.class);
	}

	/**
	 * Write an integer into the message
	 *
	 * @param number
	 */
	public void writeInt(int number) {
		this.write(number, Integer.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeLong(long number) {
		this.write(number, Long.class);
	}

	/**
	 * Write a short into the message
	 *
	 * @param number
	 */
	public void writeShort(short number) {
		this.write(number, Short.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param uuid
	 */
	public void writeUUID(UUID uuid) {
		this.write(uuid, UUID.class);
	}

	/**
	 * Write an object of the given type into the message
	 * <p>
	 * We move the head and ensure writing safety in accordance
	 * to the {@link BungeeMessageType#getContent()} length and
	 * data type at the given position
	 *
	 * @param object
	 * @param typeOf
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Send this message with the current data for the given connection
	 * The connection must be a {@link Server} or {@link ProxiedPlayer}!
	 *
	 * @param connection
	 */
	public void send(Connection connection) {

		if (connection instanceof ProxiedPlayer)
			connection = ((ProxiedPlayer) connection).getServer();

		Valid.checkBoolean(connection instanceof Server, "Connection must be ServerConnection");

		if (((Server) connection).getInfo().getPlayers().isEmpty()) {
			Debugger.debug("bungee", "NOT sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + ((Server) connection).getInfo().getName() + " server because it is empty.");

			return;
		}

		((Server) connection).sendData("BungeeCord", this.compileData());
		Debugger.debug("bungee", "Sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + ((Server) connection).getInfo().getName() + " server.");
	}

	/**
	 * Send this message with the current data for the given server info
	 *
	 * @param server
	 */
	public void send(ServerInfo server) {

		if (server.getPlayers().isEmpty()) {
			Debugger.debug("bungee", "NOT sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + server.getName() + " server because it is empty.");

			return;
		}

		server.sendData("BungeeCord", this.compileData());
		Debugger.debug("bungee", "Sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + server.getName() + " server.");
	}

	/**
	 * Broadcasts the message to all servers except the one ignored
	 *
	 * @param ignoredServerName
	 */
	public void broadcastExcept(@NonNull String ignoredServerName) {
		this.broadcast(ignoredServerName);
	}

	/**
	 * Broadcasts the message to all servers
	 */
	public void broadcast() {
		this.broadcast(null);
	}

	/*
	 * Helper method to broadcast
	 */
	private void broadcast(@Nullable String ignoredServerName) {
		String channel = this.getChannel();
		byte[] data = this.compileData();

		for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
			if (server.getPlayers().isEmpty()) {
				Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this.getAction() + " to " + server.getName() + " server because it is empty.");

				continue;
			}

			if (ignoredServerName != null && server.getName().equalsIgnoreCase(ignoredServerName)) {
				Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this.getAction() + " to " + server.getName() + " server because it is ignored.");

				continue;
			}

			server.sendData("BungeeCord", data);
			Debugger.debug("bungee", "Sending data on " + channel + " channel from " + this.getAction() + " to " + server.getName() + " server.");
		}
	}

	/**
	 * Delegate write methods for the byte array data output
	 * based on the queue
	 *
	 * @return
	 */
	public byte[] compileData() {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		for (final Object object : this.queue)
			if (object instanceof String)
				out.writeUTF((String) object);

			else if (object instanceof Boolean)
				out.writeBoolean((Boolean) object);

			else if (object instanceof Byte)
				out.writeByte((Byte) object);

			else if (object instanceof Double)
				out.writeDouble((Double) object);

			else if (object instanceof Float)
				out.writeFloat((Float) object);

			else if (object instanceof Integer)
				out.writeInt((Integer) object);

			else if (object instanceof Long)
				out.writeLong((Long) object);

			else if (object instanceof Short)
				out.writeShort((Short) object);

			else if (object instanceof byte[])
				out.write((byte[]) object);

			else if (object instanceof UUID)
				out.writeUTF(object.toString());

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());

		return out.toByteArray();
	}
}