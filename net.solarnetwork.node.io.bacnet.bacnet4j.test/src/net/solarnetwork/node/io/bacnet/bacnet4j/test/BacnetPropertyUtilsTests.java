/* ==================================================================
 * BacnetPropertyUtilsTests.java - 7/11/2022 8:04:25 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet.bacnet4j.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigInteger;
import org.junit.Test;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.DoorStatus;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import net.solarnetwork.node.io.bacnet.bacnet4j.BacnetPropertyUtils;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link BacnetPropertyUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetPropertyUtilsTests {

	@Test
	public void numberValue_null() {
		assertThat("Null parsed as null", BacnetPropertyUtils.numberValue(new Null()), is(nullValue()));
	}

	@Test
	public void numberValue_boolean() {
		assertThat("Boolean true parsed",
				BacnetPropertyUtils.numberValue(com.serotonin.bacnet4j.type.primitive.Boolean.TRUE),
				is(1));
		assertThat("Boolean false parsed",
				BacnetPropertyUtils.numberValue(com.serotonin.bacnet4j.type.primitive.Boolean.FALSE),
				is(0));
	}

	@Test
	public void numberValue_unsigned_int() {
		final int expected = 123456789;
		assertThat("UnsignedInteger int parsed",
				BacnetPropertyUtils.numberValue(new UnsignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_unsigned_long() {
		final long expected = 1234567890123456L;
		assertThat("UnsignedInteger long parsed",
				BacnetPropertyUtils.numberValue(new UnsignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_unsigned_big() {
		final BigInteger expected = new BigInteger("1234567890123456123091238109231");
		assertThat("UnsignedInteger BigInteger parsed",
				BacnetPropertyUtils.numberValue(new UnsignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_signed_int() {
		final int expected = -123456789;
		assertThat("SignedInteger int parsed",
				BacnetPropertyUtils.numberValue(new SignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_signed_long() {
		final long expected = -1234567890123456L;
		assertThat("SignedInteger long parsed",
				BacnetPropertyUtils.numberValue(new SignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_signed_big() {
		final BigInteger expected = new BigInteger("-1234567890123456123091238109231");
		assertThat("SignedInteger BigInteger parsed",
				BacnetPropertyUtils.numberValue(new SignedInteger(expected)), is(equalTo(expected)));
	}

	@Test
	public void numberValue_real() {
		final float expected = 123.456f;
		assertThat("Real parsed", BacnetPropertyUtils.numberValue(new Real(expected)),
				is(equalTo(expected)));
	}

	@Test
	public void numberValue_double() {
		final double expected = 123.4567892f;
		assertThat("Double parsed",
				BacnetPropertyUtils
						.numberValue(new com.serotonin.bacnet4j.type.primitive.Double(expected)),
				is(equalTo(expected)));
	}

	@Test
	public void numberValue_enum() {
		final DoorStatus val = DoorStatus.safetyLocked;
		assertThat("Enumerated parsed", BacnetPropertyUtils.numberValue(val),
				is(equalTo(val.intValue())));
	}

	@Test
	public void numberValue_date() {
		final Date val = new Date(2022, Month.APRIL, 7, null);
		assertThat("Date parsed", BacnetPropertyUtils.numberValue(val), is(equalTo(20220407)));
	}

	@Test
	public void numberValue_date_notFullySpecified() {
		final Date val = new Date(2022, null, 7, null);
		assertThat("Unspecified date not parsed", BacnetPropertyUtils.numberValue(val), is(nullValue()));
	}

	@Test
	public void numberValue_time_noHundreths() {
		final Time val = new Time(7, 17, 32, Time.UNSPECIFIC);
		assertThat("Time without hundredths parsed", BacnetPropertyUtils.numberValue(val),
				is(equalTo(26252)));
	}

	@Test
	public void numberValue_time_withHundreths() {
		final Time val = new Time(7, 17, 32, 82);
		assertThat("Time with hundredths parsed", BacnetPropertyUtils.numberValue(val),
				is(equalTo(26252.82f)));
	}

	@Test
	public void numberValue_time_notFullySpecified() {
		final Time val = new Time(Time.UNSPECIFIC, 17, 32, 0);
		assertThat("Unspecified time not parsed", BacnetPropertyUtils.numberValue(val), is(nullValue()));
	}

	@Test
	public void numberValue_oid() {
		final ObjectIdentifier val = new ObjectIdentifier(ObjectType.accessDoor, 1);
		assertThat("ObjectIdentifier parsed", BacnetPropertyUtils.numberValue(val), is(equalTo(30.1f)));
	}

	@Test
	public void numberValue_bitString() {
		final ServicesSupported val = new ServicesSupported();
		val.setReadProperty(true);
		val.setReadPropertyMultiple(true);
		val.setSubscribeCov(true);
		val.setSubscribeCovProperty(true);
		final long expected = (1L << 12) | (1L << 14) | (1L << 5) | (1L << 38);
		assertThat("ObjectIdentifier parsed", BacnetPropertyUtils.numberValue(val),
				is(equalTo(expected)));
	}

	@Test
	public void numberValue_octetString() {
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 };
		final OctetString val = new OctetString(data);
		String expected = ByteUtils.encodeHexString(data, 0, data.length, false);
		BigInteger result = NumberUtils.bigIntegerForNumber(BacnetPropertyUtils.numberValue(val));
		assertThat("OctetString parsed", result, is(equalTo(new BigInteger(expected, 16))));
	}

	@Test
	public void stringValue_octetString_byte() {
		final OctetString val = new OctetString(new byte[] { 2 });
		assertThat("OctetString parsed", BacnetPropertyUtils.stringValue(val), is(equalTo("2")));
	}

	@Test
	public void stringValue_octetString_6byte() {
		final OctetString val = new OctetString(new byte[] { 1, 2, 3, 4, 5, 6 });
		assertThat("OctetString parsed", BacnetPropertyUtils.stringValue(val),
				is(equalTo("1.2.3.4:1286")));
	}

	@Test
	public void stringValue_octetString_18byte() {
		final OctetString val = new OctetString(
				new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8 });
		assertThat("OctetString parsed", BacnetPropertyUtils.stringValue(val),
				is(equalTo("[102:304:506:708:900:102:304:506]:1800")));
	}

	@Test
	public void stringValue_octetString_bytes() {
		final OctetString val = new OctetString(new byte[] { 1, 2, 3, 4 });
		assertThat("ObjectIdentifier parsed", BacnetPropertyUtils.stringValue(val),
				is(equalTo("1.2.3.4")));
	}

}
