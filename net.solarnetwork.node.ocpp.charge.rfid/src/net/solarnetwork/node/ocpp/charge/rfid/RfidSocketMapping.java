/* ==================================================================
 * RfidSocketMapping.java - 30/09/2016 3:02:45 PM
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

package net.solarnetwork.node.ocpp.charge.rfid;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A mapping of a RFID UID value with an associated socket ID.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidSocketMapping {

	private String rfidUid;
	private String socketId;

	public RfidSocketMapping() {
		super();
	}

	public RfidSocketMapping(String rfidUid, String socketId) {
		super();
		this.rfidUid = rfidUid;
		this.socketId = socketId;
	}

	public String getRfidUid() {
		return rfidUid;
	}

	public void setRfidUid(String rfidUid) {
		this.rfidUid = rfidUid;
	}

	public String getSocketId() {
		return socketId;
	}

	public void setSocketId(String socketId) {
		this.socketId = socketId;
	}

	/**
	 * Get settings specifies for configuring the properties of a
	 * {@link RfidSocketMapping}.
	 * 
	 * @param prefix
	 *        The prefix to assign for property keys.
	 * @return The specifies.
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "rfidUid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "socketId", ""));
		return results;
	}
}
