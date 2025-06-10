/* ==================================================================
 * SocketcandUtilsTests.java - 20/09/2019 9:37:34 am
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.FrameMessage;
import net.solarnetwork.node.io.canbus.socketcand.Message;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * Test cases for the {@link SocketcandUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SocketcandUtilsTests {

	private char[] buffer;

	@Before
	public void setup() {
		buffer = new char[4096];
	}

	@Test
	public void decodeHexStrings_basic() {
		// GIVEN
		List<String> hexData = asList("1", "02", "33", "FF");

		// WHEN
		byte[] data = SocketcandUtils.decodeHexStrings(hexData, 0, 4);

		// THEN
		assertThat("Decoded bytes",
				Arrays.equals(data, new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33, (byte) 0xFF }),
				equalTo(true));
	}

	@Test
	public void decodeHexStrings_lowercase() {
		// GIVEN
		List<String> hexData = asList("1", "3d", "ab", "ff");

		// WHEN
		byte[] data = SocketcandUtils.decodeHexStrings(hexData, 0, 4);

		// THEN
		assertThat("Decoded bytes",
				Arrays.equals(data, new byte[] { (byte) 0x01, (byte) 0x3D, (byte) 0xAB, (byte) 0xFF }),
				equalTo(true));
	}

	@Test
	public void decodeHexStrings_sub_offset() {
		// GIVEN
		List<String> hexData = asList("1", "02", "33", "FF");

		// WHEN
		byte[] data = SocketcandUtils.decodeHexStrings(hexData, 1, 4);

		// THEN
		assertThat("Decoded bytes",
				Arrays.equals(data, new byte[] { (byte) 0x02, (byte) 0x33, (byte) 0xFF }),
				equalTo(true));
	}

	@Test
	public void decodeHexStrings_sub_short() {
		// GIVEN
		List<String> hexData = asList("1", "02", "33", "FF");

		// WHEN
		byte[] data = SocketcandUtils.decodeHexStrings(hexData, 0, 3);

		// THEN
		assertThat("Decoded bytes",
				Arrays.equals(data, new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33 }),
				equalTo(true));
	}

	@Test
	public void decodeHexStrings_sub_middle() {
		// GIVEN
		List<String> hexData = asList("1", "02", "33", "FF");

		// WHEN
		byte[] data = SocketcandUtils.decodeHexStrings(hexData, 1, 3);

		// THEN
		assertThat("Decoded bytes", Arrays.equals(data, new byte[] { (byte) 0x02, (byte) 0x33 }),
				equalTo(true));
	}

	@Test
	public void encodeHexStrings_basic() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33, (byte) 0xFF };

		// WHEN
		List<String> l = SocketcandUtils.encodeHexStrings(data, 0, 4);

		// THEN
		assertThat("Encoded bytes", l, contains("01", "02", "33", "FF"));
	}

	@Test
	public void encodeHexStrings_sub_offset() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33, (byte) 0xFF };

		// WHEN
		List<String> l = SocketcandUtils.encodeHexStrings(data, 1, 4);

		// THEN
		assertThat("Encoded bytes", l, contains("02", "33", "FF"));
	}

	@Test
	public void encodeHexStrings_sub_short() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33, (byte) 0xFF };

		// WHEN
		List<String> l = SocketcandUtils.encodeHexStrings(data, 0, 3);

		// THEN
		assertThat("Encoded bytes", l, contains("01", "02", "33"));
	}

	@Test
	public void encodeHexStrings_sub_middle() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x33, (byte) 0xFF };

		// WHEN
		List<String> l = SocketcandUtils.encodeHexStrings(data, 1, 3);

		// THEN
		assertThat("Encoded bytes", l, contains("02", "33"));
	}

	private void assertMessage(String prefix, Message m, MessageType type, String command,
			String... args) {
		assertThat(prefix + "message available", m, notNullValue());
		if ( type != null ) {
			assertThat(prefix + "type", m.getType(), equalTo(type));
		} else {
			assertThat(prefix + "type", m.getType(), nullValue());
		}
		assertThat(prefix + "command", m.getCommand(), equalTo(command));
		if ( args == null || args.length < 1 ) {
			assertThat(prefix + "arguments empty", m.getArguments(), anyOf(hasSize(0), nullValue()));
		} else {
			assertThat(prefix + "arguments", m.getArguments(), contains(args));
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void readMessage_bufferOverflow() throws IOException {
		// GIVEN
		Reader r = new StringReader("< hi ;adsf;ajsdfkl;ajsdkl;fjakls;dfjakls >");

		// WHEN
		SocketcandUtils.readMessage(r, new char[8]);

		// THEN
		// IndexOutOfBoundsException should be thrown because the buffer is not large enough for the message	
	}

	@Test
	public void readMessage_hi() throws IOException {
		// GIVEN
		Reader r = new StringReader("< hi >");

		// WHEN
		Message m = SocketcandUtils.readMessage(r, buffer);

		// THEN
		assertMessage("hi ", m, MessageType.Hi, "hi");
	}

	@Test
	public void readMessage_frame() throws IOException {
		// GIVEN
		Reader r = new StringReader("< frame 123 23.424242 11 22 33 44 >");

		// WHEN
		Message m = SocketcandUtils.readMessage(r, buffer);

		// THEN
		assertMessage("hi ", m, MessageType.Frame, "frame", "123 23.424242 11 22 33 44".split(" "));
		assertThat("Message is a FrameMessage", m, instanceOf(FrameMessage.class));

		FrameMessage fm = m.asType(FrameMessage.class);
		assertThat("Address", fm.getAddress(), equalTo(0x123));
		assertThat("Time", fm.getFractionalSeconds(), equalTo(new BigDecimal("23.424242")));
	}

	@Test
	public void longForBytes_singleByte() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0xFF };

		// WHEN
		long l = SocketcandUtils.longForBytes(data, 0);

		// THEN
		assertThat("Single byte translated to upper long bits", l, equalTo(0xFF00000000000000L));
	}

	@Test
	public void longForBytes_basic() {
		// GIVEN
		byte[] data = new byte[] { (byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98, (byte) 0x76,
				(byte) 0x54, (byte) 0x32, (byte) 0x10 };

		// WHEN
		long l = SocketcandUtils.longForBytes(data, 0);

		// THEN
		assertThat("8 bytes translated to complete long bits", l, equalTo(0xFEDCBA9876543210L));
	}

	@Test
	public void longForBytes_leadingZeros() {
		// GIVEN
		byte[] data = new byte[] { 0, 0, 0, 0, 0, 0, 0, (byte) 0x10 };

		// WHEN
		long l = SocketcandUtils.longForBytes(data, 0);

		// THEN
		assertThat("Last byte translated to lower long bits", l, equalTo(0x0000000000000010L));
	}

	@Test
	public void encodeAsHexStrings_basic() {
		// GIVEN
		long l = 0xFEDCBA9876543210L;

		// WHEN
		List<String> hex = SocketcandUtils.encodeAsHexStrings(l, true);

		// THEN
		assertThat("Hex strings encoded", hex, contains("FE", "DC", "BA", "98", "76", "54", "32", "10"));
	}

	@Test
	public void encodeAsHexStrings_trim() {
		// GIVEN
		long l = 0xFF00000000000000L;

		// WHEN
		List<String> hex = SocketcandUtils.encodeAsHexStrings(l, true);

		// THEN
		assertThat("Trailing zeros omitted", hex, contains("FF"));
	}

	@Test
	public void encodeAsHexStrings_noTrim() {
		// GIVEN
		long l = 0xFF00000000000000L;

		// WHEN
		List<String> hex = SocketcandUtils.encodeAsHexStrings(l, false);

		// THEN
		assertThat("Trailing zeros preserved", hex,
				contains("FF", "00", "00", "00", "00", "00", "00", "00"));
	}

}
