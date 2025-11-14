package com.kinegram.android.emrtdconnector.internal.protocol.message;

import androidx.annotation.NonNull;

import com.kinegram.emrtd.CheckResult;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from server to client when it finished Chip Authentication.
 */
public class WebsocketChipAuthenticationHandbackMessage extends WebsocketMessage {
    @NonNull
    public final SecureMessagingInfo secureMessagingInfo;

    @NonNull
    public CheckResult checkResult;

    public WebsocketChipAuthenticationHandbackMessage(
        @NonNull SecureMessagingInfo secureMessagingInfo,
        @NonNull CheckResult checkResult
    ) {
        super(TYPE_CA_HANDBACK);
        this.secureMessagingInfo = secureMessagingInfo;
        this.checkResult = checkResult;
    }

    @Override
    public JSONObject toJson() {
        throw new UnsupportedOperationException("Not supported");
    }

    public static WebsocketChipAuthenticationHandbackMessage fromJson(
        JSONObject json) throws JSONException {
        return new WebsocketChipAuthenticationHandbackMessage(
            SecureMessagingInfo.fromJson(json.getJSONObject("secureMessagingInfo")),
            CheckResult.valueOf(json.getString("checkResult"))
        );
    }
}
