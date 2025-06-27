/* ==================================================================
 * Stabiliti30cUtilsTests.java - 28/08/2019 9:19:22 am
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

package net.solarnetwork.node.hw.idealpower.pc.test;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;
import org.junit.Test;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFault;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFault0;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFault1;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFault2;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cFault3;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cUtils;

/**
 * Test cases for the {@link Stabiliti30cUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class Stabiliti30cUtilsTests {

	private static final int RANDOM_FAULTS0_GROUP0 = 0b0010101000100101;
	private static final int RANDOM_FAULTS0_GROUP1 = 0b0000001010000001;
	private static final int RANDOM_FAULTS0_GROUP2 = 0b0100100000100010;
	private static final int RANDOM_FAULTS0_GROUP3 = 0b0000000001000011;

	private SortedSet<Stabiliti30cFault> randomFaults0() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				RANDOM_FAULTS0_GROUP0, 
				RANDOM_FAULTS0_GROUP1, 
				RANDOM_FAULTS0_GROUP2,
				RANDOM_FAULTS0_GROUP3);
		// @formatter:on
		return faults;
	}

	@Test
	public void faultSet_NoFaults() {
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(0, 0, 0, 0);
		assertThat("No faults", faults, hasSize(0));
	}

	@Test
	public void faultSet_SortedByFaultNumber() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		assertThat("One fault per active bit", faults, hasSize(16));
		int num = -1;
		for ( Stabiliti30cFault f : faults ) {
			assertThat("Ordered by number: " + f, f.getFaultNumber(), greaterThan(num));
			num = f.getFaultNumber();
		}
	}

	@Test
	public void faultSet_Group0() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				0b1111111111111111, 
				0b0000000000000000, 
				0b0000000000000000,
				0b0000000000000000);
		// @formatter:on

		Set<Stabiliti30cFault> expected = stream(Stabiliti30cFault0.values())
				.collect(toCollection(LinkedHashSet::new));
		assertThat("Group 0 faults all active", faults, equalTo(expected));
	}

	@Test
	public void faultSet_Group1() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				0b0000000000000000, 
				0b1111111111111111, 
				0b0000000000000000,
				0b0000000000000000);
		// @formatter:on

		Set<Stabiliti30cFault> expected = stream(Stabiliti30cFault1.values())
				.collect(toCollection(LinkedHashSet::new));
		assertThat("Group 1 faults all active", faults, equalTo(expected));
	}

	@Test
	public void faultSet_Group2() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				0b0000000000000000,
				0b0000000000000000, 
				0b1111111111111111, 
				0b0000000000000000);
		// @formatter:on

		Set<Stabiliti30cFault> expected = stream(Stabiliti30cFault2.values())
				.collect(toCollection(LinkedHashSet::new));
		assertThat("Group 2 faults all active", faults, equalTo(expected));
	}

	@Test
	public void faultSet_Group3() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				0b0000000000000000,
				0b0000000000000000, 
				0b0000000000000000, 
				0b1111111111111111);
		// @formatter:on

		Set<Stabiliti30cFault> expected = stream(Stabiliti30cFault3.values())
				.collect(toCollection(LinkedHashSet::new));
		assertThat("Group 3 faults all active", faults, equalTo(expected));
	}

	@Test
	public void faultSet_AllGroups() {
		// @formatter:off
		SortedSet<Stabiliti30cFault> faults = Stabiliti30cUtils.faultSet(
				0b1111111111111111,
				0b1111111111111111, 
				0b1111111111111111, 
				0b1111111111111111);

		Set<Stabiliti30cFault> expected = Stream.of(
				stream(Stabiliti30cFault0.values()),
				stream(Stabiliti30cFault1.values()),
				stream(Stabiliti30cFault2.values()),
				stream(Stabiliti30cFault3.values()))
				.flatMap(e -> e).collect(toCollection(LinkedHashSet::new));
		// @formatter:on

		assertThat("All group faults all active", faults, equalTo(expected));
	}

	@Test
	public void faultGroupValue_GroupDoesNotExist() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		int groupValue = Stabiliti30cUtils.faultGroupDataValue(faults, 99);
		assertThat("Group 99 data value", groupValue, equalTo(0));
	}

	@Test
	public void faultGroupValue_Group0() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		int groupValue = Stabiliti30cUtils.faultGroupDataValue(faults, 0);
		assertThat("Group 0 data value", groupValue, equalTo(RANDOM_FAULTS0_GROUP0));
	}

	@Test
	public void faultGroupValue_Group1() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		int groupValue = Stabiliti30cUtils.faultGroupDataValue(faults, 1);
		assertThat("Group 1 data value", groupValue, equalTo(RANDOM_FAULTS0_GROUP1));
	}

	@Test
	public void faultGroupValue_Group2() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		int groupValue = Stabiliti30cUtils.faultGroupDataValue(faults, 2);
		assertThat("Group 2 data value", groupValue, equalTo(RANDOM_FAULTS0_GROUP2));
	}

	@Test
	public void faultGroupValue_Group3() {
		SortedSet<Stabiliti30cFault> faults = randomFaults0();
		int groupValue = Stabiliti30cUtils.faultGroupDataValue(faults, 3);
		assertThat("Group 3 data value", groupValue, equalTo(RANDOM_FAULTS0_GROUP3));
	}
}
