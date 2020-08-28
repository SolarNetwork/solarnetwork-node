/* ==================================================================
 * CozIrData.java - 27/08/2020 4:11:57 PM
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

package net.solarnetwork.node.hw.gss.co2;

import java.math.BigDecimal;

/**
 * Sensor data for a CozIr sensor.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrData {

	/** The constant {@literal 1000}. */
	public static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);

	private final BigDecimal co2;
	private final BigDecimal co2Unfiltered;
	private final BigDecimal humidity;
	private final BigDecimal temperature;

	/**
	 * Get a data instance from raw sensor values.
	 * 
	 * @param co2
	 *        the CO2 value
	 * @param co2Unfiltered
	 *        the unfiltered CO2 value
	 * @param co2ScaleFactor
	 *        the scale factor
	 * @param humidity
	 *        the humidity
	 * @param temperature
	 *        the temperature
	 * @return the data instance, never {@literal null}
	 */
	public static CozIrData forRawValue(Integer co2, Integer co2Unfiltered, int co2ScaleFactor,
			Integer humidity, Integer temperature) {
		// @formatter:off
		return new CozIrData(
				co2 != null ? new BigDecimal(co2 * co2ScaleFactor) : null,
				co2Unfiltered != null ? new BigDecimal(co2Unfiltered * co2ScaleFactor) : null,
				humidity != null ? new BigDecimal(humidity).movePointLeft(1) : null,
				temperature != null 
						? new BigDecimal(temperature).subtract(ONE_THOUSAND).movePointLeft(1)
						: null);
		// @formatter:on
	}

	/**
	 * Constructor.
	 * 
	 * @param co2
	 *        the CO2 reading
	 * @param co2Unfiltered
	 *        the CO2 (unfiltered) reading
	 * @param humidity
	 *        the humidity reading
	 * @param temperature
	 *        the temperature reading
	 */
	public CozIrData(BigDecimal co2, BigDecimal co2Unfiltered, BigDecimal humidity,
			BigDecimal temperature) {
		super();
		this.co2 = co2;
		this.co2Unfiltered = co2Unfiltered;
		this.humidity = humidity;
		this.temperature = temperature;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CozIrData{co2=");
		builder.append(co2);
		builder.append(", humidity=");
		builder.append(humidity);
		builder.append(", temperature=");
		builder.append(temperature);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the CO2 reading.
	 * 
	 * @return the CO2 reading, in PPM
	 */
	public BigDecimal getCo2() {
		return co2;
	}

	/**
	 * Get the unfiltered CO2 reading.
	 * 
	 * @return the unfiltered CO2 reading, in PPM
	 */
	public BigDecimal getCo2Unfiltered() {
		return co2Unfiltered;
	}

	/**
	 * Get the humidity.
	 * 
	 * @return the humidity as an numeric percentage (0-100)
	 */
	public BigDecimal getHumidity() {
		return humidity;
	}

	/**
	 * Get the temperature.
	 * 
	 * @return the temperature the degrees celsius
	 */
	public BigDecimal getTemperature() {
		return temperature;
	}

}
