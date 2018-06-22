/* ==================================================================
 * ModbusDataUtilsTests.java - 10/04/2018 2:26:24 PM
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

import static net.solarnetwork.node.io.modbus.ModbusWordOrder.LeastToMostSignificant;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigInteger;
import org.hamcrest.Matchers;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;

/**
 * Test cases for the {@link ModbusDataUtils} class.
 * 
 * @author matt
 * @version 1.1
 */
public class ModbusDataUtilsTests {

	@Test
	public void testParseFloat32() {
		Float result = ModbusDataUtils.parseFloat32(0x403F, 0xA7F6);
		assertThat(result, notNullValue());
		assertThat("Float value", result.doubleValue(), closeTo(2.994626, 0.000001));
	}

	@Test
	public void testParseFloat32NaN() {
		Float result = ModbusDataUtils.parseFloat32(0xFFC0, 0x0000);
		assertThat("Float value is NaN", result, nullValue());
	}

	@Test
	public void testEncodeInt32() {
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeInt32(-12313489));
		assertThat(words, arrayContaining(0xFF44, 0x1C6F));
	}

	@Test
	public void testEncodeInt32LeastSignificantWordOrder() {
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeInt32(-12313489, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x1C6F, 0xFF44));
	}

	@Test
	public void testParseInt32() {
		assertThat(ModbusDataUtils.parseInt32(0xFF44, 0x1C6F), Matchers.equalTo(-12313489));
	}

	@Test
	public void testEncodeUInt64Many() {
		for ( long l = 0; l < 5000000; l += 3 ) {
			BigInteger bint = new BigInteger(String.valueOf(l));
			Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint));
			assertThat(l + " converted", words, arrayContaining((int) ((l >> 48) & 0xFFFF),
					(int) ((l >> 32) & 0xFFFF), (int) ((l >> 16) & 0xFFFF), (int) (l & 0xFFFF)));
		}
	}

	@Test
	public void testEncodeUInt64ManyLeastSignificantWordOrder() {
		for ( long l = 0; l < 5000000; l += 3 ) {
			BigInteger bint = new BigInteger(String.valueOf(l));
			Integer[] words = ModbusDataUtils
					.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint, LeastToMostSignificant));
			assertThat(l + " converted", words, arrayContaining((int) (l & 0xFFFF),
					(int) ((l >> 16) & 0xFFFF), (int) ((l >> 32) & 0xFFFF), (int) ((l >> 48) & 0xFFFF)));
		}
	}

	@Test
	public void testEncodeUInt64Small() {
		BigInteger bint = new BigInteger("12345678", 16);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint));
		assertThat(words, arrayContaining(0, 0, 0x1234, 0x5678));
	}

	@Test
	public void testEncodeUInt64SmallLeastSignificantWordOrder() {
		BigInteger bint = new BigInteger("12345678", 16);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x5678, 0x1234, 0, 0));
	}

	@Test
	public void testEncodeUInt64Big() {
		BigInteger bint = new BigInteger("175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint));
		assertThat(words, arrayContaining(0x1758, 0x16FE, 0x2F85, 0x866B));
	}

	@Test
	public void testEncodeUInt64BigLeastSignificantWordOrder() {
		BigInteger bint = new BigInteger("175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x866B, 0x2F85, 0x16FE, 0x1758));
	}

	@Test
	public void testEncodeUInt64DroppingExcessBytes() {
		BigInteger bint = new BigInteger("FF00175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint));
		assertThat(words, arrayContaining(0x1758, 0x16FE, 0x2F85, 0x866B));
	}

	@Test
	public void testEncodeUInt64DroppingExcessBytesLeastSignificantWordOrder() {
		BigInteger bint = new BigInteger("FF00175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeUnsignedInt64(bint, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x866B, 0x2F85, 0x16FE, 0x1758));
	}

	@Test
	public void testEncodeUnsignedIntegerLargerThan64() {
		BigInteger bint = new BigInteger("FF00175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInteger(bint));
		assertThat(words, arrayContaining(0xFF00, 0x1758, 0x16FE, 0x2F85, 0x866B));
	}

	@Test
	public void testEncodeUnsignedIntegerLargerThan64LeastSignificantWordOrder() {
		BigInteger bint = new BigInteger("FF00175816FE2F85866B", 16);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeUnsignedInteger(bint, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x866B, 0x2F85, 0x16FE, 0x1758, 0xFF00));
	}

	@Test
	public void testEncodeUnsignedIntegerWayLargerThan64() {
		BigInteger bint = new BigInteger("00328586616F5866FF001755866816FE2F586685866B5866", 16);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeUnsignedInteger(bint));
		assertThat(words, arrayContaining(0x0032, 0x8586, 0x616F, 0x5866, 0xFF00, 0x1755, 0x8668, 0x16FE,
				0x2F58, 0x6685, 0x866B, 0x5866));
	}

	@Test
	public void testEncodeUnsignedIntegerWayLargerThan64LeastSignificantWordOrder() {
		BigInteger bint = new BigInteger("00328586616F5866FF001755866816FE2F586685866B5866", 16);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeUnsignedInteger(bint, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x5866, 0x866B, 0x6685, 0x2F58, 0x16FE, 0x8668, 0x1755, 0xFF00,
				0x5866, 0x616F, 0x8586, 0x0032));
	}

	@Test
	public void testParseUnsignedIntegerWayLargerThan64() {
		int[] words = new int[] { 0x0032, 0x8586, 0x616F, 0x5866, 0xFF00, 0x1755, 0x8668, 0x16FE, 0x2F58,
				0x6685, 0x866B, 0x5866 };
		BigInteger bint = ModbusDataUtils.parseUnsignedInteger(words, 0);
		assertThat(bint,
				equalTo(new BigInteger("00328586616F5866FF001755866816FE2F586685866B5866", 16)));
	}

	@Test
	public void testParseUnsignedIntegerWayLargerThan64LeastSignificantWordOrder() {
		int[] words = new int[] { 0x5866, 0x866B, 0x6685, 0x2F58, 0x16FE, 0x8668, 0x1755, 0xFF00, 0x5866,
				0x616F, 0x8586, 0x0032 };
		BigInteger bint = ModbusDataUtils.parseUnsignedInteger(words, 0, LeastToMostSignificant);
		assertThat(bint,
				equalTo(new BigInteger("00328586616F5866FF001755866816FE2F586685866B5866", 16)));
	}

	@Test
	public void testParseUnsignedIntegerWayLargerThan64NoLeadingZeros() {
		int[] words = new int[] { 0xDC32, 0x8586, 0x616F, 0x5866, 0xFF00, 0x1755, 0x8668, 0x16FE, 0x2F58,
				0x6685, 0x866B, 0x5866 };
		BigInteger bint = ModbusDataUtils.parseUnsignedInteger(words, 0);
		assertThat(bint,
				equalTo(new BigInteger("DC328586616F5866FF001755866816FE2F586685866B5866", 16)));
	}

	@Test
	public void testParseUnsignedIntegerWayLargerThan64NoLeadingZerosLeastSignificantWordOrder() {
		int[] words = new int[] { 0x5866, 0x866B, 0x6685, 0x2F58, 0x16FE, 0x8668, 0x1755, 0xFF00, 0x5866,
				0x616F, 0x8586, 0xDC32 };
		BigInteger bint = ModbusDataUtils.parseUnsignedInteger(words, 0, LeastToMostSignificant);
		assertThat(bint,
				equalTo(new BigInteger("DC328586616F5866FF001755866816FE2F586685866B5866", 16)));
	}

	@Test
	public void testEncodeFloat32() {
		Float f = Float.intBitsToFloat(0x403FA7F6);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeFloat32(f));
		assertThat(words, arrayContaining(0x403F, 0xA7F6));
	}

	@Test
	public void testEncodeFloat32LeastSignificantWordOrder() {
		Float f = Float.intBitsToFloat(0x403FA7F6);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeFloat32(f, LeastToMostSignificant));
		assertThat(words, arrayContaining(0xA7F6, 0x403F));
	}

	@Test
	public void testEncodeFloat64() {
		Double d = Double.longBitsToDouble(0x403FA7F6403FA7F6L);
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeFloat64(d));
		assertThat(words, arrayContaining(0x403F, 0xA7F6, 0x403F, 0xA7F6));
	}

	@Test
	public void testEncodeFloat64LeastToMostSignificant() {
		Double d = Double.longBitsToDouble(0x403FA7F6403FA7F6L);
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeFloat64(d, LeastToMostSignificant));
		assertThat(words, arrayContaining(0xA7F6, 0x403F, 0xA7F6, 0x403F));
	}

	@Test
	public void testEncodeBytes() {
		byte[] data = new byte[] { 1, 3, 5, 7, 9, 0xb, 0xd };
		Integer[] words = ModbusDataUtils.integerArray(ModbusDataUtils.encodeBytes(data));
		assertThat(words, arrayContaining(0x0103, 0x0507, 0x090b, 0x0d00));
	}

	@Test
	public void testEncodeBytesLeastToMostSignificant() {
		byte[] data = new byte[] { 1, 3, 5, 7, 9, 0xb, 0xd };
		Integer[] words = ModbusDataUtils
				.integerArray(ModbusDataUtils.encodeBytes(data, LeastToMostSignificant));
		assertThat(words, arrayContaining(0x0d00, 0x090b, 0x0507, 0x0103));
	}

	@Test
	public void testParseBytes() {
		int[] words = new int[] { 0x0103, 0x0507, 0x090b, 0x0d00 };
		byte[] data = ModbusDataUtils.parseBytes(words, 0);
		Byte[] bytes = new Byte[data.length];
		for ( int i = 0; i < bytes.length; i++ ) {
			bytes[i] = data[i];
		}
		assertThat(bytes, arrayContaining((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9, (byte) 0xb,
				(byte) 0xd, (byte) 0));
	}

	@Test
	public void testParseBytesLeastToMostSignificant() {
		int[] words = new int[] { 0x0d00, 0x090b, 0x0507, 0x0103 };
		byte[] data = ModbusDataUtils.parseBytes(words, 0, LeastToMostSignificant);
		Byte[] bytes = new Byte[data.length];
		for ( int i = 0; i < bytes.length; i++ ) {
			bytes[i] = data[i];
		}
		assertThat(bytes, arrayContaining((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9, (byte) 0xb,
				(byte) 0xd, (byte) 0));
	}

}
