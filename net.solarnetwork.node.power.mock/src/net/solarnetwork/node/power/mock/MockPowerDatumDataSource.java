/* ===================================================================
 * MockPowerDatumDataSource.java
 * 
 * Created Dec 2, 2009 11:21:21 AM
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

package net.solarnetwork.node.power.mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.Mock;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicSliderSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Mock implementation of {@link DatumDataSource} for {@link PowerDatum}
 * objects.
 * 
 * <p>
 * This simple implementation returns one of two different PowerDatum instances,
 * one for "daytime" and one for "nighttime". It is designed for testing and
 * debugging purposes only.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
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
 * @version 1.2
 */
public class MockPowerDatumDataSource implements DatumDataSource<PowerDatum>, SettingSpecifierProvider {

	/** The default value for the {@code hourDayStart} property. */
	public static final int DEFAULT_HOUR_DAY_START = 8;

	/** The default value for the {@code hourNightStart} property. */
	public static final int DEFAULT_HOUR_NIGHT_START = 20;

	private static final PowerDatum DAY_POWER = new MockPowerDatum(4.0, 12.0F, null, null, 0.0F, 0.0F,
			1100, 12.0, 0.144);

	private static final PowerDatum NIGHT_POWER = new MockPowerDatum(4.0, 12.0F, null, null, 1.0F,
			12.0F, 0, 12.0, 0.144);

	private final Logger log = LoggerFactory.getLogger(MockPowerDatumDataSource.class);

	private final AtomicLong counter = new AtomicLong(0);

	private Integer dayWatts = 5;
	private Integer nightWatts = 0;
	private int dayWattRandomness = 900;
	private boolean enabled = true;

	private int hourDayStart = DEFAULT_HOUR_DAY_START;
	private int hourNightStart = DEFAULT_HOUR_NIGHT_START;

	private String sourceId = "MockSource";
	private String groupUID = "Mock";

	private MessageSource messageSource = null;

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		if ( !enabled ) {
			log.debug("Not enabled");
			return null;
		}
		Calendar now = Calendar.getInstance();
		PowerDatum result = null;
		if ( now.get(Calendar.HOUR_OF_DAY) >= this.hourDayStart
				&& now.get(Calendar.HOUR_OF_DAY) < this.hourNightStart ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Returning day power between {}am and {}pm", this.hourDayStart,
						(this.hourNightStart - 12));
			}
			result = (PowerDatum) DAY_POWER.clone();
			result.setWatts(dayWatts);
			applyRandomness(result, dayWattRandomness);
		} else {
			if ( log.isDebugEnabled() ) {
				log.debug("Returning night power after {}pm", (this.hourNightStart - 12));
			}
			result = (PowerDatum) NIGHT_POWER.clone();
			result.setWatts(nightWatts);
		}
		result.setSourceId(sourceId);
		long wattHours = counter.addAndGet(Math.round(Math.random() * 100.0));
		result.setWattHourReading(wattHours);
		return result;
	}

	private void applyRandomness(final PowerDatum datum, final int r) {
		int watts = datum.getWatts();
		watts += (int) (Math.random() * r * (Math.random() > 0.2 ? 1.0f : -1.0f));
		if ( watts < 0 ) {
			watts = 0;
		}
		datum.setWatts(watts);
	}

	public static class MockPowerDatum extends PowerDatum implements Mock, Cloneable {

		private final String statusMessage = "All clear";

		private MockPowerDatum(Double batteryAmpHours, Float batteryVolts, Float acOutputAmps,
				Float acOutputVolts, Float dcOutputAmps, Float dcOutputVolts, Integer watts,
				Double ampHoursToday, Double kWattHoursToday) {
			super(batteryAmpHours, batteryVolts, acOutputAmps, acOutputVolts, dcOutputAmps,
					dcOutputVolts, watts, ampHoursToday, kWattHoursToday);
		}

		public String getStatusMessage() {
			return statusMessage;
		}

	}

	@Override
	public synchronized MessageSource getMessageSource() {
		if ( messageSource == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			messageSource = source;
		}
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Arrays.asList((SettingSpecifier) new BasicToggleSettingSpecifier("enabled", true),
				(SettingSpecifier) new BasicSliderSettingSpecifier("dayWatts", 5.0, 0.0, 20.0, 0.1),
				(SettingSpecifier) new BasicSliderSettingSpecifier("nightWatts", 0.0, 0.0, 5.0, 0.1));
	}

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
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

	public Integer getDayWatts() {
		return dayWatts;
	}

	public void setDayWatts(Integer dayWatts) {
		this.dayWatts = dayWatts;
	}

	public Integer getNightWatts() {
		return nightWatts;
	}

	public void setNightWatts(Integer nightWatts) {
		this.nightWatts = nightWatts;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

	@Override
	public String getUID() {
		return getSourceId();
	}

	public int getDayWattRandomness() {
		return dayWattRandomness;
	}

	public void setDayWattRandomness(int dayWattRandomness) {
		this.dayWattRandomness = dayWattRandomness;
	}

}
