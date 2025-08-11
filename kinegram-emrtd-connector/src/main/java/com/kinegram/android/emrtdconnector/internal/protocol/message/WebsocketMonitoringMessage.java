package com.kinegram.android.emrtdconnector.internal.protocol.message;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from client to server used for monitoring.
 */
public class WebsocketMonitoringMessage extends WebsocketMessage {
	@NonNull
	public final String message;

	public WebsocketMonitoringMessage(@NonNull String message) {
		super(TYPE_MONITORING);
		this.message = message;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("message", message);
		return json;
	}

	public static WebsocketMonitoringMessage fromJson(JSONObject json) throws JSONException {
		return new WebsocketMonitoringMessage(json.getString("message"));
	}
}
