/* ==================================================================
 * EGaugePropertyConfig.java - 14/03/2018 10:08:29 AM
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

package net.solarnetwork.node.datum.egauge.ws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Stores the configuration for accessing an eGauge register.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class EGaugePropertyConfig {

	public enum EGaugeReadingType {
		Total,
		Instantaneous
	}

	public EGaugePropertyConfig() {
		super();
	}

	public EGaugePropertyConfig(String propertyName, String registerName,
			EGaugeReadingType readingType) {
		this();
		this.propertyName = propertyName;
		this.registerName = registerName;
		this.readingType = readingType;
	}

	private String propertyName;
	private String registerName;
	private EGaugeReadingType readingType;

	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyName", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "registerName", ""));

		// drop-down menu for readingType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "readingTypeValue", EGaugeReadingType.Instantaneous.name());
		Map<String, String> propTypeTitles = new LinkedHashMap<>();
		for ( EGaugeReadingType e : EGaugeReadingType.values() ) {
			propTypeTitles.put(e.name(), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		return results;
	}

	/**
	 * @return the property
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @param property
	 *        the property to set
	 */
	public void setPropertyName(String property) {
		this.propertyName = property;
	}

	/**
	 * @return the register
	 */
	public String getRegisterName() {
		return registerName;
	}

	/**
	 * @param register
	 *        the register to set
	 */
	public void setRegisterName(String register) {
		this.registerName = register;
	}

	/**
	 * @return the readingType
	 */
	public EGaugeReadingType getReadingType() {
		return readingType;
	}

	/**
	 * @param readingType
	 *        the readingType to set
	 */
	public void setReadingType(EGaugeReadingType readingType) {
		this.readingType = readingType;
	}

	/**
	 * @return the readingType
	 */
	public String getReadingTypeValue() {
		return readingType.name();
	}

	/**
	 * @param readingType
	 *        the readingType to set
	 */
	public void setReadingTypeValue(String readingType) {
		this.readingType = EGaugeReadingType.valueOf(readingType);
	}

	@Override
	public String toString() {
		return "EGaugePropertyConfig [propertyName=" + propertyName + ", registerName=" + registerName
				+ ", readingType=" + readingType + "]";
	}

}
