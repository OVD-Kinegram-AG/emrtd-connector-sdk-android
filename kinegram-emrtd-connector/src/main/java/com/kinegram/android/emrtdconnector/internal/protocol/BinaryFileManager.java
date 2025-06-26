package com.kinegram.android.emrtdconnector.internal.protocol;

import org.java_websocket.client.WebSocketClient;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and sends binary files (sod, data groups, etc.).
 */
public class BinaryFileManager {
	private final Set<String> sentFiles = ConcurrentHashMap.newKeySet();
	private final Map<String, byte[]> receivedFiles = new ConcurrentHashMap<>();
	private final WebSocketClient websocketClient;

	public BinaryFileManager(WebSocketClient webSocketClient) {
		this.websocketClient = webSocketClient;
	}

	public void sendFile(String name, byte[] data) {
		if (shouldSendFile(name)) {
			websocketClient.send(BinaryMessageProtocol.FileMessage.encode(name, data));
			sentFiles.add(name);
		}
	}

	private boolean shouldSendFile(String name) {
		// We MUST NOT send files that the server already has
		return !sentFiles.contains(name) && !receivedFiles.containsKey(name);
	}

	public void receiveFile(String name, byte[] data) {
		receivedFiles.put(name, data);
	}

	public Optional<byte[]> getReceivedFile(String name) {
		return Optional.ofNullable(receivedFiles.get(name));
	}
}
