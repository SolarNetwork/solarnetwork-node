/* ==================================================================
 * ChargeSessionDao.java - 9/06/2015 11:41:38 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.ocpp;

import java.util.Date;
import java.util.List;
import ocpp.v15.cs.MeterValue;

/**
 * DAO API for {@link ChargeSession} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionDao {

	/**
	 * Store (create or update) a charge session.
	 * 
	 * @param session
	 *        The session to store.
	 * @return The {@code sessionId} value as stored.
	 */
	String storeChargeSession(ChargeSession session);

	/**
	 * Get an {@link ChargeSession} for a given session ID.
	 * 
	 * @param sessionId
	 *        The session ID of the session to get.
	 * @return The associated charge session, or <em>null</em> if not available.
	 */
	ChargeSession getChargeSession(String sessionId);

	/**
	 * Get an <em>incomplete</em> charge session for a given socket. An
	 * <em>incomplete</em> session is one that has no {@code ended} date.
	 * 
	 * @param socketId
	 *        The socket ID to look for.
	 * @return The first available incomplete charge session, or <em>null</em>
	 *         if not found.
	 */
	ChargeSession getIncompleteChargeSessionForSocket(String socketId);

	/**
	 * Get an <em>incomplete</em> charge session for a given transaction ID. An
	 * <em>incomplete</em> session is one that has no {@code ended} date.
	 * 
	 * @param transactionId
	 *        The transaction ID to look for.
	 * @return The first available incomplete charge session, or <em>null</em>
	 *         if not found.
	 */
	ChargeSession getIncompleteChargeSessionForTransaction(int transactionId);

	/**
	 * Get all available <em>incomplete</em> charge sessions. An
	 * <em>incomplete</em> session is one that has no {@code ended} date.
	 * 
	 * @return All incomplete charge sessions, or an empty list if none found.
	 */
	List<ChargeSession> getIncompleteChargeSessions();

	/**
	 * Get all available charge sessions that need to be posted to the central
	 * system. A session needs to be posted if either it has no
	 * {@code trasactionId} (and thus must be included in a
	 * {@code StartTransaction} message) or it has an {@code ended} date but no
	 * {@code posted} date (and thus needs to be included in a
	 * {@code StopTransaction} message). The results are ordered oldest to
	 * newest by creation date.
	 * 
	 * @param max
	 *        The maximum number of rows to return.
	 * @return All charge sessions needing posting to the central system, or an
	 *         empty list if none found.
	 */
	List<ChargeSession> getChargeSessionsNeedingPosting(int max);

	/**
	 * Store one or more meter readings, associated with a charge session.
	 * 
	 * @param sessionId
	 *        The {@link ChargeSession#getSessionId()} to associate the readings
	 *        to.
	 * @param date
	 *        The date to associate all readings with, or <em>null</em> to use
	 *        the current time.
	 * @param readings
	 *        The readings to store.
	 */
	void addMeterReadings(String sessionId, Date date, Iterable<MeterValue.Value> readings);

	/**
	 * Get all available readings for a given session.
	 * 
	 * @param sessionId
	 *        The session ID to get the readings for.
	 * @return The readings, or an empty list if none available.
	 */
	List<ChargeSessionMeterReading> findMeterReadingsForSession(String sessionId);

	/**
	 * Delete all completed charge sessions that completed on or before
	 * {@code olderThanDate}.
	 * 
	 * @param olderThanDate
	 *        The completion date to delete up to, or <em>null</em> to use the
	 *        current time.
	 * @return The number of charge sessions deleted.
	 */
	int deleteCompletedChargeSessions(Date olderThanDate);

	/**
	 * Delete all incomplete charge sessions that <b>started</b> on or before
	 * {@code olderThanDate}.
	 * 
	 * @param olderThanDate
	 *        The start (created) date to delete up to, or <em>null</em> to use
	 *        the current time.
	 * @return The number of charge sessions deleted.
	 */
	int deleteIncompletedChargeSessions(Date olderThanDate);

}
