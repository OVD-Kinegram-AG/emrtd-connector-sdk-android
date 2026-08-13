package com.kinegram.android.emrtdconnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

/**
 * Tests {@link BaggageHeader} against the parser that the DocVal server uses.
 */
public class BaggageHeaderTest {
    /** Presents the header value to the parser as the {@code baggage} entry. */
    private static final TextMapGetter<String> GETTER = new TextMapGetter<String>() {
        @Override
        public Iterable<String> keys(String carrier) {
            return Arrays.asList("baggage");
        }

        @Override
        public String get(String carrier, String key) {
            return carrier;
        }
    };

    @Test
    public void keepsValuesThatTheGrammarAllows() {
        assertHeader("k=a=b", "k", "a=b");
        assertHeader("k=a:b/c", "k", "a:b/c");
        assertHeader("k=a[b]{c}<d>?e@f", "k", "a[b]{c}<d>?e@f");
        assertHeader("k=!#$&'()*+-.^_`|~", "k", "!#$&'()*+-.^_`|~");
    }

    @Test
    public void encodesValuesThatTheGrammarForbids() {
        assertHeader("k=a%20b", "k", "a b");
        assertHeader("k=a%2Cb", "k", "a,b");
        assertHeader("k=a%3Bb", "k", "a;b");
        assertHeader("k=a%5Cb", "k", "a\\b");
        assertHeader("k=a%22b", "k", "a\"b");
        assertHeader("k=a%0Ab", "k", "a\nb");
        assertHeader("k=a%7Fb", "k", "a" + (char) 0x7F + "b");
        assertHeader("k=M%C3%BCller", "k", "Müller");
        assertHeader("k=%F0%9F%98%80", "k", "😀");
    }

    /**
     * The grammar allows the percent sign, but a parser reads it as the start of
     * an escape sequence. Without encoding it, the server would decode the value
     * a second time.
     */
    @Test
    public void encodesThePercentSign() {
        assertHeader("k=100%25", "k", "100%");
        assertHeader("k=%2541", "k", "%41");
    }

    @Test
    public void sortsEntriesByKey() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("z_last", "1");
        entries.put("a_first", "2");
        assertEquals("a_first=2,z_last=1", BaggageHeader.accept(entries).header);
    }

    @Test
    public void dropsInvalidEntriesAndKeepsTheOthers() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("good_one", "value one");
        entries.put("bad key", "x");
        entries.put("empty", "");
        entries.put("too_long", repeat("x", BaggageHeader.MAX_VALUE_LENGTH + 1));
        entries.put(null, "x");
        BaggageHeader.Entries accepted = BaggageHeader.accept(entries);
        assertEquals("good_one=value%20one", accepted.header);
        assertEquals("[good_one]", accepted.map.keySet().toString());
    }

    @Test
    public void keepsAValueOfTheMaximumLength() {
        String value = repeat("x", BaggageHeader.MAX_VALUE_LENGTH);
        assertEquals(value, BaggageHeader.accept(one("k", value)).map.get("k"));
    }

    /** The length counts the characters of the value, as the server does. */
    @Test
    public void measuresTheLengthBeforeTheEncoding() {
        String halfTheLimitInEmoji = repeat("😀", BaggageHeader.MAX_VALUE_LENGTH / 2);
        assertEquals(1, BaggageHeader.accept(one("k", halfTheLimitInEmoji)).map.size());
        assertEquals(0, BaggageHeader.accept(one("k", halfTheLimitInEmoji + "😀")).map.size());
    }

    @Test
    public void sendsNoHeaderWhenNoEntryPasses() {
        BaggageHeader.Entries accepted = BaggageHeader.accept(one("bad key", "x"));
        assertNull(accepted.header);
        assertTrue(accepted.map.isEmpty());
    }

    /**
     * Entries that do not fit into {@link BaggageHeader#MAX_LENGTH} are dropped,
     * and the header is not cut off. A short entry after a dropped one still
     * fits.
     */
    @Test
    public void dropsEntriesThatDoNotFitIntoTheHeader() {
        Map<String, String> entries = new TreeMap<>();
        int enoughToOverflow =
                BaggageHeader.MAX_LENGTH / BaggageHeader.MAX_VALUE_LENGTH + 2;
        for (int i = 0; i < enoughToOverflow; i++) {
            entries.put(String.format("key_%03d", i),
                    repeat("y", BaggageHeader.MAX_VALUE_LENGTH));
        }
        entries.put("z", "1");
        BaggageHeader.Entries accepted = BaggageHeader.accept(entries);
        assertTrue(accepted.header.length() <= BaggageHeader.MAX_LENGTH);
        assertTrue(accepted.header.endsWith(",z=1"));
        assertEquals(accepted.map, decode(accepted.header));
    }

    /**
     * What {@link ConnectionOptions#getBaggage()} reports must be what the server
     * reads. This must also hold when entries are dropped.
     */
    @Test
    public void reportsExactlyWhatTheServerReads() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("k_space", "a b");
        entries.put("k_comma", "a,b");
        entries.put("k_equals", "a=b");
        entries.put("k_percent", "100%");
        entries.put("k_umlaut", "Müller-Lüdenscheidt");
        entries.put("k_emoji", "id-😀");
        entries.put("k_quote", "a\"b\\c");
        entries.put("k_plus", "a+b");
        entries.put("bad key", "dropped");
        BaggageHeader.Entries accepted = BaggageHeader.accept(entries);
        assertEquals(accepted.map, decode(accepted.header));
        assertEquals(8, accepted.map.size());
    }

    /** Asserts the header value and that the parser returns the value again. */
    private static void assertHeader(String header, String key, String value) {
        BaggageHeader.Entries accepted = BaggageHeader.accept(one(key, value));
        assertEquals(header, accepted.header);
        assertEquals(value, decode(accepted.header).get(key));
    }

    /** Returns what the parser of the DocVal server reads from the header. */
    private static Map<String, String> decode(String header) {
        Context context = W3CBaggagePropagator.getInstance()
                .extract(Context.root(), header, GETTER);
        Map<String, String> entries = new TreeMap<>();
        Baggage.fromContext(context)
                .forEach((key, entry) -> entries.put(key, entry.getValue()));
        return entries;
    }

    private static Map<String, String> one(String key, String value) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(key, value);
        return entries;
    }

    private static String repeat(String value, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
