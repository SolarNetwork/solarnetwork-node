/* ==================================================================
 * LocalStateTypeTests.java - 14/04/2025 8:55:13 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.util.ByteUtils.encodeHexString;
import static net.solarnetwork.util.ByteUtils.objectArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.domain.LocalStateType;

/**
 * Test cases for the {@link LocalStateType} class.
 *
 * @author matt
 * @version 1.0
 */
public class LocalStateTypeTests {

	@Test
	public void encode_boolean_true() {
		byte[] result = LocalStateType.Boolean.encode(true);
		assertThat("Boolean value encoded as byte", objectArray(result), arrayContaining((byte) 1));
	}

	@Test
	public void encode_boolean_false() {
		byte[] result = LocalStateType.Boolean.encode(false);
		assertThat("Boolean value encoded as byte", objectArray(result), arrayContaining((byte) 0));
	}

	@Test
	public void encode_boolean_number_true() {
		byte[] result = LocalStateType.Boolean.encode(123);
		assertThat("Integer value encoded as byte", objectArray(result), arrayContaining((byte) 1));
	}

	@Test
	public void encode_boolean_number_false() {
		byte[] result = LocalStateType.Boolean.encode(0);
		assertThat("Integer value encoded as byte", objectArray(result), arrayContaining((byte) 0));
	}

	@Test
	public void encode_boolean_string_true() {
		byte[] result = LocalStateType.Boolean.encode("true");
		assertThat("Integer string encoded as byte", objectArray(result), arrayContaining((byte) 1));
	}

	@Test
	public void encode_boolean_string_false() {
		byte[] result = LocalStateType.Boolean.encode("not true at all");
		assertThat("Integer string encoded as byte", objectArray(result), arrayContaining((byte) 0));
	}

	@Test
	public void encode_int() {
		byte[] result = LocalStateType.Int32.encode(0xABCDEF01);
		assertThat("Int value encoded as bytes", objectArray(result),
				arrayContaining((byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x01));
	}

	@Test
	public void encode_int_string() {
		byte[] result = LocalStateType.Int32.encode("2882400001");
		assertThat("Int string encoded as bytes", objectArray(result),
				arrayContaining((byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x01));
	}

	@Test
	public void encode_long() {
		byte[] result = LocalStateType.Int64.encode(0xABCDEF01ABCDEF01L);
		assertThat("Long value encoded as bytes", objectArray(result),
				arrayContaining((byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x01, (byte) 0xAB,
						(byte) 0xCD, (byte) 0xEF, (byte) 0x01));
	}

	@Test
	public void encode_long_string() {
		byte[] result = LocalStateType.Int64.encode("12379813741167767297");
		assertThat("Long string encoded as bytes", objectArray(result),
				arrayContaining((byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x01, (byte) 0xAB,
						(byte) 0xCD, (byte) 0xEF, (byte) 0x01));
	}

	@Test
	public void encode_float() {
		byte[] result = LocalStateType.Float32.encode(123.456f);
		assertThat("Float value encoded as bytes", objectArray(result),
				arrayContaining((byte) 0x42, (byte) 0xF6, (byte) 0xE9, (byte) 0x79));
	}

	@Test
	public void encode_float_string() {
		byte[] result = LocalStateType.Float32.encode("123.456");
		assertThat("Float string encoded as bytes", objectArray(result),
				arrayContaining((byte) 0x42, (byte) 0xF6, (byte) 0xE9, (byte) 0x79));
	}

	@Test
	public void encode_double() {
		byte[] result = LocalStateType.Float64.encode(123456789.123456789);
		assertThat("Double value encoded as bytes", objectArray(result),
				arrayContaining((byte) 0x41, (byte) 0x9D, (byte) 0x6F, (byte) 0x34, (byte) 0x54,
						(byte) 0x7E, (byte) 0x6B, (byte) 0x75));
	}

	@Test
	public void encode_double_string() {
		byte[] result = LocalStateType.Float64.encode("123456789.123456789");
		assertThat("Double string encoded as bytes", objectArray(result),
				arrayContaining((byte) 0x41, (byte) 0x9D, (byte) 0x6F, (byte) 0x34, (byte) 0x54,
						(byte) 0x7E, (byte) 0x6B, (byte) 0x75));
	}

	@Test
	public void encode_integer() {
		// GIVEN
		final String nHex = "ABCDEF0123456789ABCDEF0123456789";
		final BigInteger n = new BigInteger(nHex, 16);

		// WHEN
		byte[] result = LocalStateType.Integer.encode(n);

		// THEN
		assertThat("BigInteger value encoded as bytes", encodeHexString(result, 0, result.length, false),
				is(equalTo("00" + nHex)));
	}

	@Test
	public void encode_integer_string() {
		// GIVEN
		final String nDec = "228367255721259569362527394270995113865";

		// WHEN
		byte[] result = LocalStateType.Integer.encode(nDec);

		// THEN
		final byte[] nBytes = new BigInteger(nDec).toByteArray();
		final String nHex = encodeHexString(nBytes, 0, nBytes.length, false);
		assertThat("BigInteger string value encoded as bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(nHex)));
	}

	@Test
	public void encode_decimal() {
		// GIVEN
		final String nStr = "228367255721259569362527394270995113865.228367255721259569362527394270995113865";
		final BigDecimal n = new BigDecimal(nStr);

		// WHEN
		byte[] result = LocalStateType.Decimal.encode(n);

		// THEN
		final byte[] nBytes = n.toString().getBytes(UTF_8);
		final String nHex = encodeHexString(nBytes, 0, nBytes.length, false);
		assertThat("BigDecimal value encoded as UTF-8 string bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(nHex)));
	}

	@Test
	public void encode_decimal_string() {
		// GIVEN
		final String nStr = "228367255721259569362527394270995113865.228367255721259569362527394270995113865";

		// WHEN
		byte[] result = LocalStateType.Decimal.encode(nStr);

		// THEN
		final BigDecimal n = new BigDecimal(nStr);
		final byte[] nBytes = n.toString().getBytes(UTF_8);
		final String nHex = encodeHexString(nBytes, 0, nBytes.length, false);
		assertThat("BigDecimal string value encoded as UTF-8 string bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(nHex)));
	}

	@Test
	public void encode_string() {
		// GIVEN
		final String s = "this is a string";

		// WHEN
		byte[] result = LocalStateType.String.encode(s);

		// THEN
		final byte[] sBytes = s.getBytes(UTF_8);
		final String sHex = encodeHexString(sBytes, 0, sBytes.length, false);
		assertThat("String value encoded as UTF-8 string bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(sHex)));
	}

	@Test
	public void encode_mapping() {
		// GIVEN
		final Map<String, Object> m = new LinkedHashMap<>(3);
		m.put("a", 123);
		m.put("b", "two");
		m.put("c", 234);

		// WHEN
		byte[] result = LocalStateType.Mapping.encode(m);

		// THEN
		String s = JsonUtils.getJSONString(m);
		byte[] sBytes = s.getBytes(UTF_8);
		final String sHex = encodeHexString(sBytes, 0, sBytes.length, false);
		assertThat("Map value encoded as JSON UTF-8 string bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(sHex)));
	}

	@Test
	public void encode_mapping_object() {
		// GIVEN
		KeyValuePair p = new KeyValuePair("a", "b");

		// WHEN
		byte[] result = LocalStateType.Mapping.encode(p);

		// THEN
		final Map<String, Object> m = new LinkedHashMap<>(2);
		m.put("key", "a");
		m.put("value", "b");
		String s = JsonUtils.getJSONString(m);
		byte[] sBytes = s.getBytes(UTF_8);
		final String sHex = encodeHexString(sBytes, 0, sBytes.length, false);
		assertThat("Object value encoded as JSON object UTF-8 string bytes",
				encodeHexString(result, 0, result.length, false), is(equalTo(sHex)));
	}

	@Test
	public void decode_boolean_true() {
		// GIVEN
		final Boolean val = true;

		// WHEN
		Object result = LocalStateType.Boolean.decode(LocalStateType.Boolean.encode(val));

		// THEN
		assertThat("Boolean value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_boolean_false() {
		// GIVEN
		final Boolean val = false;

		// WHEN
		Object result = LocalStateType.Boolean.decode(LocalStateType.Boolean.encode(val));

		// THEN
		assertThat("Boolean value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_int() {
		// GIVEN
		Integer val = 0xABCDEF01;

		// WHEN
		Object result = LocalStateType.Int32.decode(LocalStateType.Int32.encode(val));

		// THEN
		assertThat("Int value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_long() {
		// GIVEN
		Long val = 0xABCDEF01ABCDEF01L;

		// WHEN
		Object result = LocalStateType.Int64.decode(LocalStateType.Int64.encode(val));

		// THEN
		assertThat("Long value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_float() {
		// GIVEN
		Float val = 123.456f;

		// WHEN
		Object result = LocalStateType.Float32.decode(LocalStateType.Float32.encode(val));

		// THEN
		assertThat("Float value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_double() {
		// GIVEN
		Double val = 123456789.123456789;

		// WHEN
		Object result = LocalStateType.Float64.decode(LocalStateType.Float64.encode(val));

		// THEN
		assertThat("Double value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_integer() {
		// GIVEN
		BigInteger val = new BigInteger("ABCDEF0123456789ABCDEF0123456789", 16);

		// WHEN
		Object result = LocalStateType.Integer.decode(LocalStateType.Integer.encode(val));

		// THEN
		assertThat("BigInteger value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_decimal() {
		// GIVEN
		BigDecimal val = new BigDecimal(
				"228367255721259569362527394270995113865.228367255721259569362527394270995113865");

		// WHEN
		Object result = LocalStateType.Decimal.decode(LocalStateType.Decimal.encode(val));

		// THEN
		assertThat("BigDecimal value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_string() {
		// GIVEN
		final String val = "this is a string";

		// WHEN
		Object result = LocalStateType.String.decode(LocalStateType.String.encode(val));

		// THEN
		assertThat("String value decoded", result, is(equalTo(val)));
	}

	@Test
	public void decode_mapping() {
		// GIVEN
		final Map<String, Object> val = new LinkedHashMap<>(3);
		val.put("a", 123);
		val.put("b", "two");
		val.put("c", 234);

		// WHEN
		Object result = LocalStateType.Mapping.decode(LocalStateType.Mapping.encode(val));

		// THEN
		assertThat("Map value decoded", result, is(equalTo(val)));
	}

}
