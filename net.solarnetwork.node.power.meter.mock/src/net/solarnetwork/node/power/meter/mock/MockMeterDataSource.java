/* ==================================================================
 * MockMeterDataSource.java - 10/06/2015 1:28:07 pm
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

package net.solarnetwork.node.power.meter.mock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Mock implementation of {@link DatumDataSource} to simulate a power meter used
 * in load shedding environment.
 * 
 * @author matt
 * @version 1.0
 */
public class MockMeterDataSource implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider, EventHandler {

	private OptionalService<EventAdmin> eventAdmin;
	private MessageSource messageSource;
	private long sampleCacheMs = 5000;
	private String uid = "MockMeter";
	private String groupUID;
	private double watts = 5;
	private double wattsRandomness = 0.2;
	private Map<String, Integer> loadShedControlPowerMapping;

	private GeneralNodeACEnergyDatum sample;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<String, Integer> shedMap = new ConcurrentHashMap<String, Integer>(4);

	private final AtomicLong mockMeter = new AtomicLong(meterStartValue());

	/**
	 * Get a mock starting value for our meter. As we expect meters to only
	 * increase, the value returned here is based on the current time. Thus
	 * starting/stopping this service won't roll the meter back.
	 * 
	 * @return a starting meter value
	 */
	private long meterStartValue() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Date now = cal.getTime();
		cal.set(2010, cal.getMinimum(Calendar.MONTH), 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (now.getTime() - cal.getTimeInMillis()) / (1000L * 60);
	}

	private GeneralNodeACEnergyDatum getCurrentSample() {
		GeneralNodeACEnergyDatum currSample = sample;
		if ( isSampleExpired(currSample) ) {
			GeneralNodeACEnergyDatum newSample = new GeneralNodeACEnergyDatum();
			newSample.setCreated(new Date());
			newSample.setSourceId(uid);
			newSample.setPhase(ACPhase.Total);
			if ( currSample == null ) {
				newSample.setWattHourReading(mockMeter.get());
			} else {
				double diffHours = ((newSample.getCreated().getTime() - currSample.getCreated()
						.getTime()) / (double) (1000 * 60 * 60));
				double currWatts = this.watts;
				for ( Integer shedWatts : shedMap.values() ) {
					currWatts -= shedWatts.intValue();
				}
				currWatts += (currWatts * (Math.random() * wattsRandomness) * (Math.random() < 0.5 ? -1
						: 1));
				if ( currWatts < 0.0 ) {
					currWatts = 0;
				}
				long wh = (long) (currWatts * diffHours);
				long newWh = currSample.getWattHourReading() + wh;
				if ( mockMeter.compareAndSet(currSample.getWattHourReading(), newWh) ) {
					newSample.setWattHourReading(newWh);
					newSample.setWatts((int) currWatts);
				} else {
					newSample.setWattHourReading(currSample.getWattHourReading());
				}
			}
			log.debug("Read mock data: {}", newSample);
			currSample = newSample;
			sample = newSample;
		}
		return currSample;
	}

	private boolean isSampleExpired(GeneralNodeACEnergyDatum datum) {
		if ( datum == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - datum.getCreated().getTime();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public void handleEvent(Event event) {
		Map<String, Integer> mapping = getLoadShedControlPowerMapping();
		if ( mapping == null ) {
			return;
		}
		final String topic = event.getTopic();
		if ( topic.equals(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED)
				|| topic.equals(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED) ) {
			String controlId = (String) event.getProperty("controlId");
			String controlValue = (String) event.getProperty("value");
			Integer shedValue = mapping.get(controlId);
			if ( shedValue != null ) {
				if ( controlValue.equals(Boolean.TRUE.toString()) ) {
					// we are shedding load
					shedMap.putIfAbsent(controlId, shedValue);
				} else {
					// not shedding load
					shedMap.remove(controlId, shedValue);
				}
			}
		}
	}

	@Override
	public String getUID() {
		return uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.power.meter.mock.meter";
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockMeterDataSource defaults = new MockMeterDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.uid));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));

		results.add(new BasicTextFieldSettingSpecifier("watts", String.valueOf(defaults.watts)));
		results.add(new BasicTextFieldSettingSpecifier("wattsRandomness", String
				.valueOf(defaults.wattsRandomness)));
		results.add(new BasicTextFieldSettingSpecifier("loadShedControlPowerMappingValue", defaults
				.getLoadShedControlPowerMappingValue()));

		return results;
	}

	private String getInfoMessage() {
		StringBuilder buf = new StringBuilder();
		EnergyDatum latest = sample;
		if ( latest != null ) {
			buf.append("Latest reading: ").append(latest.getWatts()).append("W @ ")
					.append(latest.getCreated());
		}
		int shedTotal = 0;
		for ( Integer shedWatts : shedMap.values() ) {
			shedTotal += shedWatts;
		}
		if ( shedTotal > 0 ) {
			buf.append("; shedding ").append(shedTotal).append("W");
		}
		return buf.toString();
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return getDatumType();
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		return Collections.singletonList(readCurrentDatum());
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final GeneralNodeACEnergyDatum d = getCurrentSample();
		if ( d.getCreated().getTime() >= start ) {
			// we read from the meter
			postDatumCapturedEvent(d, ACEnergyDatum.class);
		}
		return d;
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createDatumCapturedEvent(Datum, Class)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the {@link Datum} to post the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @since 1.3
	 */
	protected final void postDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum, eventDatumType);
		ea.postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @return the new Event instance
	 * @since 1.3
	 */
	protected Event createDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(datum, null);
		props.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, eventDatumType.getName());
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void setWatts(double watts) {
		this.watts = watts;
	}

	public void setWattsRandomness(double wattRandomness) {
		this.wattsRandomness = wattRandomness;
	}

	public Map<String, Integer> getLoadShedControlPowerMapping() {
		return loadShedControlPowerMapping;
	}

	public void setLoadShedControlPowerMapping(Map<String, Integer> loadShedControlPowerMapping) {
		this.loadShedControlPowerMapping = loadShedControlPowerMapping;
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
	 * @see #getLoadShedControlPowerMappingValue()
	 * @see #setLoadShedControlPowerMapping(Map)
	 */
	public final void setLoadShedControlPowerMappingValue(String mapping) {
		Map<String, String> map = StringUtils.delimitedStringToMap(mapping, ",", "=");
		if ( map == null || map.size() < 0 ) {
			map = Collections.emptyMap();
		}
		Map<String, Integer> resultMap = new LinkedHashMap<String, Integer>(map.size());
		for ( Map.Entry<String, String> me : map.entrySet() ) {
			try {
				Integer value = Integer.valueOf(me.getValue());
				resultMap.put(me.getKey(), value);
			} catch ( NumberFormatException e ) {
				log.debug("Ignoring invalid load shed power value {}, mapped from control ID {}",
						me.getValue(), me.getKey());
			}
		}
		setLoadShedControlPowerMapping(resultMap);
	}

	/**
	 * Get a delimited string representation of the
	 * {@link #getLoadShedControlPowerMapping()} map.
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
	 * @see #getLoadShedControlPowerMapping()
	 */
	public final String getLoadShedControlPowerMappingValue() {
		return StringUtils.delimitedStringFromMap(getLoadShedControlPowerMapping());
	}

}
