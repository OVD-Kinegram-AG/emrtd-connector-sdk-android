package com.kinegram.android.emrtdconnector.internal.protocol;

public enum ProtocolState {
	/**
	 * The START message has not been sent yet.
	 */
	INIT,
	/**
	 * The START message has been sent and the client is waiting for the ACCEPT
	 * message from the server.
	 */
	WAITING_FOR_ACCEPT,
	/**
	 * The server has sent the ACCEPT message.
	 */
	READING_CHIP,
	/**
	 * The client has sent a CA_HANDOVER message and is now waiting for the server to finish the
	 * Chip Authentication.
	 */
	WAITING_FOR_CA_HANDBACK,
	/**
	 * The chip has been read completely and the FINISHED message has been sent
	 * to the server.
	 */
	FINISHED,
	/**
	 * The websocket connection has been closed.
	 */
	CLOSED
}
