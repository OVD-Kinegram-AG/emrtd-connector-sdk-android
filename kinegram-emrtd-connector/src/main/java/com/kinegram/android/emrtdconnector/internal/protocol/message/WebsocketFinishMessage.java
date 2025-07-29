package com.kinegram.android.emrtdconnector.internal.protocol.message;

import android.util.Base64;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from client to the server when the client finished reading the whole chip.
 */
public class WebsocketFinishMessage extends WebsocketMessage {
	/**
	 * When {@code true}, the client expects the server to send a RESULT message.
	 */
	public final boolean sendResult;

	/**
	 * The solved challenge from the ACCEPT message.
	 */
	@Nullable
	public final byte[] activeAuthenticationSignature;

	public WebsocketFinishMessage(
		boolean sendResult,
		@Nullable byte[] activeAuthenticationSignature) {
		super(TYPE_FINISH);
		this.sendResult = sendResult;
		this.activeAuthenticationSignature = activeAuthenticationSignature;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		return new JSONObject()
			.put("type", type)
			.put("sendResult", sendResult)
			.put("activeAuthenticationSignature", activeAuthenticationSignature == null
				? null :
				Base64.encodeToString(activeAuthenticationSignature, Base64.NO_WRAP));
	}

	public static WebsocketFinishMessage fromJson(JSONObject json) throws JSONException {
		throw new UnsupportedOperationException("Not supported");
	}
}
