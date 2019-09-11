/* ==================================================================
 * PowerGateFault4.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 4 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault4 implements PowerGateFault {

	GateFeedbackA(0, "Gate feedback fault phase A inverter 1."),

	GateFeedbackB(1, "Gate feedback fault phase B inverter 1."),

	GateFeedbackC(2, "Gate feedback fault phase C inverter 1."),

	GateFeedbackA2(3, "Gate feedback fault phase A inverter 2."),

	GateFeedbackB2(4, "Gate feedback fault phase B inverter 2."),

	GateFeedbackC2(5, "Gate feedback fault phase C inverter 2."),

	DcInputOverCurrent(6, "DC input timed over-current."),

	DcInputOverCurrentInstant(7, "DC input instantaneous over-current."),

	DcUnderVoltageInstant(8, "DC link instantaneous under-voltage."),

	DcOverVoltageInstant(9, "DC link instantaneous over-voltage."),

	InverterOverCurrentSoftware(10, "Inverter software over-current."),

	InverterOverCurrentHardware1(11, "Hardware over-current inverter 1."),

	InverterOverCurrentHardware2(12, "Hardware over-current inverter 2."),

	LineOverCurrent(13, "AC line over-current."),

	CurrentUnbalance(14, "AC line current unbalance.");

	private final int code;
	private final String description;

	private PowerGateFault4(int code, String description) {
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
	public static PowerGateFault4 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault4 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault4 code [" + code + "] not supported");
	}

}
