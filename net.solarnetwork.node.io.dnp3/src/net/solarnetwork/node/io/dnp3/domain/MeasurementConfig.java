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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A configuration for a DNP3 measurement integration with a
 * {@link net.solarnetwork.node.DatumDataSource} property.
 * 
 * <p>
 * This configuration maps a datum property to a DNP3 measurement.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementConfig {

	/** The default measurement type. */
	public static final MeasurementType DEFAULT_TYPE = MeasurementType.AnalogInput;

	private String dataSourceUid;
	private String sourceId;
	private String propertyName;
	private MeasurementType type = DEFAULT_TYPE;

	/**
	 * Default constructor.
	 */
	public MeasurementConfig() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param dataSourceUid
	 *        the {@link DatumDataSource#getUID()} to collect from
	 * @param sourceId
	 *        the source ID a
	 *        {@link net.solarnetwork.node.domain.Datum#getSourceId()} to
	 *        collect from
	 * @param propertyName
	 *        the datum property name to collect
	 * @param type
	 *        the DNP3 measurement type
	 */
	public MeasurementConfig(String dataSourceUid, String sourceId, String propertyName,
			MeasurementType type) {
		super();
		this.dataSourceUid = dataSourceUid;
		this.sourceId = sourceId;
		this.propertyName = propertyName;
		this.type = type;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(3);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "dataSourceUid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "sourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyName", ""));

		// drop-down menu for measurement type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "typeKey", Character.toString(DEFAULT_TYPE.getCode()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( MeasurementType e : MeasurementType.values() ) {
			propTypeTitles.put(Character.toString(e.getCode()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		return results;
	}

	public String getDataSourceUid() {
		return dataSourceUid;
	}

	public void setDataSourceUid(String dataSourceUid) {
		this.dataSourceUid = dataSourceUid;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public MeasurementType getType() {
		return type;
	}

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
}
