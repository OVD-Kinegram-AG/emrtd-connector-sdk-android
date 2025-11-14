package com.kinegram.android.emrtdconnector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for listening to WebSocket Connection Closes.
 */
public interface ClosedListener {
    /**
     * Server reported Timeout while waiting for StartMessage.
     */
    String TIMEOUT_WHILE_WAITING_FOR_START_MESSAGE = "TIMEOUT_WHILE_WAITING_FOR_START_MESSAGE";

    /**
     * Server reported Timeout while waiting for APDU Response.
     */
    String TIMEOUT_WHILE_WAITING_FOR_RESPONSE = "TIMEOUT_WHILE_WAITING_FOR_RESPONSE";

    /**
     * Max Session Time exceeded.
     */
    String MAX_SESSION_TIME_EXCEEDED = "MAX_SESSION_TIME_EXCEEDED";

    /**
     * Unexpected Message was sent to the server.
     */
    String UNEXPECTED_MESSAGE = "UNEXPECTED_MESSAGE";

    /**
     * Invalid Start Message.
     *
     * @deprecated No longer in use.
     */
    @Deprecated
    String INVALID_START_MESSAGE = "INVALID_START_MESSAGE";

    /**
     * Violation of the protocol between server and client.
     * <p>
     * Should never happen and indicates an implementation bug.
     */
    String PROTOCOL_ERROR = "PROTOCOL_ERROR";

    /**
     * The provided Client ID is not correct.
     */
    String INVALID_CLIENT_ID = "INVALID_CLIENT_ID";

    /**
     * The Access Key values are invalid.
     * <p>
     * Ensure that the CAN consists of 6 digits (0-9).
     * Ensure that the Document Number is at least 8 characters long.
     * Ensure the Date of Birth and Date of Expiry are 6 digits in format yyMMDD (as in the MRZ).
     */
    String INVALID_ACCESS_KEY_VALUES = "INVALID_ACCESS_KEY_VALUES";

    /**
     * Access Control failed.
     * Ensure that the Access Key Values (MRZ information or CAN) are correct and try again.
     */
    String ACCESS_CONTROL_FAILED = "ACCESS_CONTROL_FAILED";

    /**
     * WebSocket Communication with the Server failed.
     * Check the network connection and try again.
     * Query {@link EmrtdConnector#getWebSocketClientException()} for details.
     */
    String COMMUNICATION_FAILED = "COMMUNICATION_FAILED";

    /**
     * Server reported that a file could not be read.
     */
    String FILE_READ_ERROR = "FILE_READ_ERROR";

    /**
     * An Exception in the eMRTD reader implementation occurred.
     */
    String EMRTD_PASSPORT_READER_ERROR = "EMRTD_PASSPORT_READER_ERROR";

    /**
     * Unexpected other Server Error.
     */
    String SERVER_ERROR = "SERVER_ERROR";

    /**
     * DocVal Server was not able to post the Result to the Result-Server.
     */
    String POST_TO_RESULT_SERVER_FAILED = "POST_TO_RESULT_SERVER_FAILED";

    /**
     * Communicating with the NFC Chip failed.
     * The most likely reason is that the passport was moved away from the phone.
     * Query {@link EmrtdConnector#getNfcException()} for details.
     */
    String NFC_CHIP_COMMUNICATION_FAILED = "NFC_CHIP_COMMUNICATION_FAILED";

    /**
     * The function `emrtdConnector.cancel()` was called.
     */
    String CANCELLED_BY_USER = "CANCELLED_BY_USER";

    /**
     * Map of all expected close reasons to their close code.
     */
    Map<String, Integer> CLOSE_CODES = Collections.unmodifiableMap(new HashMap<String, Integer>() {
        {
            put(TIMEOUT_WHILE_WAITING_FOR_START_MESSAGE, 1001);
            put(TIMEOUT_WHILE_WAITING_FOR_RESPONSE, 1001);
            put(MAX_SESSION_TIME_EXCEEDED, 1011);
            put(UNEXPECTED_MESSAGE, 1008);
            put(INVALID_START_MESSAGE, 1008);
            put(PROTOCOL_ERROR, 1008);
            put(INVALID_CLIENT_ID, 4401);
            put(INVALID_ACCESS_KEY_VALUES, 1008);
            put(ACCESS_CONTROL_FAILED, 4403);
            put(COMMUNICATION_FAILED, 1011);
            put(FILE_READ_ERROR, 1011);
            put(EMRTD_PASSPORT_READER_ERROR, 1011);
            put(SERVER_ERROR, 1011);
            put(POST_TO_RESULT_SERVER_FAILED, 1011);
            put(NFC_CHIP_COMMUNICATION_FAILED, 1001);
            put(CANCELLED_BY_USER, 1001);
        }
    });

    /**
     * As a WebSocket Connection is closed, this method will be invoked.
     *
     * @param code   WebSocket Close Code
     * @param reason WebSocket Close Reason
     * @param remote true if the close was issued by the remote endpoint
     */
    void handle(int code, String reason, boolean remote);
}
