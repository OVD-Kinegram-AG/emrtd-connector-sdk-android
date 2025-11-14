package com.kinegram.android.emrtdconnector.internal.protocol.message;

import org.json.JSONObject;

/**
 * Message of unknown type.
 */
public class WebsocketUnknownMessage extends WebsocketMessage {
    public final JSONObject originalJson;

    public WebsocketUnknownMessage(String type, JSONObject originalJson) {
        super(type);
        this.originalJson = originalJson;
    }

    @Override
    public JSONObject toJson() {
        return originalJson;
    }
}
