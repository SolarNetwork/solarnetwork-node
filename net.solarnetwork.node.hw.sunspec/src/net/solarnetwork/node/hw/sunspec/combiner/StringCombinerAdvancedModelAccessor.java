/* ==================================================================
 * StringCombinerAdvancedModelAccessor.java - 10/09/2019 3:40:07 pm
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

/**
 * Advanced string combiner API.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public interface StringCombinerAdvancedModelAccessor extends StringCombinerModelAccessor {

	/**
	 * API for an individual advanced DC input (repeating block).
	 */
	interface AdvancedDcInput extends DcInput {

		/**
		 * Get the DC voltage, in volts.
		 * 
		 * @return the DC voltage
		 */
		Float getDCVoltage();

		/**
		 * Get the DC power, in watts.
		 * 
		 * @return the DC power
		 */
		Integer getDCPower();

		/**
		 * Get the DC energy delivered (imported), in watt-hours.
		 * 
		 * @return the delivered energy
		 */
		Long getDCEnergy();

		/**
		 * Get the DC performance ratio, as a percentage 0-1.
		 * 
		 * @return the DC performance ratio
		 */
		Float getDCPerformanceRatio();

		/**
		 * Get the number of modules in this input string.
		 * 
		 * @return the count of modules
		 */
		Integer getModuleCount();

	}

	/**
	 * Get the DC power, in watts.
	 * 
	 * @return the DC power
	 */
	Integer getDCPower();

	/**
	 * Get the DC energy delivered (imported), in watt-hours.
	 * 
	 * @return the delivered energy
	 */
	Long getDCEnergy();

	/**
	 * Get the DC performance ratio, as a percentage 0-1.
	 * 
	 * @return the DC performance ratio
	 */
	Float getDCPerformanceRatio();

	/**
	 * Get the list of available DC inputs.
	 * 
	 * @return the inputs
	 */
	List<AdvancedDcInput> getAdvancedDcInputs();

}
