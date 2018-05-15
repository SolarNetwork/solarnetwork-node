/* ==================================================================
 * DataUtilsTests.java - 15/05/2018 4:59:39 PM
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.UnsupportedEncodingException;
import org.junit.Test;
import net.solarnetwork.node.hw.yaskawa.ecb.DataUtils;

/**
 * Test cases for the {@link DataUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DataUtilsTests {

	@Test
	public void calcualteCrcSimple() throws UnsupportedEncodingException {
		byte[] bytes = "123456789".getBytes("US-ASCII");
		int result = DataUtils.crc16(bytes, 0, 9);
		assertThat("CRC", result, equalTo(0xBB3D));
	}

	@Test
	public void calcualteCrcOffsetPrefix() throws UnsupportedEncodingException {
		byte[] bytes = "0000123456789".getBytes("US-ASCII");
		int result = DataUtils.crc16(bytes, 4, 9);
		assertThat("CRC", result, equalTo(0xBB3D));
	}

	@Test
	public void calcualteCrcOffsetMiddle() throws UnsupportedEncodingException {
		byte[] bytes = "00001234567890000".getBytes("US-ASCII");
		int result = DataUtils.crc16(bytes, 4, 9);
		assertThat("CRC", result, equalTo(0xBB3D));
	}

	@Test
	public void calcualteCrcOffsetSuffix() throws UnsupportedEncodingException {
		byte[] bytes = "1234567890000".getBytes("US-ASCII");
		int result = DataUtils.crc16(bytes, 0, 9);
		assertThat("CRC", result, equalTo(0xBB3D));
	}

	@Test
	public void calcualteCrcNotEnoughData() throws UnsupportedEncodingException {
		byte[] bytes = "123".getBytes("US-ASCII");
		int result = DataUtils.crc16(bytes, 0, 9);
		assertThat("CRC", result, equalTo(0x0000));
	}

	@Test
	public void calculateCrcEmptyPacket() {
		byte[] bytes = new byte[] { 0x02, 0x05, 0x01, 0x00, 0x01, 0x01 };
		int result = DataUtils.crc16(bytes, 1, bytes.length - 1);
		assertThat("CRC", result, equalTo(0xAC0D));
	}

}
