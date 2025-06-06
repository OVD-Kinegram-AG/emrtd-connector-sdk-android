package com.kinegram.android.emrtdconnector.internal.protocol.message;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from client to server to start a validation session.
 */
public class WebsocketStartMessage extends WebsocketMessage {
	@NonNull
	public final String validationId;
	@NonNull
	public final String clientId;
	@NonNull
	public final String platform;
	public final boolean nfcAdapterSupportsExtendedLength;
	public final boolean enableDiagnostics;

	public WebsocketStartMessage(
		@NonNull String validationId,
		@NonNull String clientId,
		@NonNull String platform,
		boolean nfcAdapterSupportsExtendedLength,
		boolean enableDiagnostics) {
		super(TYPE_START);
		this.validationId = validationId;
		this.clientId = clientId;
		this.platform = platform;
		this.nfcAdapterSupportsExtendedLength = nfcAdapterSupportsExtendedLength;
		this.enableDiagnostics = enableDiagnostics;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("validationId", validationId);
		json.put("clientId", clientId);
		json.put("platform", platform);
		json.put("nfcAdapterSupportsExtendedLength", nfcAdapterSupportsExtendedLength);
		json.put("enableDiagnostics", enableDiagnostics);
		return json;
	}

	public static WebsocketStartMessage fromJson(JSONObject json) throws JSONException {
		return new WebsocketStartMessage(
			json.getString("validationId"),
			json.getString("clientId"),
			json.getString("platform"),
			json.getBoolean("nfcAdapterSupportsExtendedLength"),
			json.getBoolean("enableDiagnostics")
		);
	}
}
