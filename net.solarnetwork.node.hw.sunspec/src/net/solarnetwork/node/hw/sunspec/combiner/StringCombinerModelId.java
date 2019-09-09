/* ==================================================================
 * StringCombinerModelId.java - 5/10/2018 4:21:00 PM
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

package net.solarnetwork.node.hw.sunspec.combiner;

import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec inverter model IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum StringCombinerModelId implements ModelId {

	BasicStringCombiner(401, "Basic string combiner"),

	AdvancedStringCombiner(402, "Advanced string combiner"),

	BasicStringCombiner2(403, "Basic string combiner v2"),

	AdvancedStringCombiner2(404, "Advanced string combiner v2");

	final private int id;
	final private String description;

	private StringCombinerModelId(int id, String description) {
		this.id = id;
		this.description = description;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for an ID value.
	 * 
	 * @param id
	 *        the ID to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code id} is not supported
	 */
	public static StringCombinerModelId forId(int id) {
		for ( StringCombinerModelId e : StringCombinerModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
