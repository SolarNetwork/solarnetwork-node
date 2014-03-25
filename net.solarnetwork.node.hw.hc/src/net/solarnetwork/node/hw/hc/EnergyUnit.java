/* ==================================================================
 * EM5600Support.java - Mar 26, 2014 6:00:50 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.hc;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An enumeration of energy units.
 * 
 * @author matt
 * @version 1.0
 */
public enum EnergyUnit {

	WattHour(0),

	DecaWattHour(1),

	HectoWattHour(2),

	KiloWattHour(3),

	DecaKiloWattHour(4),

	HectoKiloWattHour(5),

	MegaWattHour(6);

	private final int value;
	private final int scale;

	private EnergyUnit(int value) {
		this.value = value;
		this.scale = new BigDecimal(new BigInteger(String.valueOf(value)), value).intValueExact();
	}

	/**
	 * Get an EnergyUnit for a raw Modbus register value.
	 * 
	 * @param v
	 *        the register value
	 * @return the EnergyUnit
	 * @throws IllegalArgumentException
	 *         if the value is not supported
	 */
	public EnergyUnit energyUnitForValue(final int v) {
		switch (v) {
			case 0:
				return WattHour;

			case 1:
				return DecaWattHour;

			case 2:
				return HectoWattHour;

			case 3:
				return KiloWattHour;

			case 4:
				return DecaKiloWattHour;

			case 5:
				return HectoKiloWattHour;

			case 6:
				return MegaWattHour;

			default:
				throw new IllegalArgumentException("The value " + v + " is not supported.");
		}
	}

	/**
	 * Get the raw Modbus register value for this unit.
	 * 
	 * @return the register value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Get the scale factor to apply with this unit, in terms of a divisor to
	 * apply to a raw Modbus register value to calculate the actual value in
	 * standardized form.
	 * 
	 * @return
	 */
	public int getScaleFactor() {
		return scale;
	}

}
