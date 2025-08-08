package com.kinegram.android.emrtdconnector;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the access key for the chip access procedure to authenticate to
 * the chip of the eMRTD.
 *
 * <p>Two types of {@code ChipAccessKey}s are supported:
 * <ul>
 *   <li>{@link FromCan} - Use the Card Access Number (CAN) to access the chip.
 *      </li>
 *   <li>{@link FromMrz} - Use details from the MRZ (document number, date of
 *      birth, date of expiry) to access the chip.</li>
 * </ul>
 */
public abstract class ChipAccessKey {
	protected abstract JSONObject toJson();

	/**
	 * Access key to authenticate to the chip of the eMRTD using the Card Access
	 * Number (CAN).
	 * <p>
	 * Authentication using the CAN is only supported for Password Authenticated
	 * Connection Establishment (PACE) and is not available with Basic Access
	 * Control (BAC).
	 */
	public static class FromCan extends ChipAccessKey {
		private final String can;

		/**
		 * Creates a new instance of this class.
		 *
		 * @param can The Card Access Number.
		 */
		public FromCan(String can) {
			this.can = can;
		}

		/**
		 * Gets the Card Access Number.
		 *
		 * @return The Card Access Number.
		 */
		public String getCan() {
			return can;
		}

		@Override
		protected JSONObject toJson() {
			JSONObject accessKey = new JSONObject();
			try {
				accessKey.put("can", this.can);
			} catch (JSONException e) {
				String msg = "Failed to create Access Key JSON";
				throw new IllegalStateException(msg, e);
			}
			return accessKey;
		}
	}

	/**
	 * Access key to authenticate to the chip of the eMRTD with data from the
	 * MRZ, namely the document number, data of birth, and the expiry date of
	 * the document.
	 */
	public static class FromMrz extends ChipAccessKey {
		private final String documentNumber;
		private final String dateOfBirth;
		private final String dateOfExpiry;

		/**
		 * Creates a new instance of this class.
		 *
		 * @param documentNumber The document number,
		 * @param dateOfBirth    The birth date of the document holder.
		 * @param dateOfExpiry   The expiry date of the document.
		 */
		public FromMrz(
			String documentNumber,
			String dateOfBirth,
			String dateOfExpiry) {
			this.documentNumber = documentNumber;
			this.dateOfBirth = dateOfBirth;
			this.dateOfExpiry = dateOfExpiry;
		}

		/**
		 * Gets the document number.
		 *
		 * @return The document number.
		 */
		public String getDocumentNumber() {
			return documentNumber;
		}

		/**
		 * Gets the birth date of the document holder.
		 *
		 * @return The birth date of the document holder.
		 */
		public String getDateOfBirth() {
			return dateOfBirth;
		}

		/**
		 * Gets the expiry date of the document.
		 *
		 * @return The expiry date of the document.
		 */
		public String getDateOfExpiry() {
			return dateOfExpiry;
		}

		@Override
		protected JSONObject toJson() {
			JSONObject accessKey = new JSONObject();
			try {
				accessKey.put("document_number", this.documentNumber);
				accessKey.put("date_of_birth", this.dateOfBirth);
				accessKey.put("date_of_expiry", this.dateOfExpiry);
			} catch (JSONException e) {
				String msg = "Failed to create Access Key JSON";
				throw new IllegalStateException(msg, e);
			}
			return accessKey;
		}
	}
}
