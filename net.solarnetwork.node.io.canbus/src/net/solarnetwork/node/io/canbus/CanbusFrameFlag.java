/* ==================================================================
 * CanbusFrameFlag.java - 22/11/2019 11:24:08 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus;

import net.solarnetwork.domain.Bitmaskable;

/**
 * Flags applied to a {@link CanbusFrame}.
 * 
 * <p>
 * This enumeration implements {@link Bitmaskable} with bit indexes structured
 * to match the SocketCAN frame structure used in Linux. The bitmask values
 * might not apply to other contexts.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum CanbusFrameFlag implements Bitmaskable {

	/** Error message frame. */
	ErrorMessage(29),

	/** Remote transmission request. */
	RemoteTransmissionRequest(30),

	/** Extended (29-bit address) vs standard (11-bit address) frame format. */
	ExtendedFormat(31);

	private final int index;

	private CanbusFrameFlag(int index) {
		this.index = index;
	}

	@Override
	public int bitmaskBitOffset() {
		return index;
	}

}
