package com.kinegram.android.emrtdconnector.internal;

import android.util.Base64;

import com.kinegram.android.emrtdconnector.EmrtdConnector;
import com.kinegram.android.emrtdconnector.internal.protocol.BinaryMessageProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Supplier;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * A WebSocket client that automatically adds OpenTelemetry tracing to all send and receive operations.
 * <p>
 * This class extends {@link AndroidWebsocketClient} and provides transparent tracing for WebSocket
 * communication using OpenTelemetry.
 */
public abstract class TracedAndroidWebSocketClient extends AndroidWebsocketClient {
    private final Supplier<Span> parentSpanSupplier;
    private final boolean includeSensitiveData;

    /**
     * Creates a new TracedWebSocketClient.
     *
     * @param uri                  The WebSocket URI to connect to.
     * @param headers              HTTP headers for the WebSocket handshake.
     * @param parentSpanSupplier   Supplier that provides the parent span for trace hierarchy.
     * @param includeSensitiveData If potentially sensitive data should be included.
     */
    public TracedAndroidWebSocketClient(URI uri, Map<String, String> headers, Supplier<Span> parentSpanSupplier, boolean includeSensitiveData) {
        super(uri, headers);
        this.parentSpanSupplier = parentSpanSupplier;
        this.includeSensitiveData = includeSensitiveData;
    }

    @Override
    public void send(String message) {
        Span parentSpan = parentSpanSupplier != null ? parentSpanSupplier.get() : null;
        try (Scope ignored = parentSpan == null ? null : parentSpan.makeCurrent()) {
            Span msgSpan = EmrtdConnector.getTracer().spanBuilder("websocket_transmit")
                    .setAttribute("messaging.operation.type", "send")
                    .setAttribute("messaging.system", "websocket")
                    .setAttribute("messaging.message.body.size", message.length())
                    .startSpan();

            if (includeSensitiveData) {
                msgSpan.setAttribute("messaging.message.body.content", message);
            }

            try (Scope ignored2 = msgSpan.makeCurrent()) {
                // Auto-detect message type from JSON
                try {
                    JSONObject json = new JSONObject(message);
                    if (json.has("type")) {
                        msgSpan.setAttribute("messaging.message.type", json.getString("type"));
                    }
                } catch (JSONException ignored3) {
                    // Not JSON or malformed, skip type detection
                }

                super.send(message);
                msgSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                msgSpan.recordException(e);
                msgSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                msgSpan.end();
            }
        }
    }

    @Override
    public void send(ByteBuffer binary) {
        Span parentSpan = parentSpanSupplier != null ? parentSpanSupplier.get() : null;
        try (Scope ignored = parentSpan == null ? null : parentSpan.makeCurrent()) {
            Span msgSpan = EmrtdConnector.getTracer().spanBuilder("websocket_transmit")
                    .setAttribute("messaging.operation.type", "send")
                    .setAttribute("messaging.system", "websocket")
                    .setAttribute("messaging.message.body.size", binary.remaining())
                    .startSpan();

            if (includeSensitiveData) {
                byte[] data = new byte[binary.remaining()];
                binary.duplicate().get(data);
                msgSpan.setAttribute("messaging.message.body.content_base64", Base64.encodeToString(data,
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
            }

            try (Scope ignored2 = msgSpan.makeCurrent()) {
                // Try to detect binary message type
                try {
                    BinaryMessageProtocol.Message msg = BinaryMessageProtocol.decode(binary.duplicate());
                    if (msg instanceof BinaryMessageProtocol.ApduMessage) {
                        msgSpan.setAttribute("messaging.message.type", "apdu");
                        msgSpan.setAttribute("apdu.size", ((BinaryMessageProtocol.ApduMessage) msg).getData().length);
                    } else if (msg instanceof BinaryMessageProtocol.FileMessage) {
                        BinaryMessageProtocol.FileMessage fileMsg = (BinaryMessageProtocol.FileMessage) msg;
                        msgSpan.setAttribute("messaging.message.type", "file");
                        msgSpan.setAttribute("file.name", fileMsg.getName());
                        msgSpan.setAttribute("file.size", fileMsg.getData().length);
                    } else {
                        msgSpan.setAttribute("messaging.message.type", msg.getClass().getSimpleName());
                    }
                } catch (Exception ignored3) {
                    // Failed to decode, skip type detection
                }

                super.send(binary);
                msgSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                msgSpan.recordException(e);
                msgSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                msgSpan.end();
            }
        }
    }

    @Override
    public void onMessage(String message) {
        Span parentSpan = parentSpanSupplier != null ? parentSpanSupplier.get() : null;
        try (Scope ignored = parentSpan == null ? null : parentSpan.makeCurrent()) {
            Span msgSpan = EmrtdConnector.getTracer().spanBuilder("websocket_receive")
                    .setAttribute("messaging.operation.type", "receive")
                    .setAttribute("messaging.system", "websocket")
                    .setAttribute("messaging.message.body.size", message.length())
                    .startSpan();

            if (includeSensitiveData) {
                msgSpan.setAttribute("messaging.message.body.content", message);
            }

            try (Scope ignored2 = msgSpan.makeCurrent()) {
                // Auto-detect message type from JSON
                try {
                    JSONObject json = new JSONObject(message);
                    if (json.has("type")) {
                        msgSpan.setAttribute("messaging.message.type", json.getString("type"));
                    }
                    msgSpan.addEvent("message_parsed");
                } catch (JSONException ignored3) {
                    // Failed to decode, skip type detection
                }

                handleIncomingMessage(message);
                msgSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                msgSpan.recordException(e);
                msgSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                msgSpan.end();
            }
        }
    }

    @Override
    public void onMessage(ByteBuffer binary) {
        Span parentSpan = parentSpanSupplier != null ? parentSpanSupplier.get() : null;
        try (Scope ignored = parentSpan == null ? null : parentSpan.makeCurrent()) {
            Span msgSpan = EmrtdConnector.getTracer().spanBuilder("websocket_receive")
                    .setAttribute("messaging.operation.type", "receive")
                    .setAttribute("messaging.system", "websocket")
                    .setAttribute("messaging.message.body.size", binary.remaining())
                    .startSpan();

            if (includeSensitiveData) {
                byte[] data = new byte[binary.remaining()];
                binary.duplicate().get(data);
                msgSpan.setAttribute("messaging.message.body.content_base64", Base64.encodeToString(data,
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
            }

            try (Scope ignored2 = msgSpan.makeCurrent()) {
                // Try to detect binary message type
                try {
                    BinaryMessageProtocol.Message msg = BinaryMessageProtocol.decode(binary.duplicate());
                    if (msg instanceof BinaryMessageProtocol.ApduMessage) {
                        msgSpan.setAttribute("messaging.message.type", "apdu");
                        msgSpan.setAttribute("apdu.size", ((BinaryMessageProtocol.ApduMessage) msg).getData().length);
                    } else if (msg instanceof BinaryMessageProtocol.FileMessage) {
                        BinaryMessageProtocol.FileMessage fileMsg = (BinaryMessageProtocol.FileMessage) msg;
                        msgSpan.setAttribute("messaging.message.type", "file");
                        msgSpan.setAttribute("file.name", fileMsg.getName());
                        msgSpan.setAttribute("file.size", fileMsg.getData().length);
                    } else {
                        msgSpan.setAttribute("messaging.message.type", msg.getClass().getSimpleName());
                    }
                } catch (Exception ignored3) {
                    // Failed to decode, skip type detection
                }

                handleIncomingMessage(binary);
                msgSpan.setStatus(StatusCode.OK);
            } catch (Exception e) {
                msgSpan.recordException(e);
                msgSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                msgSpan.end();
            }
        }
    }

    /**
     * Called when a text message is received from the WebSocket.
     * Subclasses should implement this method to handle incoming text messages.
     *
     * @param message The received text message
     */
    protected abstract void handleIncomingMessage(String message);

    /**
     * Called when a binary message is received from the WebSocket.
     * Subclasses should implement this method to handle incoming binary messages.
     *
     * @param binary The received binary message
     */
    protected abstract void handleIncomingMessage(ByteBuffer binary);
}
