/* ==================================================================
 * Tsl2591Operations.java - 1/09/2020 6:54:09 AM
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * API for TSL25911 operations.
 * 
 * @author matt
 * @version 1.0
 */
public interface Tsl2591Operations extends AutoCloseable {

	/**
	 * Configure the gain and integration time of the sensor.
	 * 
	 * @param gain
	 *        the gain to set
	 * @param integrationTime
	 *        the integration time to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setup(Gain gain, IntegrationTime integrationTime) throws IOException;

	/**
	 * Set the enable modes of the device.
	 * 
	 * @param modes
	 *        the modes to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setEnableModes(Set<EnableMode> modes) throws IOException;

	/**
	 * Close and release resources.
	 * 
	 * @throws IOException
	 *         if any communication error occurs
	 */
	@Override
	void close() throws IOException;

	/**
	 * Enable the ambient light sensor.
	 * 
	 * @throws IOException
	 *         if any communication error occurs
	 */
	default void enableAmbientLightSensor() throws IOException {
		setEnableModes(EnumSet.of(EnableMode.Power, EnableMode.AmbientLightSensor));
	}

	/**
	 * Disable all modes and power off the sensor.
	 * 
	 * @throws IOException
	 *         if any communication error occurs
	 */
	default void disable() throws IOException {
		setEnableModes(Collections.emptySet());
	}

	/**
	 * Get the ambient light luminosity, as a lux value.
	 * 
	 * @return the ambient light, in lux
	 * @throws IOException
	 *         if any communication error occurs
	 */
	BigDecimal getLux() throws IOException;

}
