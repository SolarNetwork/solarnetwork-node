/* ==================================================================
 * LoadShedControlInfo.java - 27/06/2015 5:03:52 pm
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
 * Status information related to a specific load shed control.
 * 
 * @author matt
 * @version 1.0
 */
public class LoadShedControlInfo {

	private String controlId;
	private Integer wattsBeforeAction;
	private Date actionDate;
	private LoadShedAction action;

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public Date getActionDate() {
		return actionDate;
	}

	public void setActionDate(Date switchedDate) {
		this.actionDate = switchedDate;
	}

	public Integer getWattsBeforeAction() {
		return wattsBeforeAction;
	}

	public void setWattsBeforeAction(Integer wattsBeforeSwitch) {
		this.wattsBeforeAction = wattsBeforeSwitch;
	}

	public LoadShedAction getAction() {
		return action;
	}

	public void setAction(LoadShedAction action) {
		this.action = action;
	}

}
