package com.kinegram.android.emrtdconnector.internal.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Binary protocol for file transfer and APDU messages over websocket.
 *
 * <p>
 * All messages start with a 1-byte identifier, followed by type-dependent data.
 *
 * <pre>
 * +--------+-----------------------+
 * | Type   | Type Dependent Data   |
 * | (1B)   | (var)                 |
 * +--------+-----------------------+
 * </pre>
 */
public class BinaryMessageProtocol {
	private static final byte MESSAGE_TYPE_FILE = 0x01;
	private static final byte MESSAGE_TYPE_APDU = 0x02;

	public interface Message {
	}

	/**
	 * A binary file with a unique name (i.e. "sod", "dg1", etc.).
	 *
	 * <pre>
	 * +--------+-----------+----------+----------+
	 * | Type   | Name Len  | Name     | Data     |
	 * | (1B)   | (1B)      | (var)    | (var)    |
	 * +--------+-----------+----------+----------+
	 * </pre>
	 *
	 * <ul>
	 *   <li><b>Type (1 byte):</b> Message type identifier (0x01 for file messages)</li>
	 *   <li><b>Name Length (1 byte):</b> Length of the file name in bytes (0-255)</li>
	 *   <li><b>Name (variable):</b> UTF-8 encoded file name/identifier</li>
	 *   <li><b>Data (variable):</b> Raw file data bytes</li>
	 * </ul>
	 */
	public static class FileMessage implements Message {
		private final String name;
		private final byte[] data;

		public FileMessage(String name, byte[] data) {
			this.name = name;
			this.data = data;
		}

		public String getName() {
			return name;
		}

		public byte[] getData() {
			return data;
		}

		private static FileMessage decode(ByteBuffer buffer) {
			if (buffer.remaining() < 2) {
				throw new IllegalArgumentException("Message too short");
			}

			byte messageType = buffer.get();
			if (messageType != MESSAGE_TYPE_FILE) {
				throw new IllegalArgumentException("Not a file message. Type: " + messageType);
			}

			byte nameLength = buffer.get();
			if (nameLength < 0) {
				throw new IllegalArgumentException("Invalid name length");
			}

			if (buffer.remaining() < nameLength) {
				throw new IllegalArgumentException("Insufficient data for name");
			}

			// Read name
			byte[] nameBytes = new byte[nameLength];
			buffer.get(nameBytes);
			String name = new String(nameBytes, StandardCharsets.UTF_8);

			// Rest is the file data
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);

			return new FileMessage(name, data);
		}

		/**
		 * Encode a file message.
		 *
		 * @param name The file identifier (e.g., "sod", "dg1")
		 * @param data The file data
		 * @return Encoded message as ByteBuffer
		 */
		public static ByteBuffer encode(String name, byte[] data) {
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

			if (nameBytes.length > 255) {
				throw new IllegalArgumentException("Name too long (max 255 bytes)");
			}

			ByteBuffer buffer = ByteBuffer.allocate(2 + nameBytes.length + data.length);
			buffer.put(MESSAGE_TYPE_FILE);
			buffer.put((byte) nameBytes.length);
			buffer.put(nameBytes);
			buffer.put(data);
			buffer.flip();

			return buffer;
		}
	}

	/**
	 * An APDU command if sent by the server or an APDU response if sent by the client.
	 *
	 * <pre>
	 * +--------+----------+
	 * | Type   | Data     |
	 * | (1B)   | (var)    |
	 * +--------+----------+
	 * </pre>
	 *
	 * <ul>
	 *   <li><b>Type (1 byte):</b> Message type identifier (0x02 for APDU)</li>
	 *   <li><b>Data (variable):</b> Raw APDU command/response bytes</li>
	 * </ul>
	 */
	public static class ApduMessage implements Message {
		private final byte[] data;

		public ApduMessage(byte[] data) {
			this.data = data;
		}

		public byte[] getData() {
			return data;
		}

		private static ApduMessage decode(ByteBuffer buffer) {
			if (buffer.remaining() < 1) {
				throw new IllegalArgumentException("Message too short");
			}

			byte messageType = buffer.get();
			if (messageType != MESSAGE_TYPE_APDU) {
				throw new IllegalArgumentException("Not an APDU message. Type: " + messageType);
			}

			// Rest is the APDU data
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);

			return new ApduMessage(data);
		}

		/**
		 * Encode an APDU message.
		 *
		 * @param data The APDU command/response data
		 * @return Encoded message as ByteBuffer
		 */
		public static ByteBuffer encode(byte[] data) {
			ByteBuffer buffer = ByteBuffer.allocate(1 + data.length);
			buffer.put(MESSAGE_TYPE_APDU);
			buffer.put(data);
			buffer.flip();

			return buffer;
		}
	}

	/**
	 * Decode a binary message.
	 *
	 * @param buffer The received ByteBuffer
	 * @return Decoded message
	 * @throws IllegalArgumentException if the message is malformed
	 */
	public static Message decode(ByteBuffer buffer) {
		if (!buffer.hasRemaining()) {
			throw new IllegalArgumentException("Empty message");
		}

		byte messageType = buffer.get(buffer.position());

		switch (messageType) {
			case MESSAGE_TYPE_FILE:
				return FileMessage.decode(buffer);
			case MESSAGE_TYPE_APDU:
				return ApduMessage.decode(buffer);
			default:
				throw new IllegalArgumentException("Unknown message type: " + messageType);
		}
	}

}
