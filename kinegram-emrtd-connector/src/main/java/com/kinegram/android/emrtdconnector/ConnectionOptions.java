package com.kinegram.android.emrtdconnector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options to configure the eMRTD read.
 */
public class ConnectionOptions {
    private final String validationId;
    private final boolean enableDiagnostics;
    private final ChipAccessKey chipAccessKey;
    private final Map<String, String> baggage;
    private final String baggageHeader;
    private final Map<String, String> httpHeaders;

    private ConnectionOptions(Builder builder) {
        this.validationId = builder.validationId;
        this.enableDiagnostics = builder.enableDiagnostics;
        this.chipAccessKey = builder.chipAccessKey;
        this.baggage = builder.baggage;
        this.baggageHeader = builder.baggageHeader;
        if (builder.httpHeaders == null) {
            this.httpHeaders = Collections.emptyMap();
        } else {
            this.httpHeaders = Collections.unmodifiableMap(
                    new LinkedHashMap<>(builder.httpHeaders));
        }
    }

    /**
     * Returns the http headers for the websocket handshake. These are the
     * headers from {@link Builder#setHttpHeaders(Map)} together with the
     * {@code baggage} header of the entries from
     * {@link Builder#setBaggage(Map)}.
     */
    Map<String, String> headersForHandshake() {
        Map<String, String> headers = new LinkedHashMap<>(httpHeaders);
        if (baggageHeader != null) {
            headers.keySet().removeIf(BaggageHeader.NAME::equalsIgnoreCase);
            headers.put(BaggageHeader.NAME, baggageHeader);
        }
        return headers;
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
     * Gets the baggage entries that become the log context of the session in
     * the DocVal server. Holds the entries that are sent, so it leaves out the
     * entries that {@link Builder#setBaggage(Map)} dropped.
     *
     * @return The baggage entries. Empty if none are sent.
     */
    public Map<String, String> getBaggage() {
        return this.baggage;
    }

    /**
     * Gets the additional http headers for the websocket connection, as they
     * were set.
     *
     * @return The http headers. Empty if none are set.
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
        private Map<String, String> baggage = Collections.emptyMap();
        private String baggageHeader;
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
         * Sets the baggage entries that become the log context of the session
         * in the DocVal server.
         * <p>
         * The server adds the entries as fields to every log line of the
         * session. It only takes keys that are on its allow-list.
         * <p>
         * The entries go into the {@code baggage} header of the handshake, as
         * defined by W3C Baggage. This method encodes the values, sorts the
         * entries by key, and replaces a {@code baggage} header from
         * {@link #setHttpHeaders(Map)}. A key must be a token of RFC 7230 and a
         * value must not be empty. An entry that the method cannot send is
         * dropped with a warning in the log. The method never throws.
         * {@code null} or an empty map removes the entries.
         *
         * @param baggage The baggage entries.
         * @return This instance of the builder for easier chaining.
         */
        public Builder setBaggage(Map<String, String> baggage) {
            if (baggage == null || baggage.isEmpty()) {
                this.baggage = Collections.emptyMap();
                this.baggageHeader = null;
                return this;
            }
            BaggageHeader.Entries entries = BaggageHeader.accept(baggage);
            this.baggage = entries.map;
            this.baggageHeader = entries.header;
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
