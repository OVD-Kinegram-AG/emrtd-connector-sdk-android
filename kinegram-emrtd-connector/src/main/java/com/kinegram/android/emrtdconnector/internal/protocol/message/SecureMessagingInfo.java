package com.kinegram.android.emrtdconnector.internal.protocol.message;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.kinegram.android.emrtdconnector.EmrtdConnector;

import org.jmrtd.protocol.AESSecureMessagingWrapper;
import org.jmrtd.protocol.DESedeSecureMessagingWrapper;
import org.jmrtd.protocol.SecureMessagingWrapper;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

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
		Span smSpan = EmrtdConnector.getTracer().spanBuilder("secure_messaging_setup")
			.setAttribute("crypto.algorithm", algorithm)
			.setAttribute("crypto.ssc", ssc)
			.startSpan();

		try (Scope ignored = smSpan.makeCurrent()) {
			smSpan.addEvent("decoding_keys");
			byte[] encBytes = Base64.decode(encKey, Base64.DEFAULT);
			byte[] macBytes = Base64.decode(macKey, Base64.DEFAULT);

			smSpan.addEvent("creating_secret_keys",
				Attributes.builder()
					.put("enc_key_length", encBytes.length)
					.put("mac_key_length", macBytes.length)
					.build());

			SecretKey kEnc = new SecretKeySpec(encBytes, algorithm);
			SecretKey kMac = new SecretKeySpec(macBytes, algorithm);

			switch (algorithm) {
				case "AES":
					smSpan.addEvent("creating_aes_wrapper");
					return new AESSecureMessagingWrapper(kEnc, kMac, ssc);
				case "DESede":
					smSpan.addEvent("creating_desede_wrapper");
					return new DESedeSecureMessagingWrapper(kEnc, kMac, ssc);
				default:
					smSpan.setStatus(StatusCode.ERROR, "Unsupported algorithm");
					throw new IllegalArgumentException("Unsupported SM algorithm: " + algorithm);
			}
		} catch (Exception e) {
			smSpan.recordException(e);
			throw e;
		} finally {
			smSpan.end();
		}
	}

	public static SecureMessagingInfo fromWrapper(SecureMessagingWrapper wrapper) {
		Span smSpan = EmrtdConnector.getTracer().spanBuilder("secure_messaging_extract")
			.setAttribute("code.function.name", wrapper.getClass().getSimpleName())
			.startSpan();

		try (Scope ignored = smSpan.makeCurrent()) {
			String alg = (wrapper instanceof AESSecureMessagingWrapper) ? "AES" : "DESede";
			smSpan.setAttribute("crypto.algorithm", alg);
			smSpan.setAttribute("crypto.ssc", wrapper.getSendSequenceCounter());

			smSpan.addEvent("extracting_keys");
			String encB64 = Base64.encodeToString(
				wrapper.getEncryptionKey().getEncoded(), Base64.NO_WRAP);
			String macB64 = Base64.encodeToString(wrapper.getMACKey().getEncoded(), Base64.NO_WRAP);

			smSpan.addEvent("secure_messaging_info_created");
			smSpan.setStatus(StatusCode.OK);
			return new SecureMessagingInfo(alg, encB64, macB64, wrapper.getSendSequenceCounter());
		} catch (Exception e) {
			smSpan.recordException(e);
			smSpan.setStatus(StatusCode.ERROR, "Key extraction failed");
			throw e;
		} finally {
			smSpan.end();
		}
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
