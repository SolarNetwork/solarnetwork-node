/* ==================================================================
 * CozIrOperations.java - 27/08/2020 4:00:04 PM
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

import java.io.IOException;
import java.util.Set;

/**
 * API for interacting with the CozIR sensor.
 * 
 * @author matt
 * @version 1.0
 */
public interface CozIrOperations {

	/**
	 * Set the operational mode of the device.
	 * 
	 * @param mode
	 *        the mode to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setMode(CozIrMode mode) throws IOException;

	/**
	 * Get the firmware version.
	 * 
	 * @return the firmware version
	 * @throws IOException
	 *         if any communication error occurs
	 */
	FirmwareVersion getFirmwareVersion() throws IOException;

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 * @throws IOException
	 *         if any communication error occurs
	 */
	String getSerialNumber() throws IOException;

	/**
	 * Get the currently configured CO2 "fresh air" zero level.
	 * 
	 * @return the currently configured level
	 * @see #setCo2FreshAirLevel(int)
	 * @throws IOException
	 *         if any communication error occurs
	 */
	int getCo2FreshAirLevel() throws IOException;

	/**
	 * Set the "fresh air" zero value to be used when
	 * {@link #calibrateAsCo2FreshAirLevel()}.
	 * 
	 * @param value
	 *        the value to set; only the 16 least-significant bits are used
	 * @see #getCo2FreshAirLevel()
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setCo2FreshAirLevel(int value) throws IOException;

	/**
	 * Calibrate the CO2 level to "fresh air" level previously configured via
	 * {@link #setCo2FreshAirLevel(int)}.
	 * 
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void calibrateAsCo2FreshAirLevel() throws IOException;

	/**
	 * Get the current altitude compensation value.
	 * 
	 * @return the altitude compensation value, or {@literal -1} if not known
	 * @throws IOException
	 *         if any communication error occurs
	 */
	int getAltitudeCompensation() throws IOException;

	/**
	 * Set the altitude compensation value.
	 * 
	 * @param value
	 *        the altitude compensation value
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setAltitudeCompensation(int value) throws IOException;

	/**
	 * Set the type of measurements to report.
	 * 
	 * @param types
	 *        the types of measurements to report
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setMeasurementOutput(Set<MeasurementType> types) throws IOException;

	/**
	 * Read the current measurements.
	 * 
	 * @return the current measurements
	 * @throws IOException
	 *         if any communication error occurs
	 */
	CozIrData getMeasurements() throws IOException;

}
