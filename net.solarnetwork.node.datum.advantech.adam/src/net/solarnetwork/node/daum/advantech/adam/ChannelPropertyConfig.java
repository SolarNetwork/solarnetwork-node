/* ==================================================================
 * ChannelPropertyConfig.java - 22/11/2018 12:55:27 PM
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

package net.solarnetwork.node.daum.advantech.adam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.NumberUtils;

/**
 * Configuration for a single datum property to be set via an ADAM channel.
 * 
 * <p>
 * {@link #getConfig()} represents the channel number.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class ChannelPropertyConfig extends GeneralDatumSamplePropertyConfig<Integer> {

	/** The default value for the {@code channel} property. */
	public static final int DEFAULT_CHANNEL = 0;

	/** The default value for the {@code propertyType} property. */
	public static final GeneralDatumSamplesType DEFAULT_PROPERTY_TYPE = GeneralDatumSamplesType.Instantaneous;

	/** The default value for the {@code offset} property. */
	public static final BigDecimal DEFAULT_OFFSET = BigDecimal.ZERO;

	/** The default value for the {@code unitMultiplier} property. */
	public static final BigDecimal DEFAULT_UNIT_MULTIPLIER = BigDecimal.ONE;

	/** The default value for the {@code decimalScale} property. */
	public static final int DEFAULT_DECIMAL_SCALE = 5;

	private String name;
	private BigDecimal offset;
	private BigDecimal unitMultiplier;
	private int decimalScale;

	/**
	 * Default constructor.
	 */
	public ChannelPropertyConfig() {
		this(null, DEFAULT_PROPERTY_TYPE, DEFAULT_CHANNEL);
	}

	/**
	 * Construct with minimal values.
	 * 
	 * @param propertyKey
	 *        the datum property name to assign
	 * @param propertyType
	 *        the datum property type
	 * @param channelNumber
	 *        the channel number
	 */
	public ChannelPropertyConfig(String propertyKey, GeneralDatumSamplesType propertyType,
			Integer channelNumber) {
		super(propertyKey, propertyType, channelNumber);
		offset = DEFAULT_OFFSET;
		unitMultiplier = DEFAULT_UNIT_MULTIPLIER;
		decimalScale = DEFAULT_DECIMAL_SCALE;
	}

	/**
	 * Get a list of setting specifiers suitable for configuring instances of
	 * this class.
	 * 
	 * @param prefix
	 *        a prefix to use for all setting keys
	 * @param channelCount
	 *        the number of channels to render
	 * @return the list of settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix, int channelCount) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", Character.toString(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// drop-down menu for channel
		BasicMultiValueSettingSpecifier channelSpec = new BasicMultiValueSettingSpecifier(
				prefix + "channel", String.valueOf(DEFAULT_CHANNEL));
		Map<String, String> channelTitles = new LinkedHashMap<String, String>(8);
		for ( int i = 0; i < channelCount; i++ ) {
			channelTitles.put(String.valueOf(i), String.valueOf(i));
		}
		channelSpec.setValueTitles(channelTitles);
		results.add(channelSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "offset", DEFAULT_OFFSET.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier",
				DEFAULT_UNIT_MULTIPLIER.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Apply the decimal scale to a number value.
	 * 
	 * @param value
	 *        the number to apply the settings to
	 * @return the value
	 * @throws NullPointerException
	 *         if {@code value} is {@literal null}
	 */
	public Number applyDecimalScale(Number value) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	/**
	 * Apply the offset to a number value.
	 * 
	 * @param value
	 *        the number to apply the offset to
	 * @return the value
	 * @throws NullPointerException
	 *         if {@code value} is {@literal null}
	 * @since 1.1
	 */
	public Number applyOffset(Number value) {
		if ( BigDecimal.ZERO.compareTo(offset) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.add(offset);
	}

	/**
	 * Apply the unit multiplier to a number value.
	 * 
	 * @param value
	 *        the number to apply the settings to
	 * @return the value
	 * @throws NullPointerException
	 *         if {@code value} is {@literal null}
	 */
	public Number applyUnitMultiplier(Number value) {
		if ( BigDecimal.ONE.compareTo(unitMultiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(unitMultiplier);
	}

	/**
	 * Apply the configured unit multiplier and decimal scale to a number value.
	 * 
	 * @param value
	 *        the number to apply the settings to
	 * @return the result, or {@literal null} if {@code value} is
	 *         {@literal null}
	 */
	public Number applyScaleAndMultipler(Number value) {
		if ( value == null ) {
			return null;
		}
		return applyDecimalScale(applyUnitMultiplier(value));
	}

	/**
	 * Apply the configured offset, unit multiplier, and decimal scale to a
	 * number value.
	 * 
	 * @param value
	 *        the number to apply the settings to
	 * @return the result, or {@literal null} if {@code value} is
	 *         {@literal null}
	 * @since 1.1
	 */
	public Number applyTransformations(Number value) {
		if ( value == null ) {
			return null;
		}
		return applyDecimalScale(applyUnitMultiplier(applyOffset(value)));
	}

	/**
	 * Get a friendly name for the channel.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the friendly name for the channel.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the channel to read data from.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning {@literal 0} if that
	 * returns {@literal null}.
	 * </p>
	 * 
	 * @return the channel
	 */
	public int getChannel() {
		Integer addr = getConfig();
		return (addr != null ? addr : 0);
	}

	/**
	 * Set the channel to read data from.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(Object)}.
	 * </p>
	 * 
	 * @param channelNumber
	 *        the channel to set
	 */
	public void setChannel(int channelNumber) {
		setConfig(channelNumber);
	}

	/**
	 * Get the offset.
	 * 
	 * @return the offset; defaults to {@link #DEFAULT_OFFSET}
	 * @since 1.1
	 */
	public BigDecimal getOffset() {
		return offset;
	}

	/**
	 * Set the offset.
	 * 
	 * <p>
	 * This value represents an offset value to add to values collected for this
	 * property so that a standardized range is captured. For example a channel
	 * might have a +/- 15 range, but the desired output is 0-30. Configuring
	 * {@literal 15} as the offset would shift the values into that range.
	 * </p>
	 * 
	 * @param offset
	 *        the offset to set
	 * @since 1.1
	 */
	public void setOffset(BigDecimal offset) {
		this.offset = offset;
	}

	/**
	 * Get the unit multiplier.
	 * 
	 * @return the multiplier; defaults to {@link #DEFAULT_UNIT_MULTIPLIER}
	 */
	public BigDecimal getUnitMultiplier() {
		return unitMultiplier;
	}

	/**
	 * Set the unit multiplier.
	 * 
	 * <p>
	 * This value represents a multiplication factor to apply to values
	 * collected for this property so that a standardized unit is captured. For
	 * example, a power meter might report power as <i>killowatts</i>, in which
	 * case {@code multiplier} can be configured as {@literal .001} to convert
	 * the value to <i>watts</i>.
	 * </p>
	 * 
	 * @param unitMultiplier
	 *        the mutliplier to set
	 */
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		this.unitMultiplier = unitMultiplier;
	}

	/**
	 * Get the decimal scale to round decimal numbers to.
	 * 
	 * @return the decimal scale; defaults to {@link #DEFAULT_DECIMAL_SCALE}
	 */
	public int getDecimalScale() {
		return decimalScale;
	}

	/**
	 * Set the decimal scale to round decimal numbers to.
	 * 
	 * <p>
	 * This is a <i>maximum</i> scale value that decimal values should be
	 * rounded to. This is applied <i>after</i> any {@code unitMultiplier} is
	 * applied. A scale of {@literal 0} would round all decimals to integer
	 * values.
	 * </p>
	 * 
	 * @param decimalScale
	 *        the scale to set, or {@literal -1} to disable rounding completely
	 */
	public void setDecimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
	}
}
