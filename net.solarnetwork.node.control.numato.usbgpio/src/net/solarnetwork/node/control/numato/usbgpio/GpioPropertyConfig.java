/* ==================================================================
 * GpioPropertyConfig.java - 24/09/2021 12:44:43 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single GPIO property to be control.
 * 
 * <p>
 * The {@link #getConfig()} value represents the GPIO address to read from,
 * starting from {@literal 0}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class GpioPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/** The {@code gpioType} property default value. */
	public static final GpioType DEFAULT_GPIO_TYPE = GpioType.Digital;

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "address", null));

		// drop-down menu for gpioType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "gpioTypeCode", String.valueOf(DEFAULT_GPIO_TYPE.getCode()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(2);
		for ( GpioType e : GpioType.values() ) {
			propTypeTitles.put(String.valueOf(e.getCode()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "pollFrequencyMs", null));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));
		return results;
	}

	private GpioType gpioType;
	private Duration pollFrequency;

	/**
	 * Constructor.
	 */
	public GpioPropertyConfig() {
		super();
		setGpioType(DEFAULT_GPIO_TYPE);
	}

	/**
	 * Get the GPIO address to control.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 * 
	 * @return the GPIO address
	 */
	public Integer getAddress() {
		return getConfig();
	}

	/**
	 * Set the GPIO address to control.
	 * 
	 * <p>
	 * This is an alias for {@link DatumSamplePropertyConfig#setConfig(Object)}.
	 * </p>
	 * 
	 * @param address
	 *        the GPIO address to set
	 */
	public void setAddress(Integer address) {
		setConfig(address);
	}

	/**
	 * Get the GPIO type.
	 * 
	 * @return the type, never {@literal null}
	 */
	public GpioType getGpioType() {
		return gpioType;
	}

	/**
	 * Set the GPIO type code value.
	 * 
	 * @param gpioType
	 *        the type; if {@literal null} then {@link #DEFAULT_GPIO_TYPE} will
	 *        be set
	 */
	public void setGpioType(GpioType gpioType) {
		if ( gpioType == null ) {
			gpioType = DEFAULT_GPIO_TYPE;
		}
		this.gpioType = gpioType;
	}

	/**
	 * Get the GPIO type code value.
	 * 
	 * @return the
	 */
	public int getGpioTypeCode() {
		return gpioType.getCode();
	}

	/**
	 * Set the GPIO type as a code value.
	 * 
	 * @param code
	 *        the code value to set; if not a valid {@link GpioType} code value
	 *        the {@link #DEFAULT_GPIO_TYPE} will be set
	 */
	public void setGpioTypeCode(int code) {
		setGpioType(CodedValue.forCodeValue(code, GpioType.class, null));
	}

	/**
	 * Get the frequency at which the GPIO value should be polled.
	 * 
	 * @return the frequency, or {@literal null} to not poll
	 */
	public Duration getPollFrequency() {
		return pollFrequency;
	}

	/**
	 * Set the frequency at which the GPIO value should be polled.
	 * 
	 * @param pollFrequency
	 *        the frequency to set, or {@literal null} to not poll
	 */
	public void setPollFrequency(Duration pollFrequency) {
		this.pollFrequency = pollFrequency;
	}

	/**
	 * Get the frequency at which the GPIO value should be polled, in
	 * milliseconds.
	 * 
	 * @return the frequency in milliseconds, or {@literal 0} to not poll
	 */
	public long getPollFrequencyMs() {
		Duration d = getPollFrequency();
		return (d != null ? d.toMillis() : 0L);
	}

	/**
	 * Set the frequency at which the GPIO value should be polled, in
	 * milliseconds.
	 * 
	 * @param ms
	 *        the frequency in milliseconds, or {@literal 0} to not poll
	 */
	public void setPollFrequencyMs(long ms) {
		setPollFrequency(ms > 0 ? Duration.ofMillis(ms) : null);

	}

}
