/* ==================================================================
 * ModbusReferenceTests.java - 24/01/2020 3:45:07 pm
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

package net.solarnetwork.node.io.modbus.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import java.util.HashSet;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link ModbusReference} interface.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusReferenceTests {

	private static enum FooRegister implements ModbusReference {

		InfoOne(0, ModbusDataType.UInt16),

		InfoTwo(1, 5, ModbusDataType.StringUtf8),

		SetupThree(6, ModbusDataType.UInt16),

		ThingFour(7, ModbusDataType.UInt16);

		private final int address;
		private final int length;
		private final ModbusDataType dataType;

		private FooRegister(int address, ModbusDataType dataType) {
			this(address, 0, dataType);
		}

		private FooRegister(int address, int length, ModbusDataType dataType) {
			this.address = address;
			this.length = length;
			this.dataType = dataType;
		}

		@Override
		public int getAddress() {
			return address;
		}

		@Override
		public ModbusDataType getDataType() {
			return dataType;
		}

		@Override
		public ModbusReadFunction getFunction() {
			return ModbusReadFunction.ReadInputRegister;
		}

		@Override
		public int getWordLength() {
			return (this.length > 0 ? this.length : dataType.getWordLength());
		}

	}

	@Test
	public void enumAddressSet_All() {
		IntRangeSet result = createAddressSet(FooRegister.class, null);
		assertThat("Resolved ranges", result.ranges(), contains(rangeOf(0, 7)));
	}

	@Test
	public void enumAddressSet_Info() {
		IntRangeSet result = createAddressSet(FooRegister.class, singleton("Info"));
		assertThat("Resolved ranges", result.ranges(), contains(rangeOf(0, 5)));
	}

	@Test
	public void enumAddressSet_InfoAndSetup() {
		IntRangeSet result = createAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Setup")));
		assertThat("Resolved ranges", result.ranges(), contains(rangeOf(0, 6)));
	}

	@Test
	public void enumAddressSet_InfoAndSetupAndThing() {
		IntRangeSet result = createAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Setup", "Thing")));
		assertThat("Resolved ranges", result.ranges(), contains(rangeOf(0, 7)));
	}

	@Test
	public void enumAddressSet_InfoAndThing() {
		IntRangeSet result = createAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Thing")));
		assertThat("Resolved ranges", result.ranges(), contains(rangeOf(0, 5), rangeOf(7, 7)));
	}

	@Test
	public void enumAddressSet_NoMatch() {
		IntRangeSet result = createAddressSet(FooRegister.class, new HashSet<>(asList("Hello")));
		assertThat("Resolved ranges", stream(result.ranges().spliterator(), false).collect(toList()),
				hasSize(0));
	}

}
