package com.kinegram.android.emrtdconnector;

import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.os.Looper;

import com.kinegram.android.emrtdconnector.internal.protocol.WebsocketSessionCoordinator;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.NfcException;
import com.kinegram.android.emrtdconnector.internal.protocol.exception.WebsocketClientException;
import com.kinegram.emrtd.EmrtdReader;

import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;

import io.opentelemetry.api.trace.*;

/**
 * Connect an eMRTD NFC Chip with the Document Validation Server.
 * <p>
 * Will connect to the NFC Chip using an {@link IsoDep android.nfc.tech.IsoDep}.
 * Will connect to the Document Validation Server using a {@link WebSocketClient org.java_websocket.client.WebSocketClient}.
 */
public class EmrtdConnector {
	private static final String RET_QUERY = "return_result=true";

	// We start with a no-op implementation. If they want to, the users of this library can then
	// provide their own tracer.
	private static Tracer tracer = getTracer(TracerProvider.noop());

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final String clientId;
	private final URI webSocketUri;
	private final ClosedListener closedListener;
	private final StatusListener statusListener;
	private final EmrtdPassportListener emrtdPassportListener;

	private WebsocketSessionCoordinator sessionCoordinator;

	private Exception nfcException;

	private Exception webSocketClientException;

	private Exception exception;

	/**
	 * Sets a OpenTelemetry tracer provider that is used to provide traces. If none is set, a no-op
	 * implementation is used.
	 *
	 * @param tracerProvider The provider to set.
	 */
	public static void setTracerProvider(TracerProvider tracerProvider) {
		tracer = getTracer(tracerProvider);
		EmrtdReader.setTracerProvider(tracerProvider);
	}

	private static Tracer getTracer(TracerProvider tracerProvider) {
		return tracerProvider.get(
			"com.kinegram.android.emrtdconnector", BuildConfig.LIBRARY_VERSION);
	}

	/**
	 * Gets the currently used OpenTelemetry tracer.
	 *
	 * @return The tracer.
	 */
	public static Tracer getTracer() {
		return tracer;
	}

	/**
	 * Get the exception that occurred during the {@link WebSocketClient} operations.
	 **/
	public Exception getWebSocketClientException() {
		return webSocketClientException;
	}

	/**
	 * Gets the exception that occurred during {@link IsoDep#connect() IsoDep#connect()} or
	 * {@link IsoDep#transceive(byte[]) IsoDep#transceive(byte[])}.
	 *
	 * <p>Value may be queried if close reason is
	 * {@link ClosedListener#NFC_CHIP_COMMUNICATION_FAILED}. In all other cases this value will be
	 * {@code null}.
	 */
	public Exception getNfcException() {
		return nfcException;
	}

	/**
	 * Gets any other exception that occurred during the session and not fit the
	 * {@link #getNfcException()} and {@link #getWebSocketClientException()}.
	 */
	public Exception getException() {
		return exception;
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
	 * Starts the session.
	 * <p>
	 * The `can` functions as the AccessKey and is required to access the chip.
	 *
	 * @param isoDep       {@link IsoDep} of an ICAO-9303 NFC Tag
	 * @param validationId Unique String to identify this session.
	 * @param can          CAN, a 6 digit number, printed on the front of the document.
	 * @deprecated Use {@link #connect(IsoDep, ConnectionOptions)} instead.
	 * <p>
	 * <pre>
	 * {@code
	 * // Before
	 * connect(isoDep, "validationId", "can");
	 * // After
	 * ConnectionOptions options = new ConnectionOptions.Builder()
	 * 			.setChipAccessKeyFromCan("can")
	 * 			.setValidationId("validationId")
	 * 			.build();
	 * connect(isoDep, options);}
	 * </pre>
	 */
	@Deprecated
	public void connect(IsoDep isoDep, String validationId, String can) {
		String msg = "`isoDep`, `validationId` or `can` is null";
		requireNonNull(msg, isoDep, validationId, can);

		ConnectionOptions options = new ConnectionOptions.Builder()
			.setChipAccessKeyFromCan(can)
			.setValidationId(validationId)
			.build();

		connect(isoDep, options);
	}

	/**
	 * Starts the session.
	 * <p>
	 * The `documentNumber`, `dateOfBirth`, `dateOfExpiry` function as the
	 * AccessKey required to access the chip.
	 *
	 * @param isoDep         {@link IsoDep} of an ICAO-9303 NFC Tag
	 * @param validationId   Unique String to identify this session.
	 * @param documentNumber Document Number from the MRZ.
	 * @param dateOfBirth    Date of Birth from the MRZ (Format: yyMMDD)
	 * @param dateOfExpiry   Date of Expiry from the MRZ (Format: yyMMDD)
	 * @deprecated Use {@link #connect(IsoDep, ConnectionOptions)} instead.
	 * <p>
	 * <pre>
	 * {@code
	 * // Before
	 * connect(isoDep, "validationId", "documentNumber", "dateOfBirth", "dateOfExpiry");
	 * // After
	 * ConnectionOptions options = new ConnectionOptions.Builder()
	 * 			.setChipAccessKeyFromMrz("documentNumber", "dateOfBirth", "dateOfExpiry")
	 * 			.setValidationId("validationId")
	 * 			.build();
	 * connect(isoDep, options);}
	 * </pre>
	 */
	@Deprecated
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

		ConnectionOptions options = new ConnectionOptions.Builder()
			.setChipAccessKeyFromMrz(documentNumber, dateOfBirth, dateOfExpiry)
			.setValidationId(validationId)
			.build();
		connect(isoDep, options);
	}

	/**
	 * Starts the session.
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * 	// Access Key values from the MRZ
	 * 	String documentNumber = "123456789";
	 * 	String dateOfBirth = "970101"; // yyMMDD
	 * 	String dateOfExpiry = "221212"; // yyMMDD
	 *
	 * 	// Unique transaction ID, usually from your server
	 * 	String validationId = UUID.randomUUID().toString();
	 *
	 * 	ConnectionOptions options = new ConnectionOptions.Builder()
	 * 			.setChipAccessKeyFromMrz(documentNumber, dateOfBirth, dateOfExpiry)
	 * 			.setValidationId(validationId)
	 * 			.build();
	 *
	 * 	emrtdConnector.connect(isoDep, options);
	 * }</pre>
	 *
	 * @param isoDep  {@link IsoDep} of an ICAO-9303 NFC Tag.
	 * @param options Options for this read. Can be constructed with the
	 *                {@link ConnectionOptions.Builder}.
	 */
	public void connect(IsoDep isoDep, ConnectionOptions options) {
		String validationId = options.getValidationId();
		ChipAccessKey chipAccessKey = options.getChipAccessKey();

		String msg = "`isoDep`, `validationId` or `chipAccessKey` is null";
		requireNonNull(msg, isoDep, validationId, chipAccessKey);
		cancel();


		sessionCoordinator = new WebsocketSessionCoordinator(
			isoDep,
			options,
			clientId,
			webSocketUri,
			status -> handler.post(() -> statusListener.handle(status)),
			(int code, String reason, boolean remote) ->
				handler.post(() -> closedListener.handle(code, reason, remote)),
			emrtdPassportListener == null ? null : (passport) ->
				handler.post(() -> emrtdPassportListener.handle(passport, null)),
			e -> {
				if (e instanceof NfcException) {
					this.nfcException = (Exception) e.getCause();
				} else if (e instanceof WebsocketClientException) {
					this.webSocketClientException = (Exception) e.getCause();
				} else {
					this.exception = e;
				}
			});
		sessionCoordinator.start();
	}

	/**
	 * Cancels this session as soon as possible.
	 * <p>
	 * The ClosedListener will be called once the WebSocket Session is closed.
	 * The Close Reason will be "CANCELLED_BY_USER".
	 */
	public void cancel() {
		if (sessionCoordinator != null) {
			sessionCoordinator.cancel();
		}
	}

	/**
	 * @return true if session is open
	 */
	public boolean isOpen() {
		return sessionCoordinator != null && sessionCoordinator.isOpen();
	}

	private static void requireNonNull(String msg, Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				throw new NullPointerException(msg);
			}
		}
	}
}
