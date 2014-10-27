/* ==================================================================
 * GetVersionActionTests.java - Oct 27, 2014 2:31:02 PM
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

package net.solarnetwork.node.control.jf2.lata.test;

import gnu.io.SerialPort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.solarnetwork.node.control.jf2.lata.GetVersionAction;
import net.solarnetwork.node.io.serial.rxtx.test.TestSerialPort;
import net.solarnetwork.node.io.serial.rxtx.test.TestSerialPortConnection;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link GetVersionAction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GetVersionActionTests {

	@Test
	public void readShortVersion() throws IOException {
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		final byte[] msg = ("V9999\r").getBytes();
		final ByteArrayInputStream input = new ByteArrayInputStream(msg);
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return input;
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return byos;
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		GetVersionAction action = new GetVersionAction(false);
		String result = action.doWithConnection(conn);
		Assert.assertEquals("9999", result);
	}

	@Test
	public void readLongVersion() throws IOException {
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		final byte[] msg = ("v0106\r").getBytes();
		final ByteArrayInputStream input = new ByteArrayInputStream(msg);
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return input;
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return byos;
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters());
		GetVersionAction action = new GetVersionAction(true);
		String result = action.doWithConnection(conn);
		Assert.assertEquals("0106", result);
	}
}
