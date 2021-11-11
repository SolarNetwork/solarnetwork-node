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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via an ADAM channel.
 * 
 * <p>
 * {@link #getConfig()} represents the channel number.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class ChannelPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/** The default value for the {@code channel} property. */
	public static final int DEFAULT_CHANNEL = 0;

	private String name;

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
	public ChannelPropertyConfig(String propertyKey, DatumSamplesType propertyType,
			Integer channelNumber) {
		super(propertyKey, propertyType, channelNumber);
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
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
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

		results.add(new BasicTextFieldSettingSpecifier(prefix + "offset", DEFAULT_INTERCEPT.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", DEFAULT_SLOPE.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
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
	 * <p>
	 * This is an alias for {@link #getIntercept()}, maintained for backwards
	 * compatibility.
	 * </p>
	 * 
	 * @return the offset; defaults to
	 *         {@link NumberDatumSamplePropertyConfig#DEFAULT_INTERCEPT}
	 * @since 1.1
	 * @deprecated since 2.0 use {@link #getIntercept()}
	 */
	@Deprecated
	public BigDecimal getOffset() {
		return getIntercept();
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
	 * <p>
	 * This is an alias for {@link #setIntercept(BigDecimal)}, maintained for
	 * backwards compatibility.
	 * </p>
	 * 
	 * @param offset
	 *        the offset to set
	 * @since 1.1
	 * @deprecated since 2.0 use {@link #setIntercept(BigDecimal)}
	 */
	@Deprecated
	public void setOffset(BigDecimal offset) {
		setIntercept(offset);
	}

	/**
	 * Get the unit multiplier.
	 * 
	 * <p>
	 * This is an alias for {@link #getSlope()}, maintained for backwards
	 * compatibility.
	 * </p>
	 * 
	 * @return the multiplier; defaults to
	 *         {@link NumberDatumSamplePropertyConfig#DEFAULT_SLOPE}
	 * @deprecated since 2.0 use {@link #getSlope()}
	 */
	@Deprecated
	public BigDecimal getUnitMultiplier() {
		return getSlope();
	}

	/**
	 * Set the unit multiplier.
	 * 
	 * <p>
	 * This is an alias for {@link #setSlope(BigDecimal)}, maintained for
	 * backwards compatibility.
	 * </p>
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
	 * @deprecated since 2.0 use {@link #setSlope(BigDecimal)}
	 */
	@Deprecated
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		setSlope(unitMultiplier);
	}

}
