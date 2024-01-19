/* ==================================================================
 * JMBusWirelessParameters.java - 06/07/2020 09:42:32 am
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

import org.openmuc.jmbus.wireless.WMBusConnection.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusMode;

/**
 * 
 * Java bean for JMBus common WMBus parameters
 * 
 * @author alex
 * @version 1.1
 */
public class JMBusWirelessParameters {

	private WMBusManufacturer manufacturer = WMBusManufacturer.AMBER;
	private WMBusMode mode = WMBusMode.C;

	/**
	 * Constructor.
	 */
	public JMBusWirelessParameters() {
		super();
	}

	/**
	 * Set the manufacturer
	 * 
	 * @param manufacturer
	 *        the manufacturer to set
	 */
	public void setManufacturer(WMBusManufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}

	/**
	 * Set the manufacturer from a String
	 * 
	 * @param manufacturer
	 *        the manufacturer string value to set
	 */
	public void setManufacturerString(String manufacturer) {
		manufacturer = manufacturer.toLowerCase();
		if ( manufacturer.equals("imst") ) {
			this.manufacturer = WMBusManufacturer.IMST;
		} else if ( manufacturer.equals("rc") ) {
			this.manufacturer = WMBusManufacturer.RADIO_CRAFTS;
		} else {
			this.manufacturer = WMBusManufacturer.AMBER;
		}
	}

	/**
	 * Get the manufacturer
	 * 
	 * @return manufacturer
	 */
	public WMBusManufacturer getManufacturer() {
		return manufacturer;
	}

	/**
	 * Get the manufacturer as a String
	 * 
	 * @return manufacturer string
	 */
	public String getManufacturerString() {
		switch (manufacturer) {
			case AMBER:
				return "amber";
			case IMST:
				return "imst";
			case RADIO_CRAFTS:
				return "rc";
			default:
				return "amber";
		}
	}

	/**
	 * Set the mode
	 * 
	 * @param mode
	 *        the mode to set
	 */
	public void setMode(WMBusMode mode) {
		this.mode = mode;
	}

	/**
	 * Set the mode from a String
	 * 
	 * @param mode
	 *        the most to set, as a string
	 */
	public void setModeString(String mode) {
		mode = mode.toLowerCase();
		if ( mode.contentEquals("compact") ) {
			this.mode = WMBusMode.C;
		} else if ( mode.contentEquals("stationary") ) {
			this.mode = WMBusMode.S;
		} else {
			this.mode = WMBusMode.T;
		}
	}

	/**
	 * Get the mode
	 * 
	 * @return mode
	 */
	public WMBusMode getMode() {
		return mode;
	}

	/**
	 * Get the mode as a string
	 * 
	 * @return mode string
	 */
	public String getModeString() {
		switch (mode) {
			case C:
				return "compact";
			case S:
				return "stationary";
			case T:
				return "frequent";
			default:
				return "frequent";
		}
	}
}
