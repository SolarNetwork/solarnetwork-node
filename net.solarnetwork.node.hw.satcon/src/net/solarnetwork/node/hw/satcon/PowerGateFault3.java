/* ==================================================================
 * PowerGateFault3.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 3 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault3 implements Fault {

	DpcbIsoPlus5V(0, "DPCB isolated +5V power supply fault."),

	DpcbPlus5V(1, "DPCB +5V power supply fault."),

	DpcbPlus15V(2, "DPCB +15V power supply fault."),

	DpcbMinus15V(3, "DPCB -15V power supply fault."),

	FpgaWatchdog(4, "FPGA watchdog timer fault."),

	SurgeSuppressor(5, "AC or DC surge suppressor fault."),

	InverterFuse1(6, "Inverter fuse 1 open."),

	InverterFuse2(7, "Inverter fuse 2 open."),

	InverterOverTemperature1(8, "Inverter hardware over-temperature 1."),

	InverterOverTemperature2(9, "Inverter hardware over-temperature 2."),

	TransformerOverTemperature(10, "Isolation transformer over-temperature."),

	ReactorOverTemperature(11, "AC filter reactor over-temperature."),

	PrechargeFault(12, "Precharge circuit fault."),

	TestModeFault(13, "Test mode fault."),

	OpenCircuitTestModeFault(14, "Open circuit test mode fault."),

	ShortCircuitTestModeFault(15, "Short circuit test mode fault.");

	private final int code;
	private final String description;

	private PowerGateFault3(int code, String description) {
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
	public static PowerGateFault3 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault3 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault3 code [" + code + "] not supported");
	}

}
