/* ==================================================================
 * InverterDerType.java - 15/10/2018 9:34:56 AM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import net.solarnetwork.node.hw.sunspec.DistributedEnergyResourceType;
import net.solarnetwork.node.hw.sunspec.ModelData;

/**
 * Enumeration of inverter DER types.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public enum InverterDerType implements DistributedEnergyResourceType {

	PV(4, "Photovoltaic generation"),

	PVAndStorage(82, "Photovoltaic generation with battery storage");

	private final int code;
	private final String description;

	private InverterDerType(int index, String description) {
		this.code = index;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for an index value.
	 * 
	 * @param code
	 *        the code to get the enum value for
	 * @return the enumeration value, or {@literal null} if {@code code} is
	 *         {@literal NaN}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static InverterDerType forCode(int code) {
		if ( (code & ModelData.NAN_ENUM16) == ModelData.NAN_ENUM16 ) {
			return null;
		}
		for ( InverterDerType e : InverterDerType.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Code [" + code + "] not supported");
	}

}
