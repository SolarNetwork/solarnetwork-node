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

import net.solarnetwork.domain.Bitmaskable;

/**
 * AE500NX system limit bitmask enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxSystemLimit implements Bitmaskable {

	/**
	 * The unit is reducing power because the output alternating current limit
	 * has been exceeded.
	 */
	Iac(8, "The unit is reducing power because the output alternating current limit has been exceeded."),

	/**
	 * The unit is reducing power because the PV array direct current limit has
	 * been exceeded.
	 */
	PvCurrent(
			16,
			"The unit is reducing power because the PV array direct current limit has been exceeded."),

	/**
	 * The unit is reducing output power because the AC power limit has been
	 * exceeded.
	 */
	Pac(17, "The unit is reducing output power because the AC power limit has been exceeded."),

	/** The MPPT is limited due to excessive DC voltage. */
	VdcHigh(18, "The MPPT is limited due to excessive DC voltage."),

	/** The MPPT is limited due to insufficient DC voltage. */
	VdcLow(19, "The MPPT is limited due to insufficient DC voltage."),

	/** The unit is consuming reactive power to limit current harmonics. */
	Headroom(20, "The unit is consuming reactive power to limit current harmonics."),

	/** The unit is reducing power due to excessive coolant temperature. */
	CoolantTemp(21, "The unit is reducing power due to excessive coolant temperature."),

	/** The unit is inhibiting PWM switching due to excessive AC current. */
	IacInhibit(22, "The unit is inhibiting PWM switching due to excessive AC current."),

	/**
	 * The unit is inhibiting PWM switching due to excessive bus capacitor
	 * voltage slew rate.
	 */
	BusSlewInhibit(
			23,
			"The unit is inhibiting PWM switching due to excessive bus capacitor voltage slew rate."),

	/** The unit is inhibiting PWM switching due to excessive power. */
	MaxPowerInhibit(24, "The unit is inhibiting PWM switching due to excessive power."),

	;

	private final int bit;
	private final String description;

	private AE500NxSystemLimit(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	/**
	 * Get a description of the status.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
