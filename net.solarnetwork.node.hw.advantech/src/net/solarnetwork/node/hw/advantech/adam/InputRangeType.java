/* ==================================================================
 * InputRangeType.java - 20/11/2018 2:41:39 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.advantech.adam;

/**
 * Enumeration of input range types.
 * 
 * @author matt
 * @version 1.0
 */
public enum InputRangeType {

	PlusMinusFifteenMilliVolts(0x00, "± 15 mV", InputUnit.Volts, -3, -15f, 15f),

	PlusMinusFiftyMilliVolts(0x01, "± 50 mV", InputUnit.Volts, -3, -50f, 50f),

	PlusMinusOneHundredMilliVolts(0x02, "± 100 mV", InputUnit.Volts, -3, -100f, 100f),

	PlusMinusFiveHundredMilliVolts(0x03, "± 500 mV", InputUnit.Volts, -3, -500f, 500f),

	PlusMinusOneVolts(0x04, "± 1 V", InputUnit.Volts, 1, -1f, 1f),

	PlusMinusTwoPointFiveVolts(0x05, "± 2.5 V", InputUnit.Volts, 1, -2.5f, 2.5f),

	PlusMinusTwentyMilliAmps(0x06, "± 20 mA", InputUnit.Amps, -3, -20f, 20f),

	FourToTwentyMilliAmps(0x07, "4~20 mA", InputUnit.Amps, -3, 4f, 20f),

	PlusMinusTenVolts(0x08, "± 10 V", InputUnit.Volts, 1, -10f, 10f),

	PlusMinusFiveVolts(0x09, "± 5 V", InputUnit.Volts, 1, -5f, 5f),

	PlusMinusOneVoltsAlt(0x0A, "± 1 V", InputUnit.Volts, 1, -1f, 1f),

	PlusMinusFiveHundredMilliVoltsAlt(0x0B, "± 500 mV", InputUnit.Volts, -3, -500f, 500f),

	PlusMinusOneHundredFiftyMilliVolts(0x0C, "± 150 mV", InputUnit.Volts, -3, -150f, 150f),

	PlusMinusTwentyMilliAmpsAlt(0x0D, "± 20 mA", InputUnit.Amps, -3, -20f, 20f),

	TypeJThermocouple(0x0E, "Type J Thermocouple 0~760 ℃", InputUnit.DegreeCelsius, 1, 0f, 760f),

	TypeKThermocouple(0x0F, "Type K Thermocouple 0~1370 ℃", InputUnit.DegreeCelsius, 1, 0f, 1370f),

	TypeTThermocouple(0x10, "Type T Thermocouple -100~400 ℃", InputUnit.DegreeCelsius, 1, -100f, 400f),

	TypeEThermocouple(0x11, "Type E Thermocouple 0~1000 ℃", InputUnit.DegreeCelsius, 1, 0f, 1000f),

	TypeRThermocouple(0x12, "Type R Thermocouple 500~1750 ℃", InputUnit.DegreeCelsius, 1, 500f, 1750f),

	TypeSThermocouple(0x13, "Type S Thermocouple 500~1750 ℃", InputUnit.DegreeCelsius, 1, 500f, 1750f),

	TypeBThermocouple(0x14, "Type B Thermocouple 500~1800 ℃", InputUnit.DegreeCelsius, 1, 500f, 1800f),

	PlusMinusFifteenVoltsAlt(0x15, "± 15 V", InputUnit.Volts, 1, -15f, 15f),

	ZeroToTenVolts(0x48, "0 ~ 10 V", InputUnit.Volts, 1, 0f, 10f),

	Unknown(-1, "Unknown", InputUnit.Unknown, 1, 0f, 0f);

	private final int code;
	private final String description;
	private final InputUnit unit;
	private final int unitScale;
	private final float min;
	private final float max;

	private InputRangeType(int code, String description, InputUnit unit, int unitScale, float min,
			float max) {
		this.code = code;
		this.description = description;
		this.unit = unit;
		this.unitScale = unitScale;
		this.min = min;
		this.max = max;
	}

	/**
	 * Get the input range code value.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the input range description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the input measurement base unit.
	 * 
	 * @return the input unit
	 */
	public InputUnit getUnit() {
		return unit;
	}

	/**
	 * Get the input measurement unit power of 10 scale.
	 * 
	 * @return the unit scale
	 */
	public int getUnitScale() {
		return unitScale;
	}

	/**
	 * Get the minimum input value.
	 * 
	 * @return the minimum
	 */
	public float getMin() {
		return min;
	}

	/**
	 * Get the maximum input value.
	 * 
	 * @return the maximum
	 */
	public float getMax() {
		return max;
	}

}
