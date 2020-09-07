/* ==================================================================
 * CozIrMode.java - 27/08/2020 5:16:07 PM
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

package net.solarnetwork.node.hw.gss.co2;

import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of CozIR operational modes.
 * 
 * @author matt
 * @version 1.0
 */
public enum CozIrMode implements CodedValue {

	/** Command mode, with measurements disabled. */
	Command(0),

	/** Streaming measurements mode. */
	Streaming(1),

	/** Polling measurement mode. */
	Polling(2);

	private int code;

	private CozIrMode(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the status, never {@literal null} and set to {@link #Command} if
	 *         not any other valid code
	 */
	public static CozIrMode forCode(int code) {
		final byte c = (byte) code;
		for ( CozIrMode v : values() ) {
			if ( v.code == c ) {
				return v;
			}
		}
		return CozIrMode.Command;
	}

}
