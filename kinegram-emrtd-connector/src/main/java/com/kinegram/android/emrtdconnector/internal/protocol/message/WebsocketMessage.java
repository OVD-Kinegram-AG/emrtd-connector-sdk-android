package com.kinegram.android.emrtdconnector.internal.protocol.message;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class WebsocketMessage {
	public static final String TYPE_START = "START";
	public static final String TYPE_ACCEPT = "ACCEPT";
	public static final String TYPE_FINISH = "FINISH";
	public static final String TYPE_RESULT = "RESULT";
	public static final String TYPE_CLOSE = "CLOSE";

	public final String type;

	protected WebsocketMessage(String type) {
		this.type = type;
	}

	public abstract JSONObject toJson() throws JSONException;

	public static WebsocketMessage fromJson(JSONObject json) throws JSONException {
		String type = json.getString("type");

		switch (type) {
			case TYPE_START:
				return WebsocketStartMessage.fromJson(json);
			case TYPE_ACCEPT:
				return WebsocketAcceptMessage.fromJson(json);
			case TYPE_FINISH:
				return WebsocketFinishMessage.fromJson(json);
			case TYPE_CLOSE:
				return WebsocketCloseMessage.fromJson(json);
			case TYPE_RESULT:
				return WebsocketResultMessage.fromJson(json);
			default:
				return new WebsocketUnknownMessage(type, json);
		}
	}
}
