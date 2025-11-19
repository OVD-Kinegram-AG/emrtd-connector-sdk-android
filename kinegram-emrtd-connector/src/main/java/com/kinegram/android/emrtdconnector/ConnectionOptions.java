package com.kinegram.android.emrtdconnector;

import java.util.HashMap;
import java.util.Map;

/**
 * Options to configure the eMRTD read.
 */
public class ConnectionOptions {
    private final String validationId;
    private final boolean enableDiagnostics;
    private final ChipAccessKey chipAccessKey;
    private final Map<String, String> httpHeaders;

    private ConnectionOptions(Builder builder) {
        this.validationId = builder.validationId;
        this.enableDiagnostics = builder.enableDiagnostics;
        this.chipAccessKey = builder.chipAccessKey;
        if (builder.httpHeaders == null) {
            this.httpHeaders = new HashMap<>();
        } else {
            this.httpHeaders = builder.httpHeaders;
        }
    }

    /**
     * Gets the validation id, a unique string to identify the session.
     *
     * @return The validation id.
     */
    public String getValidationId() {
        return validationId;
    }

    /**
     * Returns {@code true} if diagnostics are enabled.
     * <p>
     * When enabled, the DocVal server will collect and store extra diagnostic
     * information to troubleshoot issues. This data may contain personal
     * information.
     *
     * @return Whether diagnostics are enabled.
     */
    public boolean isDiagnosticsEnabled() {
        return enableDiagnostics;
    }

    /**
     * Gets the chip access key used to authenticate to the eMRTD's chip.
     *
     * @return The chip access key.
     */
    public ChipAccessKey getChipAccessKey() {
        return chipAccessKey;
    }

    /**
     * Gets additional http headers that will be used for the websocket
     * connection.
     *
     * @return The http headers.
     */
    public Map<String, String> getHttpHeaders() {
        return this.httpHeaders;
    }

    /**
     * A builder to create connection options for the {@link EmrtdConnector}.
     */
    public static class Builder {
        private String validationId;
        private boolean enableDiagnostics;
        private ChipAccessKey chipAccessKey;
        private Map<String, String> httpHeaders;

        public Builder() {
        }

        /**
         * Sets the access key for the chip access procedure to authenticate to
         * the chip of the eMRTD.
         * <p>
         * Uses the Card Access Number (CAN) as the access key.
         *
         * @param can The Card Access Number.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setChipAccessKeyFromCan(String can) {
            this.chipAccessKey = new ChipAccessKey.FromCan(can);
            return this;
        }

        /**
         * Sets the access key for the chip access procedure to authenticate to
         * the chip of the eMRTD.
         * <p>
         * Uses details from the MRZ as the access key.
         *
         * @param documentNumber The document number.
         * @param dateOfBirth    The birth date of the document holder.
         * @param dateOfExpiry   The expiry date of the document.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setChipAccessKeyFromMrz(
                String documentNumber,
                String dateOfBirth,
                String dateOfExpiry) {
            this.chipAccessKey = new ChipAccessKey.FromMrz(
                    documentNumber, dateOfBirth, dateOfExpiry);
            return this;
        }

        /**
         * Sets the access key for the chip access procedure to authenticate to
         * the chip of the eMRTD.
         *
         * @param chipAccessKey The key to set.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setChipAccessKey(ChipAccessKey chipAccessKey) {
            this.chipAccessKey = chipAccessKey;
            return this;
        }

        /**
         * Sets an unique string to identify the session.
         *
         * @param validationId The validation id.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setValidationId(String validationId) {
            this.validationId = validationId;
            return this;
        }

        /**
         * Enable or disable collection of diagnostics.
         * <p>
         * When enabled, the DocVal server will collect and store extra
         * diagnostic information to troubleshoot issues.
         * <p>
         * This information may contain personal data, so only enable it if you
         * have user consent.
         *
         * @param enableDiagnostics {@code true} to turn on diagnostics.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setEnableDiagnostics(boolean enableDiagnostics) {
            this.enableDiagnostics = enableDiagnostics;
            return this;
        }

        /**
         * Sets additional HTTP headers that will be set for the websocket
         * connection.
         * <p>
         * Can for example be used to set an Authorization header.
         *
         * @param httpHeaders The http headers.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setHttpHeaders(Map<String, String> httpHeaders) {
            this.httpHeaders = httpHeaders;
            return this;
        }

        /**
         * Build the connection options.
         *
         * @return The connection options.
         */
        public ConnectionOptions build() {
            if (chipAccessKey == null) {
                throw new IllegalArgumentException(
                        "A chip access key must be set");
            }
            return new ConnectionOptions(this);
        }
    }
}
