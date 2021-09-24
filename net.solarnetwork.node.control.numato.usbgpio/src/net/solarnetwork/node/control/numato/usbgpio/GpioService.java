/* ==================================================================
 * GpioService.java - 24/09/2021 2:17:26 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio;

import java.io.IOException;
import java.util.BitSet;

/**
 * Service API for manipulating GPIO.
 * 
 * @author matt
 * @version 1.0
 */
public interface GpioService {

	/**
	 * Get the device version information.
	 * 
	 * @return the device version
	 * @throws IOException
	 *         if a communication error occurs
	 */
	String getDeviceVersion() throws IOException;

	/**
	 * Get the ID of the device.
	 * 
	 * @return the ID, an 8-character string
	 * @throws IOException
	 *         if a communication error occurs
	 */
	String getId() throws IOException;

	/**
	 * Set the ID of the device.
	 * 
	 * @param id
	 *        the ID to set: an 8-character string
	 * @throws IOException
	 *         if a communication error occurs
	 */
	void setId(String id) throws IOException;

	/**
	 * Get the value of the GPIO address as a binary value.
	 * 
	 * @param address
	 *        the address, starting from {@literal 0}, of the GPIO to get
	 * @return {@literal true} if the GPIO is on, otherwise {@literal false}
	 * @throws IOException
	 *         if a communication error occurs
	 */
	boolean read(int address) throws IOException;

	/**
	 * Get the analog value of the GPIO address.
	 * 
	 * @param address
	 *        the address
	 * @return the analog value
	 * @throws IOException
	 *         if a communication error occurs
	 */
	int readAnalog(int address) throws IOException;

	/**
	 * Read the value of all GPIO addresses.
	 * 
	 * @return the set of enabled GPIO addresses
	 * @throws IOException
	 *         if a communication error occurs
	 */
	BitSet readAll() throws IOException;

	/**
	 * Set the value of the GPIO address as a binary value.
	 * 
	 * @param address
	 *        the address, starting from {@literal 0}, of the GPIO to get
	 * @param value
	 *        {@literal true} if the GPIO is on, otherwise {@literal false}
	 * @throws IOException
	 *         if a communication error occurs
	 */
	void set(int address, boolean value) throws IOException;

	/**
	 * Set the value of all GPIO addresses on enabled/disabled based on a
	 * {@link BitSet}.
	 * 
	 * @param values
	 *        the value to set, with bit positions representing GPIO addresses
	 * @throws IOException
	 *         if a communication error occurs
	 */
	void writeAll(BitSet values) throws IOException;

	/**
	 * Configure a bitmask to use on future
	 * {@link #configureIoDirection(BitSet)} or {@link #writeAll(BitSet)} calls.
	 * 
	 * @param set
	 * @throws IOException
	 */
	void configureWriteMask(BitSet set) throws IOException;

	/**
	 * Configure the GPIO direction of all GPIO addresses.
	 * 
	 * @param set
	 *        the GPIO addresses to enable, using bit positions
	 * @throws IOException
	 *         if a communication error occurs
	 */
	void configureIoDirection(BitSet set) throws IOException;

}
