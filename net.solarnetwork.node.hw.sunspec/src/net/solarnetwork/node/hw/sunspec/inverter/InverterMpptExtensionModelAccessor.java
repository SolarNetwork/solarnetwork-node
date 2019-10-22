/* ==================================================================
 * InverterMpptExtensionModelAccessor.java - 6/09/2019 5:25:06 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.inverter;

import java.util.List;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * API for accessing inverter MPPT extension model data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public interface InverterMpptExtensionModelAccessor extends ModelAccessor {

	/**
	 * API for an individual DC module (repeating block).
	 */
	interface DcModule {

		/**
		 * Get the module ID.
		 * 
		 * @return the module ID
		 */
		Integer getInputId();

		/**
		 * Get the module name.
		 * 
		 * @return the module name
		 */
		String getInputName();

		/**
		 * Get the DC current, in amps.
		 * 
		 * @return the DC current
		 */
		Float getDCCurrent();

		/**
		 * Get the DC voltage, in volts.
		 * 
		 * @return the DC voltage
		 */
		Float getDCVoltage();

		/**
		 * Get the DC power, in W.
		 * 
		 * @return the DC power
		 */
		Integer getDCPower();

		/**
		 * Get the DC energy delivered (imported), in Wh.
		 * 
		 * @return the delivered active energy
		 */
		Long getDCEnergyDelivered();

		/**
		 * Gets the time stamp of the data, in milliseconds since the epoch.
		 * 
		 * @return the data time stamp
		 */
		Long getDataTimestamp();

		/**
		 * Get the temperature of the module, in degrees celsius.
		 * 
		 * @return the temperature
		 */
		Float getTemperature();

		/**
		 * Get the module operating state.
		 * 
		 * @return the operating state
		 */
		OperatingState getOperatingState();

		/**
		 * Get the active events for the module.
		 * 
		 * @return the events, never {@literal null}
		 */
		Set<ModelEvent> getEvents();

	}

	/**
	 * Get the list of available DC modules.
	 * 
	 * @return the modules
	 */
	List<DcModule> getDcModules();

	/**
	 * Get the active events.
	 * 
	 * @return the events, never {@literal null}
	 */
	Set<ModelEvent> getEvents();

	/**
	 * Get the timestamp period.
	 * 
	 * @return the period
	 */
	Integer getTimestampPeriod();

}
