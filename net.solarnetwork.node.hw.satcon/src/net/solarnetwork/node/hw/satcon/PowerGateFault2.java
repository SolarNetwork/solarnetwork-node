/* ==================================================================
 * PowerGateFault2.java - 11/09/2019 11:06:27 am
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
 * Bitmask enumeration of fault 2 codes.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGateFault2 implements PowerGateFault {

	ProgramChecksum(0, "Program checksum error."),

	FpgaVersionMismtach(1, "FPGA version not compatible with firmware."),

	DataCopy1Checksum(2, "Checksum error for saved data copy 1."),

	DataCopy2Checksum(3, "Checksum error for saved data copy 1."),

	ParameterSetACopy1Checksum(4, "Checksum error for parameter set A copy 1."),

	ParameterSetACopy2Checksum(5, "Checksum error for parameter set A copy 2."),

	ParameterSetBCopy1Checksum(6, "Checksum error for parameter set B copy 1."),

	ParameterSetBCopy2Checksum(7, "Checksum error for parameter set B copy 2."),

	VoltageFeedbackScaling(8, "Voltage feedback scaling error."),

	CurrentFeedbackScaling(9, "Current feedback scaling error."),

	CurrentDifference(
			10,
			"Difference between inverter input and output current feedback is greater than Current Difference Trip for more than Current Difference Delay."),

	RatingsChange(11, "A ratings parameter has been changed."),

	StackFault(12, "DSP stack overflow."),

	AdcFault(13, "Analog to digital converter fault."),

	NvramFault(14, "Non-volatile memory fault."),

	FpgaFault(15, "FPGA bus interface fault.");

	private final int code;
	private final String description;

	private PowerGateFault2(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getFaultGroup() {
		return 2;
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
	public static PowerGateFault2 forCode(int code) {
		if ( code == 0 ) {
			return null;
		}
		for ( PowerGateFault2 c : values() ) {
			if ( code == c.code ) {
				return c;
			}
		}
		throw new IllegalArgumentException("PowerGateFault2 code [" + code + "] not supported");
	}

}
