/* ==================================================================
 * GpiodPropertyConfig.java - 24/09/2021 12:44:43 PM
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

package net.solarnetwork.node.control.gpiod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

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
public class GpiodPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/** The {@code gpioDirection} property default value. */
	public static final GpioDirection DEFAULT_GPIO_DIRECTION = GpioDirection.Input;

	/** The {@code gpioEdgeMode} property default value. */
	public static final GpioEdgeDetectionMode DEFAULT_GPIO_EDGE_MODE = GpioEdgeDetectionMode.RisingAndFalling;

	/** The default value for the {@code propertyType} property. */
	public static final DatumSamplesType DEFAULT_PROPERTY_TYPE = DatumSamplesType.Status;

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @param messageSource
	 *        message source to resolve messages with
	 * @param locale
	 *        the desired locale
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix, MessageSource messageSource,
			Locale locale) {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", String.valueOf(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		result.add(propTypeSpec);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "address", null));

		// drop-down menu for gpioDirection
		BasicMultiValueSettingSpecifier gpioDirSpec = new BasicMultiValueSettingSpecifier(
				prefix + "gpioDirectionCode", String.valueOf(DEFAULT_GPIO_DIRECTION.getCode()));
		Map<String, String> gpioDirTitles = new LinkedHashMap<>(2);
		for ( GpioDirection e : GpioDirection.values() ) {
			String title = e.toString();
			if ( messageSource != null ) {
				title = messageSource.getMessage("gpioDirection." + e.name(), null, title, locale);
			}
			gpioDirTitles.put(String.valueOf(e.getCode()), title);
		}
		gpioDirSpec.setValueTitles(gpioDirTitles);
		result.add(gpioDirSpec);

		// drop-down menu for gpioEdgeMode
		BasicMultiValueSettingSpecifier gpioEdgeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "gpioEdgeModeCode", String.valueOf(DEFAULT_GPIO_EDGE_MODE.getCode()));
		Map<String, String> gpioEdgeTitles = new LinkedHashMap<>(2);
		for ( GpioEdgeDetectionMode e : GpioEdgeDetectionMode.values() ) {
			String title = e.toString();
			if ( messageSource != null ) {
				title = messageSource.getMessage("gpioEdgeMode." + e.name(), null, title, locale);
			}
			gpioEdgeTitles.put(String.valueOf(e.getCode()), title);
		}
		gpioEdgeSpec.setValueTitles(gpioEdgeTitles);
		result.add(gpioEdgeSpec);

		return result;
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param controlId
	 *        the control ID
	 * @param address
	 *        the GPIO address
	 * @return the configuration, never {@literal null}
	 */
	public static GpiodPropertyConfig of(String controlId, Integer address) {
		GpiodPropertyConfig cfg = new GpiodPropertyConfig();
		cfg.setControlId(controlId);
		cfg.setAddress(address);
		return cfg;
	}

	private String controlId;
	private GpioDirection gpioDirection;
	private GpioEdgeDetectionMode gpioEdgeMode;

	/**
	 * Constructor.
	 */
	public GpiodPropertyConfig() {
		super();
		setGpioDirection(DEFAULT_GPIO_DIRECTION);
		setGpioEdgeMode(DEFAULT_GPIO_EDGE_MODE);
		setPropertyType(DEFAULT_PROPERTY_TYPE);
	}

	/**
	 * Save a sample value for this property configuration into a
	 * {@link DatumSamples} instance.
	 * 
	 * @param value
	 *        the value to save
	 * @param samples
	 *        the samples to save in
	 */
	public void setSampleValue(Object value, DatumSamples samples) {
		if ( samples == null ) {
			return;
		}
		final String prop = getPropertyKey();
		if ( prop == null ) {
			return;
		}
		final DatumSamplesType type = getPropertyType();
		if ( type == null ) {
			return;
		}
		if ( type == DatumSamplesType.Status ) {
			samples.putStatusSampleValue(prop, value);
		} else {
			Number n = null;
			if ( (value instanceof Number) ) {
				n = (Number) value;
			} else {
				if ( value instanceof Boolean ) {
					n = ((Boolean) value).booleanValue() ? 1 : 0;
				} else {
					n = StringUtils.numberValue(value.toString());
				}
			}
			samples.putSampleValue(type, prop, n);
		}
	}

	/**
	 * Test if this instance has a valid configuration.
	 * 
	 * <p>
	 * This method simply verifies the minimum level of configuration is
	 * available for the control to be used.
	 * </p>
	 * 
	 * @return {@literal true} if this configuration is valid for use
	 */
	public boolean isValid() {
		Integer addr = getAddress();
		return controlId != null && !controlId.trim().isEmpty() && addr != null && addr.intValue() >= 0;
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
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
	 * Get the GPIO direction.
	 * 
	 * @return the direction, never {@literal null}
	 */
	public GpioDirection getGpioDirection() {
		return gpioDirection;
	}

	/**
	 * Set the GPIO direction.
	 * 
	 * @param gpioDirection
	 *        the direction to set; if {@literal null} then
	 *        {@link #DEFAULT_GPIO_DIRECTION} will be set
	 */
	public void setGpioDirection(GpioDirection gpioDirection) {
		this.gpioDirection = (gpioDirection != null ? gpioDirection : DEFAULT_GPIO_DIRECTION);
	}

	/**
	 * Get the GPIO direction code value.
	 * 
	 * @return the GPIO direction code value
	 */
	public int getGpioDirectionCode() {
		return gpioDirection.getCode();
	}

	/**
	 * Set the GPIO direction as a code value.
	 * 
	 * @param code
	 *        the code value to set; if not a valid {@link GpioDirection} code
	 *        value the {@link #DEFAULT_GPIO_DIRECTION} will be set
	 */
	public void setGpioDirectionCode(int code) {
		setGpioDirection(CodedValue.forCodeValue(code, GpioDirection.class, null));
	}

	/**
	 * Get the GPIO edge detection mode.
	 * 
	 * @return the edge detection mode, never {@literal null}
	 */
	public GpioEdgeDetectionMode getGpioEdgeMode() {
		return gpioEdgeMode;
	}

	/**
	 * Set the GPIO edge detection mode.
	 * 
	 * @param gpioEdgeMode
	 *        the mode to set; if {@literal null} then
	 *        {@link #DEFAULT_GPIO_EDGE_MODE} will be set
	 */
	public void setGpioEdgeMode(GpioEdgeDetectionMode gpioEdgeMode) {
		this.gpioEdgeMode = (gpioEdgeMode != null ? gpioEdgeMode : DEFAULT_GPIO_EDGE_MODE);
	}

	/**
	 * Get the GPIO edge detection code value.
	 * 
	 * @return the GPIO edge detection code value
	 */
	public int getGpioEdgeModeCode() {
		return gpioEdgeMode.getCode();
	}

	/**
	 * Set the GPIO edge detection mode as a code value.
	 * 
	 * @param code
	 *        the code value to set; if not a valid
	 *        {@link GpioEdgeDetectionMode} code value the
	 *        {@link #DEFAULT_GPIO_EDGE_MODE} will be set
	 */
	public void setGpioEdgeModeCode(int code) {
		setGpioEdgeMode(CodedValue.forCodeValue(code, GpioEdgeDetectionMode.class, null));
	}

}
