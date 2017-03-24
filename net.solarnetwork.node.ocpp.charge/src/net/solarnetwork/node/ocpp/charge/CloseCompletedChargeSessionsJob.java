/* ==================================================================
 * CloseCompletedChargeSessionsJob.java - 24/03/2017 10:13:38 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge;

import java.util.List;
import java.util.ListIterator;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import ocpp.v15.cs.Measurand;

/**
 * Job to periodically look for active charge sessions that appear to have
 * finished because of a lack of power being drawn on the associated socket.
 * 
 * @author matt
 * @version 1.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class CloseCompletedChargeSessionsJob extends AbstractJob {

	private ChargeSessionManager service;
	private long maxAgeLastReading = (15 * 60 * 1000L);
	private int readingEnergyCount = 5;
	private long maxEnergy = 5;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		log.debug("Looking for OCPP active charge sessions that appear to be completed");
		for ( String socketId : service.availableSocketIds() ) {
			ChargeSession session = service.activeChargeSession(socketId);
			if ( session != null ) {
				List<ChargeSessionMeterReading> readings = service
						.meterReadingsForChargeSession(session.getSessionId());
				boolean close = false;
				if ( (readings == null || readings.isEmpty()) && session.getCreated() != null
						&& (session.getCreated().getTime() + maxAgeLastReading) < System
								.currentTimeMillis() ) {
					log.info(
							"OCCP charge session {} on socket {} has not recorded any readings since {}; closing session",
							session.getSessionId(), socketId, session.getCreated());
					close = true;
				} else if ( readings != null && !readings.isEmpty() ) {
					ChargeSessionMeterReading reading = readings.get(readings.size() - 1);
					if ( reading != null && reading.getTs() != null && reading.getTs().getTime()
							+ maxAgeLastReading < System.currentTimeMillis() ) {
						log.info(
								"OCCP charge session {} on socket {} has not recorded any readings since {}; closing session",
								session.getSessionId(), socketId, reading.getTs());
						close = true;
					} else if ( readingEnergyCount > 0 && readings.size() >= readingEnergyCount ) {
						// look to see if the energy drawn over the last few readings is about 0, meaning the battery is charged
						// we assume readings are taken at regular intervals
						ListIterator<ChargeSessionMeterReading> itr = readings
								.listIterator(readings.size());
						int count = 0;
						long whEnd = 0;
						long whStart = 0;
						while ( itr.hasPrevious() && count < readingEnergyCount ) {
							reading = itr.previous();
							if ( reading.getMeasurand() == Measurand.ENERGY_ACTIVE_IMPORT_REGISTER ) {
								count += 1;
								if ( count == 1 ) {
									whEnd = Long.valueOf(reading.getValue());
								} else if ( count == readingEnergyCount ) {
									whStart = Long.valueOf(reading.getValue());
									break;
								}
							}
						}
						if ( count == readingEnergyCount ) {
							long wh = whEnd - whStart;
							if ( wh < maxEnergy ) {
								log.info(
										"OCCP charge session {} on socket {} has only drawn {} Wh since {}; closing session",
										session.getSessionId(), socketId, wh, reading.getTs());
								close = true;
							}
						}
					}
				}
				if ( close ) {
					service.completeChargeSession(session.getIdTag(), session.getSessionId());
				}
			}
		}

	}

	/**
	 * Set the charge session manager to use.
	 * 
	 * @param service
	 *        The service to use.
	 */
	public void setService(ChargeSessionManager service) {
		this.service = service;
	}

	/**
	 * Set the number of meter readings to consider when calculating the
	 * effective energy drawn on the socket.
	 * 
	 * @param readingEnergyCount
	 *        The reading average count.
	 */
	public void setReadingEnergyCount(int readingEnergyCount) {
		this.readingEnergyCount = readingEnergyCount;
	}

	/**
	 * The maximum energy, in Wh, a charge session can draw over
	 * {@code readingEnergyCount} readings to be considered for closing. If the
	 * energy drawn is higher than this, the session will not be closed.
	 * 
	 * @param maxEnergy
	 *        The maximum energy to consider for closing a session, in Wh.
	 */
	public void setMaxEnergy(long maxEnergy) {
		this.maxEnergy = maxEnergy;
	}

	/**
	 * Set the maximum age in milliseconds from the last meter reading captured
	 * for that session (or the date the session started, if no readings are
	 * available). If this threshold is passed then the session will be closed.
	 * 
	 * @param maxAgeLastReading
	 *        The maximum time lapse allowed between meter readings, in
	 *        milliseconds.
	 */
	public void setMaxAgeLastReading(long maxAgeLastReading) {
		this.maxAgeLastReading = maxAgeLastReading;
	}

}
