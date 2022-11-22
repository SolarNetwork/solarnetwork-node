/* ==================================================================
 * SerialParameters.java - 23/11/2022 6:37:01 am
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

import net.solarnetwork.node.service.support.SerialPortBean;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
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
public class SerialParameters extends SerialPortBeanParameters {

	private int flowControlIn;
	private int flowControlOut;

	@Override
	public SerialParameters clone() {
		return (SerialParameters) super.clone();
	}

	/**
	 * Get the serial port device name.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBeanParameters#getSerialPort()}.
	 * </p>
	 * 
	 * @return the name
	 */
	public String getPortName() {
		return getSerialPort();
	}

	/**
	 * Set the serial port device name.
	 * 
	 * <p>
	 * This is an alias for
	 * {@link SerialPortBeanParameters#setSerialPort(String)}.
	 * </p>
	 * 
	 * @param name
	 *        the name to set, e.g. {@literal /dev/ttyUSB0}
	 */
	public void setPortName(String name) {
		setSerialPort(name);
	}

	/**
	 * Get the SerialPort communication speed.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#getBaud()}.
	 * </p>
	 * 
	 * @return the baud; defaults to {@literal 19200}
	 */
	public int getBaudRate() {
		return getBaud();
	}

	/**
	 * Set the SerialPort communication speed.
	 * 
	 * <p>
	 * This is an alias for {@link SerialPortBean#setBaud(int)}.
	 * </p>
	 * 
	 * @param baud
	 *        the baud to use
	 */
	public void setBaudRate(int baud) {
		setBaud(baud);
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
		return getStopBits();
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
		setStopBits(stopBits);
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
			case SerialPort.FLOWCONTROL_NONE:
				return "none";

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
		super.setFlowControl(flowControlIn | flowControlOut);
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
		super.setFlowControl(flowControlIn | flowControlOut);
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

	@Override
	public void setFlowControl(int flowControl) {
		super.setFlowControl(flowControl);
		if ( (flowControl & SerialPort.FLOWCONTROL_XONXOFF_IN) == SerialPort.FLOWCONTROL_XONXOFF_IN ) {
			flowControlIn = SerialPort.FLOWCONTROL_XONXOFF_IN;
		} else if ( (flowControl
				& SerialPort.FLOWCONTROL_RTSCTS_IN) == SerialPort.FLOWCONTROL_RTSCTS_IN ) {
			flowControlIn = SerialPort.FLOWCONTROL_RTSCTS_IN;
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

}
