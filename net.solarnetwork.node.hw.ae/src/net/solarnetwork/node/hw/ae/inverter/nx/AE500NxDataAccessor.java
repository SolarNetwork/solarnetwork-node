/* ==================================================================
 * AE500NxDataAccessor.java - 22/04/2020 11:37:34 am
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DcEnergyDataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * API for reading AE 500NX data.
 * 
 * @author matt
 * @version 1.2
 * @since 2.1
 */
public interface AE500NxDataAccessor extends DcEnergyDataAccessor, AcEnergyDataAccessor {

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the firmware version.
	 * 
	 * @return the version
	 */
	String getFirmwareVersion();

	/**
	 * Get the ambient temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getAmbientTemperature();

	/**
	 * Get the cabinet temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getCabinetTemperature();

	/**
	 * Get the coolant temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getCoolantTemperature();

	/**
	 * Get the line reactor temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 * @since 1.1
	 */
	Float getReactorTemperature();

	/**
	 * Get the DC current.
	 * 
	 * @return the DC current, in amperes
	 */
	Float getDcCurrent();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 */
	Set<AE500NxSystemStatus> getSystemStatus();

	/**
	 * Get the system limits.
	 * 
	 * @return the limits
	 */
	Set<AE500NxSystemLimit> getSystemLimits();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<AE500NxFault> getFaults();

	/**
	 * Get the complete set of active warnings, sorted by warning number.
	 * 
	 * @return the active warnings
	 */
	SortedSet<AE500NxWarning> getWarnings();

	/**
	 * Get the device operating state.
	 * 
	 * @return the device operating state
	 */
	DeviceOperatingState getDeviceOperatingState();

	/**
	 * Get the faults converted to SunSpec-compatible events.
	 * 
	 * @return the events, never {@literal null}
	 * @since 1.2
	 */
	default Set<ModelEvent> getEvents() {
		return Collections.emptySet();
	}

	/**
	 * Get an optional vendor-specific bit set of event codes.
	 * 
	 * <p>
	 * Note that all SunSpec "vendor event" fields are presented as a single bit
	 * set, with each 32-bit event group offset by 32. For example if a model
	 * defines {@code EvtVnd1} and {@code EvtVnd2} 32-bit properties, there are
	 * 64 possible bits where {@code EvtVnd1}'s first bit would be index
	 * {@code 0} and {@code EvtVnd2}'s first bit would be index {@code 32}.
	 * </p>
	 * 
	 * @return the vendor events, or {@literal null} if not supported or known
	 * @since 1.2
	 */
	default BitSet getVendorEvents() {
		return null;
	}

}
