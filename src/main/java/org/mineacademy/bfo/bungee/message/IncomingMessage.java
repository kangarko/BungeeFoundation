package org.mineacademy.bfo.bungee.message;

import java.util.UUID;

import org.mineacademy.bfo.CompressUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeAction;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.debug.Debugger;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.Server;

/**
 * Represents an incoming plugin message.
 *
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link BungeeAction} by its name *read automatically*.
 */
public final class IncomingMessage extends Message {

	/**
	 * The raw byte array to read from
	 */
	@Getter
	private final byte[] data;

	/**
	 * The input we use to read our data array
	 */
	private final ByteArrayDataInput input;

	/**
	 * Create a new incoming message from the given array
	 *
	 * NB: This uses the standardized Foundation model where the first
	 * string is the server name and the second string is the
	 * {@link BungeeAction} by its name *read automatically*.
	 *
	 * @param data
	 */
	public IncomingMessage(byte[] data) {
		this.data = data;
		this.input = ByteStreams.newDataInput(data);

		// -----------------------------------------------------------------
		// We are automatically reading the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		// Read sender UUID if any
		setSenderUid(input.readUTF());

		// Read server name
		setServerName(input.readUTF());

		// Read action
		setAction(input.readUTF());
	}

	/**
	 * Read UUID from the data
	 *
	 * @return
	 */
	public UUID readUUID() {
		moveHead(String.class);

		return UUID.fromString(input.readUTF());
	}

	/**
	 * Read map from the data
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		return SerializedMap.fromJson(this.readString());
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		moveHead(String.class);

		return CompressUtil.decompressB64(input.readUTF());
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		moveHead(Boolean.class);

		return input.readBoolean();
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		moveHead(Byte.class);

		return input.readByte();
	}

	/**
	 * Read a double from the data
	 *
	 * @return
	 */
	public double readDouble() {
		moveHead(Double.class);

		return input.readDouble();
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		moveHead(Float.class);

		return input.readFloat();
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int readInt() {
		moveHead(Integer.class);

		return input.readInt();
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		moveHead(Long.class);

		return input.readLong();
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		moveHead(Short.class);

		return input.readShort();
	}

	/**
	 * Forwards this message to another server, must be {@link ServerConnection}
	 *
	 * @param connection
	 */
	public void forward(Connection connection) {
		Valid.checkBoolean(connection instanceof Server, "Connection must be ProxiedPlayer");

		((Server) connection).sendData(getChannel(), data);
		Debugger.debug("bungee", "Forwarding data on " + getChannel() + " channel from " + getAction() + " to " + ((Server) connection).getInfo().getName() + " server.");
	}

	/**
	 * Forwards this message to all other servers except the senders one
	 *
	 * @param info
	 */
	public void forwardToOthers() {
		for (final ServerInfo server : ProxyServer.getInstance().getServers().values())
			if (!server.getName().equals(getServerName()))
				forward(server);
	}

	/**
	 * Forwards this message to all other servers including the senders one
	 *
	 * @param info
	 */
	public void forwardToAll() {
		for (final ServerInfo server : ProxyServer.getInstance().getServers().values())
			forward(server);
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param info
	 */
	public void forward(ServerInfo info) {
		info.sendData(getChannel(), data);

		Debugger.debug("bungee", "Forwarding data on " + getChannel() + " channel from " + getAction() + " to " + info.getName() + " server.");
	}
}