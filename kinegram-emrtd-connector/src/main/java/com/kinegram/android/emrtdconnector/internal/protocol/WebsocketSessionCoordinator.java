package com.kinegram.android.emrtdconnector.internal.protocol;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.kinegram.android.emrtdconnector.ClosedListener;
import com.kinegram.android.emrtdconnector.ConnectionOptions;
import com.kinegram.android.emrtdconnector.EmrtdConnector;
import com.kinegram.android.emrtdconnector.EmrtdPassport;
import com.kinegram.android.emrtdconnector.StatusListener;
import com.kinegram.android.emrtdconnector.internal.TracedAndroidWebSocketClient;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.NfcException;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.WebsocketClientException;
import com.kinegram.android.emrtdconnector.internal.protocol.message.*;
import com.kinegram.emrtd.EmrtdResult;
import com.kinegram.emrtd.EmrtdStep;
import com.kinegram.emrtd.RemoteChipAuthentication;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.protocol.SecureMessagingWrapper;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

public class WebsocketSessionCoordinator {
	private static final String TAG = WebsocketSessionCoordinator.class.getSimpleName();
	private static final int WEBSOCKET_TIMEOUT_SECONDS = 2000;

	private volatile ProtocolState state = ProtocolState.INIT;

	private final IsoDep isoDep;
	private final ConnectionOptions options;
	private final StatusListener statusListener;
	private final ClosedListener closedListener;
	private final Consumer<EmrtdPassport> emrtdPassportListener;
	private final Consumer<Exception> errorListener;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final WebSocketClient websocketClient;
	private final BinaryFileManager fileManager;
	private EmrtdChipSession chipSession;
	private final WebsocketMessageDispatcher dispatcher;
	private final String clientId;
	private final URI webSocketUri;

	private final CompletableFuture<RemoteChipAuthentication.Result> caHandbackFuture = new CompletableFuture<>();

	private Span sessionSpan;
	private Span connectSpan;


	public WebsocketSessionCoordinator(
		@NonNull IsoDep isoDep,
		@NonNull ConnectionOptions options,
		@NonNull String clientId,
		@NonNull URI webSocketUri,
		@NonNull StatusListener statusListener,
		@NonNull ClosedListener closedListener,
		@Nullable Consumer<EmrtdPassport> emrtdPassportListener,
		@NonNull Consumer<Exception> errorListener
	) {
		this.isoDep = isoDep;
		this.options = options;
		this.statusListener = statusListener;
		this.closedListener = closedListener;
		this.emrtdPassportListener = emrtdPassportListener;
		this.errorListener = errorListener;
		this.clientId = clientId;
		this.webSocketUri = webSocketUri;

		this.websocketClient = new WebsocketClientHandler(webSocketUri, options.getHttpHeaders());
		this.fileManager = new BinaryFileManager(websocketClient);
		this.dispatcher = new WebsocketMessageDispatcher(messageHandler);
	}

	private final WebsocketMessageDispatcher.WebsocketMessageHandler messageHandler =
		new WebsocketMessageDispatcher.WebsocketMessageHandler() {

			@Override
			public void onAccept(WebsocketAcceptMessage msg) {
				if (state != ProtocolState.WAITING_FOR_ACCEPT) {
					handleProtocolError("Received Accept message in illegal state: " + state);
					return;
				}

				transitionState(ProtocolState.READING_CHIP);

				if (sessionSpan != null) {
					sessionSpan.addEvent("accept_message_received",
						Attributes.builder()
							.put("has_active_auth_challenge", msg.activeAuthenticationChallenge != null)
							.build());
				}

				executor.execute(() -> {
					// Propagate span context to executor thread
					try (Scope ignored = sessionSpan != null ? sessionSpan.makeCurrent() : null) {
						try {
							chipSession = new EmrtdChipSession(isoDep, options, emrtdSessionListener);
							chipSession.start(msg.activeAuthenticationChallenge);
						} catch (Exception e) {
							handleError(new NfcException("NFC Chip Communication Failed", e),
								ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
						}
					}
				});
			}

			@Override
			public void onChipAuthenticationHandback(WebsocketChipAuthenticationHandbackMessage msg) {
				if (state != ProtocolState.WAITING_FOR_CA_HANDBACK) {
					handleProtocolError("Received CA_HAND_BACK in illegal state: " + state);
					return;
				}

				Optional<byte[]> dg1Bytes = fileManager.getReceivedFile("dg1");
				if (!dg1Bytes.isPresent()) {
					handleProtocolError("Did not receive DG1 before CA_HANDBACK message");
					return;
				}

				if (sessionSpan != null) {
					sessionSpan.addEvent("chip_auth_handback_received",
						Attributes.builder()
							.put("check_result", msg.checkResult.toString())
							.put("dg1_size", dg1Bytes.get().length)
							.build());
				}

				DG1File dg1File;
				try {
					dg1File = new DG1File(new ByteArrayInputStream(dg1Bytes.get()));
				} catch (IOException e) {
					// Should never happen with a ByteArrayInputStream from a byte[]
					throw new AssertionError(e);
				}

				try {
					caHandbackFuture.complete(new RemoteChipAuthentication.Result(
						msg.secureMessagingInfo.toWrapper(),
						msg.checkResult,
						dg1File,
						null // Not sent by the server
					));
				} catch (GeneralSecurityException e) {
					handleError(e, ClosedListener.EMRTD_PASSPORT_READER_ERROR);
					return;
				}

				transitionState(ProtocolState.READING_CHIP);
			}

			@Override
			public void onResult(WebsocketResultMessage msg) {
				if (emrtdPassportListener != null) {
					emrtdPassportListener.accept(msg.passport);
				} else {
					Log.w(TAG, "Received result message but no EmrtdPassportListener set");
				}
			}

			@Override
			public void onApduCommand(BinaryMessageProtocol.ApduMessage apduMessage) {
				try {
					byte[] responseBytes = isoDep.transceive(apduMessage.getData());
					if (responseBytes == null || responseBytes.length < 2) {
						throw new TagLostException("No Response from NFC chip");
					}
					websocketClient.send(BinaryMessageProtocol.ApduMessage.encode(responseBytes));
				} catch (Exception e) {
					handleError(new NfcException("NFC communication failed", e),
						ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
				}
			}

			@Override
			public void onFileReceived(BinaryMessageProtocol.FileMessage fileMessage) {
				fileManager.receiveFile(fileMessage.getName(), fileMessage.getData());
			}

			@Override
			public void onUnknownMessage(Object msg, Exception parseEx) {
				Log.w(TAG, "Received unknown or unexpected protocol message: " + msg, parseEx);
				handleProtocolError("Unknown/invalid protocol message: " + msg);
			}
		};

	private final EmrtdChipSession.Listener emrtdSessionListener = new EmrtdChipSession.Listener() {
		@Override
		public void onEmrtdStep(EmrtdStep emrtdStep) {
			statusListener.handle(emrtdStep.name());
			sendMonitoringMessage(new WebsocketMonitoringMessage("Step: " + emrtdStep.name()));
		}

		@Override
		public void onFileReady(String name, byte[] data) {
			fileManager.sendFile(name, data);
		}

		@Override
		public void onFinish(EmrtdResult result) {
			// When done, send finish message to server
			sendFinishMessage(result);
			transitionState(ProtocolState.FINISHED);
		}

		@Override
		public CompletableFuture<RemoteChipAuthentication.Result> onChipAuthenticationHandover(
			int maxTransceiveLength, int maxBlockSize, SecureMessagingWrapper wrapper) {
			sendChipAuthHandoverMessage(maxTransceiveLength, maxBlockSize, wrapper);
			transitionState(ProtocolState.WAITING_FOR_CA_HANDBACK);
			return caHandbackFuture;
		}


		@Override
		public void onError(Exception e, String reason) {
			handleError(e, reason);
		}
	};


	public void start() {
		sessionSpan = EmrtdConnector.getTracer().spanBuilder("emrtd_session")
			.setAttribute("emrtd_connector.validation_id", options.getValidationId())
			.setAttribute("emrtd_connector.client_id", clientId)
			.setAttribute("url.full", webSocketUri.toString())
			.setAttribute("server.address", webSocketUri.getHost())
			.setAttribute("server.port", webSocketUri.getPort())
			.setAttribute("emrtd_connector.diagnostics_enabled", options.isDiagnosticsEnabled())
			.startSpan();

		try (Scope ignored = sessionSpan.makeCurrent()) {
			Log.d(TAG, "Connecting to WebSocket Server");
			statusListener.handle(StatusListener.CONNECTING_TO_SERVER);

			connectSpan = EmrtdConnector.getTracer().spanBuilder("websocket_connect")
				.setAttribute("url.full", webSocketUri.toString())
				.setAttribute("server.address", webSocketUri.getHost())
				.setAttribute("server.port", webSocketUri.getPort())
				.setAttribute("websocket.timeout_seconds", WEBSOCKET_TIMEOUT_SECONDS)
				.startSpan();

			websocketClient.setConnectionLostTimeout(WEBSOCKET_TIMEOUT_SECONDS);
			websocketClient.setTcpNoDelay(true);
			websocketClient.connect();
		}
	}

	public void cancel() {
		if (sessionSpan != null) {
			sessionSpan.addEvent("session_cancelled");
		}
		closeConnection(ClosedListener.CANCELLED_BY_USER);
		executor.shutdown();
	}

	public boolean isOpen() {
		return websocketClient.isOpen();
	}

	public void onClose(int code, String reason, boolean remote) {
		Log.d(TAG, "Websocket closed: " + code + " " + reason);

		if (connectSpan != null) {
			connectSpan.addEvent("websocket_closed",
				Attributes.builder()
					.put("close_code", code)
					.put("close_reason", reason != null ? reason : "")
					.put("remote_initiated", remote)
					.build());
			connectSpan.end();
		}

		if (sessionSpan != null) {
			sessionSpan.addEvent("session_closed",
				Attributes.builder()
					.put("close_code", code)
					.put("close_reason", reason != null ? reason : "")
					.put("remote_initiated", remote)
					.build());
			sessionSpan.end();
		}

		transitionState(ProtocolState.CLOSED);
		executor.shutdown();
		closedListener.handle(code, reason != null ? reason : "", remote);
	}

	private void sendMonitoringMessage(WebsocketMonitoringMessage message) {
		if (!options.isDiagnosticsEnabled()) {
			// We only send monitoring messages during diagnostic sessions. Sending them all the
			// time could delay passport reading by introducing unnecessary traffic.
			return;
		}
		try {
			websocketClient.send(message.toJson().toString());
		} catch (JSONException e) {
			throw new AssertionError("Failed to create monitoring message", e);
		}
	}

	private void sendStartMessage() {
		try {
			WebsocketStartMessage startMessage = new WebsocketStartMessage(
				options.getValidationId(),
				clientId,
				"android",
				isoDep.isExtendedLengthApduSupported(),
				options.isDiagnosticsEnabled()
			);
			websocketClient.send(startMessage.toJson().toString());
		} catch (JSONException e) {
			throw new AssertionError("Failed to create start message", e);
		}
	}

	private void sendFinishMessage(EmrtdResult emrtdResult) {
		try {
			WebsocketFinishMessage finishMessage = new WebsocketFinishMessage(
				emrtdPassportListener != null,
				emrtdResult.activeAuthenticationResult == null
					? null
					: emrtdResult.activeAuthenticationResult.signature);
			websocketClient.send(finishMessage.toJson().toString());
		} catch (JSONException e) {
			handleProtocolError("Failed to make finish message: " + e.getMessage());
		}
	}

	private void sendChipAuthHandoverMessage(
		int maxTransceiveLength, int maxBlockSize, SecureMessagingWrapper wrapper) {
		try {
			WebsocketChipAuthenticationHandoverMessage msg =
				new WebsocketChipAuthenticationHandoverMessage(
					maxTransceiveLength, maxBlockSize,
					SecureMessagingInfo.fromWrapper(wrapper));
			websocketClient.send(msg.toJson().toString());
		} catch (JSONException e) {
			handleProtocolError("Failed to create CA handover message: " + e.getMessage());
		}
	}

	private void handleProtocolError(String info) {
		Exception e = new IllegalStateException(info);
		handleError(e, ClosedListener.PROTOCOL_ERROR);
	}

	private void handleError(Exception e, String reason) {
		Log.e(TAG, "Protocol error: " + reason, e);

		if (sessionSpan != null) {
			sessionSpan.recordException(e);
			sessionSpan.setStatus(StatusCode.ERROR, reason);
		}

		errorListener.accept(e);
		closeConnection(reason);
	}

	private void transitionState(ProtocolState newState) {
		if (sessionSpan != null) {
			sessionSpan.addEvent("protocol_state_transition",
				Attributes.builder()
					.put("from_state", state.name())
					.put("to_state", newState.name())
					.build());
		}
		state = newState;
	}

	private void closeConnection(String reason) {
		if (websocketClient.isOpen()) {
			websocketClient.close(
				ClosedListener.CLOSE_CODES.get(reason),
				reason
			);
		}
		transitionState(ProtocolState.CLOSED);
		closeNfcConnection();
	}

	private void closeNfcConnection() {
		if (isoDep.isConnected()) {
			try {
				isoDep.close();
			} catch (Exception e) {
				Log.w(TAG, "Failed to close IsoDep", e);
			}
		}
	}

	private class WebsocketClientHandler extends TracedAndroidWebSocketClient {
		public WebsocketClientHandler(URI uri, Map<String, String> headers) {
			super(uri, headers, () -> sessionSpan, options.isDiagnosticsEnabled());
		}

		@Override
		public void onWebsocketOpen(ServerHandshake handshake) {
			if (connectSpan != null) {
				connectSpan.addEvent("websocket_opened",
					Attributes.builder()
						.put("http.response.status_code", handshake.getHttpStatus())
						.put("http.response.status_message", handshake.getHttpStatusMessage())
						.build());
			}

			transitionState(ProtocolState.WAITING_FOR_ACCEPT);
			sendStartMessage();
		}

		@Override
		public void handleIncomingMessage(String message) {
			dispatcher.handleTextMessage(message);
		}

		@Override
		public void handleIncomingMessage(ByteBuffer binary) {
			dispatcher.handleBinaryMessage(binary);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			WebsocketSessionCoordinator.this.onClose(code, reason, remote);
		}

		@Override
		public void onError(Exception e) {
			handleError(new WebsocketClientException("WebSocket communication failed", e),
				ClosedListener.COMMUNICATION_FAILED);
		}

	}
}
