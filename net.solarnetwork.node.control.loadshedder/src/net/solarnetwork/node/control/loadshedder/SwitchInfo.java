/* ==================================================================
 * SwitchInfo.java - 27/06/2015 5:03:52 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.loadshedder;

import java.util.Date;

/**
 * Status information related to a specific switch.
 * 
 * @author matt
 * @version 1.0
 */
public class SwitchInfo {

	private String controlId;
	private Integer wattsBeforeSwitch;
	private Date switchedDate;

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public Date getSwitchedDate() {
		return switchedDate;
	}

	public void setSwitchedDate(Date switchedDate) {
		this.switchedDate = switchedDate;
	}

	public Integer getWattsBeforeSwitch() {
		return wattsBeforeSwitch;
	}

	public void setWattsBeforeSwitch(Integer wattsBeforeSwitch) {
		this.wattsBeforeSwitch = wattsBeforeSwitch;
	}

}
