package org.mineacademy.bfo.bungee.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeAction;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;

/**
 *
 *
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link BungeeAction} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * Represents a UUID consisting of 0's only
	 */
	private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	/**
	 * The pending queue to write the message
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Construct a new outgoing packet with null UUID and empty server name
	 * 
	 * @param action
	 */
	public OutgoingMessage(BungeeAction action) {
		this(NULL_UUID, "", action);
	}

	/**
	 * Construct a new outgoing packet 
	 * 
	 * @param fromSenderUid
	 * @param fromServerName
	 * @param action
	 */
	public OutgoingMessage(UUID fromSenderUid, String fromServerName, BungeeAction action) {
		setSenderUid(fromSenderUid.toString());
		setServerName(fromServerName);
		setAction(action);

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		queue.add(fromSenderUid);
		queue.add(fromServerName);
		queue.add(action.name());
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
	 * Write an object of the given type into the message
	 *
	 * We move the head and ensure writing safety in accordance
	 * to the {@link BungeeAction#getContent()} length and
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
	 * The connection must be a {@link ServerConnection}!
	 *
	 * @param connection
	 */
	public void send(Connection connection) {
		Valid.checkBoolean(connection instanceof ServerConnection, "Connection must be ServerConnection");

		((ServerConnection) connection).sendData(getChannel(), compileData());
		Debugger.debug("bungee", "Sending data on " + getChannel() + " channel from " + getAction() + " to " + ((ServerConnection) connection).getInfo().getName() + " server.");
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
	public byte[] compileData() {
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

			else if (object instanceof UUID)
				out.writeUTF(object.toString());

			else if (object instanceof byte[])
				out.write((byte[]) object);

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + getChannel() + " with action " + getAction().toString());

		return out.toByteArray();
	}
}