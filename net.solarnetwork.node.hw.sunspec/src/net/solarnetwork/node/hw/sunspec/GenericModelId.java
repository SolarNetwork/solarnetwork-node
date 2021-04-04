/* ==================================================================
 * GenericModelId.java - 8/10/2018 3:06:39 PM
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
 * Generic {@link ModelId} that can be used when an unknown model is
 * encountered.
 * 
 * @author matt
 * @version 1.1
 * @since 1.1
 */
public class GenericModelId implements ModelId {

	private final int id;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the model ID
	 */
	public GenericModelId(int id) {
		super();
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDescription() {
		return "Model " + id;
	}

	@Override
	public Class<? extends ModelAccessor> getModelAccessorType() {
		return ModelAccessor.class;
	}

	@Override
	public String toString() {
		return getDescription();
	}

}
