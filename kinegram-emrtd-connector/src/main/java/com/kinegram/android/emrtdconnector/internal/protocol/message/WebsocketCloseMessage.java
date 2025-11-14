package com.kinegram.android.emrtdconnector.internal.protocol.message;


import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from server to client before closing the session.
 * <p>
 * Only relevant for iOS and can be ignored on Android.
 */
public class WebsocketCloseMessage extends WebsocketMessage {
    public final int code;

    @NonNull
    public final String reason;

    public WebsocketCloseMessage(int code, @NonNull String reason) {
        super(TYPE_CLOSE);
        this.code = code;
        this.reason = reason;
    }

    @Override
    public JSONObject toJson() {
        throw new UnsupportedOperationException("Not supported");
    }

    public static WebsocketCloseMessage fromJson(JSONObject json) throws JSONException {
        return new WebsocketCloseMessage(json.getInt("code"), json.getString("reason"));
    }
}
