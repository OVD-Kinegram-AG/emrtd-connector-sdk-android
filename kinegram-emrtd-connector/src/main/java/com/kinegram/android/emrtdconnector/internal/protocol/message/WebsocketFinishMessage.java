package com.kinegram.android.emrtdconnector.internal.protocol.message;

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

	public WebsocketFinishMessage(boolean sendResult) {
		super(TYPE_FINISH);
		this.sendResult = sendResult;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("sendResult", sendResult);
		return json;
	}

	public static WebsocketFinishMessage fromJson(JSONObject json) throws JSONException {
		return new WebsocketFinishMessage(json.getBoolean("sendResult"));
	}
}
