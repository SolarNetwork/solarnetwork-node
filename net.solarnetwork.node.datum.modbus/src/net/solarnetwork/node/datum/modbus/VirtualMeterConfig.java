/* ==================================================================
 * VirtualMeterConfig.java - 23/08/2018 9:24:06 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to use virtual metering on.
 * 
 * <p>
 * The {@link #getConfig()} value represents the current meter reading value.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class VirtualMeterConfig extends GeneralDatumSamplePropertyConfig<BigInteger> {

	private TimeUnit timeUnit = TimeUnit.HOURS;

	public static List<SettingSpecifier> settings(String prefix, String meterReading) {
		VirtualMeterConfig defaults = new VirtualMeterConfig();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for timeUnit
		BasicMultiValueSettingSpecifier timeUnitSpec = new BasicMultiValueSettingSpecifier(
				prefix + "timeUnitName", defaults.getTimeUnitName());
		Map<String, String> timeUnitTitles = new LinkedHashMap<String, String>(3);
		for ( TimeUnit e : TimeUnit.values() ) {
			String desc = e.name().substring(0, 1) + e.name().substring(1).toLowerCase();
			timeUnitTitles.put(e.name(), desc);
		}
		timeUnitSpec.setValueTitles(timeUnitTitles);
		results.add(timeUnitSpec);

		// meter reading has "live" data, not static default
		results.add(new BasicTextFieldSettingSpecifier(prefix + "meterReading", meterReading));

		return results;
	}

	/**
	 * Get the datum property name used for this configuration.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyKey()}.
	 * </p>
	 * 
	 * @return the property name
	 */
	public String getName() {
		return getPropertyKey();
	}

	/**
	 * Set the datum property name to use.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyKey(String)}.
	 * </p>
	 * 
	 * @param name
	 *        the property name
	 */
	public void setName(String name) {
		setPropertyKey(name);
	}

	/**
	 * Get the meter reading as a string.
	 * 
	 * <p>
	 * Because the meter reading is a {@link BigDecimal}, this method can be
	 * used to get the number as a string. If the meter reading value is
	 * {@literal null} this method will return {@literal "0"}.
	 * </p>
	 * 
	 * @return the meter reading as a string, never {@literal null}
	 */
	public String getMeterReading() {
		BigInteger v = getConfig();
		return (v != null ? v.toString() : "0");
	}

	/**
	 * Set the meter reading via a string.
	 * 
	 * <p>
	 * This method silently ignores errors parsing {@code value} as a
	 * {@link BigInteger}.
	 * </p>
	 * 
	 * @param value
	 *        the meter reading value to set
	 */
	public void setMeterReading(String value) {
		try {
			setConfig(new BigInteger(value));
		} catch ( NumberFormatException e ) {
			// ignore
		}
	}

	/**
	 * Get the meter reading as a long.
	 * 
	 * <p>
	 * Because the meter reading is a {@link BigDecimal}, this method can be
	 * used to get the number as a long. If the meter reading value is
	 * {@literal null} this method will return {@literal 0}. If the meter
	 * reading value is too large to fit in a long, it will be truncated.
	 * </p>
	 * 
	 * @return the meter reading as a long
	 */
	public long getMeterReadingLong() {
		BigInteger v = getConfig();
		return (v != null ? v.longValue() : 0L);
	}

	/**
	 * Set the meter reading via a long.
	 * 
	 * @param value
	 *        the meter reading value to set
	 */
	public void setMeterReadingLong(long value) {
		setConfig(BigInteger.valueOf(value));
	}

	/**
	 * Get the time unit to virtualize the meter with.
	 * 
	 * @return the time unit
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	/**
	 * Set the time unit to virtualize the meter with.
	 * 
	 * @param timeUnit
	 *        the time unit
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		if ( timeUnit == null ) {
			return;
		}
		this.timeUnit = timeUnit;
	}

	/**
	 * Get the time unit to virtualize the meter with, as a string.
	 * 
	 * @return the time unit
	 */
	public String getTimeUnitName() {
		return timeUnit.name();
	}

	/**
	 * Set the time unit to virtualize the meter with, as a string.
	 * 
	 * <p>
	 * This method silently ignores invalid {@code timeUnit} values.
	 * </p>
	 * 
	 * @param timeUnit
	 *        the time unit to set
	 */
	public void setTimeUnitName(String timeUnit) {
		if ( timeUnit == null ) {
			return;
		}
		try {
			setTimeUnit(TimeUnit.valueOf(timeUnit));
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
	}
}
