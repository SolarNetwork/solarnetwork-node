/* ==================================================================
 * PacketTests.java - 15/05/2018 5:20:33 PM
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

package net.solarnetwork.node.hw.yaskawa.ecb.test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;
import net.solarnetwork.node.hw.yaskawa.ecb.Packet;

/**
 * Test cases for the {@link Packet} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PacketTests {

	private static Byte[] byteArray(byte[] array) {
		if ( array == null ) {
			return null;
		}
		Byte[] result = new Byte[array.length];
		for ( int i = 0, len = array.length; i < len; i++ ) {
			result[i] = array[i];
		}
		return result;
	}

	@Test
	public void crc() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0xAC, (byte) 0x6C, 0x03 });
		assertThat("Calcualted CRC", p.getCalculatedCrc(), equalTo(0x6CAC));
		assertThat("Encoded CRC", p.getCrc(), equalTo(0x6CAC));
		assertThat("Packet appears valid", p.isValid(), equalTo(true));
	}

	@Test
	public void bodyEmpty() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0xAC, (byte) 0x6C, 0x03 });
		assertThat("Body", byteArray(p.getBody()), arrayWithSize(0));
	}

	@Test
	public void body() {
		Packet p = new Packet(
				new byte[] { 0x02, 0x05, 0x01, 0x04, 0x01, 0x01, (byte) 0x10, (byte) 0x11 });
		assertThat("Body", byteArray(p.getBody()), arrayContaining((byte) 0x10, (byte) 0x11));
	}

	@Test(expected = IllegalArgumentException.class)
	public void bodyMissingData() {
		Packet p = new Packet(new byte[] { 0x02, 0x05, 0x01, 0x04, 0x01, 0x01, (byte) 0x10 });
		p.getBody();
	}

	@Test
	public void constructFromHex() throws DecoderException {
		Packet p = new Packet("020501020101AC6C03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void debugStringWithoutBody() throws DecoderException {
		Packet p = new Packet("020501020101AC6C03");
		assertThat("Debug string", p.toDebugString(), equalTo("02 05 01 02 01 01 ac6c 03"));
	}

	@Test
	public void debugStringWithBody() throws DecoderException {
		Packet p = Packet.forData("02060116", "0000137b55533234302f31323053002c000000000000c43f03");
		assertThat("Debug string", p.toDebugString(),
				equalTo("02 06 01 16 00 00 137b55533234302f31323053002c000000000000 c43f 03"));

	}

	@Test
	public void constructFromHexWithWhitespace() throws DecoderException {
		Packet p = new Packet("02 05 01 02 01 01 AC 6C 03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void forComponents() throws DecoderException {
		Packet p = Packet.forCommand(1, 1, 1, (String) null);
		assertThat("Packet valid", p.isValid(), equalTo(true));
		assertThat("Address", p.getHeader().getAddress(), equalTo((short) 1));
		assertThat("Command", p.getCommand(), equalTo((byte) 1));
		assertThat("Sub-command", p.getSubCommand(), equalTo((byte) 1));
		assertThat("CRC", p.getCrc(), equalTo(0x6CAC));
	}

	@Test
	public void forDataNoBody() {
		Packet p = Packet.forData(new byte[] { 0x02, 0x05, 0x01, 0x02 }, 0,
				new byte[] { 0x01, 0x01, (byte) 0xAC, (byte) 0x6C, 0x03 }, 0);
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void forDataNoBodyWithinOffsets() {
		byte[] bytes = new byte[] { (byte) 0xFF, 0x02, 0x05, 0x01, 0x02, 0x01, 0x01, (byte) 0xAC,
				(byte) 0x6C, 0x03, (byte) 0xFF };
		Packet p = Packet.forData(bytes, 1, bytes, 5);
		assertThat("Packet valid", p.isValid(), equalTo(true));
		assertThat("Address", p.getHeader().getAddress(), equalTo((short) 1));
		assertThat("Command", p.getCommand(), equalTo((byte) 1));
		assertThat("Sub-command", p.getSubCommand(), equalTo((byte) 1));
		assertThat("CRC", p.getCrc(), equalTo(0x6CAC));
	}

	@Test
	public void forDataWithBody() {
		byte[] bytes = new byte[] { 0x02, 0x05, 0x01, 0x04, 0x01, 0x01, (byte) 0xFF, (byte) 0xFE,
				(byte) 0x35, (byte) 0x5D, 0x03 };
		Packet p = Packet.forData(bytes, 0, bytes, 4);
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void systemIdentificationResponse() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060116", "0000137b55533234302f31323053002c000000000000c43f03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		byte countryCode = body[0];
		byte varient = body[1];
		byte[] stringData = new byte[0];
		for ( int i = 2; i < body.length; i++ ) {
			if ( body[i] == 0x00 ) {
				stringData = new byte[i - 2];
				System.arraycopy(body, 2, stringData, 0, stringData.length);
				break;
			}
		}
		String description = new String(stringData, "US-ASCII");
		assertThat("Country code", countryCode, equalTo((byte) 19));
		assertThat("Varient", varient, equalTo((byte) 123));
		assertThat("Description", description, equalTo("US240/120S"));
	}

	@Test
	public void readSerialNumberRequest() throws DecoderException {
		Packet p = Packet.forCommand(1, 1, 0, "00");
		assertThat("Packet valid", p.isValid(), equalTo(true));
	}

	@Test
	public void readSerialNumberResponse() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("0206010c", "000131343530303030343836cfcc03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		String serialNumber = new String(p.getBody(), "US-ASCII");
		assertThat("Serial number", serialNumber, equalTo("1450000486"));
	}

	@Test
	public void readProductionWeekResponse() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "00023530209503");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		String serialNumber = new String(p.getBody(), "US-ASCII");
		assertThat("Production week", serialNumber, equalTo("50"));
	}

	@Test
	public void readProductionYearResponse() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "00033134725603");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		String serialNumber = new String(p.getBody(), "US-ASCII");
		assertThat("Production year", serialNumber, equalTo("14"));
	}

	@Test
	public void readSystemRevisionResponse() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060105", "004002000de57a03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] versions = p.getBody();
		assertThat("Version length", byteArray(versions), arrayWithSize(3));
		assertThat("System revision major", versions[0], equalTo((byte) 2));
		assertThat("System revision minor", versions[1], equalTo((byte) 0));
		assertThat("System revision patch", versions[2], equalTo((byte) 13));
	}

	@Test
	public void readAmbientTemp() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "03010021078d03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] data = p.getBody();
		assertThat("Body length", byteArray(data), arrayWithSize(2));
		ByteBuffer buf = ByteBuffer.wrap(data);
		assertThat("Ambient temp", buf.getShort(), equalTo((short) 33));
	}

	@Test
	public void readHeatSinkTemp() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "03020021f78d03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] data = p.getBody();
		assertThat("Body length", byteArray(data), arrayWithSize(2));
		ByteBuffer buf = ByteBuffer.wrap(data);
		assertThat("Heat sink temp", buf.getShort(), equalTo((short) 33));
	}

	@Test
	public void readAmbientTemp_neg() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "0301FFFC862403");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] data = p.getBody();
		assertThat("Data length", byteArray(data), arrayWithSize(2));
		ByteBuffer buf = ByteBuffer.wrap(data);
		assertThat("Ambient temp", buf.getShort(), equalTo((short) -4));
	}

	@Test
	public void readLifetimeTotalEnergy() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("0206010a", "1801000000000030ab8026ad03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(8));
		BigInteger energy = new BigInteger(1, body);
		assertThat("Lifetime total energy", energy.intValue(), equalTo(3189632));
	}

	@Test
	public void readDayTotalEnergy() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("0206010a", "1501000000000000a70973fe03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(8));
		BigInteger energy = new BigInteger(1, body);
		assertThat("Day total energy", energy.intValue(), equalTo(42761));
	}

	@Test
	public void readPv1Voltage() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "1C0101A501aa03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 1 voltage V", new BigInteger(1, body).intValue(), equalTo(421));
	}

	@Test
	public void readPv1Current() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "1C02002Eb05d03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 1 current A x10", new BigInteger(1, body).intValue(), equalTo(46));
	}

	@Test
	public void readPv1Power() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "1c050070806403");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		BigInteger power = new BigInteger(1, body);
		assertThat("PV1 Power W", power.intValue(), equalTo(112));
	}

	@Test
	public void readPv2Voltage() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "240101A50cca03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 2 voltage V", new BigInteger(1, body).intValue(), equalTo(421));
	}

	@Test
	public void readPv2Current() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "2402002Ebd3d03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 2 current A x10", new BigInteger(1, body).intValue(), equalTo(46));
	}

	@Test
	public void readPv2Power() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "240500288cfe03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 2 Power W", new BigInteger(1, body).intValue(), equalTo(40));
	}

	@Test
	public void readPv3Voltage() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "2C0101A50eaa03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 3 voltage V", new BigInteger(1, body).intValue(), equalTo(421));
	}

	@Test
	public void readPv3Current() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "2C02002Ebf5d03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 3 current A x10", new BigInteger(1, body).intValue(), equalTo(46));
	}

	@Test
	public void readPv3Power() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "2C0500288e9e03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("PV 3 Power W", new BigInteger(1, body).intValue(), equalTo(40));
	}

	@Test
	public void readAcCombinedVoltage() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "470100e4d32e03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		BigInteger power = new BigInteger(1, body);
		assertThat("AC combined avg voltage", power.intValue(), equalTo(228));
	}

	@Test
	public void readAcCombinedFrequency() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "47021771ecb103");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		BigInteger freq = new BigInteger(1, body);
		assertThat("AC freq Hz x100", freq.intValue(), equalTo(6001));
	}

	@Test
	public void readAcCombinedCurrent() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "47030012f2a803");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		BigInteger current = new BigInteger(1, body);
		assertThat("AC current A x10", current.intValue(), equalTo(18));
	}

	@Test
	public void readAcCombinedActivePower() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "4704002a42bb03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("AC power W", new BigInteger(1, body).intValue(), equalTo(42));
	}

	@Test
	public void readAcCombinedRectivePower() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "4705FE9652aa03");
		assertThat(format("Packet valid (%x)", p.getCalculatedCrc()), p.isValid(), equalTo(true));
		byte[] body = p.getBody();
		assertThat("Body length", byteArray(body), arrayWithSize(2));
		assertThat("AC reactive power VAR", ByteBuffer.wrap(body).getShort(), equalTo((short) -362));
	}

	@Test
	public void readPowerFactor() throws DecoderException, UnsupportedEncodingException {
		Packet p = Packet.forData("02060104", "4706FC1B63AF03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		byte[] data = p.getBody();
		assertThat("Data length", byteArray(data), arrayWithSize(2));
		assertThat("Power factor x1000", ByteBuffer.wrap(data).getShort(), equalTo((short) -997));
	}

	@Test
	public void readPowerFactor_nak() throws DecoderException, UnsupportedEncodingException {
		Packet p = new Packet("02 15 0b 02 47 06 1dd5 03");
		assertThat("Packet valid", p.isValid(), equalTo(true));
		assertThat("Packet not accepted", p.isAcceptedResponse(), equalTo(false));
		byte[] data = p.getBody();
		assertThat("Data length", byteArray(data), arrayWithSize(0));
	}

}
