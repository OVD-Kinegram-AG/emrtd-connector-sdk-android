package com.kinegram.android.emrtdconnector.internal.protocol.exception;

import java.io.IOException;

public class NfcException extends IOException {
	public NfcException(String message, Exception cause) {
		super(message, cause);
	}
}
