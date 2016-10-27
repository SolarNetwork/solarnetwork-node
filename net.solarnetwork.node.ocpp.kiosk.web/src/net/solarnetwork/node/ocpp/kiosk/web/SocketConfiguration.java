/* ==================================================================
 * SocketConfiguration.java - 23/10/2016 7:24:34 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.kiosk.web;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A configuration of socket with energy data source.
 * 
 * @author matt
 * @version 1.0
 */
public class SocketConfiguration {

	private String meterDataSourceUID;
	private String socketId;
	private String key;

	/**
	 * Get setting specifiers for a {@link SocketConfiguration}.
	 * 
	 * @param prefix
	 *        A prefix to use for the setting keys.
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(3);
		result.add(new BasicTextFieldSettingSpecifier(prefix + "socketId", ""));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "meterDataSourceUID", ""));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "key", ""));
		return result;
	}

	public String getMeterDataSourceUID() {
		return meterDataSourceUID;
	}

	public void setMeterDataSourceUID(String meterDataSourceUID) {
		this.meterDataSourceUID = meterDataSourceUID;
	}

	public String getSocketId() {
		return socketId;
	}

	public void setSocketId(String socketID) {
		this.socketId = socketID;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
