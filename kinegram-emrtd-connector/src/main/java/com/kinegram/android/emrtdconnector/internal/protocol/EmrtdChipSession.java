package com.kinegram.android.emrtdconnector.internal.protocol;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import com.kinegram.android.emrtdconnector.ChipAccessKey;
import com.kinegram.android.emrtdconnector.ClosedListener;
import com.kinegram.android.emrtdconnector.ConnectionOptions;
import com.kinegram.android.emrtdconnector.EmrtdConnector;
import com.kinegram.android.emrtdconnector.internal.IsoDepCardService;
import com.kinegram.emrtd.AccessInformation;
import com.kinegram.emrtd.EmrtdReader;
import com.kinegram.emrtd.EmrtdReaderException;
import com.kinegram.emrtd.EmrtdResult;
import com.kinegram.emrtd.EmrtdStep;
import com.kinegram.emrtd.RemoteChipAuthentication;
import com.kinegram.emrtd.protocols.AccessControlProtocolException;

import net.sf.scuba.smartcards.CardService;

import org.jmrtd.protocol.SecureMessagingWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * Handles all logic for interacting with an NFC eMRTD.
 */
public class EmrtdChipSession {
    /**
     * NFC timeout in milliseconds.
     * <p>
     * Must be high enough to cover PACE operations (GENERAL AUTHENTICATE step
     * 2/3), which can involve large payloads and non-trivial crypto on the
     * chip.
     * <p>
     * ICAO Doc 9303-10 App. B.11 recommends that the PICC keep the total
     * processing time for a single I-Block <= 5s even when using S(WTX). This
     * is a PICC-side guideline, it is not the same as the host timeout. The
     * Android IsoDep timeout must exceed the end-to-end APDU round trip
     * (including any WTX handled by the NFC stack), so we have to use an even
     * higher limit to have some headroom.
     */
    public static final int NFC_TIMEOUT_MS = 10_000;

    /**
     * Listener for steps that happen during the session.
     */
    public interface Listener {
        void onEmrtdStep(EmrtdStep step);

        void onFileReady(String name, byte[] data); // e.g. "dg14", "sod"

        void onFinish(EmrtdResult result);

        /**
         * Delegates the chip authentication to the listener.
         */
        CompletableFuture<RemoteChipAuthentication.Result> onChipAuthenticationHandover(int maxTransceiveLength, int maxBlockSize, SecureMessagingWrapper wrapper);

        void onError(Exception e, String reason);
    }

    private final IsoDep isoDep;
    private final ConnectionOptions options;
    private final Listener listener;
    private final EmrtdReader emrtdReader = new EmrtdReader();

    public EmrtdChipSession(IsoDep isoDep, ConnectionOptions options, Listener listener) {
        this.isoDep = isoDep;
        this.options = options;
        this.listener = listener;
    }

    public void start(byte[] activeAuthenticationChallenge) {
        Span chipSessionSpan = EmrtdConnector.getTracer().spanBuilder("nfc_chip_session")
                .setAttribute("nfc.timeout_ms", NFC_TIMEOUT_MS)
                .setAttribute("emrtd_connector.validation_id", options.getValidationId())
                .setAttribute("has_active_auth_challenge", activeAuthenticationChallenge != null)
                .startSpan();

        try (Scope ignored = chipSessionSpan.makeCurrent()) {
            isoDep.setTimeout(NFC_TIMEOUT_MS);

            if (!isoDep.isConnected()) {
                chipSessionSpan.addEvent("nfc_connecting");
                isoDep.connect();
            }

            chipSessionSpan.addEvent("nfc_connected",
                    Attributes.builder()
                            .put("nfc.connected", isoDep.isConnected())
                            .put("nfc.extended_length_supported", isoDep.isExtendedLengthApduSupported())
                            .build());

            CardService cardService = new IsoDepCardService(isoDep, options.isDiagnosticsEnabled());
            try {
                EmrtdResult result = readEmrtdData(cardService, activeAuthenticationChallenge);

                chipSessionSpan.addEvent("chip_read_completed",
                        Attributes.builder()
                                .put("emrtd.data_groups_count", result.dataGroupsRawBinary.size())
                                .put("emrtd.sod_present", result.sodRawBinary != null)
                                .put("emrtd.has_active_auth_result", result.activeAuthenticationResult != null)
                                .build());

                for (Map.Entry<Integer, byte[]> entry : result.dataGroupsRawBinary.entrySet()) {
                    String name = "dg" + entry.getKey();
                    listener.onFileReady(name, entry.getValue());
                }
                listener.onFileReady("sod", result.sodRawBinary);
                listener.onFinish(result);
            } catch (EmrtdReaderException e) {
                chipSessionSpan.recordException(e);
                if (hasNfcCommunicationErrorInCauseChain(e)) {
                    chipSessionSpan.setStatus(StatusCode.ERROR, "NFC communication failed");
                    listener.onError(e, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
                } else {
                    chipSessionSpan.setStatus(StatusCode.ERROR, "EMRTD reader error");
                    listener.onError(e, ClosedListener.EMRTD_PASSPORT_READER_ERROR);
                }
            } catch (AccessControlProtocolException e) {
                chipSessionSpan.recordException(e);
                if (hasNfcCommunicationErrorInCauseChain(e)) {
                    chipSessionSpan.setStatus(StatusCode.ERROR, "NFC communication failed");
                    listener.onError(e, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
                } else {
                    chipSessionSpan.setStatus(StatusCode.ERROR, "Access control failed");
                    listener.onError(e, ClosedListener.ACCESS_CONTROL_FAILED);
                }
            } finally {
                cardService.close();
            }
        } catch (IOException | SecurityException e) {
            chipSessionSpan.recordException(e);
            chipSessionSpan.setStatus(StatusCode.ERROR, "NFC communication failed");
            listener.onError(e, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
        } finally {
            chipSessionSpan.end();
        }
    }

    /**
     * Checks if the exception or any exception in its cause chain is an NFC communication error.
     */
    private boolean hasNfcCommunicationErrorInCauseChain(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            // IOException and SecurityException are always communication errors
            if (current instanceof IOException || current instanceof SecurityException) {
                return true;
            }

            String className = current.getClass().getName();

            // TagLost exceptions are always communication errors
            if (className.contains("TagLost")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private EmrtdResult readEmrtdData(CardService cardService, byte[] activeAuthenticationChallenge)
            throws EmrtdReaderException, AccessControlProtocolException {
        return emrtdReader.read(
                cardService,
                getAccessInformation(),
                false,
                null,
                null,
                createProgressListener(),
                TagLostException.class,
                this::handleChipAuthentication,
                activeAuthenticationChallenge
        );
    }

    private EmrtdReader.ProgressListener createProgressListener() {
        return new EmrtdReader.ProgressListener() {
            @Override
            public void onNewUpdate(EmrtdStep emrtdStep) {
                listener.onEmrtdStep(emrtdStep);
            }

            @Override
            public void onFileReadProgress(int current, int total) {
                // Could be used in the future to communicate progress
            }
        };
    }

    private RemoteChipAuthentication.Result handleChipAuthentication(
            byte[] dg14Raw,
            int maxTransceiveLengthForSecureMessaging,
            int maxBlockSize,
            SecureMessagingWrapper secureMessagingWrapper) {

        Span authSpan = EmrtdConnector.getTracer().spanBuilder("chip_authentication")
                .setAttribute("nfc.max_transceive_length", maxTransceiveLengthForSecureMessaging)
                .setAttribute("nfc.max_block_size", maxBlockSize)
                .setAttribute("file.size", dg14Raw.length)
                .startSpan();

        try (Scope ignored = authSpan.makeCurrent()) {
            listener.onFileReady("dg14", dg14Raw);

            authSpan.addEvent("chip_auth_handover_started");
            CompletableFuture<RemoteChipAuthentication.Result> future = listener.onChipAuthenticationHandover(
                    maxTransceiveLengthForSecureMessaging, maxBlockSize, secureMessagingWrapper);

            RemoteChipAuthentication.Result result;
            try {
                result = future.get(20, TimeUnit.SECONDS);
                authSpan.addEvent("chip_auth_handover_completed",
                        Attributes.builder()
                                .put("emrtd.auth_successful", result != null)
                                .build());
                authSpan.setStatus(StatusCode.OK);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                authSpan.recordException(e);
                authSpan.setStatus(StatusCode.ERROR, "Chip authentication timeout or error");
                listener.onError(e, ClosedListener.PROTOCOL_ERROR);
                throw new RuntimeException(e);
            }

            return result;
        } finally {
            authSpan.end();
        }
    }

    private AccessInformation getAccessInformation() {
        ChipAccessKey accessKey = options.getChipAccessKey();

        if (accessKey instanceof ChipAccessKey.FromCan) {
            ChipAccessKey.FromCan canKey = (ChipAccessKey.FromCan) accessKey;
            return new AccessInformation.WithCan(canKey.getCan());
        } else {
            ChipAccessKey.FromMrz mrzKey = (ChipAccessKey.FromMrz) accessKey;
            return new AccessInformation.WithMrzDetails(
                    mrzKey.getDocumentNumber(),
                    mrzKey.getDateOfBirth(),
                    mrzKey.getDateOfExpiry()
            );
        }
    }
}
