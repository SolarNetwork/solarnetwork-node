/* ==================================================================
 * DerAcMeasurementModelAccessor.java - 5/10/2026 8:27:22 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.der;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;

/**
 * API for accessing DER AC measurement model data.
 *
 * <p>
 * This API extends {@link InverterModelAccessor}, which the DER models
 * supersede, with these details:
 * </p>
 *
 * <ul>
 * <li>Energy "delivered" and "exported" values are the energy injected into the
 * grid, and energy "received" values are the energy absorbed from the
 * grid.</li>
 * <li>{@link #getOperatingState()} returns the {@link InverterOperatingState}
 * equivalent of {@link #getInverterState()}.</li>
 * <li>{@link #getEvents()} returns {@link DerAlarm} values.</li>
 * <li>DC values are not available, as they are provided by the DER DC
 * measurement model.</li>
 * </ul>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerAcMeasurementModelAccessor extends InverterModelAccessor {

	/**
	 * Get the AC wiring type.
	 *
	 * @return the wiring type
	 */
	@Nullable
	DerAcWiringType getAcWiringType();

	/**
	 * Get the DER operating state.
	 *
	 * @return the operating state
	 */
	@Nullable
	DerOperatingState getDerOperatingState();

	/**
	 * Get the inverter state.
	 *
	 * @return the inverter state
	 * @see #getOperatingState()
	 */
	@Nullable
	DerInverterState getInverterState();

	/**
	 * Get the inverter operating state.
	 *
	 * @return the {@link InverterOperatingState} equivalent of
	 *         {@link #getInverterState()}
	 */
	@Override
	@Nullable
	OperatingState getOperatingState();

	/**
	 * Get the grid connection state.
	 *
	 * @return the grid connection state
	 */
	@Nullable
	DerGridConnectionState getGridConnectionState();

	/**
	 * Get the current operational characteristics.
	 *
	 * @return the characteristics, never {@code null}
	 */
	Set<DerOperationalCharacteristic> getOperationalCharacteristics();

	/**
	 * Get the ambient temperature, in degrees Celsius.
	 *
	 * @return the ambient temperature
	 */
	@Nullable
	Float getAmbientTemperature();

	/**
	 * Get the IGBT/MOSFET temperature, in degrees Celsius.
	 *
	 * @return the switch temperature
	 */
	@Nullable
	Float getSwitchTemperature();

	/**
	 * Get the active power throttling, as a percentage of the maximum active
	 * power.
	 *
	 * @return the throttling percentage, from 0 - 100
	 */
	@Nullable
	Integer getThrottlePercent();

	/**
	 * Get the active power throttling sources.
	 *
	 * @return the sources, never {@code null}
	 */
	Set<DerThrottleSource> getThrottleSources();

	/**
	 * Get the manufacturer alarm information.
	 *
	 * <p>
	 * This information is valid when the {@link DerAlarm#ManufacturerAlarm}
	 * alarm is active.
	 * </p>
	 *
	 * @return the manufacturer alarm information
	 */
	@Nullable
	String getManufacturerAlarmInfo();

}
