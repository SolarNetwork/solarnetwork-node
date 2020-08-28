/* ==================================================================
 * CozIrService.java - 28/08/2020 4:59:39 PM
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

package net.solarnetwork.node.datum.gss.cozir;

import java.io.IOException;

/**
 * Service API for the CozIR device.
 * 
 * @author matt
 * @version 1.0
 */
public interface CozIrService {

	/**
	 * Calibrate the CO2 level to "fresh air" level.
	 * 
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void calibrateAsCo2FreshAirLevel() throws IOException;

}
