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
 * AE500NX fault group 2.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxFault2 implements AE500NxFault {

	CommonMode(0, "There is too much AC common mode voltage on the PV array neutral and hot wires."),

	DcContactor(
			1,
			"The DC contactor has reported that it has unexpectedly opened or has not operated properly during startup."),

	AmbientTemp(2, "The ambient temperature has exceeded the upper limit."),

	CabinetTemp(3, "The cabinet temperature has exceeded the upper limit."),

	TieContactor(
			4,
			"The PV array tie contactor has reported that it has unexpectedly opened or has not operated properly during startup."),

	DriveAInterlock(9, "A cable or connector has become loose inside the unit."),

	DriveBInterlock(10, "A cable or connector has become loose inside the unit."),

	DriveCInterlock(11, "A cable or connector has become loose inside the unit."),

	ISenseInterlock(12, "A cable or connector has become loose inside the unit."),

	Relay1Interlock(13, "A cable or connector has become loose inside the unit."),

	Relay2Interlock(14, "A cable or connector has become loose inside the unit."),

	SetInterlock(15, "A cable or connector has become loose inside the unit."),

	ThermalInterlock(16, "A cable or connector has become loose inside the unit."),

	ChargeFail(17, "The internal DC bus voltage did not reach an acceptable level quickly enough."),

	StopButton(
			18,
			"Someone has pressed the Stop button or the external interlock is preventing the unit from operating."),

	TurnOn(
			19,
			"A cloud edge disturbed the PV voltage to the unit during turn-on before the unit's DC contactor could close."),

	Fan1(20, "Fan is not running fast enough."),

	Fan2(21, "Fan is not running fast enough."),

	Fan3(22, "Fan is not running fast enough."),

	Fan4(23, "Fan is not running fast enough."),

	Fan5(24, "Fan is not running fast enough."),

	ArrayImbalance(25, "The positive and negative bipolar PV array voltages are out of balance."),

	OverPower(
			26,
			"The available PV array power increased too fast for the inverter to back off the voltage and keep the power from exceeding the trip limit."),

	GfiFailure(27, "A failure has occurred in a ground fault detection component in the unit."),

	Fan6(28, "Fan is not running fast enough."),

	Fan7(29, "Fan is not running fast enough.");

	private final int bit;
	private final String description;

	private AE500NxFault2(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public int getFaultGroup() {
		return 1;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
