/* ==================================================================
 * GenericModelEvent.java - 10/09/2019 9:36:24 am
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

package net.solarnetwork.node.hw.sunspec;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A basic immutable implementation of {@link ModelEvent}.
 *
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public class GenericModelEvent implements ModelEvent, Comparable<GenericModelEvent> {

	private final int index;
	private final String description;

	/**
	 * Constructor.
	 *
	 * @param index
	 *        the index
	 */
	public GenericModelEvent(int index) {
		this(index, String.valueOf(index));
	}

	/**
	 * Constructor.
	 *
	 * @param index
	 *        the index
	 * @param description
	 *        the description
	 */
	public GenericModelEvent(int index, String description) {
		super();
		this.index = index;
		this.description = description;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int compareTo(GenericModelEvent o) {
		if ( o == null ) {
			return -1;
		}
		int l = getIndex();
		int r = o.getIndex();
		return (l < r ? -1 : l > r ? 1 : 0);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof GenericModelEvent) ) {
			return false;
		}
		GenericModelEvent other = (GenericModelEvent) obj;
		return index == other.index;
	}

	/**
	 * Get a set of events from a bitmask.
	 *
	 * @param bitmask
	 *        the bitmask
	 * @return the active events
	 */
	public static Set<ModelEvent> forBitmask(long bitmask) {
		if ( bitmask == 0 || (bitmask & ModelData.NAN_BITFIELD32) == ModelData.NAN_BITFIELD32 ) {
			return Collections.emptySet();
		}
		Set<ModelEvent> result = new LinkedHashSet<>(32);
		for ( int i = 0; i < 32; i++ ) {
			if ( ((bitmask >> i) & 0x1) == 1 ) {
				result.add(new GenericModelEvent(i));
			}
		}
		return result;
	}

}
