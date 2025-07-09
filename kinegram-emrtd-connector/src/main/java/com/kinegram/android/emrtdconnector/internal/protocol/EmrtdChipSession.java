package com.kinegram.android.emrtdconnector.internal.protocol;

import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import com.kinegram.android.emrtdconnector.ChipAccessKey;
import com.kinegram.android.emrtdconnector.ClosedListener;
import com.kinegram.android.emrtdconnector.ConnectionOptions;
import com.kinegram.android.emrtdconnector.internal.IsoDepCardService;
import com.kinegram.emrtd.AccessInformation;
import com.kinegram.emrtd.EmrtdReader;
import com.kinegram.emrtd.EmrtdReaderException;
import com.kinegram.emrtd.EmrtdResult;
import com.kinegram.emrtd.EmrtdStep;
import com.kinegram.emrtd.RemoteChipAuthentication;
import com.kinegram.emrtd.protocols.AccessControlProtocolException;

import net.sf.scuba.smartcards.CardService;

import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.protocol.SecureMessagingWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles all logic for interacting with an NFC eMRTD.
 */
public class EmrtdChipSession {
	public static final int NFC_TIMEOUT_MS = 1000;

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
		try {
			isoDep.setTimeout(NFC_TIMEOUT_MS);
			if (!isoDep.isConnected()) {
				isoDep.connect();
			}

			CardService cardService = new IsoDepCardService(isoDep);
			try {
				EmrtdResult result = readEmrtdData(cardService, activeAuthenticationChallenge);
				for (Map.Entry<Integer, byte[]> entry : result.dataGroupsRawBinary.entrySet()) {
					String name = "dg" + entry.getKey();
					listener.onFileReady(name, entry.getValue());
				}
				listener.onFileReady("sod", result.sodRawBinary);
				listener.onFinish(result);
			} catch (EmrtdReaderException e) {
				listener.onError(e, ClosedListener.EMRTD_PASSPORT_READER_ERROR);
			} catch (AccessControlProtocolException e) {
				listener.onError(e, ClosedListener.ACCESS_CONTROL_FAILED);
			} finally {
				cardService.close();
			}
		} catch (IOException e) {
			listener.onError(e, ClosedListener.NFC_CHIP_COMMUNICATION_FAILED);
		}
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
		DG14File dg14File,
		int maxTransceiveLengthForSecureMessaging,
		int maxBlockSize,
		SecureMessagingWrapper secureMessagingWrapper) {
		listener.onFileReady("dg14", dg14File.getEncoded());
		CompletableFuture<RemoteChipAuthentication.Result> future = listener.onChipAuthenticationHandover(maxTransceiveLengthForSecureMessaging, maxBlockSize, secureMessagingWrapper);

		RemoteChipAuthentication.Result result;
		try {
			result = future.get(20, TimeUnit.SECONDS);
		} catch (ExecutionException | InterruptedException | TimeoutException e) {
			listener.onError(e, ClosedListener.PROTOCOL_ERROR);
			throw new RuntimeException(e);
		}

		return result;
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
