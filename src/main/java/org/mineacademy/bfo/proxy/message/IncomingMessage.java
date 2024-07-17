package org.mineacademy.bfo.proxy.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.model.SimpleComponent;
import org.mineacademy.bfo.proxy.ProxyListener;
import org.mineacademy.bfo.proxy.ProxyMessage;
import org.mineacademy.bfo.remain.Remain;

import com.google.common.io.ByteArrayDataInput;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.config.ServerInfo;

/**
 * Represents an incoming plugin message.
 * <p>
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link ProxyMessage} by its name *read automatically*.
 */
public final class IncomingMessage extends Message {

	/**
	 * The raw byte array to read from
	 */
	@Getter
	private final byte[] data;

	/**
	 * The sender UUID
	 */
	@Getter
	private final UUID senderUid;

	/**
	 * The serverName
	 */
	@Getter
	private final String serverName;

	/**
	 * The input we use to read our data array
	 */
	private final ByteArrayDataInput input;

	/**
	 * The internal stream
	 */
	private final ByteArrayInputStream stream;

	/**
	 * Create a new incoming message from the given array
	 *
	 * NB: This uses the standardized Foundation header:
	 *
	 * 1. Channel name (string) (because we broadcast on BungeeCord channel)
	 * 2. Sender UUID (string)
	 * 3. Server name (string)
	 * 4  Action (String converted to enum of {@link ProxyMessage})
	 *
	 * @param listener
	 * @param senderUid
	 * @param serverName
	 * @param type
	 * @param data
	 * @param input
	 * @param stream
	 */
	public IncomingMessage(ProxyListener listener, UUID senderUid, String serverName, ProxyMessage type, byte[] data, ByteArrayDataInput input, ByteArrayInputStream stream) {
		super(listener, type);

		this.data = data;
		this.senderUid = senderUid;
		this.serverName = serverName;
		this.input = input;
		this.stream = stream;
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		return this.readCompressedString();
	}

	/**
	 * Read a component from the string data if json
	 *
	 * @return
	 */
	public Component readComponent() {
		this.moveHead(Component.class);

		return Remain.convertJsonToAdventure(this.readCompressedString());
	}

	/**
	 * Read a simple component from the string data if json
	 *
	 * @return
	 */
	public SimpleComponent readSimpleComponent() {
		this.moveHead(SimpleComponent.class);

		return SimpleComponent.fromJson(this.readCompressedString());
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(SerializedMap.class);

		return SerializedMap.fromJson(this.readCompressedString());
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

	/*
	 * Helper util to read the next compressed string
	 */
	private String readCompressedString() {
		final int length = this.input.readInt();
		final byte[] compressed = new byte[length];

		this.input.readFully(compressed);

		return Common.decompress(compressed);
	}

	/**
	 *
	 * @return
	 */
	public String getChannel() {
		return this.getListener().getChannel();
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param info
	 */
	public void forward(ServerInfo info) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			if (info.getPlayers().isEmpty()) {
				Debugger.debug("proxy", "NOT sending data on " + this.getChannel() + " channel from " + this.getMessage() + " to " + info.getName() + " server because it is empty.");

				return;
			}

			if (this.data.length > 32_700) { // Safety margin
				Common.log("[incoming] Outgoing proxy message was oversized, not sending to " + info.getName() + ". Max length: 32766 bytes, got " + this.data.length + " bytes.");

				return;
			}

			info.sendData(ProxyListener.DEFAULT_CHANNEL, this.data);
			Debugger.debug("proxy", "Forwarding data on " + this.getChannel() + " channel from " + this.getMessage() + " to " + info.getName() + " server.");
		}
	}
}