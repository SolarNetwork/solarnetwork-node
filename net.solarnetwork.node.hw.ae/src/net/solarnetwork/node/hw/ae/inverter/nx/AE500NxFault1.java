/* ==================================================================
 * AE500NxFault.java - 22/04/2020 11:38:44 am
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

package net.solarnetwork.node.hw.ae.inverter.nx;

/**
 * AE500NX fault group 1.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxFault1 implements AE500NxFault {

	/** The DC auxiliary power supply aux supply voltages are out of range. */
	AuxSupply(0, "The DC auxiliary power supply aux supply voltages are out of range."),

	/**
	 * The softstart relay did not close properly in order to charge the DC bus.
	 */
	SoftStart(2, "The softstart relay did not close properly in order to charge the DC bus."),

	/**
	 * The pump has stopped or coolant has leaked enough to have air in the
	 * system.
	 */
	CoolantFlow(3, "The pump has stopped or coolant has leaked enough to have air in the system."),

	/** The coolant has reached its maximum temperature limit. */
	CoolantTemp(4, "The coolant has reached its maximum temperature limit."),

	/** Indicates a failure of the control board. */
	DspWatchdog(5, "Indicates a failure of the control board."),

	/**
	 * Appropriate configuration file was not loaded into the unit or flash
	 * memory has failed.
	 */
	Configuration(
			6,
			"Appropriate configuration file was not loaded into the unit or flash memory has failed."),

	/** Error occurred when updating firmware. */
	ApplicationName(10, "Error occurred when updating firmware."),

	/** Current was flowing into the PV panels from the DC bus. */
	PosCurrent(11, "Current was flowing into the PV panels from the DC bus."),

	/** The DC bus voltage is too high to allow the inverter to turn on. */
	BusHigh(12, "The DC bus voltage is too high to allow the inverter to turn on."),

	/**
	 * The DC bus voltage fell below the minimum value required to allow the
	 * unit to continue to run.
	 */
	BusLow(
			13,
			"The DC bus voltage fell below the minimum value required to allow the unit to continue to run."),

	/** Negative DC from the array has gone past the trip limit. */
	NegCurrent(14, "Negative DC from the array has gone past the trip limit."),

	/** Hardware protection against incorrect input AC voltage. */
	AcVolt(15, "Hardware protection against incorrect input AC voltage."),

	/** AC output current has exceeded the allowed maximum. */
	AcCurrent(16, "AC output current has exceeded the allowed maximum."),

	/**
	 * There is an unexplained imbalance of current in the 3- phase AC output.
	 */
	ZeroSequence(17, "There is an unexplained imbalance of current in the 3- phase AC output."),

	/** A 3-phase voltage surge exceeded the limit of the unit. */
	PosSequenceHigh(19, "A 3-phase voltage surge exceeded the limit of the unit."),

	/** The mains contactor opened. */
	AcContactor(22, "The mains contactor opened."),

	/**
	 * There was a sag in 3-phase line voltage that went beyond the limit of the
	 * unit in either time or voltage.
	 */
	PosSequenceLow(
			24,
			"There was a sag in 3-phase line voltage that went beyond the limit of the unit in either time or voltage."),

	/** The unit has cycled on and off too many times in a short period. */
	Cycling(25, "The unit has cycled on and off too many times in a short period."),

	/**
	 * The line reactor temperature in the bottom of the unit cabinet has
	 * exceeded the maximum limit.
	 */
	ReactorTemp(
			26,
			"The line reactor temperature in the bottom of the unit cabinet has exceeded the maximum limit."),

	/**
	 * A low frequency has persisted too long for the parameters of the unit.
	 */
	FrequencyLow(29, "A low frequency has persisted too long for the parameters of the unit."),

	/** AC frequency has exceeded the limit set in the configuration file. */
	FrequencyHigh(30, "AC frequency has exceeded the limit set in the configuration file."),

	/** The ground current from the ground current DC side exceeds the limit. */
	GroundCurrent(31, "The ground current from the ground current DC side exceeds the limit."),

	;

	private final int bit;
	private final String description;

	private AE500NxFault1(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public int getFaultGroup() {
		return 0;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
