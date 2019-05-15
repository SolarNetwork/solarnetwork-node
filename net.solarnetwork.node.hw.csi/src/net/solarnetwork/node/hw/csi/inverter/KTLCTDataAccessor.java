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

import java.util.Set;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.PVEnergyDataAccessor;

/**
 * API for reading CSI 50KTL-CT inverter series data.
 * 
 * @author matt
 * @version 1.2
 */
public interface KTLCTDataAccessor extends PVEnergyDataAccessor, ACEnergyDataAccessor {

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

}
