/* ==================================================================
 * PowerGateFault6.java - 11/09/2019 11:06:27 am
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

package net.solarnetwork.node.hw.satcon;

/**
 * Bitmask enumeration of fault 6 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault6 implements PowerGateFault {

	FanFault1(0, "Variable speed fan fault inverter 1."),

	FanFault2(1, "Variable speed fan fault inverter 2."),

	DcInputOpen(2, "DC input contactor open when it should be closed."),

	DcInputClosed(3, "DC input contactor closed when it should be open."),

	AcOutputOpen(4, "AC output contactor open when it should be closed."),

	AcOutputClosed(5, "AC output contactor closed when it should be open.");

	private final int code;
	private final String description;

	private PowerGateFault6(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 3;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int bitmaskBitOffset() {
		return code;
	}

	/**
	 * Get an enum for a code value.
	 * 
	 * @param code
	 *        the code to get an enum for
	 * @return the enum with the given {@code code}, or {@literal null} if
	 *         {@code code} is {@literal 0}
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PowerGateFault6 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault6 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault6 code [" + code + "] not supported");
	}

}
