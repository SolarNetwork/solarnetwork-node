/* ==================================================================
 * SerialConnectionProvider.java - 13/01/2026 6:52:22 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.nifty.rtu;

import net.solarnetwork.io.modbus.serial.SerialParameters;
import net.solarnetwork.io.modbus.serial.SerialPortProvider;
import net.solarnetwork.service.Identifiable;

/**
 * API for a service that provides serial parameters.
 *
 * @author matt
 * @version 1.0
 */
public interface SerialConnectionProvider extends Identifiable {

	/**
	 * Get the serial port device name.
	 *
	 * @return the port device name
	 */
	String serialPortName();

	/**
	 * Get the serial parameters.
	 *
	 * @return the parameters
	 */
	SerialParameters serialParameters();

	/**
	 * Get the serial port provider.
	 *
	 * @return the serial port provider
	 */
	SerialPortProvider serialPortProvider();

}
