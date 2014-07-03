/* ==================================================================
 * SMAPowerDatum.java - Jul 3, 2014 7:09:14 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.power.sma.yasdi4j;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.power.PowerDatum;

/**
 * Extension of {@link PowerDatum} with SMA-specific details.
 * 
 * @author matt
 * @version 1.0
 */
public class SMAPowerDatum extends PowerDatum {

	private Map<String, Object> channelData;

	private String getStringChannelData(String key) {
		if ( channelData == null ) {
			return null;
		}
		Object value = channelData.get(key);
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String ) {
			return (String) value;
		}
		return value.toString();
	}

	private void setChannelDataValue(String key, Object value) {
		if ( channelData == null ) {
			if ( value == null ) {
				return;
			}
			channelData = new LinkedHashMap<String, Object>(4);
		}
		if ( value == null ) {
			channelData.remove(key);
		} else {
			channelData.put(key, value);
		}
	}

	public String getStatusMessage() {
		return getStringChannelData("Status");
	}

	public void setStatusMessage(String statusMessage) {
		setChannelDataValue("Status", statusMessage);
	}

	public String getFaultMessage() {
		return getStringChannelData("Error");
	}

	public void setFaultMessage(String faultMessage) {
		setChannelDataValue("Error", faultMessage);
	}

}
