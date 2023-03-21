/* ==================================================================
 * AE500NxUtils.java - 22/04/2020 2:30:01 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import static net.solarnetwork.domain.Bitmaskable.setForBitmask;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.GroupedBitmaskable;

/**
 * Utilities to help with AE500NX devices.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public final class AE500NxUtils {

	private AE500NxUtils() {
		// not available
	}

	/**
	 * Get an overall set of faults from 3 fault group data values.
	 * 
	 * @param f1
	 *        the fault group 1 data value
	 * @param f2
	 *        the fault group 2 data value
	 * @param f3
	 *        the fault group 3 data value
	 * @return the set of faults, never {@literal null}
	 */
	public static SortedSet<AE500NxFault> faultSet(int f1, int f2, int f3) {
		SortedSet<AE500NxFault> result = new TreeSet<>(GroupedBitmaskable.SORT_BY_OVERALL_INDEX);
		Set<AE500NxFault1> f1set = setForBitmask(f1, AE500NxFault1.class);
		if ( f1set != null ) {
			result.addAll(f1set);
		}
		Set<AE500NxFault2> f2set = setForBitmask(f2, AE500NxFault2.class);
		if ( f2set != null ) {
			result.addAll(f2set);
		}
		Set<AE500NxFault3> f3set = setForBitmask(f3, AE500NxFault3.class);
		if ( f3set != null ) {
			result.addAll(f3set);
		}
		return result;
	}

	/**
	 * Get an overall set of warnings from the warning group data value.
	 * 
	 * @param w1
	 *        the warning group 1 data value
	 * @return the set of faults, never {@literal null}
	 */
	public static SortedSet<AE500NxWarning> warningSet(int w1) {
		SortedSet<AE500NxWarning> result = new TreeSet<>(GroupedBitmaskable.SORT_BY_OVERALL_INDEX);
		Set<AE500NxWarning1> f1set = setForBitmask(w1, AE500NxWarning1.class);
		if ( f1set != null ) {
			result.addAll(f1set);
		}
		return result;
	}

}
