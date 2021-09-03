/* ==================================================================
 * EnergyDatum.java - Apr 1, 2014 5:02:29 PM
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

package net.solarnetwork.node.domain.datum;

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;

/**
 * Standardized API for energy related datum to implement. By "energy" we simply
 * mean this datum represents information tracked during the production or
 * consumption of energy. For example current sensors can provide approximate
 * instantaneous watt readings, watt-hour meters can provide accumulated Wh
 * readings, and usually solar inverters can provide instantaneous generated
 * power and accumulated energy production readings.
 * 
 * @author matt
 * @version 2.0
 */
public interface EnergyDatum extends net.solarnetwork.domain.datum.EnergyDatum, MutableNodeDatum {

	/**
	 * Set a watt-hour reading.
	 * 
	 * @param value
	 *        the watt hour reading
	 */
	default void getWattHourReading(Long value) {
		asMutableSampleOperations().putSampleValue(Accumulating, WATT_HOUR_READING_KEY, value);
	}

	/**
	 * Set a reverse watt-hour reading.
	 * 
	 * @return the reverse watt hour reading
	 */
	default void setReverseWattHourReading(Long value) {
		asMutableSampleOperations().putSampleValue(Accumulating,
				WATT_HOUR_READING_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY, value);
	}

	/**
	 * Get the instantaneous watts.
	 * 
	 * @param value
	 *        the watts
	 */
	default void getWatts(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, WATTS_KEY, value);
	}

	/**
	 * Tag this datum with the consumption tag, removing the generation tag.
	 * 
	 * @return {@literal true} if the consumption tag as added
	 */
	default boolean tagAsConsumption() {
		MutableDatumSamplesOperations ops = asMutableSampleOperations();
		ops.removeTag(TAG_GENERATION);
		return ops.addTag(TAG_CONSUMPTION);
	}

	/**
	 * Tag this datum with the generation tag, removing the consumption tag.
	 * 
	 * @return {@literal true} if the generation tag as added
	 */
	default boolean tagAsGeneration() {
		MutableDatumSamplesOperations ops = asMutableSampleOperations();
		ops.removeTag(TAG_CONSUMPTION);
		return ops.addTag(TAG_GENERATION);
	}

}
