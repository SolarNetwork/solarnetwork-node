/* ==================================================================
 * BasicCanbusFrame.java - 21/11/2019 1:25:42 pm
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

package net.solarnetwork.node.io.canbus.cannelloni;

import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameFlag;
import net.solarnetwork.util.ByteUtils;

/**
 * Basic implementation of {@link CanbusFrame}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCanbusFrame implements CanbusFrame {

	private final int address;
	private final byte fdFlags;
	private final byte data[];

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the 32-bit CAN address (ID) and frame flags
	 * @param fdFlags
	 *        the CAN FD flags, or {@literal 0}
	 * @param data
	 *        the message data
	 */
	public BasicCanbusFrame(int address, byte fdFlags, byte[] data) {
		super();
		this.address = address;
		this.fdFlags = fdFlags;
		this.data = data;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("CanbusFrame{address=");
		buf.append(Integer.toHexString(getAddress()));
		if ( isErrorMessage() ) {
			buf.append(", ERR");
		}
		if ( isRemoteTransmissionRequest() ) {
			buf.append(", RTR");
		}
		if ( fdFlags > 0 ) {
			buf.append(", fdFlags=");
			buf.append(fdFlags);
		}
		if ( data != null ) {
			buf.append(", ");
			buf.append("data=");
			buf.append(ByteUtils.encodeHexString(data, 0, data.length, true));
		}
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int getAddress() {
		return address & MAX_EXTENDED_ADDRESS;
	}

	/**
	 * Get the CAN FD flags, or {@literal 0}.
	 * 
	 * @return the flags
	 */
	public byte getFdFlags() {
		return fdFlags;
	}

	@Override
	public int getDataLength() {
		return (data != null ? data.length : 0);
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public boolean isExtendedAddress() {
		return isFlagged(CanbusFrameFlag.ExtendedFormat);
	}

	@Override
	public boolean isFlagged(CanbusFrameFlag flag) {
		final int mask = 1 << flag.bitmaskBitOffset();
		return (address & mask) == mask;
	}

	/**
	 * Test if the frame is an error frame.
	 * 
	 * @return {@literal true} if the frame is an error message
	 */
	public boolean isErrorMessage() {
		return isFlagged(CanbusFrameFlag.ErrorMessage);
	}

	/**
	 * Test if the frame is a remote transmission request.
	 * 
	 * @return {@literal true} if the frame is a remote transmission request
	 */
	public boolean isRemoteTransmissionRequest() {
		return isFlagged(CanbusFrameFlag.RemoteTransmissionRequest);
	}

}
