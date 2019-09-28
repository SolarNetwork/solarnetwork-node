/* ==================================================================
 * CanbusPropertyConfig.java - 25/09/2019 4:12:13 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.canbus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via a CAN bus message.
 * 
 * <p>
 * The {@link #getConfig()} value represents the CAN message bit offset to read
 * from.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/** The default {@code bitLength} property value. */
	public static final int DEFAULT_BIT_LENGTH = 32;

	/** The default {@code bitOffset} property value. */
	public static final int DEFAULT_BIT_OFFSET = 0;

	/** The default {@code dataType} property value. */
	public static final BitDataType DEFAULT_DATA_TYPE = BitDataType.Int32;

	private BitDataType dataType = DEFAULT_DATA_TYPE;
	private String unit;
	private int bitOffset = DEFAULT_BIT_OFFSET;
	private int bitLength = DEFAULT_BIT_LENGTH;

	/**
	 * Default constructor.
	 */
	public CanbusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, DEFAULT_BIT_OFFSET);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param bitOffset
	 *        the CAN message bit offset
	 */
	public CanbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType, int bitOffset) {
		super(name, datumPropertyType, bitOffset);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", ""));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", String.valueOf(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", DEFAULT_DATA_TYPE.getKey());
		Map<String, String> dataTypeTitles = new LinkedHashMap<String, String>(3);
		for ( BitDataType e : BitDataType.values() ) {
			dataTypeTitles.put(e.getKey(), e.getDescription());
		}
		dataTypeSpec.setValueTitles(dataTypeTitles);
		results.add(dataTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "unit", ""));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitOffset",
				String.valueOf(DEFAULT_BIT_OFFSET)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitLength",
				String.valueOf(DEFAULT_BIT_LENGTH)));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Get the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning {@literal 0} if that
	 * returns {@literal null}.
	 * </p>
	 * 
	 * @return the register address
	 */
	public int getAddress() {
		Integer addr = getConfig();
		return (addr != null ? addr : 0);
	}

	/**
	 * Set the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(Integer)}.
	 * </p>
	 * 
	 * @param address
	 *        the register address to set
	 */
	public void setAddress(int address) {
		setConfig(address);
	}

	/**
	 * Get the data type.
	 * 
	 * @return the type, never {@literal null}; defaults to
	 *         {@link #DEFAULT_DATA_TYPE}
	 */
	public BitDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(BitDataType dataType) {
		if ( dataType == null ) {
			dataType = DEFAULT_DATA_TYPE;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the data type as a key value.
	 * 
	 * @return the type as a key
	 */
	public String getDataTypeKey() {
		BitDataType type = getDataType();
		return (type != null ? type.getKey() : null);
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataTypeKey(String key) {
		try {
			setDataType(BitDataType.forKey(key));
		} catch ( IllegalArgumentException e ) {
			setDataType(DEFAULT_DATA_TYPE);
		}
	}

	/**
	 * Get the unit of the property value.
	 * 
	 * <p>
	 * This represents the physical unit of the value, as unit term as described
	 * in <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
	 * Units of Measure</a>.
	 * </p>
	 * 
	 * @return the property unit, or {@literal null} if not known
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Set the unit of the property value.
	 * 
	 * @param unit
	 *        the unit to use
	 * @see <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
	 *      Units of Measure</a>
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Get the offset of the least significant bit of the property value,
	 * relative to the least significant bit of the full message data value.
	 * 
	 * @return the bit offset; defaults to {@link #DEFAULT_BIT_OFFSET}
	 */
	public int getBitOffset() {
		return bitOffset;
	}

	/**
	 * Set the offset of the least significant bit of the property value,
	 * relative to the least significant bit of the full message data value.
	 * 
	 * @param bitOffset
	 *        the bit offset to use
	 */
	public void setBitOffset(int bitOffset) {
		this.bitOffset = bitOffset;
	}

	/**
	 * Get the length of the property value within the full message data value.
	 * 
	 * @return the bit length; defaults to {@link #DEFAULT_BIT_LENGTH}
	 */
	public int getBitLength() {
		return bitLength;
	}

	/**
	 * Set the length of the property value within the full message data value.
	 * 
	 * @param bitLength
	 *        the bit length to use
	 */
	public void setBitLength(int bitLength) {
		this.bitLength = bitLength;
	}

}
