/* ==================================================================
 * ION6200Data.java - 14/05/2018 1:17:03 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.meter;

import net.solarnetwork.node.io.modbus.ModbusData;

/**
 * Data object for the ION6200 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class ION6200Data extends ModbusData {

	private final boolean megawatt;

	/**
	 * Default constructor.
	 */
	public ION6200Data() {
		this(false);
	}

	/**
	 * Default constructor.
	 * 
	 * @boolean megawatt {@literal true} if this data is from the Megawatt
	 *          version of the 6200 meter
	 */
	public ION6200Data(boolean megawatt) {
		super();
		this.megawatt = megawatt;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the modbus data to copy
	 */
	public ION6200Data(ModbusData other) {
		super(other);
		this.megawatt = (other instanceof ION6200Data ? ((ION6200Data) other).megawatt : false);
	}

}
