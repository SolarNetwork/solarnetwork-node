/* ==================================================================
 * TestSerialPortConnection.java - Oct 27, 2014 7:56:52 AM
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

package net.solarnetwork.node.io.serial.rxtx.support;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import gnu.io.SerialPort;
import net.solarnetwork.node.io.serial.rxtx.SerialPortConnection;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;

/**
 * Implementation of {@link SerialPortConnection} for testing.
 * 
 * @author matt
 * @version 2.0
 */
public class TestSerialPortConnection extends SerialPortConnection {

	private final SerialPort serialPort;
	private boolean open = false;

	/**
	 * Constructor.
	 * 
	 * @param serialPort
	 *        the serial port
	 * @param params
	 *        the parameters
	 * @param executor
	 *        the executor
	 */
	public TestSerialPortConnection(SerialPort serialPort, SerialPortBeanParameters params,
			ExecutorService executor) {
		super(params, executor);
		this.serialPort = serialPort;
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		if ( open ) {
			return;
		}
		open = true;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() {
		if ( open ) {
			return;
		}
		open = false;
	}

	@Override
	public SerialPort getSerialPort() {
		return serialPort;
	}

}
