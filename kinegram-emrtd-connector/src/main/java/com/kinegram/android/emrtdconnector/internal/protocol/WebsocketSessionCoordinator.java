package com.kinegram.android.emrtdconnector.internal.protocol;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.kinegram.android.emrtdconnector.ClosedListener;
import com.kinegram.android.emrtdconnector.ConnectionOptions;
import com.kinegram.android.emrtdconnector.EmrtdPassport;
import com.kinegram.android.emrtdconnector.StatusListener;
import com.kinegram.android.emrtdconnector.internal.AndroidWebsocketClient;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.NfcException;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.WebsocketClientException;
import com.kinegram.android.emrtdconnector.internal.protocol.message.*;
import com.kinegram.emrtd.EmrtdResult;
import com.kinegram.emrtd.EmrtdStep;
import com.kinegram.emrtd.RemoteChipAuthentication;

import org.java_websocket.handshake.ServerHandshake;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.protocol.SecureMessagingWrapper;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

	private final AndroidWebsocketClient websocketClient;
	private final BinaryFileManager fileManager;
	private EmrtdChipSession chipSession;
	private final WebsocketMessageDispatcher dispatcher;
	private final String clientId;

	private final CompletableFuture<RemoteChipAuthentication.Result> caHandbackFuture = new CompletableFuture<>();


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
				state = ProtocolState.READING_CHIP;
				executor.execute(() -> {
					try {
						chipSession = new EmrtdChipSession(isoDep, options, emrtdSessionListener);
						chipSession.start(msg.activeAuthenticationChallenge);
					} catch (Exception e) {
						handleError(new NfcException("NFC Chip Communication Failed", e),
							ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
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
				state = ProtocolState.READING_CHIP;
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
			state = ProtocolState.FINISHED;
		}

		@Override
		public CompletableFuture<RemoteChipAuthentication.Result> onChipAuthenticationHandover(
			int maxTransceiveLength, int maxBlockSize, SecureMessagingWrapper wrapper) {
			sendChipAuthHandoverMessage(maxTransceiveLength, maxBlockSize, wrapper);
			state = ProtocolState.WAITING_FOR_CA_HANDBACK;
			return caHandbackFuture;
		}


		@Override
		public void onError(Exception e, String reason) {
			handleError(e, reason);
		}
	};


	public void start() {
		Log.d(TAG, "Connecting to WebSocket Server");
		statusListener.handle(StatusListener.CONNECTING_TO_SERVER);
		websocketClient.setConnectionLostTimeout(WEBSOCKET_TIMEOUT_SECONDS);
		websocketClient.setTcpNoDelay(true);
		websocketClient.connect();
	}

	public void cancel() {
		closeConnection(ClosedListener.CANCELLED_BY_USER);
		executor.shutdown();
	}

	public boolean isOpen() {
		return websocketClient.isOpen();
	}

	public void onClose(int code, String reason, boolean remote) {
		Log.d(TAG, "Websocket closed: " + code + " " + reason);
		state = ProtocolState.CLOSED;
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
		errorListener.accept(e);
		closeConnection(reason);
	}


	private void closeConnection(String reason) {
		if (websocketClient.isOpen()) {
			websocketClient.close(
				ClosedListener.CLOSE_CODES.get(reason),
				reason
			);
		}
		state = ProtocolState.CLOSED;
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

	private class WebsocketClientHandler extends AndroidWebsocketClient {
		public WebsocketClientHandler(URI uri, Map<String, String> headers) {
			super(uri, headers);
		}

		@Override
		public void onWebsocketOpen(ServerHandshake handshake) {
			state = ProtocolState.WAITING_FOR_ACCEPT;
			sendStartMessage();
		}

		@Override
		public void onMessage(String message) {
			dispatcher.handleTextMessage(message);
		}

		@Override
		public void onMessage(ByteBuffer binary) {
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
