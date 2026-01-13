/* ==================================================================
 * NiftySerialParameters.java - 20/12/2023 6:37:01 am
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

package net.solarnetwork.node.io.modbus.nifty.pjc;

import net.solarnetwork.io.modbus.rtu.netty.NettyRtuModbusClientConfig;
import net.solarnetwork.io.modbus.serial.BasicSerialParameters;
import net.solarnetwork.io.modbus.serial.SerialParity;
import net.solarnetwork.io.modbus.serial.SerialStopBits;
import net.solarnetwork.node.service.support.SerialPortBean;
import net.solarnetwork.util.ObjectUtils;
import purejavacomm.SerialPort;

/**
 * Serial parameters.
 * 
 * <p>
 * This class provides accessors that are compatible with other Modbus plugins
 * so they can be interchanged easily.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class NiftySerialParameters extends BasicSerialParameters {

	private NettyRtuModbusClientConfig config;
	private int flowControlIn;
	private int flowControlOut;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the config
	 */
	public NiftySerialParameters(NettyRtuModbusClientConfig config) {
		super();
		this.config = ObjectUtils.requireNonNullArgument(config, "config");
	}

	/**
	 * Get the serial port device name.
	 * 
	 * <p>
	 * This is an alias for {@link NettyRtuModbusClientConfig#getName()}.
	 * </p>
	 * 
	 * @return the name
	 */
	public String getPortName() {
		return config.getName();
	}

	/**
	 * Set the serial port device name.
	 * 
	 * <p>
	 * This is an alias for {@link NettyRtuModbusClientConfig#setName(String)}.
	 * </p>
	 * 
	 * @param name
	 *        the name to set, e.g. {@literal /dev/ttyUSB0}
	 */
	public void setPortName(String name) {
		config.setName(name);
	}

	/**
	 * Get the serial port data bits.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#getDataBits()}.
	 * </p>
	 * 
	 * @return the number of data bits; defaults to {@literal 8}
	 */
	public int getDatabits() {
		return getDataBits();
	}

	/**
	 * Set the serial port number of data bits.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#setDataBits(int)}.
	 * </p>
	 * 
	 * @param dataBits
	 *        the number of data bits to use
	 */
	public void setDatabits(int dataBits) {
		setDataBits(dataBits);
	}

	/**
	 * Get the serial port number of stop bits.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#getStopBits()}.
	 * </p>
	 * 
	 * @return the stop bits; defaults to {@literal 1}
	 */
	public int getStopbits() {
		SerialStopBits s = getStopBits();
		return (s != null ? s : SerialStopBits.One).getCode();
	}

	/**
	 * Set the serial port number of stop bits.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#setStopBits(int)}.
	 * </p>
	 * 
	 * @param stopBits
	 *        the number of stop bits to use
	 */
	public void setStopbits(int stopBits) {
		SerialStopBits s;
		try {
			s = SerialStopBits.forCode(stopBits);
		} catch ( IllegalArgumentException e ) {
			s = SerialStopBits.One;
		}
		setStopBits(s);
	}

	/**
	 * Convert a flow control name to the int value defined in SerialPort.
	 *
	 * @param name
	 *        the flow control name
	 * @return the flow control value
	 */
	public static int flowControlValue(String name) {
		name = name.toLowerCase();
		switch (name) {
			case "xon/xoff in":
				return SerialPort.FLOWCONTROL_XONXOFF_IN;

			case "xon/xoff out":
				return SerialPort.FLOWCONTROL_XONXOFF_OUT;

			case "rts/cts in":
				return SerialPort.FLOWCONTROL_RTSCTS_IN;

			case "rts/cts out":
				return SerialPort.FLOWCONTROL_RTSCTS_OUT;

			default:
				return SerialPort.FLOWCONTROL_NONE;
		}
	}

	/**
	 * Convert a flow control value into a name.
	 *
	 * @param value
	 *        the flow control value
	 * @return the flow control name
	 */
	public static String toFlowControlName(int value) {
		switch (value) {
			case SerialPort.FLOWCONTROL_XONXOFF_IN:
				return "xon/xoff in";

			case SerialPort.FLOWCONTROL_XONXOFF_OUT:
				return "xon/xoff out";

			case SerialPort.FLOWCONTROL_RTSCTS_IN:
				return "rts/cts in";

			case SerialPort.FLOWCONTROL_RTSCTS_OUT:
				return "rts/cts out";

			default:
				return "none";
		}
	}

	/**
	 * Get the input flow control.
	 * 
	 * @return the input flow control
	 */
	public int getFlowControlIn() {
		return flowControlIn;
	}

	/**
	 * Set the input flow control.
	 * 
	 * @param flowControlIn
	 *        the flow control to set
	 */
	public void setFlowControlIn(int flowControlIn) {
		this.flowControlIn = flowControlIn;
	}

	/**
	 * Get the input flow control name.
	 * 
	 * @return the input flow control
	 */
	public String getFlowControlInString() {
		return toFlowControlName(getFlowControlIn());
	}

	/**
	 * Set the input flow control name.
	 * 
	 * @param flowControlIn
	 *        the flow control to set
	 */
	public void setFlowControlInString(String flowControlIn) {
		setFlowControlIn(flowControlValue(flowControlIn));
	}

	/**
	 * Get the output flow control.
	 * 
	 * @return the output flow control
	 */
	public int getFlowControlOut() {
		return flowControlOut;
	}

	/**
	 * Set the output flow control.
	 * 
	 * @param flowControlOut
	 *        the output flow control to set
	 */
	public void setFlowControlOut(int flowControlOut) {
		this.flowControlOut = flowControlOut;
	}

	/**
	 * Get the output flow control name.
	 * 
	 * @return the output flow control
	 */
	public String getFlowControlOutString() {
		return toFlowControlName(getFlowControlOut());
	}

	/**
	 * Set the output flow control name.
	 * 
	 * @param flowControlOut
	 *        the output flow control to set
	 */
	public void setFlowControlOutString(String flowControlOut) {
		setFlowControlOut(flowControlValue(flowControlOut));
	}

	/**
	 * Set the flow control.
	 * 
	 * @param flowControl
	 *        the flow control
	 */
	public void setFlowControl(int flowControl) {
		if ( (flowControl & SerialPort.FLOWCONTROL_XONXOFF_IN) == SerialPort.FLOWCONTROL_XONXOFF_IN ) {
			flowControlIn = SerialPort.FLOWCONTROL_XONXOFF_IN;
		} else if ( (flowControl
				& SerialPort.FLOWCONTROL_RTSCTS_IN) == SerialPort.FLOWCONTROL_RTSCTS_IN ) {
			flowControlIn = SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_IN;
		} else {
			flowControlIn = SerialPort.FLOWCONTROL_NONE;
		}
		if ( (flowControl & SerialPort.FLOWCONTROL_XONXOFF_OUT) == SerialPort.FLOWCONTROL_XONXOFF_OUT ) {
			flowControlOut = SerialPort.FLOWCONTROL_XONXOFF_OUT;
		} else if ( (flowControl
				& SerialPort.FLOWCONTROL_RTSCTS_OUT) == SerialPort.FLOWCONTROL_RTSCTS_OUT ) {
			flowControlOut = SerialPort.FLOWCONTROL_RTSCTS_OUT;
		} else {
			flowControlOut = SerialPort.FLOWCONTROL_NONE;
		}
	}

	/**
	 * Get the parity as a string value.
	 * 
	 * @return the parity, or {@literal null} if not supported
	 * @see #setParityString(String)
	 */
	public String getParityString() {
		switch (getParity()) {
			case None:
				return "none";

			case Odd:
				return "odd";

			case Even:
				return "even";

			default:
				return null;
		}
	}

	/**
	 * Set the parity as a string value.
	 * 
	 * <p>
	 * This method accepts the following values:
	 * </p>
	 * 
	 * <ul>
	 * <li>none</li>
	 * <li>odd</li>
	 * <li>even</li>
	 * </ul>
	 * 
	 * @param parity
	 *        the parity value to set
	 */
	public void setParityString(String parity) {
		parity = parity.toLowerCase();
		if ( parity.equals("none") ) {
			setParity(SerialParity.None);
		} else if ( parity.equals("odd") ) {
			setParity(SerialParity.Odd);
		} else if ( parity.equals("even") ) {
			setParity(SerialParity.Even);
		}
	}

	/**
	 * Get the receive timeout.
	 * 
	 * <p>
	 * This is an alias for {@link #getReadTimeout()}.
	 * </p>
	 * 
	 * @return the receive timeout; defaults to {@literal -1}
	 */
	public int getReceiveTimeout() {
		return getReadTimeout();
	}

	/**
	 * Set the SerialPort receive timeout setting.
	 * 
	 * <p>
	 * If set to anything less than 0 then the receive timeout will be disabled.
	 * </p>
	 * 
	 * <p>
	 * This is an alias for {@link #setReadTimeout(int)}.
	 * </p>
	 * 
	 * @param receiveTimeout
	 *        the receive timeout to use
	 */
	public void setReceiveTimeout(int receiveTimeout) {
		setReadTimeout(receiveTimeout);
	}

}
