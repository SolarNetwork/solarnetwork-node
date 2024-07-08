/* ==================================================================
 * LoadShedControlConfigPriorityComparator.java - 27/06/2015 5:06:21 pm
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

import java.util.Comparator;

/**
 * Comparator for {@link LoadShedControlConfig} to order by priority from lowest
 * to highest, with {@literal null} sorted last. A secondary sort on
 * <b>controlId</b> is used if both priority values are null.
 *
 * @author matt
 * @version 1.0
 */
public class LoadShedControlConfigPriorityComparator implements Comparator<LoadShedControlConfig> {

	/**
	 * A static instance of the comparator.
	 */
	public static final LoadShedControlConfigPriorityComparator COMPARATOR = new LoadShedControlConfigPriorityComparator();

	/**
	 * Constructor.
	 */
	public LoadShedControlConfigPriorityComparator() {
		super();
	}

	@Override
	public int compare(LoadShedControlConfig o1, LoadShedControlConfig o2) {
		if ( o1.getPriority() != null && o2.getPriority() != null ) {
			return o1.getPriority().compareTo(o2.getPriority());
		}
		if ( o2.getPriority() == null && o1.getPriority() != null ) {
			return -1;
		}
		if ( o1.getPriority() == null && o2.getPriority() != null ) {
			return 1;
		}
		if ( o1.getControlId() != null && o2.getControlId() != null ) {
			return o1.getControlId().compareToIgnoreCase(o2.getControlId());
		}
		if ( o2.getControlId() == null ) {
			return -1;
		}
		if ( o1.getControlId() == null ) {
			return 1;
		}
		return 0;
	}

}
