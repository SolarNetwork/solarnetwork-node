/* ==================================================================
 * ModbusServerUtils.java - 18/09/2020 8:40:23 AM
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

package net.solarnetwork.node.io.modbus.server.impl;

import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;

/**
 * Utilities for the Modbus server.
 * 
 * @author matt
 * @version 1.0
 */
public final class ModbusServerUtils {

	private ModbusServerUtils() {
		// don't construct me
	}

	/**
	 * Prepare a response from a request.
	 * 
	 * @param req
	 *        the request
	 * @param res
	 *        the response
	 */
	public static void prepareResponse(ModbusRequest req, ModbusResponse res) {
		if ( req.isHeadless() ) {
			res.setHeadless();
		} else {
			res.setTransactionID(req.getTransactionID());
			res.setProtocolID(req.getProtocolID());
		}
		res.setUnitID(req.getUnitID());
	}

}
