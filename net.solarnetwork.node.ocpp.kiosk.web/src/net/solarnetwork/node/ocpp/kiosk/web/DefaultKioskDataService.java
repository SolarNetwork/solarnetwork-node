/* ==================================================================
 * DefaultKioskDataService.java - 23/10/2016 6:31:26 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.kiosk.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;

/**
 * Default implementation of {@link KioskDataService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultKioskDataService
		implements KioskDataService, EventHandler, SettingSpecifierProvider {

	/**
	 * The name used to schedule the {@link KioskDataServiceRefreshJob} as.
	 */
	public static final String KIOSK_REFRESH_JOB_NAME = "OCPP_KioskRefresh";

	/**
	 * The job and trigger group used to schedule the
	 * {@link KioskDataServiceRefreshJob} with.
	 */
	public static final String SCHEDULER_GROUP = "OCPP";

	/**
	 * The interval at which to refresh the kiosk data.
	 */
	public static final long REFRESH_JOB_INTERVAL = 2 * 1000L;

	// model data for kiosk
	private final Map<String, Object> kioskData;

	// a mapping of socket ID -> model data
	private final Map<String, Map<String, Object>> socketDataMap;

	// a cache of socket ID -> data source for meter data
	private final Map<String, DatumDataSource<ACEnergyDatum>> socketMeterDataSources;

	private List<SocketConfiguration> socketConfigurations;
	private Collection<DatumDataSource<ACEnergyDatum>> meterDataSources;
	private ChargeSessionManager chargeSessionManager;
	private OptionalService<SimpMessageSendingOperations> messageSendingOps;
	private Scheduler scheduler;
	private MessageSource messageSource;
	private SimpleTrigger refreshKioskDataTrigger;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public DefaultKioskDataService() {
		super();
		kioskData = new ConcurrentHashMap<String, Object>(8);
		socketMeterDataSources = new HashMap<String, DatumDataSource<ACEnergyDatum>>(2);
		socketDataMap = new ConcurrentHashMap<String, Map<String, Object>>(2);
		kioskData.put("socketData", socketDataMap);
		socketConfigurations = new ArrayList<SocketConfiguration>(2);
	}

	@Override
	public void startup() {
		log.info("Starting up OCPP kiosk data service");
		configureKioskRefreshJob(REFRESH_JOB_INTERVAL);
	}

	@Override
	public void shutdown() {
		configureKioskRefreshJob(0);
	}

	@Override
	public Map<String, Object> getKioskData() {
		return kioskData;
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if ( topic.equals(ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED)
				|| topic.equals(ChargeSessionManager.EVENT_TOPIC_SESSION_ENDED) ) {
			handleSessionEvent(event);
		}
	}

	private String socketKeyForId(String socketId) {
		List<SocketConfiguration> confs = getSocketConfigurations();
		if ( socketId == null || confs == null || confs.isEmpty() ) {
			return null;
		}
		for ( SocketConfiguration conf : confs ) {
			if ( socketId.equals(conf.getSocketId()) ) {
				return conf.getKey();
			}
		}
		return null;
	}

	private void handleSessionEvent(Event event) {
		final boolean sessionStarted = (ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED
				.equals(event.getTopic()));
		final String sessionId = (String) event
				.getProperty(ChargeSessionManager.EVENT_PROPERTY_SESSION_ID);
		final String socketId = (String) event
				.getProperty(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID);
		final String socketKey = socketKeyForId(socketId);
		if ( socketKey == null ) {
			return;
		}
		Map<String, Object> sessionData = socketDataMap.get(socketKey);
		if ( sessionData == null ) {
			sessionData = new HashMap<String, Object>(8);
		}
		if ( sessionStarted ) {
			sessionData.put("sessionId", sessionId);
			sessionData.put("socketId", socketId);
			Number n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_DATE);
			if ( n != null ) {
				sessionData.put("startDate", n);
			}
			sessionData.put("duration", new AtomicLong(0L));
			n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER);
			sessionData.put("power", new AtomicInteger(n != null ? n.intValue() : 0));
			n = (Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY);
			sessionData.put("energyStart", n != null ? n : 0L);
			sessionData.put("energy", new AtomicInteger(0));
			sessionData.put("endDate", new AtomicLong(0));
			socketDataMap.put(socketKey, Collections.unmodifiableMap(sessionData));
		} else {
			updateSessionData(sessionData,
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER),
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY),
					(Number) event.getProperty(ChargeSessionManager.EVENT_PROPERTY_DATE));
		}
		postMessage(MESSAGE_TOPIC_KIOSK_DATA, kioskData);
		if ( !sessionStarted ) {
			socketDataMap.remove(socketKey);
		}
	}

	private void postMessage(String topic, Object payload) {
		SimpMessageSendingOperations ops = (messageSendingOps != null ? messageSendingOps.service()
				: null);
		if ( ops == null ) {
			return;
		}
		ops.convertAndSend(topic, payload);
	}

	private void updateSessionData(Map<String, Object> sessionData, Number powerReading,
			Number energyReading, Number endDate) {
		// update power value
		AtomicInteger power = (AtomicInteger) sessionData.get("power");
		if ( powerReading != null && power != null ) {
			power.set(powerReading.intValue());
		}

		// update energy value
		Number energyStart = (Number) sessionData.get("energyStart");
		AtomicInteger energy = (AtomicInteger) sessionData.get("energy");
		if ( energyReading != null && energyStart != null && energy != null ) {
			int oldEnergy = energy.get();
			int newEnergy = (int) (energyReading.longValue() - energyStart.longValue());
			energy.compareAndSet(oldEnergy, newEnergy);
		}

		// update duration, end date
		Number startDate = (Number) sessionData.get("startDate");
		long durationDate = System.currentTimeMillis();
		AtomicLong end = (AtomicLong) sessionData.get("endDate");
		if ( endDate != null && end != null ) {
			end.compareAndSet(0, endDate.longValue());
			durationDate = end.get();
		}
		AtomicLong duration = (AtomicLong) sessionData.get("duration");
		if ( startDate != null && duration != null ) {
			duration.set(durationDate - startDate.longValue());
		}
	}

	private Map<String, Object> sessionDataForSocket(String socketId) {
		final String socketKey = socketKeyForId(socketId);
		if ( socketKey == null ) {
			return null;
		}
		return socketDataMap.get(socketKey);
	}

	// TODO: repopulate kiosk data when configured after startup
	//	private void populateSessionDataForSocket(String socketId) {
	//		// either no session active on socket, or we've restarted and need to re-create the info
	//		ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
	//		if ( session == null ) {
	//			// no session info for this socket
	//			return;
	//		}
	//		// maybe the node has restarted mid-session; pretend we got a Start event
	//		Map<String, Object> eventData = new HashMap<String, Object>(8);
	//		eventData.put(ChargeSessionManager.EVENT_PROPERTY_DATE, session.getCreated().getTime());
	//		eventData.put(ChargeSessionManager.EVENT_PROPERTY_SESSION_ID, session.getSessionId());
	//		eventData.put(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID, session.getSocketId());
	//
	//		List<ChargeSessionMeterReading> readings = chargeSessionManager
	//				.meterReadingsForChargeSession(session.getSessionId());
	//		if ( readings != null && !readings.isEmpty() ) {
	//			int left = 2;
	//			for ( ChargeSessionMeterReading reading : readings ) {
	//				if ( ReadingContext.TRANSACTION_BEGIN.equals(reading.getContext()) ) {
	//					if ( Measurand.POWER_ACTIVE_IMPORT.equals(reading.getMeasurand()) ) {
	//						Integer power = Integer.valueOf(reading.getValue());
	//						eventData.put(ChargeSessionManager.EVENT_PROPERTY_METER_READING_POWER,
	//								power);
	//						left--;
	//					} else if ( Measurand.ENERGY_ACTIVE_IMPORT_REGISTER
	//							.equals(reading.getMeasurand()) ) {
	//						Long energy = Long.valueOf(reading.getValue());
	//						eventData.put(ChargeSessionManager.EVENT_PROPERTY_METER_READING_ENERGY,
	//								energy);
	//						left--;
	//					}
	//				}
	//				if ( left < 1 ) {
	//					break;
	//				}
	//			}
	//		}
	//		handleEvent(new Event(ChargeSessionManager.EVENT_TOPIC_SESSION_STARTED, eventData));
	//	}

	@Override
	public void refreshKioskData() {
		if ( socketConfigurations == null || socketConfigurations.isEmpty() ) {
			return;
		}
		for ( SocketConfiguration socketConf : socketConfigurations ) {
			final String socketId = socketConf.getSocketId();
			if ( socketId == null ) {
				continue;
			}
			final Map<String, Object> sessionData = sessionDataForSocket(socketId);
			if ( sessionData == null ) {
				continue;
			}

			// get socket activation state

			DatumDataSource<ACEnergyDatum> meterDataSource = socketMeterDataSources.get(socketId);
			if ( meterDataSource == null ) {
				for ( DatumDataSource<ACEnergyDatum> ds : meterDataSources ) {
					if ( socketConf.getMeterDataSourceUID().equals(ds.getUID()) ) {
						meterDataSource = ds;
						// cache the data source mapping as we don't expect it to change
						socketMeterDataSources.put(socketId, ds);
						break;
					}
				}
			}
			if ( meterDataSource == null ) {
				log.warn("Meter data source {} not available for socket {}",
						socketConf.getMeterDataSourceUID(), socketId);
				continue;
			}

			// get meter readings for this socket
			ACEnergyDatum meterData = meterDataSource.readCurrentDatum();
			if ( meterData != null ) {
				updateSessionData(sessionData, meterData.getWatts(), meterData.getWattHourReading(),
						null);
				postMessage(MESSAGE_TOPIC_KIOSK_DATA, kioskData);
			}
		}
	}

	private boolean configureKioskRefreshJob(final long interval) {
		final Scheduler sched = scheduler;
		if ( sched == null ) {
			log.warn("No scheduler avaialable, cannot schedule OCPP kiosk refresh job");
			return false;
		}
		SimpleTrigger trigger = refreshKioskDataTrigger;
		if ( trigger != null ) {
			// check if interval actually changed
			if ( trigger.getRepeatInterval() == interval ) {
				log.debug("OCPP kiosk refresh interval unchanged at {}s", interval);
				return true;
			}
			// trigger has changed!
			if ( interval == 0 ) {
				try {
					sched.unscheduleJob(trigger.getKey());
					log.info("Unscheduled OCPP kiosk refresh job");
				} catch ( SchedulerException e ) {
					log.error("Error unscheduling OCPP kiosk refresh job", e);
				} finally {
					refreshKioskDataTrigger = null;
				}
			} else {
				trigger = TriggerBuilder.newTrigger().withIdentity(trigger.getKey())
						.forJob(KIOSK_REFRESH_JOB_NAME, SCHEDULER_GROUP)
						.withSchedule(
								SimpleScheduleBuilder.repeatMinutelyForever((int) (interval / (60000L))))
						.build();
				try {
					sched.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling Loxone datum logger job", e);
				} finally {
					refreshKioskDataTrigger = null;
				}
			}
			return true;
		} else if ( interval == 0 ) {
			return true;
		}

		synchronized ( sched ) {
			try {
				final JobKey jobKey = new JobKey(KIOSK_REFRESH_JOB_NAME, SCHEDULER_GROUP);
				JobDetail jobDetail = sched.getJobDetail(jobKey);
				if ( jobDetail == null ) {
					jobDetail = JobBuilder.newJob(KioskDataServiceRefreshJob.class).withIdentity(jobKey)
							.storeDurably().build();
					sched.addJob(jobDetail, true);
				}
				final TriggerKey triggerKey = new TriggerKey(KIOSK_REFRESH_JOB_NAME, SCHEDULER_GROUP);
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
						.startAt(new Date(System.currentTimeMillis() + interval))
						.usingJobData(new JobDataMap(Collections.singletonMap("dataService", this)))
						.withSchedule(
								SimpleScheduleBuilder.repeatSecondlyForever((int) (interval / (1000L)))
										.withMisfireHandlingInstructionNextWithExistingCount())
						.build();
				sched.scheduleJob(trigger);
				log.info("Scheduled OCPP kiosk refresh job to run every {} seconds", (interval / 1000));
				refreshKioskDataTrigger = trigger;
				return true;
			} catch ( Exception e ) {
				log.error("Error scheduling OCPP kiosk refresh job", e);
				return false;
			}
		}
	}

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "OCPP Kiosk Data Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));

		// dynamic list of SocketConfiguration
		Collection<SocketConfiguration> socketConfs = getSocketConfigurations();
		BasicGroupSettingSpecifier socketConfsGroup = SettingsUtil.dynamicListSettingSpecifier(
				"socketConfigurations", socketConfs,
				new SettingsUtil.KeyedListCallback<SocketConfiguration>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(SocketConfiguration value,
							int index, String key) {
						BasicGroupSettingSpecifier socketConfGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(socketConfGroup);
					}
				});
		results.add(socketConfsGroup);
		return results;
	}

	public FilterableService getFilterableChargeSessionManager() {
		return (chargeSessionManager instanceof FilterableService
				? (FilterableService) chargeSessionManager : null);
	}

	/**
	 * Set the collection of all available meter data sources from which to find
	 * the appropriate ones to associate with each configured socket.
	 * 
	 * @param meterDataSources
	 *        The collection of meter data sources.
	 */
	public void setMeterDataSources(Collection<DatumDataSource<ACEnergyDatum>> meterDataSources) {
		this.meterDataSources = meterDataSources;
	}

	/**
	 * Set the charge session manager to use.
	 * 
	 * @param chargeSessionManager
	 *        The charge session manager.
	 */
	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	public List<SocketConfiguration> getSocketConfigurations() {
		return socketConfigurations;
	}

	public void setSocketConfigurations(List<SocketConfiguration> socketConfigurations) {
		this.socketConfigurations = socketConfigurations;
	}

	/**
	 * Get the number of configured {@code socketConfigurations} elements.
	 * 
	 * @return The number of {@code socketConfigurations} elements.
	 */
	public int getSocketConfigurationsCount() {
		List<SocketConfiguration> l = getSocketConfigurations();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code socketConfigurations} elements.
	 * 
	 * @param count
	 *        The desired number of {@code socketConfigurations} elements.
	 */
	public void setSocketConfigurationsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<SocketConfiguration> l = getSocketConfigurations();
		int lCount = (l == null ? 0 : l.size());
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			if ( l == null ) {
				l = new ArrayList<SocketConfiguration>(count);
				setSocketConfigurations(l);
			}
			l.add(new SocketConfiguration());
			lCount++;
		}
	}

	public void setMessageSendingOps(OptionalService<SimpMessageSendingOperations> messageSendingOps) {
		this.messageSendingOps = messageSendingOps;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
