/* ===================================================================
 * SmaControl.java
 * 
 * Created Sep 7, 2009 10:27:14 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.hw.sma.protocol;

/**
 * SMA packet control enumeration.
 * 
 * <p>
 * Each SMA packet contains a <em>control</em> byte that specifies the desired
 * device to control, or if the packet is a response from a device request.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaControl {

	/** Request from a single device. */
	RequestSingle(0),

	/** Response. */
	Response(64),

	/** Request from all connected devices. */
	RequestGroup(128),

	/** Unknown. */
	Unknown(-1);

	private int code;

	private SmaControl(int code) {
		this.code = code;
	}

	/**
	 * Get the code value for this control type.
	 * 
	 * @return code value
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Get a SmaControl instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the SmaControl
	 */
	public static SmaControl forCode(int code) {
		switch (code) {
			case 0:
				return RequestSingle;

			case 64:
				return Response;

			case 128:
				return RequestGroup;

			default:
				return Unknown;
		}
	}

}
