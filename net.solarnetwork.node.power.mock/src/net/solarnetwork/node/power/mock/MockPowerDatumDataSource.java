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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.power.mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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
 * <p>This simple implementation returns one of two different PowerDatum 
 * instances, one for "daytime" and one for "nighttime". It is designed
 * for testing and debugging purposes only.</p>
 *
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>hourDayStart</dt>
 *   <dd>The hour to consider as the start of "daytime". Defaults to
 *   {@link #DEFAULT_HOUR_DAY_START}.</dd>
 *   
 *   <dt>hourNightStart</dt>
 *   <dd>The hour to consider as the start of "nighttime". Defaults to
 *   {@link #DEFAULT_HOUR_NIGHT_START}.</dd>
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockPowerDatumDataSource implements DatumDataSource<PowerDatum>, SettingSpecifierProvider {

	/** The default value for the {@code hourDayStart} property. */
	public static final int DEFAULT_HOUR_DAY_START = 8;

	/** The default value for the {@code hourNightStart} property. */
	public static final int DEFAULT_HOUR_NIGHT_START = 20;
	
	private static final PowerDatum DAY_POWER 
		= new MockPowerDatum(4.0, 12.0F, null, null, 0.0F, 0.0F, 5.0F, 
				18.0F, 12.0, 0.144);

	private static final PowerDatum NIGHT_POWER 
		= new MockPowerDatum(4.0, 12.0F, null, null, 1.0F, 12.0F, 0.0F, 
				0.0F, 12.0, 0.144);
	
	private final Logger log = LoggerFactory.getLogger(
			MockPowerDatumDataSource.class);
	
	private Float dayPvAmps = 5.0F;
	private Float nightPvAmps = 0.0F;
	private boolean enabled = true;

	private int hourDayStart = DEFAULT_HOUR_DAY_START;
	private int hourNightStart = DEFAULT_HOUR_NIGHT_START;

	private MessageSource messageSource = null;

	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	public PowerDatum readCurrentDatum() {
		if ( !enabled ) {
			log.debug("Not enabled");
			return null;
		}
		Calendar now = Calendar.getInstance();
		if ( now.get(Calendar.HOUR_OF_DAY) >= this.hourDayStart && 
				now.get(Calendar.HOUR_OF_DAY) < this.hourNightStart ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Returning day power between {}am and {}pm", this.hourDayStart,
						(this.hourNightStart - 12));
			}
			PowerDatum result = (PowerDatum) DAY_POWER.clone();
			result.setPvAmps(dayPvAmps);
			return result;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Returning night power after {}pm", (this.hourNightStart - 12));
		}
		PowerDatum result = (PowerDatum) NIGHT_POWER.clone();
		result.setPvAmps(nightPvAmps);
		return result;
	}
	
	private static class MockPowerDatum extends PowerDatum 
	implements Mock, Cloneable {

		private MockPowerDatum(Double batteryAmpHours, Float batteryVolts,
				Float acOutputAmps, Float acOutputVolts, Float dcOutputAmps,
				Float dcOutputVolts, Float pvAmps, Float pvVolts,
				Double ampHoursToday, Double kWattHoursToday) {
			super(batteryAmpHours, batteryVolts, acOutputAmps, acOutputVolts, dcOutputAmps,
					dcOutputVolts, pvAmps, pvVolts, ampHoursToday, kWattHoursToday);
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
		return Arrays
				.asList((SettingSpecifier) new BasicToggleSettingSpecifier("enabled", true),
						(SettingSpecifier) new BasicSliderSettingSpecifier("dayPvAmps", 5.0, 0.0,
								20.0, 0.1), (SettingSpecifier) new BasicSliderSettingSpecifier(
								"nightPvAmps", 0.0, 0.0, 5.0, 0.1));
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

	public Float getDayPvAmps() {
		return dayPvAmps;
	}

	public void setDayPvAmps(Float dayPvAmps) {
		this.dayPvAmps = dayPvAmps;
	}

	public Float getNightPvAmps() {
		return nightPvAmps;
	}

	public void setNightPvAmps(Float nightPvAmps) {
		this.nightPvAmps = nightPvAmps;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
}
