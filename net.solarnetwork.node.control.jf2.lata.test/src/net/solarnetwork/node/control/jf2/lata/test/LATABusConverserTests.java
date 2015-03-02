/* ==================================================================
 * LATABusConverserTests.java - Oct 27, 2014 1:28:15 PM
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
import net.solarnetwork.node.control.jf2.lata.LATABusConverser;
import net.solarnetwork.node.control.jf2.lata.command.AddressableCommand;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandInterface;
import net.solarnetwork.node.io.serial.rxtx.test.TestSerialPort;
import net.solarnetwork.node.io.serial.rxtx.test.TestSerialPortConnection;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link LATABusConverser} class.
 * 
 * @author matt
 * @version 1.0
 */
public class LATABusConverserTests extends AbstractNodeTest {

	private static final String SWITCH_1_IDENTIFIER = "100000BD";

	private ByteArrayOutputStream busOutputForCommand(CommandInterface cmd) throws IOException {
		ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
		expectedOutput.write(Command.StartResetMode.getCommandData());
		expectedOutput.write(Command.SetSpeed.getCommandData());
		expectedOutput.write(Command.StartOperationalMode.getCommandData());
		expectedOutput.write(cmd.getCommandData());
		return expectedOutput;
	}

	@Test
	public void writeSwitchOff() throws IOException {
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		final ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
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
				new SerialPortBeanParameters(), null);
		LATABusConverser action = new LATABusConverser(new AddressableCommand(SWITCH_1_IDENTIFIER,
				Command.SwitchOff));
		String result = action.doWithConnection(conn);
		Assert.assertNull(result);

		ByteArrayOutputStream expectedOutput = busOutputForCommand(action.getCommand());
		Assert.assertArrayEquals(expectedOutput.toByteArray(), byos.toByteArray());
	}

	@Test
	public void readSwitch1() throws IOException {
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		final AddressableCommand cmd = new AddressableCommand(SWITCH_1_IDENTIFIER, Command.SwitchStatus);
		final byte[] cmdData = cmd.getCommandData();
		final byte[] msg = ("T" + SWITCH_1_IDENTIFIER + "26464").getBytes();
		final ByteArrayInputStream input = new ByteArrayInputStream(msg);
		final SerialPort serialPort = new TestSerialPort() {

			@Override
			public InputStream getInputStream() throws IOException {
				return input;
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return new OutputStream() {

					@Override
					public void write(int b) throws IOException {
						byos.write(b);
						if ( new String(byos.toByteArray()).endsWith(new String(cmdData)) ) {
							// once we've written our command, reset the input buffer because LATABusConverser drains it
							input.reset();
						}
					}
				};
			}
		};
		TestSerialPortConnection conn = new TestSerialPortConnection(serialPort,
				new SerialPortBeanParameters(), null);
		LATABusConverser action = new LATABusConverser(cmd);
		String result = action.doWithConnection(conn);
		Assert.assertEquals(new String(msg), result);
	}

}
