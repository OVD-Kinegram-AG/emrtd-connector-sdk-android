/*
 * This file is part of the SCUBA smart card framework.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Inspired by the work of Max Guenther (max.math.guenther@googlemail.com) for
 * aJMRTD (an Android client for JMRTD, released under the LGPL license).
 *
 * Copyright (C) 2009-2013 The SCUBA team.
 *
 * $Id: $
 */

package com.kinegram.android.emrtdconnector.internal;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;

import com.kinegram.emrtd.EmrtdReader;

import net.sf.scuba.smartcards.APDUEvent;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ResponseAPDU;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * Card service implementation for sending APDUs to a terminal using the
 * Android NFC (<code>android.nfc.tech</code>) classes available in Android
 * SDK 2.3.3 (API 10) and higher.
 *
 * @author Pim Vullers (pim@cs.ru.nl)
 * @author Tim Vogel (kurzdigital.com)
 */
// TODO This file is copied from the android-sdk. Do we want to use the SDK instead?
//  Probably not, it would be overkill to add a whole library just for a single class.
public class IsoDepCardService extends CardService {
	private static final Logger LOGGER = Logger.getLogger("net.sf.scuba");
	private final IsoDep isoDep;
	private int apduCount;

	/**
	 * Constructs a new card service.
	 *
	 * @param isoDep the card terminal to connect to
	 */
	public IsoDepCardService(IsoDep isoDep) {
		this.isoDep = isoDep;
		apduCount = 0;
	}

	/**
	 * Opens a session with the card.
	 */
	@Override
	public void open() throws CardServiceException {
		if (isOpen()) {
			return;
		}
		try {
			isoDep.connect();
			if (!isoDep.isConnected()) {
				throw new CardServiceException("Failed to connect");
			}
			state = SESSION_STARTED_STATE;
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to connect", e);
			throw new CardServiceException(e.toString());
		}
	}

	/**
	 * Whether there is an open session with the card.
	 */
	@Override
	public boolean isOpen() {
		if (isoDep.isConnected()) {
			state = SESSION_STARTED_STATE;
			return true;
		} else {
			state = SESSION_STOPPED_STATE;
			return false;
		}
	}

	/**
	 * Sends an APDU to the card.
	 *
	 * @param ourCommandAPDU the command apdu to send
	 * @return the response from the card, including the status word
	 * @throws CardServiceException - if the card operation failed
	 */
	@Override
	public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) throws CardServiceException {
		Span span = EmrtdReader.getTracer().spanBuilder("iso_dep_transmit")
			.startSpan();
		try (Scope ignored = span.makeCurrent()) {
			if (!isOpen()) {
				throw new TagLostException("Not Connected");
			}
			span.addEvent(
				"transmit_apdu_command",
				Attributes.builder()
					.put("command_apdu.cla", ourCommandAPDU.getCLA())
					.put("command_apdu.ins", ourCommandAPDU.getINS())
					.put("command_apdu.p1", ourCommandAPDU.getP1())
					.put("command_apdu.p2", ourCommandAPDU.getP2())
					.put("command_apdu.nc", ourCommandAPDU.getNc())
					.put("command_apdu.ne", ourCommandAPDU.getNe())
					// TODO Disabled for privacy reasons. Should only be enabled on user consent.
					// .put("command_apdu.bytes_base64",
					// Base64.getEncoder().encodeToString(commandAPDU.getBytes()))
					.build());
			byte[] responseBytes = isoDep.transceive(ourCommandAPDU.getBytes());
			if (responseBytes == null || responseBytes.length < 2) {
				throw new TagLostException("No Response");
			}
			span.addEvent(
				"received_apdu_response",
				Attributes.builder()
					.put("apdu_response.length", responseBytes.length)
					// TODO Disabled for privacy reasons. Should only be enabled on user consent.
					// .put("apdu_response.bytes_base64",
					// Base64.getEncoder().encodeToString(responseBytes))
					.build());
			ResponseAPDU ourResponseAPDU = new ResponseAPDU(responseBytes);
			APDUEvent event = new APDUEvent(
				this, "ISODep", ++apduCount, ourCommandAPDU, ourResponseAPDU
			);
			notifyExchangedAPDU(event);
			return ourResponseAPDU;
		} catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw new CardServiceException(e.getMessage(), e);
		} finally {
			span.end();
		}
	}

	@Override
	public byte[] getATR() {
		Tag tag;
		if (isoDep == null || (tag = isoDep.getTag()) == null) {
			return null;
		}
		return NfcB.get(tag) != null ? isoDep.getHiLayerResponse() : isoDep.getHistoricalBytes();
	}

	@Override
	public boolean isExtendedAPDULengthSupported() {
		return isoDep.isExtendedLengthApduSupported();
	}

	/**
	 * Closes the session with the card.
	 */
	@Override
	public void close() {
		try {
			isoDep.close();
			state = SESSION_STOPPED_STATE;
		} catch (Exception e) {
			/* Disconnect failed? Fine... */
		}
	}

	/**
	 * Determines whether an exception indicates a tag is lost event.
	 *
	 * @param e an exception
	 * @return whether the exception indicates a tag is lost event
	 */
	@Override
	public boolean isConnectionLost(Exception e) {
		// TagLostException is usually wrapped in a CardServiceException or deeper
		Throwable t = e;
		while (t != null) {
			if (t instanceof TagLostException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}
}
