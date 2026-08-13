package com.kinegram.android.emrtdconnector;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds the value of the {@code baggage} header from a map of entries. The
 * format is W3C Baggage (Working Draft, https://www.w3.org/TR/baggage/).
 */
class BaggageHeader {
    /** Name of the header that carries the entries. */
    static final String NAME = "baggage";

    /**
     * Maximum number of characters of a value. The DocVal server drops longer
     * values, so a longer value is already dropped here.
     */
    static final int MAX_VALUE_LENGTH = 128;

    /**
     * Maximum number of characters of the whole header value. This is the limit
     * of the specification. The encoded header holds only ASCII characters, so
     * one character is one byte.
     */
    static final int MAX_LENGTH = 8192;

    /** Punctuation that a key may contain, from the token rule of RFC 7230. */
    private static final String TOKEN_PUNCTUATION = "!#$%&'*+-.^_`|~";

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private static final String TAG = BaggageHeader.class.getSimpleName();

    private BaggageHeader() {
    }

    /** The entries that the header carries, and the value of the header. */
    static final class Entries {
        /** The entries, sorted by key. Empty if the header carries none. */
        final Map<String, String> map;

        /** The header value, or {@code null} if the header carries no entry. */
        final String header;

        private Entries(Map<String, String> map, String header) {
            this.map = Collections.unmodifiableMap(map);
            this.header = header;
        }
    }

    /**
     * Returns the entries that pass the checks, sorted by key, together with the
     * header value that carries them. Sorting keeps the header stable, because
     * it does not depend on the order of the given map.
     * <p>
     * Never throws. Writes a warning for every entry that it drops. An entry is
     * dropped if:
     * <ul>
     *   <li>the key is not a token of RFC 7230,
     *   <li>the value is empty,
     *   <li>the value has more than {@link #MAX_VALUE_LENGTH} characters,
     *   <li>the entry does not fit into {@link #MAX_LENGTH} characters.
     * </ul>
     *
     * @param entries The entries to check.
     * @return The accepted entries and the header value.
     */
    static Entries accept(Map<String, String> entries) {
        Map<String, String> accepted = new TreeMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || !isToken(key)) {
                Log.w(TAG, "Ignoring baggage entry with an invalid key: " + key);
            } else if (value == null || value.isEmpty()) {
                Log.w(TAG, "Ignoring baggage entry with an empty value for key "
                        + key);
            } else if (value.length() > MAX_VALUE_LENGTH) {
                Log.w(TAG, "Ignoring baggage entry for key " + key
                        + ", because the value has more than "
                        + MAX_VALUE_LENGTH + " characters");
            } else {
                accepted.put(key, value);
            }
        }
        return encode(accepted);
    }

    /**
     * Returns the given entries and their header value, without the entries that
     * do not fit into {@link #MAX_LENGTH} characters. Removes those entries from
     * the given map.
     */
    private static Entries encode(Map<String, String> accepted) {
        StringBuilder header = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator =
                accepted.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            int lengthBeforeEntry = header.length();
            if (lengthBeforeEntry > 0) {
                header.append(',');
            }
            header.append(entry.getKey()).append('=');
            appendEncoded(header, entry.getValue());
            if (header.length() > MAX_LENGTH) {
                header.setLength(lengthBeforeEntry);
                iterator.remove();
                Log.w(TAG, "Ignoring baggage entry for key " + entry.getKey()
                        + ", because the header is limited to " + MAX_LENGTH
                        + " characters");
            }
        }
        return new Entries(
                accepted, header.length() == 0 ? null : header.toString());
    }

    /**
     * Appends the value to the header, with every UTF-8 byte that a value must
     * not hold written as a percent-encoded octet.
     * <p>
     * {@code java.net.URLEncoder} is not usable here, because it writes a space
     * as {@code +}, which a percent-decoder keeps as a plus sign.
     */
    private static void appendEncoded(StringBuilder header, String value) {
        for (byte encodedByte : value.getBytes(StandardCharsets.UTF_8)) {
            int octet = encodedByte & 0xFF;
            if (isBaggageOctet(octet)) {
                header.append((char) octet);
            } else {
                header.append('%')
                        .append(HEX_DIGITS[octet >> 4])
                        .append(HEX_DIGITS[octet & 0x0F]);
            }
        }
    }

    /**
     * Returns {@code true} if the octet can stay as it is in a value.
     * <p>
     * These are the baggage-octets of the specification, that is the visible
     * ASCII characters without comma, semicolon, backslash and double quote.
     * The percent sign is the one exception: the specification allows it, but a
     * parser reads it as the start of an escape sequence.
     */
    private static boolean isBaggageOctet(int octet) {
        if (octet == '%') {
            return false;
        }
        return octet == 0x21
                || (octet >= 0x23 && octet <= 0x2B)
                || (octet >= 0x2D && octet <= 0x3A)
                || (octet >= 0x3C && octet <= 0x5B)
                || (octet >= 0x5D && octet <= 0x7E);
    }

    /** Returns {@code true} if the key is a non-empty token of RFC 7230. */
    private static boolean isToken(String key) {
        if (key.isEmpty()) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            char character = key.charAt(i);
            if (!isAlphanumeric(character)
                    && TOKEN_PUNCTUATION.indexOf(character) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlphanumeric(int character) {
        return (character >= 'A' && character <= 'Z')
                || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9');
    }
}
