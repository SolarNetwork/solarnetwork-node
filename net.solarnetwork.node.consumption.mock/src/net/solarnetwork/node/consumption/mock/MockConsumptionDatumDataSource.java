/* ===================================================================
 * MockConsumptionDatumDataSource.java
 * 
 * Created Dec 4, 2009 9:19:17 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.consumption.mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.Mock;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link DatumDataSource} for {@link ConsumptionDatum}
 * objects.
 * 
 * <p>
 * This simple implementation returns one of two different ConsumptionDatum
 * instances, one for "daytime" and one for "nighttime". It is designed for
 * testing and debugging purposes only.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>sourceId</dt>
 * <dd>The {@code sourceId} to assign to all returned {@link ConsumptionDatum}
 * instances. Defaults to {@link #DEFAULT_MOCK_SOURCE_ID}.</dd>
 * 
 * <dt>hourDayStart</dt>
 * <dd>The hour to consider as the start of "daytime". Defaults to
 * {@link #DEFAULT_HOUR_DAY_START}.</dd>
 * 
 * <dt>hourNightStart</dt>
 * <dd>The hour to consider as the start of "nighttime". Defaults to
 * {@link #DEFAULT_HOUR_NIGHT_START}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.3
 */
public class MockConsumptionDatumDataSource implements DatumDataSource<ConsumptionDatum>,
		MultiDatumDataSource<ConsumptionDatum> {

	/** The default value for the {@code hourDayStart} property. */
	public static final int DEFAULT_HOUR_DAY_START = 8;

	/** The default value for the {@code hourNightStart} property. */
	public static final int DEFAULT_HOUR_NIGHT_START = 20;

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_MOCK_SOURCE_ID = "MockSource";

	private static final ConsumptionDatum DAY = new MockConsumptionDatum(DEFAULT_MOCK_SOURCE_ID, 480);

	private static final ConsumptionDatum NIGHT = new MockConsumptionDatum(DEFAULT_MOCK_SOURCE_ID, 45);

	private final Logger log = LoggerFactory.getLogger(MockConsumptionDatumDataSource.class);

	private OptionalService<EventAdmin> eventAdmin;
	private String sourceId = DEFAULT_MOCK_SOURCE_ID;
	private String groupUID = "Mock";
	private int hourDayStart = DEFAULT_HOUR_DAY_START;
	private int hourNightStart = DEFAULT_HOUR_NIGHT_START;
	private int dayWattRandomness = 200;
	private int nightWattRandomness = 50;

	private final AtomicLong counter = new AtomicLong(0);

	public static final class MockConsumptionDatum extends ConsumptionDatum implements Mock,
			ACEnergyDatum {

		private final ACPhase phase;

		public MockConsumptionDatum(String sourceId, Integer watts) {
			this(ACPhase.Total, sourceId, watts, null);
		}

		public MockConsumptionDatum(ACPhase phase, String sourceId, Integer watts, Long wattHourReading) {
			super(sourceId, watts);
			this.phase = phase;
			setWattHourReading(wattHourReading);
		}

		@Override
		public ACPhase getPhase() {
			return phase;
		}

		@Override
		public Integer getRealPower() {
			return getWatts();
		}

		@Override
		public Integer getApparentPower() {
			return getWatts();
		}

		@Override
		public Integer getReactivePower() {
			return getWatts();
		}

		@Override
		public Float getEffectivePowerFactor() {
			return getPowerFactor();
		}

		@Override
		public Float getFrequency() {
			return 50f;
		}

		@Override
		public Float getVoltage() {
			return 240.33f;
		}

		@Override
		public Float getCurrent() {
			return getWatts().floatValue() / getVoltage();
		}

		@Override
		public Float getPhaseVoltage() {
			return getVoltage();
		}

		@Override
		public Float getPowerFactor() {
			return 1.123f;
		}

	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		Calendar now = Calendar.getInstance();
		ConsumptionDatum result = null;
		if ( now.get(Calendar.HOUR_OF_DAY) >= this.hourDayStart
				&& now.get(Calendar.HOUR_OF_DAY) < this.hourNightStart ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Returning day consumption between " + this.hourDayStart + "am and "
						+ (this.hourNightStart - 12) + "pm");
			}
			result = (ConsumptionDatum) DAY.clone();
			applyRandomness(result, dayWattRandomness);
		} else {
			if ( log.isDebugEnabled() ) {
				log.debug("Returning night consumption after " + (this.hourNightStart - 12) + "pm");
			}
			result = (ConsumptionDatum) NIGHT.clone();
			applyRandomness(result, nightWattRandomness);
		}
		result.setCreated(now.getTime());
		result.setSourceId(sourceId);
		long wattHours = counter.addAndGet(Math.round(Math.random() * 100.0));
		result.setWattHourReading(wattHours);
		postDatumCapturedEvent(result, ConsumptionDatum.class);
		return result;
	}

	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return getDatumType();
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		ConsumptionDatum d = readCurrentDatum();
		if ( d == null ) {
			return Collections.emptyList();
		}
		Collection<ConsumptionDatum> results = Arrays.asList(
				d,
				new MockConsumptionDatum(ACPhase.PhaseA, d.getSourceId(), d.getWatts(), d
						.getWattHourReading()),
				new MockConsumptionDatum(ACPhase.PhaseB, d.getSourceId(), d.getWatts(), d
						.getWattHourReading()),
				new MockConsumptionDatum(ACPhase.PhaseC, d.getSourceId(), d.getWatts(), d
						.getWattHourReading()));
		Date created = d.getCreated();
		for ( ConsumptionDatum datum : results ) {
			if ( datum == d ) {
				continue; // already posted in readCurrentDatum
			}
			datum.setCreated(created);
			postDatumCapturedEvent(datum, ConsumptionDatum.class);
		}
		return results;
	}

	private void applyRandomness(final ConsumptionDatum datum, final int r) {
		int watts = datum.getWatts();
		watts += (int) (Math.random() * r * (Math.random() > 0.2 ? 1.0f : -1.0f));
		if ( watts < 0 ) {
			watts = 0;
		}
		datum.setWatts(watts);
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

	@Override
	public String getUID() {
		return getSourceId();
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public int getHourDayStart() {
		return hourDayStart;
	}

	public void setHourDayStart(int hourDayStart) {
		this.hourDayStart = hourDayStart;
	}

	public int getHourNightStart() {
		return hourNightStart;
	}

	public void setHourNightStart(int hourNightStart) {
		this.hourNightStart = hourNightStart;
	}

	public int getDayWattRandomness() {
		return dayWattRandomness;
	}

	public void setDayWattRandomness(int dayAmpRandomness) {
		this.dayWattRandomness = dayAmpRandomness;
	}

	public int getNightWattRandomness() {
		return nightWattRandomness;
	}

	public void setNightWattRandomness(int nightAmpRandomness) {
		this.nightWattRandomness = nightAmpRandomness;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
