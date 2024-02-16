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
	 * Reading Data Group 7.
	 */
	String READ_DG7 = "READ_DG7";

	/**
	 * Reading Data Group 11.
	 */
	String READ_DG11 = "READ_DG11";

	/**
	 * Reading Data Group 12.
	 */
	String READ_DG12 = "READ_DG12";

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
