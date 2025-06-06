package com.kinegram.android.emrtdconnector.internal.protocol.exception;

public class WebsocketClientException extends RuntimeException {
	public WebsocketClientException(String message, Exception cause) {
		super(message, cause);
	}
}
