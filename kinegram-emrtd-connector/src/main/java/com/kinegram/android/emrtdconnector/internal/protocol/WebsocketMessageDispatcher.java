package com.kinegram.android.emrtdconnector.internal.protocol;

import android.util.Log;

import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketAcceptMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketChipAuthenticationHandbackMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketResultMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

/**
 * Consumes string and binary byte buffer messages from the websocket, parses them and then
 * dispatches them to the given handler.
 */
public class WebsocketMessageDispatcher {
    private static final String TAG = WebsocketMessageDispatcher.class.getSimpleName();

    private final WebsocketMessageHandler handler;

    public WebsocketMessageDispatcher(WebsocketMessageHandler handler) {
        this.handler = handler;
    }

    /**
     * To be called by the websocket client on receipt of a text (JSON) message
     */
    public void handleTextMessage(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            WebsocketMessage wsMsg = WebsocketMessage.fromJson(obj);
            switch (wsMsg.type) {
                case WebsocketMessage.TYPE_ACCEPT:
                    handler.onAccept((WebsocketAcceptMessage) wsMsg);
                    break;
                case WebsocketMessage.TYPE_CA_HANDBACK:
                    handler.onChipAuthenticationHandback((WebsocketChipAuthenticationHandbackMessage) wsMsg);
                    break;
                case WebsocketMessage.TYPE_RESULT:
                    handler.onResult((WebsocketResultMessage) wsMsg);
                    break;
                case WebsocketMessage.TYPE_CLOSE:
                    // NOOP - Only used in iOS
                    break;
                default:
                    handler.onUnknownMessage(wsMsg, null);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON message: " + message, e);
            handler.onUnknownMessage(message, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error handling text message", e);
            handler.onUnknownMessage(message, e);
        }
    }

    /**
     * To be called by the websocket client on receipt of a binary message
     */
    public void handleBinaryMessage(ByteBuffer binary) {
        try {
            BinaryMessageProtocol.Message msg = BinaryMessageProtocol.decode(binary);
            if (msg instanceof BinaryMessageProtocol.ApduMessage) {
                handler.onApduCommand((BinaryMessageProtocol.ApduMessage) msg);
            } else if (msg instanceof BinaryMessageProtocol.FileMessage) {
                handler.onFileReceived((BinaryMessageProtocol.FileMessage) msg);
            } else {
                handler.onUnknownMessage(msg, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse binary message", e);
            handler.onUnknownMessage(binary, e);
        }
    }

    public interface WebsocketMessageHandler {
        void onAccept(WebsocketAcceptMessage msg);

        void onChipAuthenticationHandback(WebsocketChipAuthenticationHandbackMessage msg);

        void onResult(WebsocketResultMessage msg);

        void onApduCommand(BinaryMessageProtocol.ApduMessage msg);

        void onFileReceived(BinaryMessageProtocol.FileMessage msg);

        void onUnknownMessage(Object msg, Exception parseException);
    }
}
