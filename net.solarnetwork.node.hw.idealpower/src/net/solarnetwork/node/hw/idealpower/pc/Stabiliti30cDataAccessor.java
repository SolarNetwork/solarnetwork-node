/* ==================================================================
 * Stabiliti30cDataAccessor.java - 28/08/2019 12:06:26 pm
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

package net.solarnetwork.node.hw.idealpower.pc;

import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * API for reading Stabiliti 30C series power control system data.
 * 
 * @author matt
 * @version 1.0
 */
public interface Stabiliti30cDataAccessor extends DataAccessor {

	/* === Port 1 === */

	/**
	 * Get the configured P1 port type.
	 * 
	 * @return the port type
	 */
	Stabiliti30cAcPortType getP1PortType();

	/**
	 * Get the P1 active (real) power, in watts.
	 * 
	 * <p>
	 * This is positive when exporting to grid.
	 * </p>
	 * 
	 * @return the active power
	 */
	Integer getP1ActivePower();

	/**
	 * Get the P1 control method.
	 * 
	 * @return the control method
	 */
	Stabiliti30cAcControlMethod getP1ControlMethod();

	/**
	 * Get the P1 active (real) power setpoint for voltage-following mode.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP1ControlMethod()} is
	 * {@link Stabiliti30cAcControlMethod#GridPower} or
	 * {@link Stabiliti30cAcControlMethod#FacilityPower}.
	 * </p>
	 * 
	 * @return the active power setpoint, in watts
	 */
	Integer getP1ActivePowerSetpoint();

	/**
	 * Get the P1 line-to-line voltage setpoint for voltage-forming mode.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP1ControlMethod()} is
	 * {@link Stabiliti30cAcControlMethod#FacilityPower}.
	 * </p>
	 * 
	 * @return the active power setpoint, in volts
	 */
	Integer getP1VoltageSetpoint();

	/**
	 * Get the P1 AC frequency setpoint for voltage-forming mode.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP1ControlMethod()} is
	 * {@link Stabiliti30cAcControlMethod#FacilityPower}.
	 * </p>
	 * 
	 * @return the frequency setpoint, in hertz
	 */
	Float getP1FrequencySetpoint();

	/**
	 * Get the P1 current soft limit, in amps.
	 * 
	 * @return the current limit
	 */
	Float getP1CurrentLimit();

	/* === Port 2 === */

	/**
	 * Get the P2 voltage, positive to negative, in volts.
	 * 
	 * @return the voltage
	 */
	Float getP2Voltage();

	/**
	 * Get the P2 power, positive when exporting to battery (charging), in
	 * watts.
	 * 
	 * @return the power
	 */
	Integer getP2Power();

	/**
	 * Get the P2 current, in amps.
	 * 
	 * @return the current
	 */
	Float getP2Current();

	/**
	 * Get the P2 control method.
	 * 
	 * @return the control method
	 */
	Stabiliti30cDcControlMethod getP2ControlMethod();

	/**
	 * Get the P2 current limit.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP2ControlMethod()} is
	 * {@link Stabiliti30cDcControlMethod#DcCurrent}.
	 * </p>
	 * 
	 * @return the current setpoint, in amps
	 */
	Float getP2CurrentSetpoint();

	/**
	 * Get the P2 power setpoint.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP2ControlMethod()} is
	 * {@link Stabiliti30cDcControlMethod#DcPower}.
	 * </p>
	 * 
	 * @return the power setpoint, in watts
	 */
	Integer getP2PowerSetpoint();

	/**
	 * Get the P2 maximum DC operating voltage.
	 * 
	 * @return the voltage limit, in volts
	 */
	Integer getP2VoltageMaximumLimit();

	/**
	 * Get the P2 minimum DC operating voltage.
	 * 
	 * @return the voltage limit, in volts
	 */
	Integer getP2VoltageMinimumLimit();

	/**
	 * Get the P2 import (discharge) soft power limit.
	 * 
	 * @return the power limit, in watts
	 */
	Integer getP2ImportPowerLimit();

	/**
	 * Get the P2 export (charge) soft power limit.
	 * 
	 * @return the power limit, in watts
	 */
	Integer getP2ExportPowerLimit();

	/**
	 * Get the P2 current soft limit absolute value.
	 * 
	 * @return the current limit, in amps
	 */
	Float getP2CurrentLimit();

	/* === Port 3 === */

	/**
	 * Get the P3 voltage, positive to negative, in volts.
	 * 
	 * @return the voltage
	 */
	Float getP3Voltage();

	/**
	 * Get the P3 power, negative when importing from PV, watts.
	 * 
	 * @return the power
	 */
	Integer getP3Power();

	/**
	 * Get the P3 current, in amps.
	 * 
	 * @return the current
	 */
	Float getP3Current();

	/**
	 * Get the P3 control method.
	 * 
	 * @return the control method
	 */
	Stabiliti30cDcControlMethod getP3ControlMethod();

	/**
	 * Get the P3 MPPT start time offset.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP3ControlMethod()} is
	 * {@link Stabiliti30cDcControlMethod#Mppt}.
	 * </p>
	 * 
	 * @return the time offset, in minutes from midnight
	 */
	Integer getP3MpptStartTimeOffsetSetpoint();

	/**
	 * Get the P3 MPPT stop time offset.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP3ControlMethod()} is
	 * {@link Stabiliti30cDcControlMethod#Mppt}.
	 * </p>
	 * 
	 * @return the time offset, in minutes from midnight
	 */
	Integer getP3MpptStopTimeOffsetSetpoint();

	/**
	 * Get the P3 MPPT stop time offset.
	 * 
	 * <p>
	 * This setpoint applies if {@link #getP3ControlMethod()} is
	 * {@link Stabiliti30cDcControlMethod#Mppt}.
	 * </p>
	 * 
	 * @return the voltage minimum, in volts
	 */
	Integer getP3MpptVoltageMinimumSetpoint();

	/**
	 * Get the P3 maximum voltage.
	 * 
	 * @return the voltage maximum, in volts
	 */
	Integer getP3VoltageMaximum();

	/**
	 * Get the P3 minimum voltage.
	 * 
	 * @return the voltage minimum, in volts
	 */
	Integer getP3VoltageMinimum();

	/**
	 * Get the P3 import soft power limit.
	 * 
	 * @return the power limit, in watts
	 */
	Integer getP3ImportPowerLimit();

	/**
	 * Get the P3 current soft limit absolute value.
	 * 
	 * @return the current limit, in amps
	 */
	Float getP3CurrentLimit();

	/* === Device information === */

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
	 * Get the communications module version.
	 * 
	 * @return the comms version
	 */
	String getCommunicationsVersion();

	/* === Operational status === */

	/**
	 * Get the manual mode status.
	 * 
	 * @return {@code true} if the manual mode has been enabled
	 */
	boolean isManualModeEnabled();

	/**
	 * Get the watchdog timeout value.
	 * 
	 * @return the watchdog timeout, in seconds; {@literal 0} means the timeout
	 *         is diabled; {@literal -1} means a timeout fault has occurred
	 */
	Integer getWatchdogTimeout();

	/**
	 * Get the standardized operating state.
	 * 
	 * @return the operating state
	 */
	DeviceOperatingState getDeviceOperatingState();

	/**
	 * Get the system information.
	 * 
	 * @return the information
	 */
	Set<Stabiliti30cSystemInfo> getSystemInfo();

	/**
	 * Get the operating mode.
	 * 
	 * @return the mode
	 */
	Stabiliti30cOperatingMode getOperatingMode();

	/**
	 * Get the system status.
	 * 
	 * @return the status
	 */
	Set<Stabiliti30cSystemStatus> getSystemStatus();

	/**
	 * Get the complete set of active faults, sorted by fault number.
	 * 
	 * @return the active faults
	 */
	SortedSet<Stabiliti30cFault> getFaults();

}
