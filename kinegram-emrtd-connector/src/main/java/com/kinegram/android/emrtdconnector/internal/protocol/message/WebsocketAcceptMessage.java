package com.kinegram.android.emrtdconnector.internal.protocol.message;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from server to client as a response to the START message.
 */
public class WebsocketAcceptMessage extends WebsocketMessage {
	/**
	 * Challenge for the chip to solve, if the chip supports active authentication (AA) and not chip
	 * authentication (which is preferred over AA).
	 */
	@NonNull
	public final byte[] activeAuthenticationChallenge;

	public WebsocketAcceptMessage(@NonNull byte[] activeAuthenticationChallenge) {
		super(TYPE_ACCEPT);
		this.activeAuthenticationChallenge = activeAuthenticationChallenge;
	}

	@Override
	public JSONObject toJson() {
		throw new UnsupportedOperationException("Not supported");
	}

	public static WebsocketAcceptMessage fromJson(JSONObject json) throws JSONException {
		return new WebsocketAcceptMessage(
			Base64.decode(json.getString("activeAuthenticationChallenge"), Base64.DEFAULT)
		);
	}
}
