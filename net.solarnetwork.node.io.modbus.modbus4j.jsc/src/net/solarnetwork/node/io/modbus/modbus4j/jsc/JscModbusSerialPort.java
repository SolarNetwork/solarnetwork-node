/* ==================================================================
 * JscModbusSerialPort.java - 23/11/2022 6:25:11 am
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

package net.solarnetwork.node.io.modbus.modbus4j.jsc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;

/**
 * PJC implementation of {@link SerialPortWrapper}.
 * 
 * @author matt
 * @version 1.0
 */
public class JscModbusSerialPort implements SerialPortWrapper {

	private static final Logger log = LoggerFactory.getLogger(JscModbusSerialPort.class);

	private final SerialPortBeanParameters serialParams;
	private SerialPort serialPort;
	private InputStream in;
	private OutputStream out;

	/**
	 * Constructor.
	 * 
	 * @param serialParams
	 *        the parameters
	 */
	public JscModbusSerialPort(SerialPortBeanParameters serialParams) {
		super();
		this.serialParams = serialParams;
	}

	@Override
	public void open() throws Exception {
		if ( serialPort == null ) {
			try {
				serialPort = SerialPort.getCommPort(serialParams.getSerialPort());
				setupSerialPortParameters(serialPort);
				if ( !serialPort.openPort() ) {
					throw new IOException(
							"Serial port " + serialParams.getSerialPort() + " failed to open");
				}
			} catch ( RuntimeException e ) {
				try {
					close();
				} catch ( Exception e2 ) {
					// ignore this
				}
				throw new IOException(
						"Error opening serial port " + serialParams.getSerialPort() + ":" + e.toString(),
						e);
			}
		}
	}

	/**
	 * Test if the serial port has been opened.
	 * 
	 * @return boolean
	 */
	public boolean isOpen() {
		return (serialPort != null);
	}

	private void setupSerialPortParameters(SerialPort serialPort) {
		if ( log.isDebugEnabled() ) {
			log.debug("Setting serial port baud = {}, dataBits = {}, stopBits = {}, parity = {}",
					new Object[] { serialParams.getBaud(), serialParams.getDataBits(),
							serialParams.getStopBits(), serialParams.getParity() });
		}
		serialPort.setComPortParameters(serialParams.getBaud(), serialParams.getDataBits(),
				serialParams.getStopBits(), serialParams.getParity());

		if ( serialParams.getFlowControl() >= 0 ) {
			log.debug("Setting flow control to {}", serialParams.getFlowControl());
			serialPort.setFlowControl(serialParams.getFlowControl());
		}

		if ( serialParams.getRtsFlag() >= 0 ) {
			boolean mode = serialParams.getRtsFlag() > 0 ? true : false;
			log.debug("Setting RTS to {}", mode);
			serialPort.setRs485ModeParameters(mode, mode, 0, 0);
		}
	}

	@Override
	public void close() throws Exception {
		if ( serialPort == null ) {
			return;
		}
		try {
			if ( in != null ) {
				log.debug("Closing serial port {} InputStream", this.serialPort);
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this
					log.warn("Exception closing serial port {} input stream: {}", this.serialPort,
							e.getMessage());
				}
			}
			if ( out != null ) {
				log.debug("Closing serial port {} OutputStream", this.serialPort);
				try {
					out.close();
				} catch ( IOException e ) {
					// ignore this
					log.warn("Exception closing serial port {} output stream: {}", this.serialPort,
							e.getMessage());
				}
			}
			log.debug("Closing serial port {}", this.serialPort);
			serialPort.closePort();
			log.trace("Serial port {} closed", this.serialPort);
		} finally {
			in = null;
			out = null;
			serialPort = null;
		}
	}

	@Override
	public InputStream getInputStream() {
		if ( in != null ) {
			return in;
		}
		try {
			if ( !isOpen() ) {
				open();
			}
			in = serialPort.getInputStream();
			return in;
		} catch ( Exception e ) {
			throw new RuntimeException("Error opening serial input stream", e);
		}
	}

	@Override
	public OutputStream getOutputStream() {
		if ( out != null ) {
			return out;
		}
		try {
			if ( !isOpen() ) {
				open();
			}
			out = serialPort.getOutputStream();
			return out;
		} catch ( Exception e ) {
			throw new RuntimeException("Error opening serial output stream", e);
		}
	}

	@Override
	public int getBaudRate() {
		return serialParams.getBaud();
	}

	@Override
	public int getDataBits() {
		return serialParams.getDataBits();
	}

	@Override
	public int getStopBits() {
		return serialParams.getStopBits();
	}

	@Override
	public int getParity() {
		return serialParams.getParity();
	}

	/**
	 * Get the serial port.
	 * 
	 * @return the serial port
	 */
	public SerialPort getSerialPort() {
		return serialPort;
	}

}
