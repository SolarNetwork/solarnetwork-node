/* ==================================================================
 * ConfigurableSerialNetwork.java - 2/02/2021 10:12:48 AM
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

package net.solarnetwork.node.io.serial;

import net.solarnetwork.node.support.SerialPortBeanParameters;

/**
 * A configurable serial network.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public interface ConfigurableSerialNetwork extends SerialNetwork {

	/**
	 * Get a mutable serial port parameters object.
	 * 
	 * <p>
	 * The returned object allows changing the serial port parameters. Changes
	 * apply to future serial port actions.
	 * </p>
	 * 
	 * @return the mutable parameters
	 */
	SerialPortBeanParameters getSerialParams();
}
