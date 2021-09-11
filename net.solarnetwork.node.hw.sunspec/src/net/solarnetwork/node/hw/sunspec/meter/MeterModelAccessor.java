/* ==================================================================
 * MeterModelAccessor.java - 22/05/2018 6:26:19 AM
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
 * You should have exported a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.hw.sunspec.meter;

import java.util.Set;
import net.solarnetwork.domain.datum.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * API for accessing meter model data.
 * 
 * @author matt
 * @version 1.1
 */
public interface MeterModelAccessor extends ModelAccessor, AcEnergyDataAccessor {

	/**
	 * Get the AC frequency value, in Hz.
	 * 
	 * @return the frequency
	 */
	@Override
	Float getFrequency();

	/**
	 * Get the current, in A.
	 * 
	 * @return the current
	 */
	@Override
	Float getCurrent();

	/**
	 * Get the voltage, in V.
	 * 
	 * @return the voltage
	 */
	@Override
	Float getVoltage();

	/**
	 * Get the power factor, as a decimal from -1.0 to 1.0.
	 * 
	 * @return the power factor
	 */
	@Override
	Float getPowerFactor();

	/**
	 * Get the active (real) power, in W.
	 * 
	 * @return the active power
	 */
	@Override
	Integer getActivePower();

	/**
	 * Get the apparent power, in VA.
	 * 
	 * @return the apparent power
	 */
	@Override
	Integer getApparentPower();

	/**
	 * Get the reactive power, in VAR.
	 * 
	 * @return the reactive power
	 */
	@Override
	Integer getReactivePower();

	/**
	 * Get the active energy imported (delivered), in Wh.
	 * 
	 * @return the imported active energy
	 */
	Long getActiveEnergyImported();

	/**
	 * Get the active energy exported (received), in Wh.
	 * 
	 * @return the exported active energy
	 */
	Long getActiveEnergyExported();

	/**
	 * Get the reactive energy imported (delivered), in VARh.
	 * 
	 * @return the imported reactive energy
	 */
	Long getReactiveEnergyImported();

	/**
	 * Get the reactive energy exported (received), in VARh.
	 * 
	 * @return the exported reactive energy
	 */
	Long getReactiveEnergyExported();

	/**
	 * Get the apparent energy imported (delivered), in VAh.
	 * 
	 * @return the imported apparent energy
	 */
	Long getApparentEnergyImported();

	/**
	 * Get the apparent energy exported (received), in VAh.
	 * 
	 * @return the exported apparent energy
	 */
	Long getApparentEnergyExported();

	/**
	 * Get an accessor for phase-specific measurements.
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	@Override
	MeterModelAccessor accessorForPhase(AcPhase phase);

	/**
	 * Get a "reversed" model accessor, where import/export directions are
	 * switched.
	 * 
	 * @return the reversed accessor
	 * @since 1.1
	 */
	@Override
	default MeterModelAccessor reversed() {
		return new ReversedMeterModelAccessor(this);
	}

	/**
	 * Get the active events.
	 * 
	 * @return the events (never {@literal null})
	 */
	Set<ModelEvent> getEvents();

}
