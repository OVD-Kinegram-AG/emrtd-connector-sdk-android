package com.kinegram.android.emrtdconnector;

import org.json.JSONException;

/**
 * Interface to listen for an {@link EmrtdPassport} result.
 */
public interface EmrtdPassportListener {
	/**
	 * As the server sent the result to this client this method will be invoked.
	 *
	 * @param emrtdPassport containing the Result
	 * @param exception     non null if an Exception occurred while parsing server message
	 */
	void handle(EmrtdPassport emrtdPassport, JSONException exception);
}
