package org.mineacademy.bfo.bungee.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeListener;
import org.mineacademy.bfo.bungee.BungeeMessageType;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.Server;

/**
 * Represents an incoming plugin message.
 * <p>
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link BungeeMessageType} by its name *read automatically*.
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
	 * The internal stream
	 */
	private final ByteArrayInputStream stream;

	/**
	 * The channel name
	 */
	@Getter
	private final String channel;

	/**
	 * Create a new incoming message from the given array
	 * <p>
	 * NB: This uses the standardized Foundation model where the first
	 * string is the server name and the second string is the
	 * {@link BungeeMessageType} by its name *read automatically*.
	 *
	 * @param data
	 */
	public IncomingMessage(String channel, byte[] data) {
		this(SimplePlugin.getInstance().getBungeeCord(), channel, data);
	}

	/**
	 * Create a new incoming message from the given array
	 * <p>
	 * NB: This uses the standardized Foundation model where the first
	 * string is the server name and the second string is the
	 * {@link BungeeMessageType} by its name *read automatically*.
	 *
	 * @param listener
	 * @param data
	 */
	public IncomingMessage(BungeeListener listener, byte[] data) {
		this(listener, listener.getChannel(), data);
	}

	private IncomingMessage(BungeeListener listener, String channel, byte[] data) {
		super(listener);

		this.channel = channel;
		this.data = data;
		this.stream = new ByteArrayInputStream(data);
		this.input = ByteStreams.newDataInput(this.stream);

		// -----------------------------------------------------------------
		// We are automatically reading the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		// Read senders UUID
		this.setSenderUid(this.input.readUTF());

		// Read server name
		this.setServerName(this.input.readUTF());

		// Read action
		this.setAction(this.input.readUTF());
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		return this.input.readUTF();
	}

	/**
	 * Read a UUID from the string data
	 *
	 * @return
	 */
	public UUID readUUID() {
		this.moveHead(UUID.class);

		return UUID.fromString(this.input.readUTF());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(String.class);

		return SerializedMap.fromJson(this.input.readUTF());
	}

	/**
	 * Read an enumerator from the given string data
	 *
	 * @param <T>
	 * @param typeOf
	 * @return
	 */
	public <T extends Enum<T>> T readEnum(Class<T> typeOf) {
		this.moveHead(typeOf);

		return ReflectionUtil.lookupEnum(typeOf, this.input.readUTF());
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		this.moveHead(Boolean.class);

		return this.input.readBoolean();
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		this.moveHead(Byte.class);

		return this.input.readByte();
	}

	/**
	 * Reads the rest of the bytes
	 *
	 * @return
	 */
	public byte[] readBytes() {
		this.moveHead(byte[].class);

		final byte[] array = new byte[this.stream.available()];

		try {
			this.stream.read(array);

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return array;
	}

	/**
	 * Read a double from the data
	 *
	 * @return
	 */
	public double readDouble() {
		this.moveHead(Double.class);

		return this.input.readDouble();
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		this.moveHead(Float.class);

		return this.input.readFloat();
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int readInt() {
		this.moveHead(Integer.class);

		return this.input.readInt();
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		this.moveHead(Long.class);

		return this.input.readLong();
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		this.moveHead(Short.class);

		return this.input.readShort();
	}

	/**
	 * Forwards this message to another server, must be {@link Server}
	 *
	 * @param connection
	 */
	public void forward(Connection connection) {
		Valid.checkBoolean(connection instanceof Server, "Connection must be Server");
		final Server server = (Server) connection;

		if (server.getInfo().getPlayers().isEmpty()) {
			Debugger.debug("bungee", "NOT sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + server.getInfo().getName() + " server because it is empty.");

			return;
		}

		server.sendData(this.getChannel(), this.data);
		Debugger.debug("bungee", "Forwarding data on " + this.getChannel() + " channel from " + this.getAction() + " to " + ((Server) connection).getInfo().getName() + " server.");
	}

	/**
	 * Forwards this message to all other servers except the senders one
	 *
	 */
	public void forwardToOthers() {
		for (final ServerInfo server : ProxyServer.getInstance().getServers().values())
			if (!server.getName().equals(this.getServerName()))
				this.forward(server);
	}

	/**
	 * Forwards this message to all other servers including the senders one
	 *
	 */
	public void forwardToAll() {
		for (final ServerInfo server : ProxyServer.getInstance().getServers().values())
			this.forward(server);
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param info
	 */
	public void forward(ServerInfo info) {

		if (info.getPlayers().isEmpty()) {
			Debugger.debug("bungee", "NOT sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + info.getName() + " server because it is empty.");

			return;
		}

		info.sendData(this.getChannel(), this.data);
		Debugger.debug("bungee", "Forwarding data on " + this.getChannel() + " channel from " + this.getAction() + " to " + info.getName() + " server.");
	}
}