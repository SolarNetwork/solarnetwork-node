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

import java.util.ArrayList;
import java.util.List;

/**
 * Bean for a SetControlParameter instruction.
 * 
 * @author matt
 * @version 1.0
 */
public class SetControlParameterInstruction {

	private String controlId;
	private List<InstructionParameter> params;

	private InstructionParameter getOrCreateInstructionParam(int index) {
		if ( params == null ) {
			params = new ArrayList<InstructionParameter>(3);
		}
		while ( params.size() <= index ) {
			params.add(new InstructionParameter());
		}
		return params.get(index);
	}

	public void setParameterName(String name) {
		InstructionParameter p = getOrCreateInstructionParam(0);
		p.setName(name);
	}

	public String getParameterName() {
		return (params != null && params.size() > 0 ? params.get(0).getName() : null);
	}

	public void setParameterValue(String value) {
		InstructionParameter p = getOrCreateInstructionParam(0);
		p.setValue(value);
	}

	public String getParameterValue() {
		return (params != null && params.size() > 0 ? params.get(0).getValue() : null);
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public List<InstructionParameter> getParams() {
		return params;
	}

	public void setParams(List<InstructionParameter> params) {
		this.params = params;
	}

}
