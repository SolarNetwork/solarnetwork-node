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
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
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

	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "registerName", ""));

		return results;
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
		return "EGaugePropertyConfig [registerName=" + registerName + "]";
	}

}
