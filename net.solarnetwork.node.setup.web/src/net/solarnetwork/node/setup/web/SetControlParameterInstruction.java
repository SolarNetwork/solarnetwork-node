/* ==================================================================
 * SetControlParameterInstruction.java - Jul 10, 2013 5:25:41 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web;

/**
 * Bean for a SetControlParameter instruction.
 * 
 * @author matt
 * @version 1.0
 */
public class SetControlParameterInstruction {

	private String controlId;
	private String parameterValue;

	/**
	 * Default constructor.
	 */
	public SetControlParameterInstruction() {
		super();
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the parameter value.
	 * 
	 * @return the parameter value
	 */
	public String getParameterValue() {
		return parameterValue;
	}

	/**
	 * Set the parameter value.
	 * 
	 * @param parameterValue
	 *        the parameter value to set
	 */
	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}

}
