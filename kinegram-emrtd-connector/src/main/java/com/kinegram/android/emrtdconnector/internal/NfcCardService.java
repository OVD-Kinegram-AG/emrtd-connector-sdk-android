package com.kinegram.android.emrtdconnector.internal;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.util.Log;

import com.kinegram.android.emrtdconnector.EmrtdConnector;

import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ResponseAPDU;

import java.io.IOException;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * Sends APDUs to the chip over the {@link IsoDep} connection of an NFC tag.
 * <p>
 * Each {@link #transmit(CommandAPDU)} is traced as one span. A lost tag is
 * always a {@link TagLostException} in the cause chain of the thrown
 * {@link CardServiceException}.
 */
public class NfcCardService extends CardService {
    private static final String TAG = NfcCardService.class.getSimpleName();
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private final IsoDep isoDep;
    private final boolean enableDiagnostics;

    /**
     * @param isoDep            the connection to the chip
     * @param enableDiagnostics whether the trace holds the raw APDU bytes
     */
    public NfcCardService(IsoDep isoDep, boolean enableDiagnostics) {
        this.isoDep = isoDep;
        this.enableDiagnostics = enableDiagnostics;
    }

    /**
     * Connects to the chip. Does nothing if the connection is open already.
     *
     * @throws CardServiceException if the connection cannot be opened
     */
    @Override
    public void open() throws CardServiceException {
        if (isOpen()) {
            return;
        }
        try {
            isoDep.connect();
        } catch (IOException e) {
            Log.w(TAG, "Cannot connect to the chip", e);
            throw new CardServiceException(e.toString());
        }
        if (!isOpen()) {
            throw new CardServiceException("NFC connection failed");
        }
    }

    /**
     * Returns whether the connection to the chip is open.
     */
    @Override
    public boolean isOpen() {
        return isoDep.isConnected();
    }

    /**
     * Sends one command to the chip.
     *
     * @return the response of the chip, including the status word
     * @throws CardServiceException if the exchange fails. The cause is the
     *                              original error.
     */
    @Override
    public ResponseAPDU transmit(CommandAPDU command) throws CardServiceException {
        Span span = EmrtdConnector.getTracer().spanBuilder("iso_dep_transmit")
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            if (!isOpen()) {
                throw new TagLostException("Not Connected");
            }
            span.addEvent("transmit_apdu_command", commandAttributes(command));
            byte[] response = isoDep.transceive(command.getBytes());
            if (response == null) {
                // The IsoDep docs say that this cannot happen.
                throw new AssertionError("Unexpected IsoDep null response");
            }
            span.addEvent("received_apdu_response", responseAttributes(response));
            if (response.length < 2) {
                // A response without a status word is handled like a lost tag.
                throw new TagLostException(
                        "Received APDU response with less than 2 bytes ("
                                + response.length + ")");
            }
            return new ResponseAPDU(response);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw new CardServiceException(e.getMessage(), e);
        } finally {
            span.end();
        }
    }

    /**
     * Returns the higher layer response for an NfcB tag and the historical
     * bytes for all other tags. Returns null if there is no tag.
     */
    @Override
    public byte[] getATR() {
        Tag tag = isoDep.getTag();
        if (tag == null) {
            return null;
        }
        if (NfcB.get(tag) != null) {
            return isoDep.getHiLayerResponse();
        }
        return isoDep.getHistoricalBytes();
    }

    @Override
    public boolean isExtendedAPDULengthSupported() {
        return isoDep.isExtendedLengthApduSupported();
    }

    /**
     * Closes the connection to the chip. Never throws.
     */
    @Override
    public void close() {
        try {
            isoDep.close();
        } catch (Exception e) {
            // The session ends either way, so this error does not matter.
            Log.d(TAG, "Ignored error on close", e);
        }
    }

    /**
     * Returns whether the exception or one of its causes is a
     * {@link TagLostException}.
     */
    @Override
    public boolean isConnectionLost(Exception e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof TagLostException) {
                return true;
            }
        }
        return false;
    }

    private Attributes commandAttributes(CommandAPDU command) {
        AttributesBuilder attributes = Attributes.builder()
                .put("command_apdu.cla", command.getCLA())
                .put("command_apdu.ins", command.getINS())
                .put("command_apdu.p1", command.getP1())
                .put("command_apdu.p2", command.getP2())
                .put("command_apdu.nc", command.getNc())
                .put("command_apdu.ne", command.getNe());
        if (enableDiagnostics) {
            attributes.put("command_apdu.bytes_hex", toHex(command.getBytes()));
        }
        return attributes.build();
    }

    private Attributes responseAttributes(byte[] response) {
        AttributesBuilder attributes = Attributes.builder()
                .put("apdu_response.length", response.length);
        if (enableDiagnostics) {
            attributes.put("apdu_response.bytes_hex", toHex(response));
        }
        return attributes.build();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(HEX_DIGITS[(b >> 4) & 0x0F]);
            hex.append(HEX_DIGITS[b & 0x0F]);
        }
        return hex.toString();
    }
}
