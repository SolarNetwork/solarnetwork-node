/* ==================================================================
 * ChargeSessionManager_v15.java - 9/06/2015 11:00:33 am
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

package net.solarnetwork.node.ocpp.charge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.ocpp.AuthorizationManager;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.ocpp.ChargeConfiguration;
import net.solarnetwork.node.ocpp.ChargeConfigurationDao;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionDao;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import net.solarnetwork.node.ocpp.OCPPException;
import net.solarnetwork.node.ocpp.Socket;
import net.solarnetwork.node.ocpp.SocketDao;
import net.solarnetwork.node.ocpp.support.CentralSystemServiceFactorySupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.CentralSystemService;
import ocpp.v15.cs.ChargePointErrorCode;
import ocpp.v15.cs.ChargePointStatus;
import ocpp.v15.cs.IdTagInfo;
import ocpp.v15.cs.Measurand;
import ocpp.v15.cs.MeterValue;
import ocpp.v15.cs.MeterValue.Value;
import ocpp.v15.cs.MeterValuesRequest;
import ocpp.v15.cs.ReadingContext;
import ocpp.v15.cs.StartTransactionRequest;
import ocpp.v15.cs.StartTransactionResponse;
import ocpp.v15.cs.StatusNotificationRequest;
import ocpp.v15.cs.StatusNotificationResponse;
import ocpp.v15.cs.StopTransactionRequest;
import ocpp.v15.cs.StopTransactionResponse;
import ocpp.v15.cs.TransactionData;
import ocpp.v15.cs.UnitOfMeasure;

/**
 * Default implementation of {@link ChargeSessionManager}.
 * 
 * @author matt
 * @version 2.3
 */
public class ChargeSessionManager_v15 extends CentralSystemServiceFactorySupport
		implements ChargeSessionManager, ChargeSessionManager_v15Settings, EventHandler {

	/**
	 * The name used to schedule the {@link PostOfflineChargeSessionsJob} as.
	 */
	public static final String POST_OFFLINE_CHARGE_SESSIONS_JOB_NAME = "OCPP_PostOfflineChargeSessions";

	/**
	 * The name used to schedule the {@link CloseCompletedChargeSessionsJob} as.
	 */
	public static final String CLOSE_COMPLETED_CHARGE_SESSIONS_JOB_NAME = "OCPP_CloseCompletedChargeSessions";

	/**
	 * The name used to schedule the
	 * {@link PostActiveChargeSessionsMeterValuesJob} as.
	 */
	public static final String CLOSE_POST_ACTIVE_CHARGE_SESSIONS_METER_VALUES_JOB_NAME = "OCPP_PostActiveChargeSessionsMeterValues";

	/**
	 * The name used to schedule the {@link PurgePostedChargeSessionsJob} as.
	 */
	public static final String PURGE_POSTED_CHARGE_SESSIONS_JOB_NAME = "OCPP_PurgePostedChargeSessions";

	/**
	 * The job and trigger group used to schedule the
	 * {@link PostOfflineChargeSessionsJob} with. Note the trigger name will be
	 * the {@link #getUID()} property value.
	 */
	public static final String SCHEDULER_GROUP = "OCPP";

	/**
	 * The interval at which to try posting offline charge session data to the
	 * central system, in seconds.
	 */
	public static final int POST_OFFLINE_CHARGE_SESSIONS_JOB_INTERVAL = 600;

	/**
	 * The interval at which to try posting active charge session meter value
	 * data to the central system, in sesconds.
	 */
	public static final int POST_ACTIVE_CHARGE_SESSIONS_METER_VALUES_JOB_INTERVAL = 600;

	/**
	 * The interval at which to try closing sessions that appear to be completed
	 * but are still active.
	 */
	public static final int CLOSE_COMPLETED_CHARGE_SESSIONS_JOB_INTERVAL = 600;

	/**
	 * The interval at which to purge charge sessions that have been posted.
	 */
	public static final int PURGE_POSTED_CHARGE_SESSIONS_JOB_INTERVAL = 1800;

	private OptionalService<EventAdmin> eventAdmin;
	private AuthorizationManager authManager;
	private ChargeConfigurationDao chargeConfigurationDao;
	private ChargeSessionDao chargeSessionDao;
	private SocketDao socketDao;
	private TransactionTemplate transactionTemplate;
	private Executor executor = Executors.newSingleThreadExecutor(); // to kick off the handleEvent() thread
	private Map<String, Integer> socketConnectorMapping = Collections.emptyMap();
	private Map<String, String> socketMeterSourceMapping = Collections.emptyMap();
	private OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> meterDataSource;
	private Scheduler scheduler;
	private SimpleTrigger postOfflineChargeSessionsTrigger;
	private SimpleTrigger closeCompletedChargeSessionsTrigger;
	private SimpleTrigger postActiveChargeSessionsMeterValuesTrigger;
	private SimpleTrigger purgePostedChargeSessionsTrigger;

	private final ConcurrentMap<String, Object> socketReadingsIgnoreMap = new ConcurrentHashMap<String, Object>(
			8);

	/**
	 * Initialize the OCPP client. Call this once after all properties
	 * configured.
	 */
	@Override
	public void startup() {
		log.info("Starting up OCPP ChargeSessionManager {}", getUID());
		configurePostOfflineChargeSessionsJob(POST_OFFLINE_CHARGE_SESSIONS_JOB_INTERVAL);
		configureCloseCompletedChargeSessionJob(CLOSE_COMPLETED_CHARGE_SESSIONS_JOB_INTERVAL);
		configurePurgePostedChargeSessionsJob(PURGE_POSTED_CHARGE_SESSIONS_JOB_INTERVAL);

		// configure aspects from OCPP properties
		handleChargeConfigurationUpdated();
	}

	/**
	 * Shutdown the OCPP client, releasing any associated resources.
	 */
	@Override
	public void shutdown() {
		configurePostOfflineChargeSessionsJob(0);
		configureCloseCompletedChargeSessionJob(0);
		configurePostActiveChargeSessionsMeterValuesJob(0);
		configurePurgePostedChargeSessionsJob(0);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ChargeSession activeChargeSession(String socketId) {
		return chargeSessionDao.getIncompleteChargeSessionForSocket(socketId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ChargeSession activeChargeSession(Number transactionId) {
		int xid = transactionId.intValue();
		return chargeSessionDao.getIncompleteChargeSessionForTransaction(xid);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Collection<String> availableSocketIds() {
		return new HashSet<String>(socketConnectorMapping.keySet());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String initiateChargeSession(final String idTag, final String socketId,
			final Integer reservationId) {
		final Integer connectorId = socketConnectorMapping.get(socketId);
		if ( connectorId == null ) {
			log.error("No connector ID configured for socket ID {}", socketId);
			throw new OCPPException("No connector ID available for " + socketId);
		}

		// is the socket enabled?
		if ( socketDao.isEnabled(socketId) == false ) {
			log.info("Socket {} is disabled, not initiating charge session for {}", socketId, idTag);
			throw new OCPPException("Socket disabled", null, AuthorizationStatus.BLOCKED);
		}

		// is there an active session already? if so, DENY
		ChargeSession session = activeChargeSession(socketId);
		if ( session != null ) {
			throw new OCPPException(
					"An active charge session exists already on " + session.getSocketId(), null,
					AuthorizationStatus.CONCURRENT_TX);
		}

		final long now = System.currentTimeMillis();
		final Object socketLock = ignoreReadingsForSocket(socketId);
		synchronized ( socketLock ) {
			try {
				final AuthorizationStatus authorized = authManager.authorize(idTag);
				log.debug("{} authorized: {}", idTag, authorized);
				if ( authorized != AuthorizationStatus.ACCEPTED ) {
					throw new OCPPException("Unauthorized", null, authorized);
				}

				final String meterSourceId = socketMeterSourceMapping.get(socketId);
				if ( meterSourceId == null ) {
					log.warn(
							"No meter source ID available for socket ID {}, starting meter value will not be available for charge session",
							socketId);
				}

				// send status message
				postStatusNotification(ChargePointStatus.OCCUPIED, connectorId, now);

				final ACEnergyDatum meterReading = getMeterReading(meterSourceId);

				session = new ChargeSession();
				session.setCreated(new Date(now));
				session.setIdTag(idTag);
				session.setSocketId(socketId);

				StartTransactionResponse res = postStartTransaction(idTag, reservationId, connectorId,
						session, now, (meterReading != null ? meterReading.getWattHourReading() : null));
				if ( res != null && res.getIdTagInfo() != null
						&& res.getIdTagInfo().getStatus() == AuthorizationStatus.ACCEPTED ) {
					final String sessionId = chargeSessionDao.storeChargeSession(session);

					// insert transaction begin readings
					List<Value> readings = readingsForDatum(meterReading);
					for ( Value v : readings ) {
						v.setContext(ReadingContext.TRANSACTION_BEGIN);
					}
					chargeSessionDao.addMeterReadings(sessionId,
							(meterReading != null ? meterReading.getCreated() : new Date(now)),
							readings);
					postChargeSessionStateEvent(session, true, meterReading);
					postConfigurationChangedEvent();
					return sessionId;
				}

				throw new OCPPException("StartTransaction failed for IdTag " + idTag, null,
						res != null && res.getIdTagInfo() != null ? res.getIdTagInfo().getStatus()
								: null);
			} finally {
				resumeReadingsForSocket(socketId, socketLock);
			}
		}
	}

	private void postChargeSessionStateEvent(ChargeSession session, boolean started,
			ACEnergyDatum datum) {
		Map<String, Object> props = new HashMap<String, Object>(4);
		if ( started && session.getCreated() != null ) {
			props.put(EVENT_PROPERTY_DATE, session.getCreated().getTime());
		} else if ( !started && session.getEnded() != null ) {
			props.put(EVENT_PROPERTY_DATE, session.getEnded().getTime());
		}
		props.put(EVENT_PROPERTY_SESSION_ID, session.getSessionId());
		props.put(EVENT_PROPERTY_SOCKET_ID, session.getSocketId());
		if ( datum != null ) {
			props.put(EVENT_PROPERTY_METER_READING_POWER, datum.getWatts());
			props.put(EVENT_PROPERTY_METER_READING_ENERGY, datum.getWattHourReading());
		}
		postEvent(started ? EVENT_TOPIC_SESSION_STARTED : EVENT_TOPIC_SESSION_ENDED, props);
	}

	private void postConfigurationChangedEvent() {
		postEvent(Constants.EVENT_TOPIC_CONFIGURATION_CHANGED, null);
	}

	private void postEvent(String topic, Map<String, Object> props) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		admin.postEvent(new Event(topic, props));
	}

	private StatusNotificationResponse postStatusNotification(final ChargePointStatus status,
			final Integer connectorId, final long now) {
		return postStatusNotification(status, connectorId, null, null, null, now);
	}

	/**
	 * Post a status notification update to the central system.
	 * 
	 * @param status
	 *        The status to post.
	 * @param connectorId
	 *        The ID of the associated connector.
	 * @param info
	 *        An optional info message.
	 * @param errorCode
	 *        An optional error code. If not provided,
	 *        {@link ChargePointErrorCode#NO_ERROR} will be used.
	 * @param internalErrorCode
	 *        An optional internal error code.
	 * @param now
	 *        A timestamp to use.
	 * @return The response, or <em>null</em> if not able to post the status.
	 */
	private StatusNotificationResponse postStatusNotification(final ChargePointStatus status,
			final Integer connectorId, final String info, final ChargePointErrorCode errorCode,
			final String internalErrorCode, final long now) {
		final CentralSystemServiceFactory system = getCentralSystem();
		final CentralSystemService client = (system != null ? system.service() : null);
		StatusNotificationResponse res = null;
		if ( client != null ) {
			StatusNotificationRequest req = new StatusNotificationRequest();
			req.setConnectorId(connectorId.intValue());
			req.setInfo(info);
			req.setStatus(status);
			req.setErrorCode(errorCode != null ? errorCode : ChargePointErrorCode.NO_ERROR);
			req.setVendorErrorCode(internalErrorCode);
			req.setTimestamp(newXmlCalendar(now));
			try {
				res = client.statusNotification(req, system.chargeBoxIdentity());
				log.info("OCPP central system status updated to {}", status);
			} catch ( RuntimeException e ) {
				// log the error, but we don't stop the session from starting
				log.error("Error communicating with OCPP central system for StatusNotification", e);
			}
		}
		return res;
	}

	/**
	 * Post the {@code StartTransaction} message.
	 * 
	 * @param idTag
	 *        The ID tag.
	 * @param reservationId
	 *        An optional OCPP reservation ID.
	 * @param connectorId
	 *        The OCPP connector ID.
	 * @param session
	 *        The ChargeSession associated with the transaction.
	 * @param now
	 *        The current time.
	 * @param meterReading
	 *        An optional meter reading.
	 * @return The response, or <em>null</em> if no central system is available.
	 */
	private StartTransactionResponse postStartTransaction(String idTag, Integer reservationId,
			final Integer connectorId, ChargeSession session, final long now,
			final Number meterReading) {
		final CentralSystemServiceFactory system = getCentralSystem();
		final CentralSystemService client = (system != null ? system.service() : null);
		StartTransactionResponse res = null;
		if ( client != null ) {
			StartTransactionRequest req = new StartTransactionRequest();
			req.setConnectorId(connectorId);
			req.setIdTag(idTag);
			req.setReservationId(reservationId);
			req.setTimestamp(newXmlCalendar(now));
			if ( meterReading != null ) {
				req.setMeterStart(meterReading.intValue());
			}
			try {
				res = client.startTransaction(req, system.chargeBoxIdentity());
				IdTagInfo info = res.getIdTagInfo();
				AuthorizationStatus status = (info != null ? res.getIdTagInfo().getStatus() : null);
				session.setStatus(status);
				session.setParentIdTag(info != null ? info.getParentIdTag() : null);
				session.setExpiryDate(info != null ? info.getExpiryDate() : null);
				if ( res.getIdTagInfo() != null && AuthorizationStatus.ACCEPTED.equals(status) ) {
					session.setTransactionId(res.getTransactionId());
				}
			} catch ( RuntimeException e ) {
				// log the error, but we don't stop the session from starting
				log.error("Error communicating with OCPP central system for StartTransaction", e);
			}
		}
		return res;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void completeChargeSession(String idTag, String sessionId) {
		// get active session
		ChargeSession session = chargeSessionDao.getChargeSession(sessionId);
		if ( session == null ) {
			throw new OCPPException("No such charge session", null, AuthorizationStatus.INVALID);
		}
		if ( session.getEnded() != null ) {
			throw new OCPPException("Session already complete", null, AuthorizationStatus.EXPIRED);
		}
		if ( session.getIdTag() == null || !session.getIdTag().equals(idTag) ) {
			throw new OCPPException("IdTag does not match", null, AuthorizationStatus.INVALID);
		}

		final long now = System.currentTimeMillis();
		final String socketId = session.getSocketId();

		final Integer connectorId = socketConnectorMapping.get(socketId);
		if ( connectorId == null ) {
			log.error("No connector ID configured for socket ID {}", socketId);
			throw new OCPPException("No connector ID available for " + socketId);
		}

		// mark this socket as "stopping" so the subsequent meter reading doesn't get added
		final Object socketLock = ignoreReadingsForSocket(socketId);
		synchronized ( socketLock ) {
			ACEnergyDatum meterReading = null;
			try {
				// get current meter reading
				final String meterSourceId = socketMeterSourceMapping.get(socketId);
				if ( meterSourceId == null ) {
					log.warn(
							"No meter source ID available for socket ID {}, final meter value will not be available for charge session",
							session.getSocketId());
				}
				meterReading = getMeterReading(meterSourceId);

				// add end transaction readings
				List<Value> readings = readingsForDatum(meterReading);
				for ( Value v : readings ) {
					v.setContext(ReadingContext.TRANSACTION_END);
				}
				chargeSessionDao.addMeterReadings(sessionId,
						(meterReading != null ? meterReading.getCreated() : new Date(now)), readings);

				// post the stop transaction, if we have a transaction ID
				postStopTransaction(idTag, session, now,
						(meterReading != null ? meterReading.getWattHourReading() : null));

				// persist changes to DB
				session.setEnded(new Date(now));
				chargeSessionDao.storeChargeSession(session);
			} finally {
				postChargeSessionStateEvent(session, false, meterReading);
				postStatusNotification(ChargePointStatus.AVAILABLE, connectorId, now);
				resumeReadingsForSocket(socketId, socketLock);
				postConfigurationChangedEvent();
			}
		}
	}

	/**
	 * Post a {@code StopTransaction} message to the central system. If the
	 * message is posted successfully, then the {@link IdTagInfo#getStatus()}
	 * value will be passed to
	 * {@link ChargeSession#setStatus(AuthorizationStatus)}.
	 * 
	 * @param idTag
	 *        The ID tag.
	 * @param session
	 *        The active session that is stopping.
	 * @param now
	 *        The current date.
	 * @param meterReading
	 *        An optional meter reading, to populate the {@code meterStop} value
	 *        with.
	 * @return The response, or <em>null</em> if no transaction ID available or
	 *         the central system is not available.
	 */
	private StopTransactionResponse postStopTransaction(String idTag, ChargeSession session,
			final long now, final Number meterReading) {
		CentralSystemServiceFactory system = getCentralSystem();
		CentralSystemService client = (system != null ? system.service() : null);
		StopTransactionResponse res = null;
		if ( session.getTransactionId() != null && client != null ) {
			StopTransactionRequest req = new StopTransactionRequest();
			req.setIdTag(idTag);
			if ( meterReading != null ) {
				req.setMeterStop(meterReading.intValue());
			}
			req.setTimestamp(newXmlCalendar(now));
			req.setTransactionId(session.getTransactionId());

			// add any associated readings
			List<ChargeSessionMeterReading> readings = chargeSessionDao
					.findMeterReadingsForSession(session.getSessionId());
			TransactionData data = transactionDataForMeterReadings(readings);
			if ( data.getValues().size() > 0 ) {
				req.getTransactionData().add(data);
			}

			res = client.stopTransaction(req, system.chargeBoxIdentity());
			if ( res.getIdTagInfo() != null ) {
				IdTagInfo info = res.getIdTagInfo();
				if ( info.getStatus() != null ) {
					session.setStatus(info.getStatus());
				}
				session.setPosted(new Date(now));
			}
		}
		return res;
	}

	private TransactionData transactionDataForMeterReadings(List<ChargeSessionMeterReading> readings) {
		TransactionData data = new TransactionData();
		MeterValue currMeterValue = null;
		long currTimestamp = -1;
		for ( ChargeSessionMeterReading r : readings ) {
			if ( r.getTs().getTime() != currTimestamp ) {
				currMeterValue = new MeterValue();
				data.getValues().add(currMeterValue);
				currTimestamp = r.getTs().getTime();
				currMeterValue.setTimestamp(newXmlCalendar(currTimestamp));
			}
			currMeterValue.getValue().add(r);
		}
		return data;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<ChargeSessionMeterReading> meterReadingsForChargeSession(String sessionId) {
		return chargeSessionDao.findMeterReadingsForSession(sessionId);
	}

	@Override
	public String socketIdForConnectorId(Number connectorId) {
		Integer connId = (connectorId != null ? connectorId.intValue() : null);
		if ( connId == null ) {
			return null;
		}
		Map<String, Integer> map = getSocketConnectorMapping();
		if ( map == null ) {
			return null;
		}
		for ( Map.Entry<String, Integer> me : map.entrySet() ) {
			if ( connId.equals(me.getValue()) ) {
				return me.getKey();
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void configureSocketEnabledState(final Collection<String> socketIds, final boolean enabled) {
		for ( String socketId : socketIds ) {
			boolean existing = socketDao.isEnabled(socketId);
			if ( existing == enabled ) {
				continue;
			}
			socketDao.storeSocket(new Socket(socketId, enabled));
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int postCompleteOfflineSessions(final int max) {
		List<ChargeSession> toPost = chargeSessionDao.getChargeSessionsNeedingPosting(max);
		for ( ChargeSession session : toPost ) {
			Integer connectorId = getSocketConnectorMapping().get(session.getSocketId());
			List<ChargeSessionMeterReading> readings = chargeSessionDao
					.findMeterReadingsForSession(session.getSessionId());
			Long startWh = null;
			Long endWh = null;
			for ( ChargeSessionMeterReading reading : readings ) {
				if ( Measurand.ENERGY_ACTIVE_IMPORT_REGISTER.equals(reading.getMeasurand()) ) {
					if ( ReadingContext.TRANSACTION_BEGIN.equals(reading.getContext()) ) {
						startWh = Long.valueOf(reading.getValue());
					} else if ( ReadingContext.TRANSACTION_END.equals(reading.getContext()) ) {
						endWh = Long.valueOf(reading.getValue());
					}
				}
			}
			if ( session.getTransactionId() == null ) {
				StartTransactionResponse resp = postStartTransaction(session.getIdTag(), null,
						connectorId, session, System.currentTimeMillis(), startWh);
				if ( resp != null && resp.getIdTagInfo() != null
						&& AuthorizationStatus.ACCEPTED.equals(resp.getIdTagInfo().getStatus()) ) {
					session.setTransactionId(resp.getTransactionId());
					chargeSessionDao.storeChargeSession(session);
				}
			}
			if ( session.getEnded() != null && session.getPosted() == null ) {
				final Date postDate = new Date();
				StopTransactionResponse resp = postStopTransaction(session.getIdTag(), session,
						postDate.getTime(), endWh);
				if ( resp != null ) {
					chargeSessionDao.storeChargeSession(session);
				}
			}
		}
		return toPost.size();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void postActiveChargeSessionsMeterValues() {
		final CentralSystemServiceFactory system = getCentralSystem();
		final CentralSystemService client = (system != null ? system.service() : null);
		if ( client == null ) {
			return;
		}
		for ( String socketId : availableSocketIds() ) {
			final ChargeSession session = activeChargeSession(socketId);
			if ( session == null ) {
				continue;
			}
			final Integer connectorId = socketConnectorMapping.get(socketId);
			List<ChargeSessionMeterReading> readings = meterReadingsForChargeSession(
					session.getSessionId());
			TransactionData data = transactionDataForMeterReadings(readings);
			final int valuesCount = data.getValues().size();
			if ( valuesCount > 0 ) {
				// post just the most recent (last) value available
				MeterValuesRequest req = new MeterValuesRequest();
				req.getValues().add(data.getValues().get(valuesCount - 1));
				req.setConnectorId(connectorId);
				req.setTransactionId(session.getTransactionId());
				client.meterValues(req, system.chargeBoxIdentity());
				log.info(
						"Posted {} meter values for active charge session {} on socket {} to OCPP central server",
						data.getValues().size(), session.getSessionId(), socketId);
			}
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deletePostedChargeSessions(Date olderThanDate) {
		return chargeSessionDao.deletePostedChargeSessions(olderThanDate);
	}

	private Map<String, Object> standardJobMap() {
		Map<String, Object> jobMap = new HashMap<String, Object>();
		jobMap.put("service", this);
		if ( transactionTemplate != null ) {
			jobMap.put("transactionTemplate", transactionTemplate);
		}
		return jobMap;
	}

	private boolean configurePostOfflineChargeSessionsJob(final int seconds) {
		postOfflineChargeSessionsTrigger = scheduleIntervalJob(scheduler, seconds,
				postOfflineChargeSessionsTrigger,
				new JobKey(POST_OFFLINE_CHARGE_SESSIONS_JOB_NAME, SCHEDULER_GROUP),
				PostOfflineChargeSessionsJob.class, new JobDataMap(standardJobMap()),
				"OCPP post offline charge sessions");
		return ((seconds > 0 && postOfflineChargeSessionsTrigger != null)
				|| (seconds < 1 && postOfflineChargeSessionsTrigger == null));
	}

	private boolean configureCloseCompletedChargeSessionJob(final int seconds) {
		closeCompletedChargeSessionsTrigger = scheduleIntervalJob(scheduler, seconds,
				closeCompletedChargeSessionsTrigger,
				new JobKey(CLOSE_COMPLETED_CHARGE_SESSIONS_JOB_NAME, SCHEDULER_GROUP),
				CloseCompletedChargeSessionsJob.class, new JobDataMap(standardJobMap()),
				"OCPP close completed charge sessions");
		return ((seconds > 0 && closeCompletedChargeSessionsTrigger != null)
				|| (seconds < 1 && closeCompletedChargeSessionsTrigger == null));
	}

	private boolean configurePostActiveChargeSessionsMeterValuesJob(final int seconds) {
		postActiveChargeSessionsMeterValuesTrigger = scheduleIntervalJob(scheduler, seconds,
				postActiveChargeSessionsMeterValuesTrigger,
				new JobKey(CLOSE_POST_ACTIVE_CHARGE_SESSIONS_METER_VALUES_JOB_NAME, SCHEDULER_GROUP),
				PostActiveChargeSessionsMeterValuesJob.class, new JobDataMap(standardJobMap()),
				"OCPP post active charge sessions meter values");
		return ((seconds > 0 && postActiveChargeSessionsMeterValuesTrigger != null)
				|| (seconds < 1 && postActiveChargeSessionsMeterValuesTrigger == null));
	}

	private boolean configurePurgePostedChargeSessionsJob(final int seconds) {
		purgePostedChargeSessionsTrigger = scheduleIntervalJob(scheduler, seconds,
				purgePostedChargeSessionsTrigger,
				new JobKey(PURGE_POSTED_CHARGE_SESSIONS_JOB_NAME, SCHEDULER_GROUP),
				PurgePostedChargeSessionsJob.class, new JobDataMap(standardJobMap()),
				"OCPP purge posted charge sessions");
		return ((seconds > 0 && purgePostedChargeSessionsTrigger != null)
				|| (seconds < 1 && purgePostedChargeSessionsTrigger == null));
	}

	// Datum support

	private ACEnergyDatum getMeterReading(String sourceId) {
		OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> service = meterDataSource;
		if ( service == null || sourceId == null ) {
			return null;
		}
		Iterable<DatumDataSource<ACEnergyDatum>> dataSources = service.services();
		for ( DatumDataSource<ACEnergyDatum> dataSource : dataSources ) {
			if ( dataSource instanceof MultiDatumDataSource<?> ) {
				@SuppressWarnings("unchecked")
				Collection<ACEnergyDatum> datums = ((MultiDatumDataSource<ACEnergyDatum>) dataSource)
						.readMultipleDatum();
				if ( datums != null ) {
					for ( ACEnergyDatum datum : datums ) {
						if ( sourceId.equals(datum.getSourceId()) ) {
							return datum;
						}
					}
				}
			} else {
				ACEnergyDatum datum = dataSource.readCurrentDatum();
				if ( datum != null && sourceId.equals(sourceId) ) {
					return datum;
				}
			}
		}
		log.warn("Meter reading unavailable for source {}", sourceId);
		return null;
	}

	/**
	 * Mark a socket to ignore meter readings.
	 * 
	 * @param socketId
	 *        The socket ID to ignore readings from, or to stop ignoring.
	 * @return An object suitable for synchronizing on and that must be passed
	 *         to {@link #resumeReadingsForSocket(String, Object)} to clear the
	 *         ignore flag.
	 * @see #shouldIgnoreReadingsForSocket(String)
	 * @see #resumeReadingsForSocket(String, Object)
	 */
	private Object ignoreReadingsForSocket(final String socketId) {
		Object lock = new Object();
		Object existingLock = socketReadingsIgnoreMap.putIfAbsent(socketId, lock);
		Object result = (existingLock != null ? existingLock : lock);
		log.info("Ignoring readings on socket {} using {} lock {}", socketId,
				(existingLock != null ? "existing" : "new"), result);
		return result;
	}

	/**
	 * Clear the ignore flag previously set via
	 * {@link #ignoreReadingsForSocket(String)}.
	 * 
	 * @param socketId
	 *        The socket ID to resume listening to.
	 * @param socketLock
	 *        The lock previously returned from
	 *        {@link #ignoreReadingsForSocket(String)}.
	 */
	private void resumeReadingsForSocket(final String socketId, final Object socketLock) {
		if ( socketLock != null ) {
			boolean removed = socketReadingsIgnoreMap.remove(socketId, socketLock);
			if ( removed ) {
				log.info("Resuming listening for readings on socket {} after releasing lock {}",
						socketId, socketLock);
			} else {
				log.warn(
						"Unable to resume listening for readings on {} with lock {}; current lock is {}",
						socketId, socketLock, socketReadingsIgnoreMap.get(socketId));
			}
		}
	}

	/**
	 * Test if a socket ID is marked as "stopping" from a previous call to
	 * {@link #markSocketAsStopping(String)}.
	 * 
	 * @param socketId
	 *        The socket ID to test.
	 * @return <em>true</em> if the socket is considered in "stopping" mode.
	 */
	private boolean shouldIgnoreReadingsForSocket(String socketId) {
		return socketReadingsIgnoreMap.containsKey(socketId);
	}

	// EventHandler

	@Override
	public void handleEvent(final Event event) {
		final String topic = event.getTopic();
		Runnable r = null;
		if ( topic.equals(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED) ) {
			r = new Runnable() {

				@Override
				public void run() {
					try {
						handleDatumCapturedEvent(event);
					} catch ( RuntimeException e ) {
						log.error("Error handling event {}", topic, e);
					}
				}
			};
		} else if ( topic.equals(ChargeConfigurationDao.EVENT_TOPIC_CHARGE_CONFIGURATION_UPDATED) ) {
			r = new Runnable() {

				@Override
				public void run() {
					try {
						handleChargeConfigurationUpdated();
					} catch ( RuntimeException e ) {
						log.error("Error handling event {}", topic, e);
					}
				}
			};
		}
		if ( r != null ) {
			if ( executor != null ) {
				// kick off to new thread so we don't block the event thread
				executor.execute(r);
			} else {
				r.run();
			}
		}
	}

	private Map<String, Object> mapForEventProperties(Event event) {
		Map<String, Object> map = new HashMap<String, Object>(8);
		if ( event != null ) {
			for ( String name : event.getPropertyNames() ) {
				Object o = event.getProperty(name);
				map.put(name, o);
			}
		}
		return map;
	}

	private void handleDatumCapturedEvent(Event event) {
		Map<String, Object> eventProperties = mapForEventProperties(event);
		Object propValue = eventProperties.get("sourceId");
		String sourceId;
		if ( propValue instanceof String ) {
			sourceId = (String) propValue;
		} else {
			return;
		}
		log.debug("Received datum captured event: {}", eventProperties);

		// locate the socket ID for the given source ID
		for ( Map.Entry<String, String> me : socketMeterSourceMapping.entrySet() ) {
			if ( sourceId.equals(me.getValue()) ) {
				handleDatumCapturedEvent(me.getKey(), sourceId, eventProperties);
				return;
			}
		}
	}

	private void handleDatumCapturedEvent(String socketId, String sourceId,
			Map<String, Object> eventProperties) {
		if ( shouldIgnoreReadingsForSocket(socketId) ) {
			log.info("Ignoring DATUM_CAPTURED event for socket {} that is in transitioning state",
					socketId);
			return;
		}
		ChargeSession active = activeChargeSession(socketId);
		if ( active == null ) {
			return;
		}

		final long created = (eventProperties.get("created") instanceof Number
				? ((Number) eventProperties.get("created")).longValue()
				: System.currentTimeMillis());

		// reconstruct Datum from event properties
		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		ClassUtils.setBeanProperties(datum, eventProperties, true);

		// store readings in DB
		List<Value> readings = readingsForDatum(datum);
		chargeSessionDao.addMeterReadings(active.getSessionId(), new Date(created), readings);
	}

	private void handleChargeConfigurationUpdated() {
		ChargeConfiguration config = chargeConfigurationDao.getChargeConfiguration();
		if ( config.getMeterValueSampleInterval() >= 0 ) {
			configurePostActiveChargeSessionsMeterValuesJob(config.getMeterValueSampleInterval());
		}
	}

	private List<Value> readingsForDatum(ACEnergyDatum datum) {
		List<Value> readings = new ArrayList<Value>(4);
		if ( datum != null ) {
			if ( datum.getWattHourReading() != null ) {
				Value reading = new Value();
				reading.setContext(ReadingContext.SAMPLE_PERIODIC);
				reading.setMeasurand(Measurand.ENERGY_ACTIVE_IMPORT_REGISTER);
				reading.setUnit(UnitOfMeasure.WH);
				reading.setValue(datum.getWattHourReading().toString());
				readings.add(reading);
			}

			if ( datum.getWatts() != null ) {
				Value reading = new Value();
				reading.setContext(ReadingContext.SAMPLE_PERIODIC);
				reading.setMeasurand(Measurand.POWER_ACTIVE_IMPORT);
				reading.setUnit(UnitOfMeasure.W);
				reading.setValue(datum.getWatts().toString());
				readings.add(reading);
			}
		}
		return readings;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.charge";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Charge Session Manager";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		ChargeSessionManager_v15 defaults = new ChargeSessionManager_v15();
		results.add(new BasicTextFieldSettingSpecifier("filterableAuthManager.propertyFilters['UID']",
				"OCPP Central System"));
		results.add(new BasicTextFieldSettingSpecifier("meterDataSource.propertyFilters['groupUID']",
				"OCPP Meter"));
		results.add(new BasicTextFieldSettingSpecifier("socketMeterSourceMappingValue",
				defaults.getSocketMeterSourceMappingValue()));
		results.add(new BasicTextFieldSettingSpecifier("socketConnectorMappingValue",
				defaults.getSocketConnectorMappingValue()));
		return results;
	}

	@Override
	protected String getInfoMessage(Locale locale) {
		StringBuilder buf = new StringBuilder();
		if ( chargeSessionDao != null ) {
			List<ChargeSession> incomplete = chargeSessionDao.getIncompleteChargeSessions();
			Set<String> active = new LinkedHashSet<String>(incomplete.size());
			if ( incomplete.size() > 0 ) {
				for ( ChargeSession s : incomplete ) {
					active.add(s.getSessionId());
				}
				List<String> reversed = new ArrayList<String>(active);
				Collections.reverse(reversed);
				buf.append(
						getMessageSource().getMessage("status.active",
								new Object[] { incomplete.size(),
										StringUtils.commaDelimitedStringFromCollection(reversed) },
								locale));
			}
			List<ChargeSession> needPosting = chargeSessionDao.getChargeSessionsNeedingPosting(100);
			if ( needPosting.size() > 0 ) {
				List<String> need = new ArrayList<String>(needPosting.size());
				for ( ChargeSession s : needPosting ) {
					if ( active.contains(s.getSessionId()) ) {
						continue;
					}
					need.add(s.getSessionId());
				}
				if ( buf.length() > 0 ) {
					buf.append("; ");
				}
				String needIds = StringUtils.commaDelimitedStringFromCollection(
						(need.size() > 10 ? need.subList(0, 10) : need));
				buf.append(getMessageSource().getMessage("status.needPosting",
						new Object[] { need.size(), needIds }, locale));
				if ( need.size() > 10 ) {
					buf.append("\u2026"); // ellipsis
				}
			}
		}
		if ( buf.length() < 1 ) {
			buf.append(getMessageSource().getMessage("status.none", null, locale));
		}
		return buf.toString();
	}

	// Accessors

	public AuthorizationManager getAuthManager() {
		return authManager;
	}

	@Override
	public FilterableService getFilterableAuthManager() {
		AuthorizationManager mgr = authManager;
		if ( mgr instanceof FilterableService ) {
			return (FilterableService) mgr;
		}
		return null;
	}

	public void setAuthManager(AuthorizationManager authManager) {
		this.authManager = authManager;
	}

	public ChargeSessionDao getChargeSessionDao() {
		return chargeSessionDao;
	}

	public void setChargeSessionDao(ChargeSessionDao chargeSessionDao) {
		this.chargeSessionDao = chargeSessionDao;
	}

	/**
	 * Get the mapping of SolarNode {@code socketId} values to corresponding
	 * OCPP {@code connectorId} values.
	 * 
	 * @return The socket ID mapping, never <em>null</em>.
	 */
	public final Map<String, Integer> getSocketConnectorMapping() {
		return socketConnectorMapping;
	}

	/**
	 * Set a mapping of SolarNode {@code socketId} values to corresponding OCPP
	 * {@code connectorId} values.
	 * 
	 * @param socketConnectorMapping
	 *        The mapping to use.
	 */
	public final void setSocketConnectorMapping(Map<String, Integer> socketConnectorMapping) {
		this.socketConnectorMapping = (socketConnectorMapping != null ? socketConnectorMapping
				: Collections.<String, Integer> emptyMap());
	}

	/**
	 * Set a {@code socketConnectorMapping} Map via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param mapping
	 *        The encoding mapping to set.
	 * @see #getSocketConnectorMappingValue()
	 * @see #setSocketConnectorMapping(Map)
	 */
	@Override
	public final void setSocketConnectorMappingValue(String mapping) {
		Map<String, String> map = StringUtils.delimitedStringToMap(mapping, ",", "=");
		if ( map == null || map.size() < 0 ) {
			map = Collections.emptyMap();
		}
		Map<String, Integer> socketMap = new LinkedHashMap<String, Integer>(map.size());
		for ( Map.Entry<String, String> me : map.entrySet() ) {
			try {
				Integer connId = Integer.valueOf(me.getValue());
				socketMap.put(me.getKey(), connId);
			} catch ( NumberFormatException e ) {
				log.debug("Ignoring invalid connector ID {}, mapped from socket ID {}", me.getValue(),
						me.getKey());
			}
		}
		setSocketConnectorMapping(socketMap);
	}

	/**
	 * Get a delimited string representation of the
	 * {@link #getSocketConnectorMapping()} map.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * @return the encoded mapping
	 * @see #getSocketConnectorMapping()
	 */
	public final String getSocketConnectorMappingValue() {
		return StringUtils.delimitedStringFromMap(socketConnectorMapping);
	}

	@Override
	public OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> getMeterDataSource() {
		return meterDataSource;
	}

	public void setMeterDataSource(
			OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> meterDataSource) {
		this.meterDataSource = meterDataSource;
	}

	/**
	 * Get
	 * 
	 * @return
	 */
	public final Map<String, String> getSocketMeterSourceMapping() {
		return socketMeterSourceMapping;
	}

	/**
	 * Set a mapping of SolarNode {@code socketId} values to corresponding
	 * SolarNode {@code sourceId} values representing the meter source to obtain
	 * meter data from.
	 * 
	 * @param socketMeterSourceMapping
	 *        The mapping to use.
	 */
	public final void setSocketMeterSourceMapping(Map<String, String> socketMeterSourceMapping) {
		this.socketMeterSourceMapping = (socketMeterSourceMapping != null ? socketMeterSourceMapping
				: Collections.<String, String> emptyMap());
	}

	/**
	 * Set a {@code socketMeterSourceMapping} Map via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param mapping
	 *        The encoding mapping to set.
	 * @see #getSocketMeterSourceMappingValue()
	 * @see #setSocketMeterSourceMapping(Map)
	 */
	@Override
	public final void setSocketMeterSourceMappingValue(String mapping) {
		Map<String, String> map = StringUtils.delimitedStringToMap(mapping, ",", "=");
		if ( map == null || map.size() < 0 ) {
			map = Collections.emptyMap();
		}
		setSocketMeterSourceMapping(map);
	}

	/**
	 * Get a delimited string representation of the
	 * {@link #getSocketMeterSourceMapping()} map.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * @return the encoded mapping
	 * @see #getSocketMeterSourceMapping()
	 */
	public final String getSocketMeterSourceMappingValue() {
		return StringUtils.delimitedStringFromMap(socketMeterSourceMapping);
	}

	public SocketDao getSocketDao() {
		return socketDao;
	}

	public void setSocketDao(SocketDao socketDao) {
		this.socketDao = socketDao;
	}

	/**
	 * Set the Scheduler to use for the {@link PostOfflineChargeSessionsJob}.
	 * 
	 * @param scheduler
	 *        The scheduler to use.
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void setUid(String uid) {
		if ( uid != null && !uid.equals(getUid()) ) {
			configurePostOfflineChargeSessionsJob(0);
			super.setUid(uid);
			configurePostOfflineChargeSessionsJob(POST_OFFLINE_CHARGE_SESSIONS_JOB_INTERVAL);
		}
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void setChargeConfigurationDao(ChargeConfigurationDao chargeConfigurationDao) {
		this.chargeConfigurationDao = chargeConfigurationDao;
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Set an {@link Executor} to handle events with.
	 * 
	 * @param executor
	 *        the executor, or {@literal null} to handle events on the calling
	 *        thread; defaults to a single-thread executor service
	 * @since 2.3
	 */
	public void setEventExecutor(Executor executor) {
		this.executor = executor;
	}

}
