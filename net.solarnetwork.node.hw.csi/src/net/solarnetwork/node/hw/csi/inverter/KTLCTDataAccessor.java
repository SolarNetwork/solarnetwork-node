/* ==================================================================
 * KTLCTDataAccessor.java - 1/08/2018 1:25:31 PM
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

package net.solarnetwork.node.hw.csi.inverter;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DcEnergyDataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * API for reading CSI 50KTL-CT inverter series data.
 * 
 * @author matt
 * @version 2.2
 */
public interface KTLCTDataAccessor extends DcEnergyDataAccessor, AcEnergyDataAccessor {

	/**
	 * Get the inverter device type.
	 * 
	 * @return the device type
	 */
	KTLCTInverterType getInverterType();

	/**
	 * Get the inverter work mode.
	 * 
	 * @return the work mode
	 * @since 1.1
	 */
	KTLCTInverterWorkMode getWorkMode();

	/**
	 * Get the warnings.
	 * 
	 * @return the warnings
	 */
	Set<KTLCTWarn> getWarnings();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 * @since 2.2
	 */
	SortedSet<KTLCTFault> getFaults();

	/**
	 * Get the fault 0 set.
	 * 
	 * @return the faults
	 */
	Set<KTLCTFault0> getFaults0();

	/**
	 * Get the fault 1 set.
	 * 
	 * @return the faults
	 */
	Set<KTLCTFault1> getFaults1();

	/**
	 * Get the fault 2 set.
	 * 
	 * @return the faults
	 */
	Set<KTLCTFault2> getFaults2();

	/**
	 * Get the permanent fault set.
	 * 
	 * @return the faults
	 */
	Set<KTLCTPermanentFault> getPermanentFaults();

	/**
	 * Get the device model name.
	 * 
	 * @return the model name
	 */
	String getModelName();

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
	 * @since 1.1
	 */
	KTLCTFirmwareVersion getFirmwareVersion();

	/**
	 * Get the module (heat sink) temperature, in degrees Celsius.
	 * 
	 * @return the module temperature
	 */
	Float getModuleTemperature();

	/**
	 * Get the internal (ambient) temperature, in degrees Celsius.
	 * 
	 * @return the internal temperature
	 */
	Float getInternalTemperature();

	/**
	 * Get the transformer temperature, in degrees Celsius.
	 * 
	 * @return the transformer temperature
	 */
	Float getTransformerTemperature();

	/**
	 * Get the active energy delivered today, in Wh.
	 * 
	 * @return the delivered active energy today only
	 */
	Long getActiveEnergyDeliveredToday();

	/**
	 * Get the PV 1 string voltage.
	 * 
	 * @return the voltage for PV string 1
	 */
	Float getPv1Voltage();

	/**
	 * Get the PV 1 string current, in A.
	 * 
	 * @return the current for PV string 1
	 */
	Float getPv1Current();

	/**
	 * Get the PV 2 string voltage.
	 * 
	 * @return the voltage for PV string 2
	 */
	Float getPv2Voltage();

	/**
	 * Get the PV 2 string current, in A.
	 * 
	 * @return the current for PV string 2
	 */
	Float getPv2Current();

	/**
	 * Get the PV 3 string voltage.
	 * 
	 * @return the voltage for PV string 3
	 */
	Float getPv3Voltage();

	/**
	 * Get the PV 3 string current, in A.
	 * 
	 * @return the current for PV string 3
	 */
	Float getPv3Current();

	/**
	 * Get the DC current, in A.
	 * 
	 * @return the DC current
	 * @since 2.1
	 */
	Float getDcCurrent();

	/**
	 * Get the device operating state.
	 * 
	 * @return the state
	 * @since 1.2
	 */
	DeviceOperatingState getDeviceOperatingState();

	/**
	 * Get an enforced output power limit, in a percentage of maximum power
	 * capability.
	 * 
	 * @return the output power limit as a percentage (0 - 1)
	 * @since 1.2
	 */
	Float getOutputPowerLimitPercent();

	/**
	 * Get the inverter efficiency rating, as a percentage from 0-1.
	 * 
	 * @return the efficiency rating
	 * @since 2.1
	 */
	Float getEfficiency();

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
