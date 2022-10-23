/* ==================================================================
 * PVITLInverterType.java - 2/08/2018 9:51:04 AM
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

/**
 * Enumeration of inverter types.
 * 
 * @author matt
 * @version 1.0
 */
public enum PVITLInverterType {

	/** 14TL. */
	PVI_14TL("14TL"),

	/** 20TL. */
	PVI_20TL("20TL"),

	/** 23TL. */
	PVI_23TL("23TL"),

	/** 28TL. */
	PVI_28TL("28TL"),

	/** 36TL. */
	PVI_36TL("36TL"),

	;

	private final String description;

	private PVITLInverterType(String description) {
		this.description = description;
	}

	/**
	 * Get a description of the type.
	 * 
	 * @return a description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for a given model name.
	 * 
	 * @param modelName
	 *        the model name to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code modelName} is not supported
	 */
	public static PVITLInverterType forModelName(String modelName) {
		if ( modelName != null ) {
			String ucModelName = modelName.toUpperCase();
			for ( PVITLInverterType s : values() ) {
				if ( ucModelName.contains(s.description) ) {
					return s;
				}
			}
		}
		throw new IllegalArgumentException("Unsupported model name: " + modelName);
	}

}
