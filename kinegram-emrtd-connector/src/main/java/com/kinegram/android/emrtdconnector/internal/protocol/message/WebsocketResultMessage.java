package com.kinegram.android.emrtdconnector.internal.protocol.message;

import com.kinegram.android.emrtdconnector.EmrtdPassport;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message sent from server to the client if requested.
 */
public class WebsocketResultMessage extends WebsocketMessage {
    /**
     * The passport that is also sent to the result server.
     */
    public final EmrtdPassport passport;

    public WebsocketResultMessage(EmrtdPassport passport) {
        super(TYPE_RESULT);
        this.passport = passport;
    }

    @Override
    public JSONObject toJson() {
        throw new UnsupportedOperationException("Not supported");
    }

    public static WebsocketResultMessage fromJson(JSONObject json) throws JSONException {
        return new WebsocketResultMessage(new EmrtdPassport(json.getJSONObject("passport")));
    }
}
