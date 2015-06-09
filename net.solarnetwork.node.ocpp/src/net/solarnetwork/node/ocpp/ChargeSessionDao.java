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
import ocpp.v15.MeterValue;

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
	 */
	void storeChargeSession(ChargeSession session);

	/**
	 * Get an {@link ChargeSession} for a given session ID.
	 * 
	 * @param sessionId
	 *        The session ID of the session to get.
	 * @return The associated charge session, or <em>null</em> if not available.
	 */
	ChargeSession getChargeSession(String sessionId);

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
	void storeMeterReadings(String sessionId, Date date, Iterable<MeterValue.Value> readings);

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
