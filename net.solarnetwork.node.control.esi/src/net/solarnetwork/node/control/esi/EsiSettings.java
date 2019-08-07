/* ==================================================================
 * EsiSettings.java - 7/08/2019 2:45:59 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi;

import java.util.Set;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.node.control.esi.domain.ResourceCharacteristics;

/**
 * General settings for ESI integration.
 * 
 * @author matt
 * @version 1.0
 */
public class EsiSettings {

	private Set<DerProgramType> programTypes;

	private ResourceCharacteristics resourceCharacteristics;

	/**
	 * Get the set of DER program types to participate in.
	 * 
	 * @return the DER program types
	 */
	public Set<DerProgramType> getProgramTypes() {
		return programTypes;
	}

	/**
	 * Set the set of DER program types to participate in.
	 * 
	 * @param programTypes
	 *        the DER program types
	 */
	public void setProgramTypes(Set<DerProgramType> programTypes) {
		this.programTypes = programTypes;
	}

	/**
	 * Get the resource characteristics.
	 * 
	 * @return the resource characteristics
	 */
	public ResourceCharacteristics getResourceCharacteristics() {
		return resourceCharacteristics;
	}

	/**
	 * Set the resource characteristics.
	 * 
	 * @param resourceCharacteristics
	 *        the resource characteristics
	 */
	public void setResourceCharacteristics(ResourceCharacteristics resourceCharacteristics) {
		this.resourceCharacteristics = resourceCharacteristics;
	}

}
