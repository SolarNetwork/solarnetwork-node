/* ==================================================================
 * IntRangeSetUtilsTests.java - 14/05/2018 6:56:22 PM
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

package net.solarnetwork.node.io.modbus.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import org.junit.Test;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Test cases for the {@link IntRangeSetUtils} class.
 * 
 * @author matt
 * @version 1.1
 */
public class IntRangeSetUtilsTests {

	@Test
	public void combineToReduceSizeReduced() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 1);
		set.addAll(3, 5);
		set.addAll(100, 101);
		assertThat("Original range count", set.ranges(), arrayWithSize(3));
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 5)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(100, 101)));
	}

	@Test
	public void combineToReduceSizeNotReduced() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 5);
		set.addAll(100, 101);
		assertThat("Original range count", set.ranges(), arrayWithSize(2));
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 5)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(100, 101)));
	}

	@Test
	public void javaDocExample() {
		IntRangeSet set = new IntRangeSet();
		set.addAll(0, 1);
		set.addAll(3, 5);
		set.addAll(20, 28);
		set.addAll(404, 406);
		set.addAll(412, 418);
		IntRangeSet result = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("New copy returned", result, allOf(notNullValue(), not(sameInstance(set))));
		assertThat("New range count", result.ranges(), arrayWithSize(2));
		IntRange[] ranges = result.ranges();
		assertThat("Range 0", ranges[0], equalTo(new IntRange(0, 28)));
		assertThat("Range 1", ranges[1], equalTo(new IntRange(404, 418)));
	}

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
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class, null);
		assertThat("Resolved ranges", result.ranges(), equalTo(new IntRange[] { new IntRange(0, 7) }));
	}

	@Test
	public void enumAddressSet_Info() {
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class,
				singleton("Info"));
		assertThat("Resolved ranges", result.ranges(), equalTo(new IntRange[] { new IntRange(0, 5) }));
	}

	@Test
	public void enumAddressSet_InfoAndSetup() {
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Setup")));
		assertThat("Resolved ranges", result.ranges(), equalTo(new IntRange[] { new IntRange(0, 6) }));
	}

	@Test
	public void enumAddressSet_InfoAndSetupAndThing() {
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Setup", "Thing")));
		assertThat("Resolved ranges", result.ranges(), equalTo(new IntRange[] { new IntRange(0, 7) }));
	}

	@Test
	public void enumAddressSet_InfoAndThing() {
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class,
				new HashSet<>(asList("Info", "Thing")));
		assertThat("Resolved ranges", result.ranges(),
				equalTo(new IntRange[] { new IntRange(0, 5), new IntRange(7, 7) }));
	}

	@Test
	public void enumAddressSet_NoMatch() {
		IntRangeSet result = IntRangeSetUtils.createRegisterAddressSet(FooRegister.class,
				new HashSet<>(asList("Hello")));
		assertThat("Resolved ranges", result.ranges(), equalTo(new IntRange[0]));
	}
}
