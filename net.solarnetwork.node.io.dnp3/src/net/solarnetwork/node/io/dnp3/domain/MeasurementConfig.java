/* ==================================================================
 * MeasurementConfig.java - 21/02/2019 4:39:11 pm
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

package net.solarnetwork.node.io.dnp3.domain;

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
 * A configuration for a DNP3 measurement integration with a
 * {@link net.solarnetwork.node.service.DatumDataSource} property.
 *
 * <p>
 * This configuration maps a datum property to a DNP3 measurement.
 * </p>
 *
 * <p>
 * The {@code config} value represents the source ID.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
public class MeasurementConfig extends NumberDatumSamplePropertyConfig<String> {

	/** The default measurement type: {@code AnalogInput}. */
	public static final MeasurementType DEFAULT_TYPE = MeasurementType.AnalogInput;

	private MeasurementType type;
	private Integer index;

	/**
	 * Default constructor.
	 */
	public MeasurementConfig() {
		super();
		setType(DEFAULT_TYPE);
	}

	/**
	 * Constructor.
	 *
	 * @param sourceId
	 *        the source ID to collect from
	 * @param propertyName
	 *        the datum property name to collect
	 * @param type
	 *        the DNP3 measurement type
	 */
	public MeasurementConfig(String sourceId, String propertyName, MeasurementType type) {
		super();
		setSourceId(sourceId);
		setPropertyName(propertyName);
		setType(type);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 * @since 3.0
	 */
	public static List<SettingSpecifier> serverSettings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(6);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "sourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyName", ""));

		// drop-down menu for measurement type
		BasicMultiValueSettingSpecifier measTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "typeKey", Character.toString(DEFAULT_TYPE.getCode()));
		Map<String, String> measTypeTitles = new LinkedHashMap<>(3);
		for ( MeasurementType e : MeasurementType.values() ) {
			measTypeTitles.put(Character.toString(e.getCode()), e.getTitle());
		}
		measTypeSpec.setValueTitles(measTypeTitles);
		results.add(measTypeSpec);

		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", DEFAULT_SLOPE.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 * @since 3.0
	 */
	public static List<SettingSpecifier> clientSettings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(6);

		// drop-down menu for measurement type
		BasicMultiValueSettingSpecifier measTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "typeKey", Character.toString(DEFAULT_TYPE.getCode()));
		Map<String, String> measTypeTitles = new LinkedHashMap<>(3);
		for ( MeasurementType e : MeasurementType.values() ) {
			measTypeTitles.put(Character.toString(e.getCode()), e.getTitle());
		}
		measTypeSpec.setValueTitles(measTypeTitles);
		results.add(measTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "index", null));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", String.valueOf(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(4);
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyName", ""));

		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "unitMultiplier", DEFAULT_SLOPE.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		return results;
	}

	/**
	 * Test if the configuration is valid as a server configuration.
	 *
	 * @return {@code true} if the configuration is valid for a server
	 * @since 3.0
	 */
	public boolean isValidForServer() {
		final String sourceId = getSourceId();
		final String propertyKey = getPropertyKey();
		return (type != null && sourceId != null && !sourceId.isEmpty() && propertyKey != null
				&& !propertyKey.isEmpty());
	}

	/**
	 * Test if the configuration is valid as a client configuration.
	 *
	 * @return {@code true} if the configuration is valid for a client
	 * @since 3.0
	 */
	public boolean isValidForClient() {
		final String propertyKey = getPropertyKey();
		return (type != null && propertyKey != null && !propertyKey.isEmpty() && index != null
				&& index.intValue() >= 0);
	}

	/**
	 * Get the source ID.
	 *
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return getConfig();
	}

	/**
	 * Set the source ID.
	 *
	 * <p>
	 * This is an alias for {@link #setConfig(String)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		setConfig(sourceId);
	}

	/**
	 * Get the property name.
	 *
	 * <p>
	 * This is an alias for {@link #getPropertyKey()}.
	 * </p>
	 *
	 * @return the property name
	 */
	public String getPropertyName() {
		return getPropertyKey();
	}

	/**
	 * Set the property name.
	 *
	 * <p>
	 * This is an alias for {@link #setPropertyKey(String)}.
	 * </p>
	 *
	 * @param propertyName
	 *        the property name to set
	 */
	public void setPropertyName(String propertyName) {
		setPropertyKey(propertyName);
	}

	/**
	 * Get the measurement type.
	 *
	 * @return the type
	 */
	public MeasurementType getType() {
		return type;
	}

	/**
	 * Set the measurement type.
	 *
	 * @param type
	 *        the type to set
	 */
	public void setType(MeasurementType type) {
		this.type = type;
	}

	/**
	 * Get the measurement type key.
	 *
	 * <p>
	 * This returns the configured {@link #getType()}
	 * {@link MeasurementType#getCode()} value as a string. If the type is not
	 * available, {@link MeasurementType#AnalogInput} will be returned.
	 * </p>
	 *
	 * @return the measurement type key
	 */
	public String getTypeKey() {
		MeasurementType type = getType();
		if ( type == null ) {
			type = DEFAULT_TYPE;
		}
		return Character.toString(type.getCode());
	}

	/**
	 * Set the measurement type via a key value.
	 *
	 * <p>
	 * This uses the first character of {@code key} as a {@link MeasurementType}
	 * code value to call {@link #setType(MeasurementType)}. If there is any
	 * problem parsing the type, {@link MeasurementType#AnalogInput} is set.
	 * </p>
	 *
	 * @param key
	 *        the measurement type key to set
	 */
	public void setTypeKey(String key) {
		MeasurementType type = null;
		if ( key != null && key.length() > 0 ) {
			try {
				type = MeasurementType.forCode(key.charAt(0));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( type == null ) {
			type = DEFAULT_TYPE;
		}
		setType(type);
	}

	/**
	 * Get the unit multiplier.
	 *
	 * <p>
	 * This is an alias for {@link #getUnitSlope()}.
	 * </p>
	 *
	 * @return the multiplier; defaults to
	 *         {@link NumberDatumSamplePropertyConfig#DEFAULT_SLOPE}
	 */
	public BigDecimal getUnitMultiplier() {
		return getUnitSlope();
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
	 * <p>
	 * This is an alias for {@link #setUnitSlope(BigDecimal)}.
	 * </p>
	 *
	 * @param unitMultiplier
	 *        the mutliplier to set
	 */
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		setUnitSlope(unitMultiplier);
	}

	/**
	 * Get an explicit index.
	 *
	 * @return an explicit measurement index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * Set an explicit index.
	 *
	 * @param index
	 *        the index to set
	 */
	public void setIndex(Integer index) {
		this.index = index;
	}

}
