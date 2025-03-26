package com.kinegram.android.emrtdconnector;

import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Connect an eMRTD NFC Chip with the Document Validation Server.
 * <p>
 * Will connect to the NFC Chip using an {@link IsoDep android.nfc.tech.IsoDep}.
 * Will connect to the Document Validation Server using a {@link WebSocketClient org.java_websocket.client.WebSocketClient}.
 */
public class EmrtdConnector {
	private static final String TAG = "EmrtdConnector";
	private static final String RET_QUERY = "return_result=true";
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final String clientId;
	private final URI webSocketUri;
	private final ClosedListener closedListener;
	private final StatusListener statusListener;
	private final EmrtdPassportListener emrtdPassportListener;
	private WebSocketClient webSocketClient;

	/**
	 * Exception that occurred during {@link IsoDep#connect() IsoDep#connect()} or
	 * {@link IsoDep#transceive(byte[]) IsoDep#transceive(byte[])}.
	 * Value may be queried if close reason is {@link ClosedListener#NFC_CHIP_COMMUNICATION_FAILED}.
	 * In all other cases this value will be null.
	 */
	private Exception nfcException;

	/**
	 * Exception that occurred during the {@link WebSocketClient} operations.
	 **/
	private Exception webSocketClientException;

	public Exception getWebSocketClientException() {
		return webSocketClientException;
	}

	public Exception getNfcException() {
		return nfcException;
	}

	/**
	 * @param clientId              Client Id
	 * @param webSocketUrl          Url of the WebSocket endpoint
	 * @param closedListener        will be called when the WebSocket connection is closed.
	 * @param statusListener        will be called every time the Server provides an
	 *                              update about the current status.
	 *                              Can be null if user of EmrtdConnector is not interested
	 *                              in status updates.
	 * @param emrtdPassportListener will be called when the Server returns the result.
	 *                              Can be null if the user of EmrtdConnector is not interested
	 *                              in the Result.
	 * @throws URISyntaxException If the WebSocketUrl is an invalid string.
	 */
	public EmrtdConnector(
			String clientId,
			String webSocketUrl,
			ClosedListener closedListener,
			StatusListener statusListener,
			EmrtdPassportListener emrtdPassportListener
	) throws URISyntaxException {
		String msg = "`clientId`, `websocketUrl` or `closedListener` is null";
		requireNonNull(msg, clientId, webSocketUrl, closedListener);

		URI webSocketUri;
		if (emrtdPassportListener != null
				&& !webSocketUrl.toLowerCase().contains(RET_QUERY)) {
			webSocketUri = new URI(webSocketUrl +
					(webSocketUrl.contains("?") ? "&" : "?") +
					RET_QUERY);
		} else {
			webSocketUri = new URI(webSocketUrl);
		}
		this.clientId = clientId;
		this.webSocketUri = webSocketUri;
		this.closedListener = closedListener;
		this.statusListener = statusListener;
		this.emrtdPassportListener = emrtdPassportListener;
	}

	/**
	 * Starts the Session.
	 * <p>
	 * The `can` functions as the AccessKey and is required to access the chip.
	 *
	 * @param isoDep       {@link IsoDep} of an ICAO-9303 NFC Tag
	 * @param validationId Unique String to identify this session.
	 * @param can          CAN, a 6 digit number, printed on the front of the document.
	 * @deprecated Use {@link #connect(IsoDep, String, ChipAccessKey)} with
	 * {@link ChipAccessKey.FromCan} instead.
	 * <p>
	 * <pre>
	 * {@code
	 * // Before
	 * connect(isoDep, "validationId", "can");
	 * // After
	 * connect(isoDep, "validationId", new ChipAccessKey.FromCan("can"));}
	 * </pre>
	 */
	@Deprecated
	public void connect(IsoDep isoDep, String validationId, String can) {
		String msg = "`isoDep`, `validationId` or `can` is null";
		requireNonNull(msg, isoDep, validationId, can);

		connect(isoDep, validationId, new ChipAccessKey.FromCan(can));
	}

	/**
	 * Starts the Session.
	 * <p>
	 * The `documentNumber`, `dateOfBirth`, `dateOfExpiry` function as the
	 * AccessKey required to access the chip.
	 *
	 * @param isoDep         {@link IsoDep} of an ICAO-9303 NFC Tag
	 * @param validationId   Unique String to identify this session.
	 * @param documentNumber Document Number from the MRZ.
	 * @param dateOfBirth    Date of Birth from the MRZ (Format: yyMMDD)
	 * @param dateOfExpiry   Date of Expiry from the MRZ (Format: yyMMDD)
	 * @deprecated Use {@link #connect(IsoDep, String, ChipAccessKey)} with
	 * {@link ChipAccessKey.FromMrz} instead.
	 * <p>
	 * <pre>
	 * {@code
	 * // Before
	 * connect(isoDep, "validationId", "documentNumber", "dateOfBirth", "dateOfExpiry");
	 * // After
	 * connect(isoDep, "validationId", new ChipAccessKey.FromMrz("documentNumber", "dateOfBirth", "dateOfExpiry"));}
	 * </pre>
	 */
	public void connect(
			IsoDep isoDep,
			String validationId,
			String documentNumber,
			String dateOfBirth,
			String dateOfExpiry
	) {
		String msg = "`isoDep`, `validationId` or `documentNumber`, " +
				"`dateOfBirth` or `dateOfExpiry` is null";
		requireNonNull(msg, isoDep, validationId, documentNumber,
				dateOfBirth, dateOfExpiry);

		connect(isoDep, validationId, new ChipAccessKey.FromMrz(documentNumber, dateOfBirth, dateOfExpiry));
	}

	/**
	 * Starts the session.
	 *
	 * @param isoDep        {@link IsoDep} of an ICAO-9303 NFC Tag.
	 * @param validationId  Unique string to identify this session.
	 * @param chipAccessKey Access key for the chip access procedure to
	 *                      authenticate to the chip of the eMRTD.
	 */
	public void connect(
			IsoDep isoDep,
			String validationId,
			ChipAccessKey chipAccessKey
	) {
		String msg = "`isoDep`, `validationId` or `chipAccessKey` is null";
		requireNonNull(msg, isoDep, validationId, chipAccessKey);

		JSONObject accessKey = chipAccessKey.toJson();
		String startMessageString;
		try {
			JSONObject startMessage = new JSONObject();
			startMessage.put("client_id", clientId);
			startMessage.put("validation_id", validationId);
			startMessage.put("access_key", accessKey);
			startMessage.put("platform", "android");
			startMessage.put("nfc_adapter_supports_extended_length",
					isoDep.isExtendedLengthApduSupported());
			startMessageString = startMessage.toString();
		} catch (JSONException e) {
			String msg2 = "Failed to create StartMessage JSON";
			throw new IllegalStateException(msg2, e);
		}

		cancel();
		webSocketClient = new WebSocketClient(webSocketUri) {
			@Override
			protected void onSetSSLParameters(SSLParameters sslParameters) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					// Fix for Android SDK < 24
					// https://github.com/TooTallNate/Java-WebSocket/wiki/No-such-method-error-setEndpointIdentificationAlgorithm
					super.onSetSSLParameters(sslParameters);
				}
			}

			@Override
			public void onOpen(ServerHandshake handshake) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && !"ws".equals(webSocketUri.getScheme())) {
					// Fix for Android SDK < 24
					// https://github.com/TooTallNate/Java-WebSocket/wiki/No-such-method-error-setEndpointIdentificationAlgorithm
					try {
						HostnameVerifier hostnameVerifier;
						hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
						SSLSession session = ((SSLSocket) getSocket()).getSession();
						String expectedHostname = webSocketUri.getHost();
						if (!hostnameVerifier.verify(expectedHostname, session)) {
							String msg = "Invalid host" + session.getPeerPrincipal();
							Log.e(TAG, msg);
							throw new SSLHandshakeException((msg));
						}
						// All good. Continue
					} catch (Exception e) {
						closeBecauseOfWebSocketClientException(e);
						return;
					}
				}

				Log.d(TAG, "Connecting to NFC Tag");
				try {
					isoDep.setTimeout(1000);
					isoDep.connect();
				} catch (IOException e) {
					closeBecauseOfIsoDepException(e);
					return;
				}

				try {
					Log.d(TAG, "Sending Start Message to Server");
					send(startMessageString);
				} catch (Exception e) {
					closeBecauseOfWebSocketClientException(e);
				}
			}

			@Override
			public void onMessage(String message) {
				JSONObject obj;
				try {
					obj = new JSONObject(message);
				} catch (Exception e) {
					String msg = "Failed to parse Text Message as JSON\n%s";
					Log.e(TAG, String.format(msg, message), e);
					return;
				}

				if (obj.has("status") || obj.has("emrtd_passport")) {
					onStatusUpdate(obj.optString("status"));
					onEmrtdPassport(obj.optJSONObject("emrtd_passport"));
				} else if (obj.has("close_code")) {
					// Message can be ignored with good conscience.
					// The iOS native WebSocket implementation in iOS 13 and 14
					// has an issue. Sometimes the delegate function for *close* is not called.
					// That is why the server sends a close code as a text message.
				} else {
					String msg = "Unexpected Text Message received\n%s";
					Log.w(TAG, String.format(msg, message));
				}
			}

			@Override
			public void onMessage(ByteBuffer command) {
				byte[] response;
				try {
					response = isoDep.transceive(command.array());
				} catch (Exception e) {
					closeBecauseOfIsoDepException(e);
					return;
				}
				try {
					send(response);
				} catch (Exception e) {
					closeBecauseOfWebSocketClientException(e);
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				String msg = "Close Code %d - %s(Remote: %s)";
				Log.d(TAG, String.format(msg, code, reason, remote));

				if (isoDep.isConnected()) {
					try {
						isoDep.close();
					} catch (IOException e) {
						Log.e(TAG, "IsoDep close Error", e);
					}
				}
				if (reason == null) {
					reason = "";
				}
				onClosed(code, reason, remote);
			}

			@Override
			public void onError(Exception e) {
				closeBecauseOfWebSocketClientException(e);
			}

			private void closeBecauseOfIsoDepException(Exception e) {
				Log.e(TAG, "NFC Chip Communication Failed", e);
				nfcException = e;
				if (isOpen()) {
					close(1001, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
				}
			}

			private void closeBecauseOfWebSocketClientException(Exception e) {
				Log.e(TAG, "WebServer Communication Failed", e);
				webSocketClientException = e;
				if (isOpen()) {
					close(1001, ClosedListener.COMMUNICATION_FAILED);
				}
			}
		};
		Log.d(TAG, "Connecting to WebSocket Server");
		onStatusUpdate(StatusListener.CONNECTING_TO_SERVER);
		webSocketClient.setConnectionLostTimeout(2000);
		webSocketClient.setTcpNoDelay(true);
		webSocketClient.connect();
	}

	/**
	 * Cancels this session as soon as possible.
	 * <p>
	 * The ClosedListener will be called once the WebSocket Session is closed.
	 * The Close Reason will be "CANCELLED_BY_USER".
	 */
	public void cancel() {
		if (webSocketClient != null && webSocketClient.isOpen()) {
			webSocketClient.close(
					1001, ClosedListener.CANCELLED_BY_USER);
		}
	}

	/**
	 * @return true if session is open
	 */
	public boolean isOpen() {
		return webSocketClient != null && webSocketClient.isOpen();
	}

	private void onStatusUpdate(String status) {
		if (status != null && statusListener != null) {
			handler.post(() -> statusListener.handle(status));
		}
	}

	private void onEmrtdPassport(JSONObject emrtdPassportObject) {
		if (emrtdPassportObject != null && emrtdPassportListener != null) {
			try {
				EmrtdPassport emrtdPassport = new EmrtdPassport(emrtdPassportObject);
				handler.post(() -> emrtdPassportListener.handle(emrtdPassport, null));
			} catch (JSONException e) {
				Log.e(TAG, "Failed to decode EmrtdPassport JSON", e);
				handler.post(() -> emrtdPassportListener.handle(null, e));
			}
		}
	}

	private void onClosed(int code, String reason, boolean remote) {
		handler.post(() -> closedListener.handle(code, reason, remote));
	}

	private static void requireNonNull(String msg, Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				throw new NullPointerException(msg);
			}
		}
	}
}
