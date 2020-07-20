/* ==================================================================
 * MBusPropertyConfig.java - 09/07/2020 10:43:58 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.mbus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via M-Bus.
 * 
 * <p>
 * The {@link #getConfig()} value represents the mbus address to read from.
 * </p>
 * 
 * @author alex
 * @version 1.0
 */
public class MBusPropertyConfig extends GeneralDatumSamplePropertyConfig<MBusDataDescription> {

	private MBusDataType dataType;
	private BigDecimal unitMultiplier;
	private int decimalScale;

	/**
	 * Default constructor.
	 */
	public MBusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, MBusDataDescription.NotSupported);
		dataType = MBusDataType.None;
		unitMultiplier = BigDecimal.ONE;
		decimalScale = 0;
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the mbus data type
	 * @param description
	 *        the mbus data description
	 */
	public MBusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			MBusDataType dataType, MBusDataDescription dataDescription) {
		this(name, datumPropertyType, dataType, dataDescription, BigDecimal.ONE, 0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the mbus data type
	 * @param description
	 *        the mbus data description
	 * @param unitMultiplier
	 *        the unit multiplier
	 * @param decimalScale
	 *        for numbers, the maximum decimal scale to support, or
	 *        {@literal -1} for no limit
	 */
	public MBusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			MBusDataType dataType, MBusDataDescription dataDescription, BigDecimal unitMultiplier,
			int decimalScale) {
		super(name, datumPropertyType, dataDescription);
		this.dataType = dataType;
		this.unitMultiplier = unitMultiplier;
		this.decimalScale = decimalScale;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		MBusPropertyConfig defaults = new MBusPropertyConfig();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "datumPropertyTypeKey", defaults.getDatumPropertyTypeValue());
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// drop-down menu for dataDescription
		BasicMultiValueSettingSpecifier dataDescriptionSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataDescriptionKey", defaults.getDataDescriptionKey());
		Map<String, String> dataDescriptionTitles = new LinkedHashMap<String, String>(256);
		for ( MBusDataDescription e : MBusDataDescription.values() ) {
			dataDescriptionTitles.put(e.toString(), e.toString());
		}
		dataDescriptionSpec.setValueTitles(dataDescriptionTitles);
		results.add(dataDescriptionSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", defaults.getDataTypeKey());
		Map<String, String> dataTypeTitles = new LinkedHashMap<String, String>(8);
		for ( MBusDataType e : MBusDataType.values() ) {
			dataTypeTitles.put(e.toString(), e.toString());
		}
		dataTypeSpec.setValueTitles(dataTypeTitles);
		results.add(dataTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale", "0"));
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
	 * Get the datum property type key.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyTypeKey()}.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeValue() {
		return getPropertyTypeKey();
	}

	/**
	 * Set the datum property type via a key value.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyTypeKey(String)}.
	 * </p>
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeValue(String key) {
		setPropertyTypeKey(key);
	}

	/**
	 * Get the data type.
	 * 
	 * @return the type
	 */
	public MBusDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(MBusDataType dataType) {
		if ( dataType == null ) {
			return;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the data type as a key value.
	 * 
	 * @return the ype as a key
	 */
	public String getDataTypeKey() {
		MBusDataType type = getDataType();
		return (type != null ? type.toString() : null);
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataTypeKey(String key) {
		setDataType(MBusDataType.valueOf(key));
	}

	/**
	 * Get the description to read data for.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning
	 * {@literal MBusDataDescription.NotSupported} if that returns
	 * {@literal null}.
	 * </p>
	 * 
	 * @return the data description
	 */
	public MBusDataDescription getDataDescription() {
		MBusDataDescription desc = getConfig();
		return (desc != null ? desc : MBusDataDescription.NotSupported);
	}

	/**
	 * Set the data description to read for.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(MBusDataDescription)}.
	 * </p>
	 * 
	 * @param desc
	 *        the data description to set
	 */
	public void setDataDescription(MBusDataDescription desc) {
		setConfig(desc);
	}

	/**
	 * Get the data description as a key value.
	 * 
	 * @return the description as a key
	 */
	public String getDataDescriptionKey() {
		MBusDataDescription desc = getDataDescription();
		return (desc != null ? desc.toString() : null);
	}

	/**
	 * Set the data description as a string value.
	 * 
	 * @param dataDescription
	 *        the description to set
	 */
	public void setDataDescriptionKey(String key) {
		setDataDescription(MBusDataDescription.valueOf(key));
	}

	/**
	 * Get the unit multiplier.
	 * 
	 * @return the multiplier
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
	 * @return the decimal scale
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
