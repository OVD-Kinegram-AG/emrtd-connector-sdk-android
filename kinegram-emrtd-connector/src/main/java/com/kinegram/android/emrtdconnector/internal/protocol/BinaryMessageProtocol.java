package com.kinegram.android.emrtdconnector.internal.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Binary protocol for file transfer messages over websocket.
 *
 * <p>Protocol format:
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
public class BinaryMessageProtocol {
	private static final byte MESSAGE_TYPE_FILE = 0x01;

	/**
	 * Encode a file message
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
