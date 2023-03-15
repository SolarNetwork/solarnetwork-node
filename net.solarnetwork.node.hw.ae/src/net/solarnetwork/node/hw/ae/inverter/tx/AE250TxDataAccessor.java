/* ==================================================================
 * AE250TxDataAccessor.java - 27/07/2018 2:12:14 PM
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
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.hw.ae.inverter.tx;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DcEnergyDataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * API for reading AE 250TX data.
 * 
 * @author matt
 * @version 2.2
 */
public interface AE250TxDataAccessor extends DcEnergyDataAccessor, AcEnergyDataAccessor {

	/**
	 * Get the inverter type.
	 * 
	 * @return the name
	 */
	AEInverterType getInverterType();

	/**
	 * Get the device ID number.
	 * 
	 * @return the ID number
	 */
	String getIdNumber();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision
	 */
	String getFirmwareRevision();

	/**
	 * Get the device register mapping version.
	 * 
	 * @return the map version
	 */
	Integer getMapVersion();

	/**
	 * Get the inverter configuration.
	 * 
	 * @return the configuration
	 */
	AEInverterConfiguration getInverterConfiguration();

	/**
	 * Get the inverter's rated power, in W.
	 * 
	 * @return the rated power
	 */
	Integer getInverterRatedPower();

	/**
	 * Get the DC current, in A.
	 * 
	 * @return the DC current
	 * @since 2.1
	 */
	Float getDcCurrent();

	/**
	 * Get the PV voltage, in volts.
	 * 
	 * @return the PV voltage
	 * @since 2.1
	 */
	Float getPvVoltage();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 * @since 1.1
	 */
	AE250TxSystemStatus getSystemStatus();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<AE250TxFault> getFaults();

	/**
	 * Get the complete set of active warnings, sorted by warning number.
	 * 
	 * @return the active warnings
	 */
	SortedSet<AE250TxWarning> getWarnings();

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
	 * @since 2.2
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
	 * @since 2.2
	 */
	default BitSet getVendorEvents() {
		return null;
	}

}
