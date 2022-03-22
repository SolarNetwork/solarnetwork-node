/* ==================================================================
 * ExpressionRootTests.java - 20/02/2019 4:20:30 pm
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

package net.solarnetwork.node.datum.modbus.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.datum.modbus.ExpressionConfig;
import net.solarnetwork.node.datum.modbus.ExpressionRoot;

/**
 * Test cases for the {@link ExpressionConfig} class.
 * 
 * @author matt
 * @version 1.1
 */
public class ExpressionRootTests {

	@Test
	public void regRefSimple() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("regs[123]");
		assertThat(set, contains(123));
	}

	@Test
	public void regRefGet1Arg() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getInt16(123)");
		assertThat(set, contains(123));
	}

	@Test
	public void regRefGet1Arg_singleArgMultiReg() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getInt32(123)");
		assertThat("getInt32(123) adds implicit 124", set, contains(123, 124));
	}

	@Test
	public void regRefGet2Arg() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getInt32(123, 234)");
		assertThat(set, contains(123, 234));
	}

	@Test
	public void regRefGet4Arg() {
		Set<Integer> set = ExpressionRoot
				.registerAddressReferences("sample.getInt64(123, 234, 345, 456)");
		assertThat(set, contains(123, 234, 345, 456));
	}

	@Test
	public void regRefAsciiString() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getAsciiString(0, 8, true)");
		assertThat(set, contains(0, 1, 2, 3, 4, 5, 6, 7));
	}

	@Test
	public void regRefUtf8String() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getUtf8String(0, 8, true)");
		assertThat(set, contains(0, 1, 2, 3, 4, 5, 6, 7));
	}

	@Test
	public void regRefBytes() {
		Set<Integer> set = ExpressionRoot.registerAddressReferences("sample.getBytes(0, 8)");
		assertThat(set, contains(0, 1, 2, 3, 4, 5, 6, 7));
	}

	@Test
	public void regRefCombo() {
		Set<Integer> set = ExpressionRoot
				.registerAddressReferences("regs[3] * regs[8] + sample.getInt64(123, 234, 345, 456)");
		assertThat(set, contains(3, 8, 123, 234, 345, 456));
	}

	@Test
	public void regRefCombo_singleArgMultiReg() {
		Set<Integer> set = ExpressionRoot
				.registerAddressReferences("regs[3] * regs[8] + sample.getInt64(123)");
		assertThat("getInt64(123) adds implicit 124,125,126", set, contains(3, 8, 123, 124, 125, 126));
	}

	@Test
	public void regRefMixedWithNumbers() {
		Set<Integer> set = ExpressionRoot
				.registerAddressReferences("2 + regs[1] * props['foo'] + sample.getInt32(123, 234)");
		assertThat(set, contains(1, 123, 234));
	}
}
