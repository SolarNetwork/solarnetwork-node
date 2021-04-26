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
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration bean for a mapping of a datum property to a message field
 * property.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumFieldConfig {

	private String datumProperty;
	private String fieldProperty;

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
		results.add(new BasicTextFieldSettingSpecifier(prefix + "fieldProperty", ""));

		return results;
	}

	/**
	 * Get the datum property name.
	 * 
	 * @return the datum property name
	 */
	public String getDatumProperty() {
		return datumProperty;
	}

	/**
	 * Set the datum property name.
	 * 
	 * @param datumProperty
	 *        the property name to set
	 */
	public void setDatumProperty(String datumProperty) {
		this.datumProperty = datumProperty;
	}

	/**
	 * Get the message field property name.
	 * 
	 * @return the field property name
	 */
	public String getFieldProperty() {
		return fieldProperty;
	}

	/**
	 * Set the message field property name.
	 * 
	 * @param fieldProperty
	 *        the field property name to set
	 */
	public void setFieldProperty(String fieldProperty) {
		this.fieldProperty = fieldProperty;
	}

}
