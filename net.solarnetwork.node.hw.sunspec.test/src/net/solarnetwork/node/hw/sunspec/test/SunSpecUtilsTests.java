/* ==================================================================
 * SunSpecUtilsTests.java - 6/10/2026 12:14:31 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.hw.sunspec.SunSpecUtils;

/**
 * Test cases for the {@link SunSpecUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class SunSpecUtilsTests {

	/** Test flags. */
	private enum TestFlag implements Bitmaskable {

		A(0),

		B(1),

		C(15),

		D(30),

		;

		private final int offset;

		private TestFlag(int offset) {
			this.offset = offset;
		}

		@Override
		public int bitmaskBitOffset() {
			return offset;
		}

	}

	@Test
	public void bitfieldValues_null() {
		assertThat("Not available", SunSpecUtils.bitfieldValues(null, 1, TestFlag.class),
				is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_zero() {
		assertThat("No bits set", SunSpecUtils.bitfieldValues(0, 2, TestFlag.class),
				is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_bitfield16() {
		assertThat("Values for bits set", SunSpecUtils.bitfieldValues(0x0003, 1, TestFlag.class),
				is(equalTo(EnumSet.of(TestFlag.A, TestFlag.B))));
	}

	@Test
	public void bitfieldValues_bitfield16MostSignificantBit() {
		assertThat("Bitfield16 with MSB set not implemented",
				SunSpecUtils.bitfieldValues(0x8003, 1, TestFlag.class), is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_bitfield16NotImplemented() {
		assertThat("Bitfield16 not implemented", SunSpecUtils.bitfieldValues(0xFFFF, 1, TestFlag.class),
				is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_bitfield32() {
		assertThat("Values for bits set, including bit 15",
				SunSpecUtils.bitfieldValues(0x40008001L, 2, TestFlag.class),
				is(equalTo(EnumSet.of(TestFlag.A, TestFlag.C, TestFlag.D))));
	}

	@Test
	public void bitfieldValues_bitfield32MostSignificantBit() {
		assertThat("Bitfield32 with MSB set not implemented",
				SunSpecUtils.bitfieldValues(0x80000001L, 2, TestFlag.class), is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_bitfield32NotImplemented() {
		assertThat("Bitfield32 not implemented",
				SunSpecUtils.bitfieldValues(0xFFFFFFFFL, 2, TestFlag.class), is(equalTo(Set.of())));
	}

	@Test
	public void bitfieldValues_undefinedBits() {
		assertThat("Undefined bits ignored", SunSpecUtils.bitfieldValues(0x0005, 1, TestFlag.class),
				is(equalTo(EnumSet.of(TestFlag.A))));
	}

}
