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
 * AE500NX fault group 3.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxFault3 implements AE500NxFault {

	GfiInterlock(
			0,
			"The cable for the ground fault detection and interruption device is loose or removed."),

	SoftStartStuck(1, "The softstart contactor is likely to be welded closed and unable to open."),

	PvTieStuck(2, "The PV Tie contactor is likely to be welded closed and unable to open."),

	DcContactorStuck(3, "The DC contactor is likely to be welded closed and unable to open."),

	AcContactorStuck(4, "The AC contactor is likely to be welded closed and unable to open."),

	Fan8(8, "Fan is not running fast enough."),

	PhaseALow(9, "Phase A low."),

	PhaseBLow(10, "Phase B low."),

	PhaseCLow(11, "Phase C low."),

	PhaseAHigh(12, "Phase A high."),

	PhaseBHigh(13, "Phase B high."),

	PhaseCHigh(14, "Phase C high.");

	private final int bit;
	private final String description;

	private AE500NxFault3(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public int getFaultGroup() {
		return 2;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
