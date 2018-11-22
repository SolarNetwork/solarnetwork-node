/* ==================================================================
 * FactoryInstanceIdComparator.java - 23/11/2018 12:02:09 PM
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

package net.solarnetwork.node.settings.support;

import java.util.Comparator;

/**
 * Compare two factory instance IDs numerically.
 * 
 * @author matt
 * @version 1.0
 * @since 1.61
 */
public class FactoryInstanceIdComparator implements Comparator<String> {

	/** A static instance that can be used for sorting. */
	public static final FactoryInstanceIdComparator INSTANCE = new FactoryInstanceIdComparator();

	@Override
	public int compare(String o1, String o2) {
		if ( o1 == o2 ) {
			return 0;
		} else if ( o1 == null ) {
			return -1;
		} else if ( o2 == null ) {
			return 1;
		}
		try {
			int l = Integer.parseInt(o1);
			int r = Integer.parseInt(o2);
			return (l < r ? -1 : l > r ? 1 : 0);
		} catch ( NumberFormatException e ) {
			// bail
			return o1.compareTo(o2);
		}
	}

}
