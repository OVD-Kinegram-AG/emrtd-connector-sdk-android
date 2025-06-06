package com.kinegram.android.emrtdconnector.internal.protocol;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.kinegram.android.emrtdconnector.ChipAccessKey;
import com.kinegram.android.emrtdconnector.ClosedListener;
import com.kinegram.android.emrtdconnector.ConnectionOptions;
import com.kinegram.android.emrtdconnector.EmrtdPassport;
import com.kinegram.android.emrtdconnector.StatusListener;
import com.kinegram.android.emrtdconnector.internal.AndroidWebsocketClient;
import com.kinegram.android.emrtdconnector.internal.IsoDepCardService;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.NfcException;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.WebsocketClientException;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketAcceptMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketCloseMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketFinishMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketResultMessage;
import com.kinegram.android.emrtdconnector.internal.protocol.message.WebsocketStartMessage;
import com.kinegram.emrtd.AccessInformation;
import com.kinegram.emrtd.EmrtdReader;
import com.kinegram.emrtd.EmrtdReaderException;
import com.kinegram.emrtd.EmrtdResult;
import com.kinegram.emrtd.EmrtdStep;
import com.kinegram.emrtd.protocols.AccessControlProtocolException;

import net.sf.scuba.smartcards.CardService;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class WebsocketProtocol {
	/**
	 * Tag for logging.
	 */
	private static final String TAG = WebsocketProtocol.class.getSimpleName();

	private final EmrtdReader emrtdReader = new EmrtdReader();
	private final IsoDep isoDep;
	private final ConnectionOptions options;
	private final WebSocketClient websocketClient;
	private final StatusListener statusListener;
	private final Consumer<EmrtdPassport> emrtdPassportListener;
	private final Consumer<Exception> errorListener;

	private ProtocolState state = ProtocolState.START_NOT_SENT;

	public WebsocketProtocol(
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
		this.emrtdPassportListener = emrtdPassportListener;
		this.errorListener = errorListener;
		this.websocketClient = new AndroidWebsocketClient(webSocketUri, options.getHttpHeaders()) {

			@Override
			public void onWebsocketOpen(ServerHandshake handshake) {
				WebsocketStartMessage startMessage = new WebsocketStartMessage(
					options.getValidationId(),
					clientId,
					"android",
					isoDep.isExtendedLengthApduSupported(),
					false);

				try {
					websocketClient.send(startMessage.toJson().toString());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				state = ProtocolState.WAIT_FOR_ACCEPT;
			}

			@Override
			public void onMessage(String message) {
				try {
					WebsocketMessage websocketMessage = WebsocketMessage
						.fromJson(new JSONObject(message));

					if (websocketMessage instanceof WebsocketAcceptMessage) {
						handleAccept((WebsocketAcceptMessage) websocketMessage);
					} else if (websocketMessage instanceof WebsocketResultMessage) {
						handleResult((WebsocketResultMessage) websocketMessage);
					} else if (websocketMessage instanceof WebsocketCloseMessage) {
						// Noop - only relevant for iOS
					} else {
						throw new IllegalStateException(
							"Received unexpected websocket message of type " + websocketMessage.type
								+ " while in state " + state.name());
					}
				} catch (JSONException e) {
					throw new RuntimeException("Failed to parse incoming websocket message", e);
				}
			}

			@Override
			public void onMessage(ByteBuffer binary) {
				throw new IllegalStateException(
					"Received unexpected binary message while in state " + state.name());
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				String msg = "Close Code %d - %s(Remote: %s)";
				Log.d(TAG, String.format(msg, code, reason, remote));

				if (isoDep.isConnected()) {
					try {
						isoDep.close();
					} catch (IOException e) {
						// Not a big problem, so we don't propagate the error to the user but just
						// log it
						Log.e(TAG, "IsoDep close Error", e);
					}
				}
				if (reason == null) {
					reason = "";
				}

				String finalReason = reason;
				closedListener.handle(code, finalReason, remote);
			}

			@Override
			public void onError(Exception e) {
				Log.e(TAG, "WebServer Communication Failed", e);

				errorListener.accept(
					new WebsocketClientException("WebServer Communication Failed", e));
				if (isOpen()) {
					close(1001, ClosedListener.COMMUNICATION_FAILED);
				}
			}

		};
	}

	/**
	 * Closes the websocket connection.
	 */
	public void cancel() {
		if (websocketClient != null && websocketClient.isOpen()) {
			websocketClient.close(1001, ClosedListener.CANCELLED_BY_USER);
		}
	}

	/**
	 * Checks if the websocket connection is open.
	 */
	public boolean isOpen() {
		return websocketClient.isOpen();
	}

	/**
	 * Connect to the websocket and send the START message to start the
	 * protocol.
	 */
	public void start() {
		Log.d(TAG, "Connecting to WebSocket Server");
		statusListener.handle(StatusListener.CONNECTING_TO_SERVER);
		// TODO Is 2000 seconds(!) really what was intended? I assume Tim thought this was in ms?
		this.websocketClient.setConnectionLostTimeout(2000);
		this.websocketClient.setTcpNoDelay(true);
		this.websocketClient.connect();
	}

	public void handleResult(WebsocketResultMessage message) {
		if (emrtdPassportListener == null) {
			// This should not happen. We only request the server to send a result, when the
			// listener is NOT null. This is a protocol violation.
			Log.w(TAG, "Received RESULT message without asking for it");
			return;
		}
		emrtdPassportListener.accept(message.passport);
	}

	public void handleAccept(WebsocketAcceptMessage message) throws JSONException {
		state = ProtocolState.ACCEPTED;

		Log.d(TAG, "Connecting to NFC Tag");
		try {
			isoDep.setTimeout(1000);
			isoDep.connect();
		} catch (IOException e) {
			errorListener.accept(new NfcException("NFC Chip Communication Failed", e));
			websocketClient.close(1001, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
			return;
		}

		final EmrtdResult result;
		final CardService cardService = new IsoDepCardService(isoDep);
		try {
			result = emrtdReader.read(
				cardService,
				getAccessInformation(),
				false, // Verification is done on the server, the client cannot be trusted
				null,
				null,
				new EmrtdReader.ProgressListener() {
					@Override
					public void onNewUpdate(EmrtdStep emrtdStep) {
						statusListener.handle(emrtdStep.name());
					}

					@Override
					public void onFileReadProgress(int i, int i1) {
						// TODO Do we want to share this with the user? Probably at some point, but
						//  this can be done later.
					}
				}, TagLostException.class);
		} catch (EmrtdReaderException e) {
			websocketClient.close(1011, ClosedListener.EMRTD_PASSPORT_READER_ERROR);
			return;
		} catch (AccessControlProtocolException e) {
			websocketClient.close(4403, ClosedListener.ACCESS_CONTROL_FAILED);
			return;
		} finally {
			cardService.close();
		}

		websocketClient.send(BinaryMessageProtocol.encode("sod", result.sodRawBinary));
		for (Map.Entry<Integer, byte[]> entry : result.dataGroupsRawBinary.entrySet()) {
			websocketClient.send(
				BinaryMessageProtocol.encode("dg" + entry.getKey(), entry.getValue()));
		}

		WebsocketFinishMessage finishMessage = new WebsocketFinishMessage(
			this.emrtdPassportListener != null
		);
		websocketClient.send(finishMessage.toJson().toString());
		state = ProtocolState.FINISHED;
	}

	@NonNull
	private AccessInformation getAccessInformation() {
		AccessInformation accessInformation;
		if (options.getChipAccessKey() instanceof ChipAccessKey.FromCan) {
			ChipAccessKey.FromCan accessKey = (ChipAccessKey.FromCan) options.getChipAccessKey();
			accessInformation = new AccessInformation.WithCan(accessKey.getCan());
		} else {
			ChipAccessKey.FromMrz accessKey = (ChipAccessKey.FromMrz) options.getChipAccessKey();
			accessInformation = new AccessInformation.WithMrzDetails(
				accessKey.getDocumentNumber(),
				accessKey.getDateOfBirth(),
				accessKey.getDateOfExpiry());
		}
		return accessInformation;
	}

}
