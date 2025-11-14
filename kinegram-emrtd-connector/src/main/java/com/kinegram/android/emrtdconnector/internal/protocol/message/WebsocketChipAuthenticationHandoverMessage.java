package com.kinegram.android.emrtdconnector.internal.protocol.message;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from client to server to hand over Chip Authentication to the server.
 */
public class WebsocketChipAuthenticationHandoverMessage extends WebsocketMessage {
    public final int maxTransceiveLengthForSecureMessaging;
    public final int maxBlockSize;
    @NonNull
    public final SecureMessagingInfo secureMessagingInfo;

    public WebsocketChipAuthenticationHandoverMessage(
        int maxTransceiveLengthForSecureMessaging,
        int maxBlockSize,
        @NonNull SecureMessagingInfo secureMessagingInfo
    ) {
        super(TYPE_CA_HANDOVER);
        this.maxTransceiveLengthForSecureMessaging = maxTransceiveLengthForSecureMessaging;
        this.maxBlockSize = maxBlockSize;
        this.secureMessagingInfo = secureMessagingInfo;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("maxTransceiveLengthForSecureMessaging", maxTransceiveLengthForSecureMessaging);
        json.put("maxBlockSize", maxBlockSize);
        json.put("secureMessagingInfo", secureMessagingInfo.toJson());
        return json;
    }

    public static WebsocketAcceptMessage fromJson(JSONObject json) {
        throw new UnsupportedOperationException("Not supported");
    }
}
