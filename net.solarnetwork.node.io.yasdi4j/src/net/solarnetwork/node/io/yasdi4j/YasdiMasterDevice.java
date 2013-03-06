/* ==================================================================
 * YasdiMasterDevice.java - Mar 7, 2013 10:00:07 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.yasdi4j;

import de.michaeldenk.yasdi4j.YasdiDevice;

/**
 * Implementation of {@link YasdiMaster}.
 * 
 * @author matt
 * @version 1.0
 */
public class YasdiMasterDevice implements YasdiMaster {

	private final YasdiDevice device;

	/**
	 * Constructor.
	 * 
	 * @param device
	 *        the device
	 */
	public YasdiMasterDevice(YasdiDevice device) {
		super();
		this.device = device;
	}

	@Override
	public YasdiDevice getDevice() {
		return device;
	}

}
