/* ==================================================================
 * BacnetPropertyConfig.java - 4/11/2022 2:00:32 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.bacnet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyCovRef;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyRef;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via a BACnet property.
 * 
 * <p>
 * The {@link #getConfig()} value represents the BACnet property identifier to
 * read.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	private Integer objectType;
	private Integer objectNumber;
	private Float covIncrement;

	/**
	 * Default constructor.
	 */
	public BacnetPropertyConfig() {
		super();
		setPropertyType(null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param propertyKey
	 *        the property key
	 * @param propertyType
	 *        the property type
	 * @param propertyId
	 *        the BACnet property ID
	 */
	public BacnetPropertyConfig(String propertyKey, DatumSamplesType propertyType, Integer propertyId) {
		super(propertyKey, propertyType, propertyId);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectType", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "objectNumber", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyId", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "covIncrement", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", "");
		Map<String, String> propTypeTitles = new LinkedHashMap<>(4);
		propTypeTitles.put("", "");
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Test if this instance represents a valid configuration.
	 * 
	 * <p>
	 * This only verifies that the configuration is complete, not that actual
	 * BACnet device properties exist for the configured values.
	 * </p>
	 * 
	 * @return {@literal true} if this instance represents a valid configuration
	 */
	public boolean isValid() {
		if ( objectType == null || objectType.intValue() < 0 || objectNumber == null
				|| objectNumber.intValue() < 0 ) {
			return false;
		}
		final Integer propId = getPropertyId();
		final String datumPropName = getPropertyKey();
		if ( (propId != null && propId.intValue() < 0) || datumPropName == null
				|| datumPropName.trim().isEmpty() ) {
			return false;
		}
		return true;
	}

	/**
	 * Get a reference for a given device ID and the configuration of this
	 * instance.
	 * 
	 * @param deviceId
	 *        the device ID
	 * @return the reference, or {@literal null} if any of the required
	 *         configuration is missing
	 */
	public BacnetDeviceObjectPropertyRef toRef(Integer deviceId) {
		if ( deviceId == null || objectType == null || objectNumber == null ) {
			return null;
		}
		final Integer propId = getPropertyId();
		final int pid = (propId != null ? propId.intValue() : BacnetPropertyType.PresentValue.getCode());
		final Float covInc = getCovIncrement();
		if ( covInc != null && !covInc.isInfinite() && covInc.floatValue() > 0.0f ) {
			return new SimpleBacnetDeviceObjectPropertyCovRef(deviceId, objectType, objectNumber, pid,
					covInc);
		}
		return new SimpleBacnetDeviceObjectPropertyRef(deviceId, objectType, objectNumber, pid);
	}

	/**
	 * Get the object type.
	 * 
	 * @return the object type
	 */
	public Integer getObjectType() {
		return objectType;
	}

	/**
	 * Set the object type.
	 * 
	 * @param objectType
	 *        the object type to set
	 */
	public void setObjectType(Integer objectType) {
		this.objectType = objectType;
	}

	/**
	 * Get the object (instance) number.
	 * 
	 * @return the object number
	 */
	public Integer getObjectNumber() {
		return objectNumber;
	}

	/**
	 * Set the object (instance) number.
	 * 
	 * @param objectNumber
	 *        the object number to set
	 */
	public void setObjectNumber(Integer objectNumber) {
		this.objectNumber = objectNumber;
	}

	/**
	 * Get the BACnet property ID.
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 * 
	 * @return the property ID
	 */
	public Integer getPropertyId() {
		return getConfig();
	}

	/**
	 * Set the BACnet property ID.
	 * <p>
	 * This is an alias for {@link #setConfig(Integer)}.
	 * </p>
	 * 
	 * @param propertyId
	 *        the property ID to set
	 */
	public void setPropertyId(Integer propertyId) {
		setConfig(propertyId);
	}

	/**
	 * Get the change-of-value increment (notification threshold).
	 * 
	 * @return the COV increment
	 */
	public Float getCovIncrement() {
		return covIncrement;
	}

	/**
	 * Set the change-of-value increment (notification threshold).
	 * 
	 * @param covIncrement
	 *        the COV increment to set
	 */
	public void setCovIncrement(Float covIncrement) {
		this.covIncrement = covIncrement;
	}

}
