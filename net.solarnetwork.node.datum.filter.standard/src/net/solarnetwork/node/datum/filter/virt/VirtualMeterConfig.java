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

package net.solarnetwork.node.datum.filter.virt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Configuration for a single datum property to use virtual metering on.
 * 
 * <p>
 * The {@link #getConfig()} value represents the current meter reading value.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.6
 */
public class VirtualMeterConfig extends DatumSamplePropertyConfig<BigInteger> {

	/** The {@code propertyType} default value. */
	public static final DatumSamplesType DEFAULT_PROPERTY_TYPE = DatumSamplesType.Instantaneous;

	/**
	 * The {@code timeUnit} default value.
	 * 
	 * @since 1.2
	 */
	public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.HOURS;

	/**
	 * The {@code maxAgeSeconds} default value.
	 * 
	 * @since 1.2
	 */
	public static final long DEFAULT_MAX_AGE_SECONDS = TimeUnit.HOURS.toSeconds(1);

	/**
	 * The {@code virtualMeterScale} property default value.
	 * 
	 * @since 1.2
	 */
	public static final int DEFAULT_VIRTUAL_METER_SCALE = 6;

	private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
	private long maxAgeSeconds = DEFAULT_MAX_AGE_SECONDS;
	private int rollingAverageCount = 0;
	private String readingPropertyName = null;
	private int virtualMeterScale = DEFAULT_VIRTUAL_METER_SCALE;
	private boolean trackOnlyWhenReadingChanges;
	private boolean includeInstantaneousDiffProperty;
	private String instantaneousDiffPropertyName = null;

	/**
	 * Constructor.
	 */
	public VirtualMeterConfig() {
		super();
		setPropertyType(DEFAULT_PROPERTY_TYPE);
	}

	/**
	 * Get a list of settings suitable for configuring an instance of this
	 * class.
	 * 
	 * @param prefix
	 *        the message key prefix
	 * @param expressionServices
	 *        an optional list of expression services
	 * @return the settings
	 */
	public List<SettingSpecifier> settings(String prefix,
			OptionalServiceCollection<ExpressionService> expressionServices) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", Character.toString(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( DatumSamplesType e : EnumSet.of(DatumSamplesType.Instantaneous,
				DatumSamplesType.Accumulating, DatumSamplesType.Status) ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "readingPropertyName", null));

		// drop-down menu for timeUnit
		BasicMultiValueSettingSpecifier timeUnitSpec = new BasicMultiValueSettingSpecifier(
				prefix + "timeUnitName", DEFAULT_TIME_UNIT.name());
		Map<String, String> timeUnitTitles = new LinkedHashMap<String, String>(3);
		for ( TimeUnit e : TimeUnit.values() ) {
			String desc = e.name().substring(0, 1) + e.name().substring(1).toLowerCase();
			timeUnitTitles.put(e.name(), desc);
		}
		timeUnitSpec.setValueTitles(timeUnitTitles);
		results.add(timeUnitSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxAgeSeconds",
				String.valueOf(DEFAULT_MAX_AGE_SECONDS)));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "virtualMeterScale",
				String.valueOf(DEFAULT_VIRTUAL_METER_SCALE)));

		results.add(new BasicToggleSettingSpecifier(prefix + "trackOnlyWhenReadingChanges", false));

		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "rollingAverageCount", String.valueOf(0)));

		results.add(new BasicToggleSettingSpecifier(prefix + "includeInstantaneousDiffProperty", false));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "instantaneousDiffPropertyName", null));

		return results;
	}

	/**
	 * Get the reading property name to use.
	 * 
	 * <p>
	 * This will return {@link #getReadingPropertyName()} if not empty.
	 * Otherwise a name derived from {@link #getPropertyKey()} with
	 * {@link #getTimeUnit()} appended will be used. The time unit will be
	 * formatted as a capitalized word, for example {@link TimeUnit#HOURS}
	 * becomes {@literal Hours}.
	 * </p>
	 * 
	 * @return the reading property name to use
	 * @since 1.1
	 */
	public String readingPropertyName() {
		String name = getReadingPropertyName();
		if ( name == null || name.isEmpty() ) {
			TimeUnit timeUnit = getTimeUnit();
			String tuName = timeUnit.name().substring(0, 1) + timeUnit.name().substring(1).toLowerCase();
			name = getPropertyKey() + tuName;
		}
		return name;
	}

	/**
	 * Get the instantaneous difference property name to use.
	 * 
	 * <p>
	 * This will return {@link #getInstantaneousDiffPropertyName()} if not
	 * empty. Otherwise a name dervied from {@link #readingPropertyName} with
	 * {@literal Diff} appended will be used.
	 * </p>
	 * 
	 * @return the instantaneous difference property name to use
	 * @since 2.0
	 */
	public String instantaneousDiffPropertyName() {
		String name = getInstantaneousDiffPropertyName();
		if ( name == null || name.isEmpty() ) {
			name = readingPropertyName() + "Diff";
		}
		return name;
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
	 * Because the meter reading is a {@link BigInteger}, this method can be
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

	/**
	 * Get the maximum age between readings where time aggregation can be
	 * applied.
	 * 
	 * @return the maximum reading age, in seconds; defaults to 1 hour
	 */
	public long getMaxAgeSeconds() {
		return maxAgeSeconds;
	}

	/**
	 * Set the maximum age between readings where time aggregation can be
	 * applied.
	 * 
	 * @param maxAgeSeconds
	 *        the maximum reading age, in seconds
	 */
	public void setMaxAgeSeconds(long maxAgeSeconds) {
		this.maxAgeSeconds = maxAgeSeconds;
	}

	/**
	 * Get the instantaneous rolling average sample count.
	 * 
	 * @return the count; defaults to {@code 0}
	 * @since 1.1
	 */
	public int getRollingAverageCount() {
		return rollingAverageCount;
	}

	/**
	 * Set the instantaneous rolling average sample count.
	 * 
	 * <p>
	 * When set to something greater than {@literal 1}, then apply a rolling
	 * average of this many instantaneous property samples to the transformed
	 * output value. This has the effect of smoothing the instantaneous values
	 * to an average over the time period leading into each output sample.
	 * </p>
	 * 
	 * @param rollingAverageCount
	 *        the count to set
	 * @since 1.1
	 */
	public void setRollingAverageCount(int rollingAverageCount) {
		this.rollingAverageCount = rollingAverageCount;
	}

	/**
	 * Get the accumulating property name to use.
	 * 
	 * @return the name, or {@literal null} to use a standard name
	 * @since 1.1
	 */
	public String getReadingPropertyName() {
		return readingPropertyName;
	}

	/**
	 * Set the accumulating property name to use.
	 * 
	 * @param readingPropertyName
	 *        the name to set, or {@literal null} to use a standard name
	 * @since 1.1
	 */
	public void setReadingPropertyName(String readingPropertyName) {
		this.readingPropertyName = readingPropertyName;
	}

	/**
	 * Get the scale (maximum number of decimal points) for the virtual meter.
	 * 
	 * @return the scale to round to, or {@literal -1} for no rounding; defaults
	 *         to {@link #DEFAULT_VIRTUAL_METER_SCALE}
	 * @since 1.2
	 */
	public int getVirtualMeterScale() {
		return virtualMeterScale;
	}

	/**
	 * Set the scale (maximum number of decimal points) for the virtual meter.
	 * 
	 * @param virtualMeterScale
	 *        the scale to set; if less than 0 then no rounding will occur
	 * @since 1.2
	 */
	public void setVirtualMeterScale(int virtualMeterScale) {
		this.virtualMeterScale = virtualMeterScale;
	}

	/**
	 * Get the "track only when reading changes" flag.
	 * 
	 * @return {@literal true} to only update the "previously seen" data if the
	 *         new reading changes from the previous reading
	 * @since 1.2
	 */
	public boolean isTrackOnlyWhenReadingChanges() {
		return trackOnlyWhenReadingChanges;
	}

	/**
	 * Set the "track only when reading changes" flag.
	 * 
	 * @param trackOnlyWhenReadingChanges
	 *        {@literal true} to only update the "previously seen" data if the
	 *        new reading changes from the previous reading
	 * @since 1.2
	 */
	public void setTrackOnlyWhenReadingChanges(boolean trackOnlyWhenReadingChanges) {
		this.trackOnlyWhenReadingChanges = trackOnlyWhenReadingChanges;
	}

	/**
	 * Get the "include instantaneous difference" flag.
	 * 
	 * @return {@literal true} if an instantaneous difference property should be
	 *         created
	 * @since 2.0
	 */
	public boolean isIncludeInstantaneousDiffProperty() {
		return includeInstantaneousDiffProperty;
	}

	/**
	 * Set the "include instantaneous difference" flag.
	 * 
	 * @param includeInstantaneousDiffProperty
	 *        {@literal true} if an instantaneous difference property should be
	 *        created
	 * @since 2.0
	 */
	public void setIncludeInstantaneousDiffProperty(boolean includeInstantaneousDiffProperty) {
		this.includeInstantaneousDiffProperty = includeInstantaneousDiffProperty;
	}

	/**
	 * Get the instantaneous difference property name to use.
	 * 
	 * @return the name, or {@literal null} to use a standard name
	 * @since 2.0
	 */
	public String getInstantaneousDiffPropertyName() {
		return instantaneousDiffPropertyName;
	}

	/**
	 * Set the instantaneous difference property name to use.
	 * 
	 * @param instantaneousDiffPropertyName
	 *        the name, or {@literal null} to use a standard name
	 * @since 2.0
	 */
	public void setInstantaneousDiffPropertyName(String instantaneousDiffPropertyName) {
		this.instantaneousDiffPropertyName = instantaneousDiffPropertyName;
	}

}
