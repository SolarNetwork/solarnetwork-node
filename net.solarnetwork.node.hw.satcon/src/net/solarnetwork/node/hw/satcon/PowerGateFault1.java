/* ==================================================================
 * PowerGateFault1.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 1 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault1 implements Fault {

	DcInputOverVoltage(
			0,
			"DC input voltage above DC Input Overvoltage Trip for more than DC Input Overvoltage Delay."),

	DcInputUnderVoltage(
			1,
			"DC input voltage below DC Input Undervoltage Trip for more than DC Input Undervoltage Delay."),

	DcOverVoltage(
			2,
			"DC link voltage above DC Link Overvoltage Trip for more than DC Link Overvoltage Delay."),

	DcUnderVoltage(
			3,
			"DC link voltage below DC Link Undervoltage Trip for more than DC Link Undervoltage Delay."),

	DcGroundFault(4, "DC ground overcurrent detected."),

	LineOverVoltageSlow(
			5,
			"Line voltage above Line Overvoltage Trip Slow for more than Line Overvoltage Delay Slow."),

	LineOverVoltageFast(
			6,
			"Line voltage above Line Overvoltage Trip Fast for more than Line Overvoltage Delay Fast."),

	LineUnderVoltageSlow(
			7,
			"Line voltage below Line Undervoltage Trip Slow for more than Line Undervoltage Delay Slow."),

	LineUnderVoltageFast(
			8,
			"Line voltage below Line Overvolt Trip Fast for more than Line Overvolt Delay."),

	VoltageUnbalance(
			9,
			"Line voltage unbalance above Voltage Unbalance Trip for more than Voltage Unbalance Delay."),

	LineOverFrequency(
			10,
			"Line frequency more than Line Overfrequency Trip above rated for more than Line Overfrequency Delay."),

	LineUnderFrequencySlow(
			11,
			"Line frequency more than Line Underfrequency Trip below rated for more than Line Underfrequency Delay."),

	LineUnderFrequencyFast(12, "Line frequency more than 3.0 Hz below rated."),

	NeutralOverCurrent(
			13,
			"Neutral current above Neutral Overcurrent Trip for more than Neutral Overcurrent Delay.");

	private final int code;
	private final String description;

	private PowerGateFault1(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 1;
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
	public static PowerGateFault1 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault1 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault1 code [" + code + "] not supported");
	}

}
