package com.kinegram.android.emrtdconnector.internal.protocol;

public enum ProtocolState {
	/**
	 * The START message has not been sent yet.
	 */
	START_NOT_SENT,
	/**
	 * The START message has been sent and the client is waiting for the ACCEPT
	 * message from the server.
	 */
	WAIT_FOR_ACCEPT,
	/**
	 * The server has sent the ACCEPT message.
	 */
	ACCEPTED,
	/**
	 * The chip has been read completely and the FINISHED message has been sent
	 * to the server.
	 */
	FINISHED
}
