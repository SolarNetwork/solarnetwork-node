/* ==================================================================
 * SmaChannelTypeGroup.java - Nov 1, 2013 9:23:34 AM
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

package net.solarnetwork.node.hw.sma.protocol;

/**
 * The SMA channel group type.
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaChannelTypeGroup {

	/** Input type. */
	Input(1),

	/** Output type. */
	Output(2),

	/** Parameter type. */
	Parameter(4),

	/** Instantaneous value type. */
	InstantaneousValue(8),

	/** Archive data type. */
	ArchiveData(16),

	/** Test type. */
	Test(32),

	/** Unknown type. */
	Unknown(-1);

	private short code;

	private SmaChannelTypeGroup(int code) {
		this.code = (short) code;
	}

	/**
	 * Get the channel type code value.
	 * 
	 * @return code value
	 */
	public short getCode() {
		return this.code;
	}

	/**
	 * Get an SmaChannelType instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the SmaChannelTypeGroup
	 */
	public static SmaChannelTypeGroup forCode(int code) {
		switch (code) {
			case 1:
				return Input;

			case 2:
				return Output;

			case 4:
				return Parameter;

			case 8:
				return InstantaneousValue;

			case 16:
				return ArchiveData;

			case 32:
				return Test;

			default:
				return Unknown;
		}
	}

}
