/* ==================================================================
 * Stabiliti30cUtils.java - 28/08/2019 7:00:49 am
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

package net.solarnetwork.node.hw.idealpower.pc;

import static net.solarnetwork.domain.Bitmaskable.setForBitmask;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.Bitmaskable;

/**
 * Utility methods for the Stabiliti 30C device series.
 * 
 * @author matt
 * @version 1.0
 */
public final class Stabiliti30cUtils {

	/** A comparator of faults by their numbers in ascending order. */
	public static final Comparator<Stabiliti30cFault> SORT_BY_FAULT_NUMBER = new SortByFaultNumber();

	/**
	 * A comparator of faults by their severity in descending order, then
	 * numbers in ascending order.
	 */
	public static final Comparator<Stabiliti30cFault> SORT_BY_FAULT_SEVERITY = new SortByFaultSeverity();

	/**
	 * A {@link Stabiliti30cFault} that can be used when sorting/filtering
	 * faults by severity.
	 * 
	 * <p>
	 * For example, you could use this fault to extract the tail set of faults
	 * with this severity or higher, like this:
	 * </p>
	 * 
	 * <pre>
	 * <code>
	 * SortedSet&lt;Stabiliti30cFault&gt; faults = getFaults();
	 * SortedSet&lt;Stabiliti30cFault&gt; faultsBySeverity = new TreeSet<>(SORT_BY_FAULT_SEVERITY);
	 * faultsBySeverity.addAll(faults);
	 * SortedSet&lt;Stabiliti30cFault&gt; severeFaults = faults.tailSet(ABORT2_SEVERITY);
	 * </code>
	 * </pre>
	 */
	public static final Stabiliti30cFault ABORT2_SEVERITY = new FaultSeverity(
			Stabiliti30cFaultSeverity.Abort2);

	/**
	 * A utility implementation of {@link Stabiliti30cFault} to help with
	 * filtering by severity.
	 */
	private static class FaultSeverity implements Stabiliti30cFault {

		private final Stabiliti30cFaultSeverity severity;

		private FaultSeverity(Stabiliti30cFaultSeverity severity) {
			super();
			this.severity = severity;
		}

		@Override
		public int bitmaskBitOffset() {
			return -1;
		}

		@Override
		public int getCode() {
			return -1;
		}

		@Override
		public int getFaultGroup() {
			return -1;
		}

		@Override
		public int getFaultNumber() {
			return -1;
		}

		@Override
		public String getDescription() {
			return severity.toString();
		}

		@Override
		public Stabiliti30cFaultSeverity getSeverity() {
			return severity;
		}

		@Override
		public String toString() {
			return severity + " fault";
		}

	}

	private static class SortByFaultNumber implements Comparator<Stabiliti30cFault> {

		@Override
		public int compare(Stabiliti30cFault o1, Stabiliti30cFault o2) {
			if ( o1 == o2 ) {
				return 0;
			} else if ( o1 == null ) {
				return -1;
			} else if ( o2 == null ) {
				return 1;
			}
			int n1 = o1.getFaultNumber();
			int n2 = o2.getFaultNumber();
			return (n2 == n1 ? 0 : n1 < n2 ? -1 : 1);
		}

	}

	private static class SortByFaultSeverity implements Comparator<Stabiliti30cFault> {

		@Override
		public int compare(Stabiliti30cFault o1, Stabiliti30cFault o2) {
			if ( o1 == o2 ) {
				return 0;
			} else if ( o1 == null ) {
				return -1;
			} else if ( o2 == null ) {
				return 1;
			}
			if ( o1.getSeverity() != null && o2.getSeverity() != null ) {
				// first sort by severity in descending order
				int s1 = o1.getSeverity().getCode();
				int s2 = o2.getSeverity().getCode();
				if ( s1 < s2 ) {
					return 1;
				} else if ( s1 > s2 ) {
					return -1;
				}
			}

			// then sort by number in ascending order
			int n1 = o1.getFaultNumber();
			int n2 = o2.getFaultNumber();
			return (n2 == n1 ? 0 : n1 < n2 ? -1 : 1);
		}

	}

	/**
	 * Get an overall set of faults from four fault group data values.
	 * 
	 * @param f0
	 *        the fault group 0 data value
	 * @param f1
	 *        the fault group 1 data value
	 * @param f2
	 *        the fault group 2 data value
	 * @param f3
	 *        the fault group 3 data value
	 * @return the set of faults, never {@literal null}
	 */
	public static SortedSet<Stabiliti30cFault> faultSet(int f0, int f1, int f2, int f3) {
		SortedSet<Stabiliti30cFault> result = new TreeSet<>(SORT_BY_FAULT_NUMBER);
		Set<Stabiliti30cFault0> f0set = setForBitmask(f0, Stabiliti30cFault0.class);
		if ( f0set != null ) {
			result.addAll(f0set);
		}
		Set<Stabiliti30cFault1> f1set = setForBitmask(f1, Stabiliti30cFault1.class);
		if ( f1set != null ) {
			result.addAll(f1set);
		}
		Set<Stabiliti30cFault2> f2set = setForBitmask(f2, Stabiliti30cFault2.class);
		if ( f2set != null ) {
			result.addAll(f2set);
		}
		Set<Stabiliti30cFault3> f3set = setForBitmask(f3, Stabiliti30cFault3.class);
		if ( f3set != null ) {
			result.addAll(f3set);
		}
		return result;
	}

	/**
	 * Get a fault data value from a set of faults.
	 * 
	 * @param faults
	 *        the set of faults to extract the data value from
	 * @param group
	 *        the fault group to extract
	 * @return the fault group data value
	 */
	public static int faultGroupDataValue(Set<Stabiliti30cFault> faults, int group) {
		if ( faults == null || faults.isEmpty() ) {
			return 0;
		}
		Set<Stabiliti30cFault> set = new HashSet<>(16);
		for ( Stabiliti30cFault f : faults ) {
			if ( group == f.getFaultGroup() ) {
				set.add(f);
			}
		}
		if ( set.isEmpty() ) {
			return 0;
		}
		return Bitmaskable.bitmaskValue(set);
	}

}
