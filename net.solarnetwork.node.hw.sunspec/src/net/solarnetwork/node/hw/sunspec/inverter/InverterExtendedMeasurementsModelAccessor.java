/* ==================================================================
 * InverterExtendedMeasurementsModelAccessor.java - 6/10/2026 7:41:03 am
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

package net.solarnetwork.node.hw.sunspec.inverter;

import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing SunSpec inverter controls extended measurements and status
 * model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>122</b>.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface InverterExtendedMeasurementsModelAccessor extends ModelAccessor {

	/**
	 * Get the PV inverter connection status.
	 *
	 * @return the status flags, never {@code null}
	 */
	Set<InverterConnectionStatus> getPvConnectionStatus();

	/**
	 * Get the storage inverter connection status.
	 *
	 * @return the status flags, never {@code null}
	 */
	Set<InverterConnectionStatus> getStorageConnectionStatus();

	/**
	 * Test if the inverter is connected at the electrical connection point
	 * (ECP).
	 *
	 * @return {@literal true} if connected, {@literal false} if disconnected,
	 *         or {@code null} if not available
	 */
	@Nullable
	Boolean isEcpConnected();

	/**
	 * Get the lifetime active energy output.
	 *
	 * @return the energy, in Wh, or {@code null} if not available
	 */
	@Nullable
	Long getActiveEnergyExported();

	/**
	 * Get the lifetime apparent energy output.
	 *
	 * @return the energy, in VAh, or {@code null} if not available
	 */
	@Nullable
	Long getApparentEnergyExported();

	/**
	 * Get the lifetime reactive energy output in quadrant 1.
	 *
	 * @return the energy, in VARh, or {@code null} if not available
	 */
	@Nullable
	Long getReactiveEnergyQ1();

	/**
	 * Get the lifetime reactive energy output in quadrant 2.
	 *
	 * @return the energy, in VARh, or {@code null} if not available
	 */
	@Nullable
	Long getReactiveEnergyQ2();

	/**
	 * Get the lifetime reactive energy output in quadrant 3.
	 *
	 * @return the energy, in VARh, or {@code null} if not available
	 */
	@Nullable
	Long getReactiveEnergyQ3();

	/**
	 * Get the lifetime reactive energy output in quadrant 4.
	 *
	 * @return the energy, in VARh, or {@code null} if not available
	 */
	@Nullable
	Long getReactiveEnergyQ4();

	/**
	 * Get the reactive power available without affecting the active power
	 * output.
	 *
	 * @return the reactive power, in VAR, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerAvailable();

	/**
	 * Get the active power available.
	 *
	 * @return the active power, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerAvailable();

	/**
	 * Get the setpoint limits that have been reached.
	 *
	 * <p>
	 * The SunSpec model notes that the device clears these flags when they are
	 * read.
	 * </p>
	 *
	 * @return the limits, never {@code null}
	 */
	Set<InverterSetpointLimit> getSetpointLimitsReached();

	/**
	 * Get the inverter controls that are currently active.
	 *
	 * @return the controls, never {@code null}
	 */
	Set<InverterControlFunction> getActiveControls();

	/**
	 * Get the source of time synchronization.
	 *
	 * @return the source, or {@code null} if not available
	 */
	@Nullable
	String getTimeSource();

	/**
	 * Get the device time.
	 *
	 * @return the time, or {@code null} if not available
	 */
	@Nullable
	Instant getDeviceTime();

	/**
	 * Get the ride-throughs that are currently active.
	 *
	 * @return the ride-throughs, never {@code null}
	 */
	Set<InverterRideThrough> getActiveRideThroughs();

	/**
	 * Get the isolation resistance.
	 *
	 * @return the resistance, in ohms, or {@code null} if not available
	 */
	@Nullable
	Float getIsolationResistance();

}
