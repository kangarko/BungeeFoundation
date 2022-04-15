package org.mineacademy.bfo.bungee.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeListener;
import org.mineacademy.bfo.bungee.BungeeMessageType;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

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

		setSenderUid(senderUid.toString());
		setServerName(server);
		setAction(action);

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		queue.add(senderUid);
		queue.add(getServerName());
		queue.add(getAction());
	}

	/**
	 * Write the given strings into the message
	 *
	 * @param messages
	 */
	public void writeString(String... messages) {
		for (final String message : messages)
			write(message, String.class);
	}

	/**
	 * Write a boolean into the message
	 *
	 * @param bool
	 */
	public void writeBoolean(boolean bool) {
		write(bool, Boolean.class);
	}

	/**
	 * Write a byte into the message
	 *
	 * @param number
	 */
	public void writeByte(byte number) {
		write(number, Byte.class);
	}

	/**
	 * Write a double into the message
	 *
	 * @param number
	 */
	public void writeDouble(double number) {
		write(number, Double.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeFloat(float number) {
		write(number, Float.class);
	}

	/**
	 * Write an integer into the message
	 *
	 * @param number
	 */
	public void writeInt(int number) {
		write(number, Integer.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeLong(long number) {
		write(number, Long.class);
	}

	/**
	 * Write a short into the message
	 *
	 * @param number
	 */
	public void writeShort(short number) {
		write(number, Short.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param uuid
	 */
	public void writeUUID(UUID uuid) {
		write(uuid, UUID.class);
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

		moveHead(typeOf);
		queue.add(object);
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

		((Server) connection).sendData(getChannel(), compileData());
		Debugger.debug("bungee", "Sending data on " + getChannel() + " channel from " + getAction() + " to " + ((Server) connection).getInfo().getName() + " server.");
	}

	/**
	 * Send this message with the current data for the given server info
	 *
	 * @param server
	 */
	public void send(ServerInfo server) {
		server.sendData(getChannel(), compileData());

		Debugger.debug("bungee", "Sending data on " + getChannel() + " channel from " + getAction() + " to " + server.getName() + " server.");
	}

	/**
	 * Delegate write methods for the byte array data output
	 * based on the queue
	 *
	 * @return
	 */
	private byte[] compileData() {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		for (final Object object : queue)
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

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + getChannel() + " with action " + getAction().toString());

		return out.toByteArray();
	}
}