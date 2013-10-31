/* ===================================================================
 * SmaCommand.java
 * 
 * Created Sep 7, 2009 10:27:08 AM
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
 * An SMA command.
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaCommand {

	/** The "net start" command, to initiate communication. */
	NetStart(6),

	/** Get all available channel info. */
	GetChannelInfo(9),

	/** Synchronize channels for reading data. */
	SynOnline(10),

	/** Read data from a channel. */
	GetData(11),

	/** Set a data value on a channel. */
	SetData(12),

	/** Unknown command. */
	Unknown(-1);

	private int code;

	private SmaCommand(int code) {
		this.code = code;
	}

	/**
	 * Get the channel type code value.
	 * 
	 * @return code value
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Get a SmaCommand instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the SmaCommand
	 */
	public static SmaCommand forCode(int code) {
		switch (code) {
			case 6:
				return NetStart;

			case 9:
				return GetChannelInfo;

			case 10:
				return SynOnline;

			case 11:
				return GetData;

			case 12:
				return SetData;

			default:
				return Unknown;
		}
	}

}
