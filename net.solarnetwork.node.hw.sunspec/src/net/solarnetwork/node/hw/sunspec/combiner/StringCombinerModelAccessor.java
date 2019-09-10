/* ==================================================================
 * StringCombinerModelAccessor.java - 10/09/2019 6:59:43 am
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

package net.solarnetwork.node.hw.sunspec.combiner;

import java.util.List;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * API for accessing string combiner model data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public interface StringCombinerModelAccessor extends ModelAccessor {

	/**
	 * API for an individual DC input (repeating block).
	 */
	interface DcInput {

		/**
		 * Get the module ID.
		 * 
		 * @return the module ID
		 */
		Integer getInputId();

		/**
		 * Get the DC current, in amps.
		 * 
		 * @return the DC current
		 */
		Float getDCCurrent();

		/**
		 * Get the DC charge delivered (imported), in amp-hours.
		 * 
		 * @return the delivered charge
		 */
		Long getDCChargeDelivered();

		/**
		 * Get the active events for the module.
		 * 
		 * @return the events, never {@literal null}
		 * @see StringCombinerModelEvent
		 */
		Set<ModelEvent> getEvents();

		/**
		 * Get the active vendor events.
		 * 
		 * @return the vendor events, never {@literal null}
		 */
		Set<ModelEvent> getVendorEvents();

	}

	/**
	 * Get the DC current, in amps.
	 * 
	 * @return the DC current
	 */
	Float getDCCurrent();

	/**
	 * Get the DC charge delivered (imported), in amp-hours.
	 * 
	 * @return the delivered charge
	 */
	Long getDCChargeDelivered();

	/**
	 * Get the DC voltage, in volts.
	 * 
	 * @return the DC voltage
	 */
	Float getDCVoltage();

	/**
	 * Get the temperature of the combiner, in degrees celsius.
	 * 
	 * @return the temperature
	 */
	Float getTemperature();

	/**
	 * Get the active events.
	 * 
	 * @return the events, never {@literal null}
	 * @see StringCombinerModelEvent
	 */
	Set<ModelEvent> getEvents();

	/**
	 * Get the active vendor events.
	 * 
	 * @return the vendor events, never {@literal null}
	 */
	Set<ModelEvent> getVendorEvents();

	/**
	 * Get the list of available DC inputs.
	 * 
	 * @return the inputs
	 */
	List<DcInput> getDcInputs();

}
