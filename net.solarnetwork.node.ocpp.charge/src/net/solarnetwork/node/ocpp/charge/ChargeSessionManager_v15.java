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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.ocpp.AuthorizationManager;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionDao;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.ChargeSessionMeterReading;
import net.solarnetwork.node.ocpp.OCPPException;
import net.solarnetwork.node.ocpp.support.CentralSystemServiceFactorySupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;
import ocpp.v15.AuthorizationStatus;
import ocpp.v15.CentralSystemService;
import ocpp.v15.IdTagInfo;
import ocpp.v15.Measurand;
import ocpp.v15.MeterValue;
import ocpp.v15.MeterValue.Value;
import ocpp.v15.ReadingContext;
import ocpp.v15.StartTransactionRequest;
import ocpp.v15.StartTransactionResponse;
import ocpp.v15.StopTransactionRequest;
import ocpp.v15.StopTransactionResponse;
import ocpp.v15.TransactionData;
import ocpp.v15.UnitOfMeasure;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ChargeSessionManager}.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargeSessionManager_v15 extends CentralSystemServiceFactorySupport implements
		ChargeSessionManager, EventHandler {

	private AuthorizationManager authManager;
	private ChargeSessionDao chargeSessionDao;
	private Map<String, Integer> socketConnectorMapping = Collections.emptyMap();
	private Map<String, String> socketMeterSourceMapping = Collections.emptyMap();
	private OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> meterDataSource;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ChargeSession activeChargeSession(String socketId) {
		return chargeSessionDao.getIncompleteChargeSessionForSocket(socketId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String initiateChargeSession(String idTag, String socketId, Integer reservationId) {
		final Integer connectorId = socketConnectorMapping.get(socketId);
		if ( connectorId == null ) {
			log.error("No connector ID configured for socket ID {}", socketId);
			throw new OCPPException("No connector ID available for " + socketId);
		}

		// is there an active session already? if so, DENY
		ChargeSession session = activeChargeSession(socketId);
		if ( session != null ) {
			throw new OCPPException("An active charge session exists already on "
					+ session.getSocketId(), null, AuthorizationStatus.CONCURRENT_TX);
		}

		final long now = System.currentTimeMillis();
		final String meterSourceId = socketMeterSourceMapping.get(socketId);
		if ( meterSourceId == null ) {
			log.warn(
					"No meter source ID available for socket ID {}, starting meter value will not be available for charge session",
					socketId);
		}

		final ACEnergyDatum meterReading = getMeterReading(meterSourceId);

		session = new ChargeSession();
		session.setCreated(new Date(now));
		session.setIdTag(idTag);
		session.setSocketId(socketId);

		final boolean authorized = authManager.authorize(idTag);
		log.debug("{} authorized: {}", idTag, authorized);

		// we ALLOW the session even if no client available and post the results up later
		postStartTransaction(idTag, reservationId, connectorId, session, now, meterReading);

		final String sessionId = chargeSessionDao.storeChargeSession(session);

		// insert transaction begin readings
		List<Value> readings = readingsForDatum(meterReading);
		for ( Value v : readings ) {
			v.setContext(ReadingContext.TRANSACTION_BEGIN);
		}
		chargeSessionDao.addMeterReadings(sessionId, (meterReading != null ? meterReading.getCreated()
				: new Date(now)), readings);

		return sessionId;
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
			final EnergyDatum meterReading) {
		final CentralSystemServiceFactory system = getCentralSystem();
		final CentralSystemService client = (system != null ? system.service() : null);
		StartTransactionResponse res = null;
		if ( client != null ) {
			StartTransactionRequest req = new StartTransactionRequest();
			req.setConnectorId(connectorId);
			req.setIdTag(idTag);
			req.setReservationId(reservationId);
			req.setTimestamp(newXmlCalendar(now));
			if ( meterReading != null && meterReading.getWattHourReading() != null ) {
				req.setMeterStart(meterReading.getWattHourReading().intValue());
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

		// get current meter reading
		final String meterSourceId = socketMeterSourceMapping.get(session.getSocketId());
		if ( meterSourceId == null ) {
			log.warn(
					"No meter source ID available for socket ID {}, final meter value will not be available for charge session",
					session.getSocketId());
		}
		final ACEnergyDatum meterReading = getMeterReading(meterSourceId);

		// add end transaction readings
		List<Value> readings = readingsForDatum(meterReading);
		for ( Value v : readings ) {
			v.setContext(ReadingContext.TRANSACTION_END);
		}
		chargeSessionDao.addMeterReadings(sessionId, (meterReading != null ? meterReading.getCreated()
				: new Date(now)), readings);

		// post the stop transaction, if we have a transaction ID
		postStopTransaction(idTag, session, now, meterReading);

		// persist changes to DB
		session.setEnded(new Date(now));
		chargeSessionDao.storeChargeSession(session);
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
			final long now, final EnergyDatum meterReading) {
		CentralSystemServiceFactory system = getCentralSystem();
		CentralSystemService client = (system != null ? system.service() : null);
		StopTransactionResponse res = null;
		if ( session.getTransactionId() != null && client != null ) {
			StopTransactionRequest req = new StopTransactionRequest();
			req.setIdTag(idTag);
			if ( meterReading != null && meterReading.getWattHourReading() != null ) {
				req.setMeterStop(meterReading.getWattHourReading().intValue());
			}
			req.setTimestamp(newXmlCalendar(now));
			req.setTransactionId(session.getTransactionId());

			// add any associated readings
			List<ChargeSessionMeterReading> readings = chargeSessionDao
					.findMeterReadingsForSession(session.getSessionId());
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
			if ( data.getValues().size() > 0 ) {
				req.getTransactionData().add(data);
			}

			res = client.stopTransaction(req, system.chargeBoxIdentity());
			if ( res.getIdTagInfo() != null ) {
				IdTagInfo info = res.getIdTagInfo();
				if ( info.getStatus() != null ) {
					session.setStatus(info.getStatus());
				}
			}
		}
		return res;
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

	// EventHandler

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		try {
			if ( topic.equals(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED) ) {
				handleDatumCapturedEvent(event);
			}
		} catch ( RuntimeException e ) {
			log.error("Error handling event {}", topic, e);
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
		ChargeSession active = activeChargeSession(socketId);
		if ( active == null ) {
			return;
		}

		final long created = (eventProperties.get("created") instanceof Number ? ((Number) eventProperties
				.get("created")).longValue() : System.currentTimeMillis());

		// reconstruct Datum from event properties
		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		ClassUtils.setBeanProperties(datum, eventProperties, true);

		// store readings in DB
		List<Value> readings = readingsForDatum(datum);
		chargeSessionDao.addMeterReadings(active.getSessionId(), new Date(created), readings);
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
		results.add(new BasicTextFieldSettingSpecifier("meterDataSource.propertyFilters['UID']",
				"OCPP Meter"));
		results.add(new BasicTextFieldSettingSpecifier("socketMeterSourceMappingValue", defaults
				.getSocketMeterSourceMappingValue()));
		results.add(new BasicTextFieldSettingSpecifier("socketConnectorMappingValue", defaults
				.getSocketConnectorMappingValue()));
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
				buf.append(getMessageSource().getMessage(
						"status.active",
						new Object[] { incomplete.size(),
								StringUtils.commaDelimitedStringFromCollection(reversed) }, locale));
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
				String needIds = StringUtils.commaDelimitedStringFromCollection((need.size() > 10 ? need
						.subList(0, 10) : need));
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
}
