package com.kinegram.android.emrtdconnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.kinegram.emrtd.DataGroupSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Tests how {@link ConnectionOptions} keeps the headers of the caller apart from
 * the headers of the handshake, and what it makes of a data group selection.
 */
public class ConnectionOptionsTest {
    @Test
    public void httpHeadersHoldOnlyWhatWasSet() {
        ConnectionOptions options = optionsWith(
                one("Authorization", "Bearer token"),
                one("example_id", "1a2b3c4d5e6f"));
        assertEquals("[Authorization]", options.getHttpHeaders().keySet().toString());
    }

    @Test
    public void handshakeHeadersHoldTheBaggageHeader() {
        ConnectionOptions options = optionsWith(
                one("Authorization", "Bearer token"),
                one("example_id", "1a2b3c4d5e6f"));
        Map<String, String> handshake = options.headersForHandshake();
        assertEquals("Bearer token", handshake.get("Authorization"));
        assertEquals("example_id=1a2b3c4d5e6f", handshake.get("baggage"));
    }

    @Test
    public void handshakeHeadersHoldNoBaggageHeaderWithoutEntries() {
        ConnectionOptions options = optionsWith(
                one("Authorization", "Bearer token"), null);
        assertFalse(options.headersForHandshake().containsKey("baggage"));
    }

    /** A header name is not case sensitive, so the case must not open a gap. */
    @Test
    public void baggageEntriesReplaceAManualBaggageHeader() {
        ConnectionOptions options = optionsWith(
                one("Baggage", "stale=1"),
                one("example_id", "1a2b3c4d5e6f"));
        Map<String, String> handshake = options.headersForHandshake();
        assertEquals("[baggage]", handshake.keySet().toString());
        assertEquals("example_id=1a2b3c4d5e6f", handshake.get("baggage"));
    }

    @Test
    public void aReadWithoutASelectionReadsTheStandardDataGroups() {
        ConnectionOptions options = new ConnectionOptions.Builder()
                .setChipAccessKeyFromCan("123456")
                .build();
        assertEquals(DataGroupSet.STANDARD, options.getFilesToRead());
    }

    @Test
    public void aSelectionReachesTheOptions() {
        ConnectionOptions options = new ConnectionOptions.Builder()
                .setChipAccessKeyFromCan("123456")
                .setFilesToRead(DataGroupSet.MINIMAL_KYC)
                .build();
        assertEquals(DataGroupSet.MINIMAL_KYC, options.getFilesToRead());
    }

    /** Clearing a selection must not leave a read without any files. */
    @Test
    public void noSelectionFallsBackToTheStandardDataGroups() {
        ConnectionOptions options = new ConnectionOptions.Builder()
                .setChipAccessKeyFromCan("123456")
                .setFilesToRead(DataGroupSet.MINIMAL)
                .setFilesToRead(null)
                .build();
        assertEquals(DataGroupSet.STANDARD, options.getFilesToRead());
    }

    private static ConnectionOptions optionsWith(
            Map<String, String> httpHeaders, Map<String, String> baggage) {
        return new ConnectionOptions.Builder()
                .setChipAccessKeyFromCan("123456")
                .setHttpHeaders(httpHeaders)
                .setBaggage(baggage)
                .build();
    }

    private static Map<String, String> one(String key, String value) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(key, value);
        return entries;
    }
}
