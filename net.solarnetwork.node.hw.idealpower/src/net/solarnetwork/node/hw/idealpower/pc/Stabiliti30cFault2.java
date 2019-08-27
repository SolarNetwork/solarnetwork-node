/* ==================================================================
 * Stabiliti30cFault0.java - 27/08/2019 5:11:50 pm
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

package net.solarnetwork.node.hw.idealpower.pc;

/**
 * Bitmask enumeration of fault 2 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cFault2 implements Stabiliti30cFault {

	AcOverVoltageLevel3(0, "AC over voltage level 3 trip"),

	AcOverVoltageLevel4(1, "AC over voltage level 4 trip"),

	AcUnderFrequencyLevel1(2, "AC under frequency level 1 trip"),

	AcUnderFrequencyLevel2(3, "AC under frequency level 2 trip"),

	AcUnderFrequencyLevel3(4, "AC under frequency level 3 trip"),

	AcUnderFrequencyLevel4(5, "AC under frequency level 4 trip"),

	AcOverFrequencyLevel1(6, "AC over frequency level 1 trip"),

	AcOverFrequencyLevel2(7, "AC over frequency level 2 trip"),

	AcOverFrequencyLevel3(8, "AC over frequency level 3 trip"),

	AcOverFrequencyLevel4(9, "AC over frequency level 4 trip"),

	WatchdogTimeout(10, "Watchdog timeout"),

	EmergencyStop(11, "Emergency stop active"),

	SensingFault(12, "Sensing fault"),

	ArcFault(13, "Arc fault"),

	CommsProcessorInitiatedShutdown(14, "Comms processor initiated shutdown"),

	Surge(15, "Surge detect");

	private final int code;
	private final String description;

	private Stabiliti30cFault2(int code, String description) {
		this.code = code;
		this.description = description;
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

	@Override
	public int getFaultGroup() {
		return 2;
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
	public static Stabiliti30cFault2 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( Stabiliti30cFault2 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("Stabiliti30cFault2 code [" + code + "] not supported");
	}

}
