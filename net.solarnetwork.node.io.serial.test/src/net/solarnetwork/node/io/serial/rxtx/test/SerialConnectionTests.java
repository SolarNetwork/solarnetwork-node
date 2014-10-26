/* ==================================================================
 * SerialConnectionTests.java - Oct 27, 2014 7:12:50 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.rxtx.test;

import gnu.io.SerialPort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.node.io.serial.SerialUtils;
import net.solarnetwork.node.io.serial.rxtx.SerialPortConnection;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

/**
 * Test cases for the {@link SerialPortConnection} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SerialConnectionTests extends AbstractNodeTest {

	@Test
	public void readMarkedMessageSmallChunks() throws IOException {
		final byte[] xml = FileCopyUtils
				.copyToByteArray(getClass().getResourceAsStream("message-1.xml"));
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return new TestSerialPortInputStream(new ByteArrayInputStream(xml), 0, 4, 0);
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		byte[] result = conn.readMarkedMessage("<msg>".getBytes(SerialUtils.ASCII_CHARSET),
				"</msg>".getBytes(SerialUtils.ASCII_CHARSET));
		Assert.assertArrayEquals(xml, result);
	}

	@Test
	public void readMarkedMessageLargeChunks() throws IOException {
		final byte[] xml = FileCopyUtils
				.copyToByteArray(getClass().getResourceAsStream("message-1.xml"));
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return new TestSerialPortInputStream(new ByteArrayInputStream(xml), 0, 64, 0);
			}
		};
		SerialPortBeanParameters serialParams = new SerialPortBeanParameters();
		serialParams.setReceiveThreshold(64);
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort, serialParams);
		byte[] result = conn.readMarkedMessage("<msg>".getBytes(SerialUtils.ASCII_CHARSET),
				"</msg>".getBytes(SerialUtils.ASCII_CHARSET));
		Assert.assertArrayEquals(xml, result);
	}

	@Test
	public void readMarkedMessageMessy() throws IOException {
		final byte[] xml = FileCopyUtils
				.copyToByteArray(getClass().getResourceAsStream("message-1.xml"));
		final byte[] msg = FileCopyUtils
				.copyToByteArray(getClass().getResourceAsStream("message-2.txt"));
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return new TestSerialPortInputStream(new ByteArrayInputStream(msg), 0, 4, 0);
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		byte[] result = conn.readMarkedMessage("<msg>".getBytes(SerialUtils.ASCII_CHARSET),
				"</msg>".getBytes(SerialUtils.ASCII_CHARSET));
		Assert.assertArrayEquals(xml, result);
	}

	@Test
	public void readFixedMarkedMessage() throws IOException {
		final byte[] msg = { 'T', 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return new TestSerialPortInputStream(new ByteArrayInputStream(msg), 0, 4, 0);
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		byte[] result = conn.readMarkedMessage(new byte[] { msg[0] }, 10);
		Assert.assertArrayEquals(msg, result);
	}

	@Test
	public void drainInputBuffer() throws IOException {
		final byte[] msg = { 1, 2, 3 };
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return new TestSerialPortInputStream(new ByteArrayInputStream(msg), 0, 4, 0);
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		byte[] result = conn.drainInputBuffer();
		Assert.assertArrayEquals(msg, result);
	}

}
