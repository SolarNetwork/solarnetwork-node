/* ==================================================================
 * CommonModelId.java - 22/05/2018 11:26:25 AM
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

package net.solarnetwork.node.hw.sunspec;

/**
 * {@link ModelId} for the SunSpec common model standard.
 * 
 * @author matt
 * @version 1.1
 */
public enum CommonModelId implements ModelId {

	CommonModel(1, "Common model");

	private final int id;
	private final String description;

	private CommonModelId(int id, String description) {
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

	@Override
	public Class<? extends ModelAccessor> getModelAccessorType() {
		return CommonModelAccessor.class;
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
	public static CommonModelId forId(int id) {
		if ( id == 1 ) {
			return CommonModel;
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}

}
