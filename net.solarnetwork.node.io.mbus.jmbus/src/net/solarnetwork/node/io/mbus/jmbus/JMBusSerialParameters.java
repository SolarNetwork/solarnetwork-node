/* ==================================================================
 * JMBusSerialWMBusParameters.java - 06/07/2020 09:31:19 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.jmbus;

/**
 * 
 * Java bean for JMBus serial WMBus parameters
 * 
 * @author alex
 * @version 1.1
 */
public class JMBusSerialParameters {

	private String portName = "/dev/ttyS0";
	private int baudRate = 9600;

	/**
	 * Constructor.
	 */
	public JMBusSerialParameters() {
		super();
	}

	/**
	 * Set port name
	 * 
	 * @param portName
	 *        the port name to set
	 */
	public void setPortName(String portName) {
		this.portName = portName;
	}

	/**
	 * Get the port name
	 * 
	 * @return port name
	 */
	public String getPortName() {
		return portName;
	}

	/**
	 * Set the baud rate
	 * 
	 * @param baudRate
	 *        the baud rate to set
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	/**
	 * Get the baud rate
	 * 
	 * @return baud rate
	 */
	public int getBaudRate() {
		return baudRate;
	}
}
