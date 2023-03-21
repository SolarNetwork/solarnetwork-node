/* ==================================================================
 * KTLCTFault.java - 17/03/2023 6:23:19 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import net.solarnetwork.domain.GroupedBitmaskable;

/**
 * API for KTL CT faults.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public interface KTLCTFault extends GroupedBitmaskable {

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	String getDescription();

	@Override
	default int getGroupSize() {
		return 16;
	}

}
