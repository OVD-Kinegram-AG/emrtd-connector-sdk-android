package com.kinegram.android.emrtdconnector;

/**
 * Interface for listening to status updates.
 */
public interface StatusListener {
    /**
     * Connecting to the WebSocket Server.
     */
    String CONNECTING_TO_SERVER = "CONNECTING_TO_SERVER";

    /**
     * Reading File Atr/Info.
     */
    String READ_ATR_INFO = "READ_ATR_INFO";

    /**
     * Performing Access Control.
     */
    String ACCESS_CONTROL = "ACCESS_CONTROL";

    /**
     * Reading File SOD.
     */
    String READ_SOD = "READ_SOD";

    /**
     * Reading Data Group 14.
     */
    String READ_DG14 = "READ_DG14";

    /**
     * Performing Chip Authentication.
     */
    String CHIP_AUTHENTICATION = "CHIP_AUTHENTICATION";

    /**
     * Reading Data Group 15.
     */
    String READ_DG15 = "READ_DG15";

    /**
     * Performing Active Authentication.
     */
    String ACTIVE_AUTHENTICATION = "ACTIVE_AUTHENTICATION";

    /**
     * Reading Data Group 1.
     */
    String READ_DG1 = "READ_DG1";

    /**
     * Reading Data Group 2.
     */
    String READ_DG2 = "READ_DG2";

    /**
     * Reading Data Group 3.
     */
    String READ_DG3 = "READ_DG3";

    /**
     * Reading Data Group 4.
     */
    String READ_DG4 = "READ_DG4";

    /**
     * Reading Data Group 5.
     */
    String READ_DG5 = "READ_DG5";

    /**
     * Reading Data Group 6.
     */
    String READ_DG6 = "READ_DG6";

    /**
     * Reading Data Group 7.
     */
    String READ_DG7 = "READ_DG7";

    /**
     * Reading Data Group 8.
     */
    String READ_DG8 = "READ_DG8";

    /**
     * Reading Data Group 9.
     */
    String READ_DG9 = "READ_DG9";

    /**
     * Reading Data Group 10.
     */
    String READ_DG10 = "READ_DG10";

    /**
     * Reading Data Group 11.
     */
    String READ_DG11 = "READ_DG11";

    /**
     * Reading Data Group 12.
     */
    String READ_DG12 = "READ_DG12";

    /**
     * Reading Data Group 13.
     */
    String READ_DG13 = "READ_DG13";

    /**
     * Reading Data Group 16.
     */
    String READ_DG16 = "READ_DG16";

    /**
     * Performing Passive Authentication.
     */
    String PASSIVE_AUTHENTICATION = "PASSIVE_AUTHENTICATION";

    /**
     * The DocVal Server finished the eMRTD Session.
     */
    String DONE = "DONE";

    /**
     * As a status update is available this method will be invoked.
     *
     * @param status new current status of the process
     */
    void handle(String status);
}
