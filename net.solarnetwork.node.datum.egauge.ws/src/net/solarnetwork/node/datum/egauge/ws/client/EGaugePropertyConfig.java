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

package net.solarnetwork.node.datum.egauge.ws.client;

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

	public EGaugePropertyConfig() {
		super();
	}

	public EGaugePropertyConfig(String registerName) {
		this();
		this.registerName = registerName;
	}

	private String registerName;

	public static List<SettingSpecifier> settings(String prefix, List<String> registerNames) {
		List<SettingSpecifier> results = new ArrayList<>();

		if ( registerNames == null || registerNames.isEmpty() ) {
			results.add(new BasicTextFieldSettingSpecifier(prefix + "registerName", ""));
		} else {
			BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
					prefix + "registerName", registerNames.get(0));
			Map<String, String> propTypeTitles = new LinkedHashMap<>();
			for ( String registerName : registerNames ) {
				propTypeTitles.put(registerName, registerName);
			}
			propTypeSpec.setValueTitles(propTypeTitles);
			results.add(propTypeSpec);
		}

		return results;
	}

	/**
	 * Test if this configuration appears to be valid.
	 * 
	 * <p>
	 * This only verifies that expected properties have non-empty values.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration appears valid,
	 *         {@literal false} otherwise
	 */
	public boolean isValid() {
		if ( registerName == null || registerName.trim().isEmpty() ) {
			return false;
		}
		return true;
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

	@Override
	public String toString() {
		return "EGaugePropertyConfig{registerName=" + registerName + "}";
	}

}
