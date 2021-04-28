/* ==================================================================
 * DatumFieldConfig.java - 26/04/2021 8:52:32 PM
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

package net.solarnetwork.node.io.protobuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration bean for a mapping of a datum property to a message field
 * property.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumFieldConfig extends GeneralDatumSamplePropertyConfig<String> {

	/** The default {@code propertyType} property value. */
	public static final GeneralDatumSamplesType DEFAULT_PROPERTY_TYPE = GeneralDatumSamplesType.Instantaneous;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * The {@code propertyType} is set to
	 * {@link GeneralDatumSamplesType#Instantaneous}.
	 * </p>
	 */
	public DatumFieldConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param datumProperty
	 *        the datum property
	 * @param propertyType
	 *        the property type
	 * @param fieldProperty
	 *        the message field property
	 */
	public DatumFieldConfig(String datumProperty, GeneralDatumSamplesType propertyType,
			String fieldProperty) {
		super(datumProperty, propertyType, fieldProperty);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "datumProperty", ""));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", Character.toString(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "fieldProperty", ""));

		return results;
	}

	/**
	 * Get the datum property name.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyKey()}.
	 * </p>
	 * 
	 * @return the datum property name
	 */
	public String getDatumProperty() {
		return getPropertyKey();
	}

	/**
	 * Set the datum property name.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyKey(String)}.
	 * </p>
	 * 
	 * @param datumProperty
	 *        the property name to set
	 */
	public void setDatumProperty(String datumProperty) {
		setPropertyKey(datumProperty);
	}

	/**
	 * Get the message field property name.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * </p>
	 * 
	 * @return the field property name
	 */
	public String getFieldProperty() {
		return getConfig();
	}

	/**
	 * Set the message field property name.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(String)}.
	 * </p>
	 * 
	 * @param fieldProperty
	 *        the field property name to set
	 */
	public void setFieldProperty(String fieldProperty) {
		setConfig(fieldProperty);
	}

}
