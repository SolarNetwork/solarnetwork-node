/* ==================================================================
 * PowerGateFault5.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 5 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault5 implements PowerGateFault {

	InternalAirHighTemperature(0, "Internal air high temperature fault."),

	InverterAirHighTemperature(1, "Inverter air high temperature fault."),

	HeatsinkHighTemperature1(2, "Heatsink 1 high temperature fault."),

	HeatsinkHighTemperature2(3, "Heatsink 2 high temperature fault."),

	HeatsinkHighTemperature3(4, "Heatsink 3 high temperature fault."),

	HeatsinkHighTemperature4(5, "Heatsink 4 high temperature fault."),

	HeatsinkHighTemperature5(6, "Heatsink 5 high temperature fault."),

	HeatsinkHighTemperature6(7, "Heatsink 6 high temperature fault."),

	InternalAirLowTemperature(8, "Internal air low temperature fault."),

	InverterAirLowTemperature(9, "Inverter air low temperature fault."),

	HeatsinkLowTemperature1(10, "Heatsink 1 low temperature fault."),

	HeatsinkLowTemperature2(11, "Heatsink 2 low temperature fault."),

	HeatsinkLowTemperature3(12, "Heatsink 3 low temperature fault."),

	HeatsinkLowTemperature4(13, "Heatsink 4 low temperature fault."),

	HeatsinkLowTemperature5(14, "Heatsink 5 low temperature fault."),

	HeatsinkLowTemperature6(15, "Heatsink 6 low temperature fault.");

	private final int code;
	private final String description;

	private PowerGateFault5(int code, String description) {
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
	public static PowerGateFault5 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault5 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault5 code [" + code + "] not supported");
	}

}
