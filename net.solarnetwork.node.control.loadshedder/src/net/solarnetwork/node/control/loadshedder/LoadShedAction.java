/* ==================================================================
 * LoadShedAction.java - 29/06/2015 7:24:57 am
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

/**
 * A load shed outcome to execute after evaluating load conditions.
 * 
 * @author matt
 * @version 1.0
 */
public class LoadShedAction {

	private String controlId;
	private Integer shedWatts;

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public Integer getShedWatts() {
		return shedWatts;
	}

	public void setShedWatts(Integer shedWatts) {
		this.shedWatts = shedWatts;
	}

}
