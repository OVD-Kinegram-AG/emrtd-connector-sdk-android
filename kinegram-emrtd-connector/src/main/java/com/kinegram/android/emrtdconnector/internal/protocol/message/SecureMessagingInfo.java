package com.kinegram.android.emrtdconnector.internal.protocol.message;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.jmrtd.protocol.AESSecureMessagingWrapper;
import org.jmrtd.protocol.DESedeSecureMessagingWrapper;
import org.jmrtd.protocol.SecureMessagingWrapper;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;

/**
 * Information needed for secure messaging.
 */
public class SecureMessagingInfo {
	@NonNull
	public final String algorithm;
	@NonNull
	public final String encKey;
	@NonNull
	public final String macKey;
	public final long ssc;

	/**
	 * @param algorithm The algorithm used, currently either AES or DESede.
	 * @param encKey    The session key for encryption in base64.
	 * @param macKey    The session key for macs in base64.
	 * @param ssc       The value of the send sequence counter.
	 */
	public SecureMessagingInfo(
		@NonNull String algorithm, @NonNull String encKey, @NonNull String macKey, long ssc) {
		this.algorithm = algorithm;
		this.encKey = encKey;
		this.macKey = macKey;
		this.ssc = ssc;
	}

	public SecureMessagingWrapper toWrapper() throws GeneralSecurityException {
		byte[] encBytes = Base64.decode(encKey, Base64.DEFAULT);
		byte[] macBytes = Base64.decode(macKey, Base64.DEFAULT);

		SecretKey kEnc = new SecretKeySpec(encBytes, algorithm);
		SecretKey kMac = new SecretKeySpec(macBytes, algorithm);

		switch (algorithm) {
			case "AES":
				return new AESSecureMessagingWrapper(kEnc, kMac, ssc);
			case "DESede":
				return new DESedeSecureMessagingWrapper(kEnc, kMac, ssc);
			default:
				throw new IllegalArgumentException("Unsupported SM algorithm: " + algorithm);
		}
	}

	public static SecureMessagingInfo fromWrapper(SecureMessagingWrapper wrapper) {
		String alg = (wrapper instanceof AESSecureMessagingWrapper) ? "AES" : "DESede";
		String encB64 = Base64.encodeToString(
			wrapper.getEncryptionKey().getEncoded(), Base64.NO_WRAP);
		String macB64 = Base64.encodeToString(wrapper.getMACKey().getEncoded(), Base64.NO_WRAP);
		return new SecureMessagingInfo(alg, encB64, macB64, wrapper.getSendSequenceCounter());
	}

	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("algorithm", algorithm);
		json.put("encKey", encKey);
		json.put("macKey", macKey);
		json.put("ssc", ssc);
		return json;
	}

	public static SecureMessagingInfo fromJson(JSONObject json) throws JSONException {
		return new SecureMessagingInfo(
			json.getString("algorithm"),
			json.getString("encKey"),
			json.getString("macKey"),
			json.getLong("ssc")
		);
	}
}
