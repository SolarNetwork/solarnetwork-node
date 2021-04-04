/* ==================================================================
 * SmaDeviceKind.java - 14/09/2020 9:48:37 AM
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

package net.solarnetwork.node.hw.sma.domain;

import net.solarnetwork.domain.CodedValue;

/**
 * API for an SMA device kind.
 * 
 * <p>
 * This API is designed so that multiple {@link CodedValue} implementations can
 * be used, as SMA redesigned the codes assigned to devices.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaDeviceKind extends CodedValue {

	/**
	 * Get a description of this device kind.
	 * 
	 * @return a description, never {@literal null}
	 */
	String getDescription();

}
