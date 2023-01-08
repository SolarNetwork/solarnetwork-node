/* ==================================================================
 * PjcModbusSerialPort.java - 23/11/2022 6:25:11 am
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

package net.solarnetwork.node.io.modbus.modbus4j.pjc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;
import purejavacomm.UnsupportedCommOperationException;

/**
 * PJC implementation of {@link SerialPortWrapper}.
 * 
 * @author matt
 * @version 1.0
 */
public class PjcModbusSerialPort implements SerialPortWrapper, SerialPortEventListener {

	private static final Logger log = LoggerFactory.getLogger(PjcModbusSerialPort.class);

	/** A class-level logger with the suffix SERIAL_EVENT. */
	private static final Logger eventLog = LoggerFactory
			.getLogger(PjcModbusSerialPort.class.getName() + ".SERIAL_EVENT");

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
	public PjcModbusSerialPort(SerialPortBeanParameters serialParams) {
		super();
		this.serialParams = serialParams;
	}

	@Override
	public void open() throws Exception {
		if ( serialPort == null ) {
			CommPortIdentifier portId = getCommPortIdentifier(serialParams.getSerialPort());
			try {
				serialPort = (SerialPort) portId.open(serialParams.getCommPortAppName(), 2000);
				setupSerialPortParameters(serialPort, eventLog.isTraceEnabled() ? this : null);
			} catch ( PortInUseException e ) {
				throw new IOException("Serial port " + serialParams.getSerialPort() + " in use", e);
			} catch ( TooManyListenersException e ) {
				try {
					close();
				} catch ( Exception e2 ) {
					// ignore this
				}
				throw new IOException(
						"Serial port " + serialParams.getSerialPort() + " has too many listeners", e);
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

	/**
	 * Set up the SerialPort for use, configuring with class properties.
	 * 
	 * <p>
	 * This method can be called once when wanting to start using the serial
	 * port.
	 * </p>
	 * 
	 * @param serialPort
	 *        the serial port to setup
	 * @param listener
	 *        a listener to pass to
	 *        {@link SerialPort#addEventListener(SerialPortEventListener)}
	 */
	private void setupSerialPortParameters(SerialPort serialPort, SerialPortEventListener listener)
			throws TooManyListenersException {
		if ( listener != null ) {
			serialPort.addEventListener(listener);
		}

		serialPort.notifyOnDataAvailable(true);

		try {

			if ( serialParams.getReceiveFraming() >= 0 ) {
				serialPort.enableReceiveFraming(serialParams.getReceiveFraming());
				if ( !serialPort.isReceiveFramingEnabled() ) {
					log.warn("Receive framing configured as {} but not supported by driver.",
							serialParams.getReceiveFraming());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive framing set to {}", serialParams.getReceiveFraming());
				}
			} else {
				serialPort.disableReceiveFraming();
			}

			if ( serialParams.getReceiveTimeout() >= 0 ) {
				serialPort.enableReceiveTimeout(serialParams.getReceiveTimeout());
				if ( !serialPort.isReceiveTimeoutEnabled() ) {
					log.warn("Receive timeout configured as {} but not supported by driver.",
							serialParams.getReceiveTimeout());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive timeout set to {}", serialParams.getReceiveTimeout());
				}
			} else {
				serialPort.disableReceiveTimeout();
			}
			if ( serialParams.getReceiveThreshold() >= 0 ) {
				serialPort.enableReceiveThreshold(serialParams.getReceiveThreshold());
				if ( !serialPort.isReceiveThresholdEnabled() ) {
					log.warn("Receive threshold configured as [{}] but not supported by driver.",
							serialParams.getReceiveThreshold());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive threshold set to {}", serialParams.getReceiveThreshold());
				}
			} else {
				serialPort.disableReceiveThreshold();
			}

			if ( log.isDebugEnabled() ) {
				log.debug("Setting serial port baud = {}, dataBits = {}, stopBits = {}, parity = {}",
						new Object[] { serialParams.getBaud(), serialParams.getDataBits(),
								serialParams.getStopBits(), serialParams.getParity() });
			}
			serialPort.setSerialPortParams(serialParams.getBaud(), serialParams.getDataBits(),
					serialParams.getStopBits(), serialParams.getParity());

			if ( serialParams.getFlowControl() >= 0 ) {
				log.debug("Setting flow control to {}", serialParams.getFlowControl());
				serialPort.setFlowControlMode(serialParams.getFlowControl());
			}

			if ( serialParams.getDtrFlag() >= 0 ) {
				boolean mode = serialParams.getDtrFlag() > 0 ? true : false;
				log.debug("Setting DTR to {}", mode);
				serialPort.setDTR(mode);
			}
			if ( serialParams.getRtsFlag() >= 0 ) {
				boolean mode = serialParams.getRtsFlag() > 0 ? true : false;
				log.debug("Setting RTS to {}", mode);
				serialPort.setRTS(mode);
			}

		} catch ( UnsupportedCommOperationException e ) {
			throw new RuntimeException(e);
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
			serialPort.close();
			log.trace("Serial port {} closed", this.serialPort);
		} finally {
			in = null;
			out = null;
			serialPort = null;
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if ( eventLog.isTraceEnabled() && event.getEventType() != SerialPortEvent.DATA_AVAILABLE ) {
			eventLog.trace("SerialPortEvent {}");
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

	private CommPortIdentifier getCommPortIdentifier(final String portId) throws IOException {
		// first try directly
		CommPortIdentifier commPortId = null;
		try {
			commPortId = CommPortIdentifier.getPortIdentifier(portId);
			if ( commPortId != null ) {
				log.debug("Found port identifier: {}", portId);
				return commPortId;
			}
		} catch ( NoSuchPortException e ) {
			log.debug("Port {} not found, inspecting available ports...", portId);
		}
		Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
		List<String> foundNames = new ArrayList<String>(5);
		while ( portIdentifiers.hasMoreElements() ) {
			CommPortIdentifier commPort = portIdentifiers.nextElement();
			log.trace("Inspecting available port identifier: {}", commPort.getName());
			foundNames.add(commPort.getName());
			if ( commPort.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& portId.equals(commPort.getName()) ) {
				commPortId = commPort;
				log.debug("Found port identifier: {}", portId);
				break;
			}
		}
		if ( commPortId == null ) {
			throw new IOException("Couldn't find port identifier for [" + portId + "]; available ports: "
					+ foundNames);
		}
		return commPortId;
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
