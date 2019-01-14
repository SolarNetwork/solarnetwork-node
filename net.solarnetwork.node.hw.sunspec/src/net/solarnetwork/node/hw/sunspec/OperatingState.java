/* ==================================================================
 * OperatingState.java - 5/10/2018 4:34:08 PM
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

package net.solarnetwork.node.hw.sunspec;

/**
 * API for an operational state.
 * 
 * @author matt
 * @version 1.0
 */
public interface OperatingState {

	/**
	 * Get the state code.
	 * 
	 * @return the code
	 */
	int getCode();

	/**
	 * Get a description of the state.
	 * 
	 * @return a description
	 */
	String getDescription();

}
