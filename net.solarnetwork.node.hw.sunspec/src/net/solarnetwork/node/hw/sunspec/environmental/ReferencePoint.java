/* ==================================================================
 * ReferencePoint.java - 9/07/2023 4:30:30 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

/**
 * Reference point data.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class ReferencePoint {

	private final Integer irradiance;
	private final Float current;
	private final Float voltage;
	private final Float temperature;

	/**
	 * Constructor.
	 * 
	 * @param irradiance
	 *        the irradiance, in W/m2
	 * @param current
	 *        the current, in amps
	 * @param voltage
	 *        the voltage, in volts
	 * @param temperature
	 *        the temperature, in degrees celsius
	 */
	public ReferencePoint(Integer irradiance, Float current, Float voltage, Float temperature) {
		super();
		this.irradiance = irradiance;
		this.current = current;
		this.voltage = voltage;
		this.temperature = temperature;
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        an array of raw reference point data, in irradiance, current,
	 *        voltage, temperature order; the array values are copied and
	 *        adjusted to scale
	 */
	public ReferencePoint(Integer[] data) {
		super();
		this.irradiance = (data != null && data.length > 0 ? data[0] : null);
		this.current = (data != null && data.length > 1 ? (data[1].floatValue() / 100f) : null);
		this.voltage = (data != null && data.length > 2 ? (data[2].floatValue() / 100f) : null);
		this.temperature = (data != null && data.length > 3 ? (data[3].floatValue() / 10f) : null);
	}

	/**
	 * Get the irradiance.
	 * 
	 * @return the irradiance, in W/m2
	 */
	public Integer getIrradiance() {
		return irradiance;
	}

	/**
	 * Get the current.
	 * 
	 * @return the current, in amps
	 */
	public Float getCurrent() {
		return current;
	}

	/**
	 * Get the voltage.
	 * 
	 * @return the voltage, in volts
	 */
	public Float getVoltage() {
		return voltage;
	}

	/**
	 * Get the temperature, in degrees celsius
	 * 
	 * @return the temperature
	 */
	public Float getTemperature() {
		return temperature;
	}

}
