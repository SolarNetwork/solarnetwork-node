/* ==================================================================
 * Stabiliti30cControlAccessor.java - 30/08/2019 10:04:17 am
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

import java.io.IOException;

/**
 * API for controlling Stabiliti 30C series power control systems.
 * 
 * @author matt
 * @version 1.1
 */
public interface Stabiliti30cControlAccessor {

	/**
	 * Control manual mode.
	 * 
	 * @param enabled
	 *        {@literal true} to start manual mode, {@literal false} to stop
	 *        manual mode
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setManualModeEnabled(boolean enabled) throws IOException;

	/**
	 * Update the watchdog timeout.
	 * 
	 * @param seconds
	 *        the seconds to start counting from, or {@literal 0} to disable the
	 *        timeout
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setWatchdogTimeout(int seconds) throws IOException;

	/* === Port 1 === */

	/**
	 * Set the P1 control method.
	 * 
	 * @param method
	 *        the control method
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP1ControlMethod(Stabiliti30cAcControlMethod method) throws IOException;

	/**
	 * Set the P1 active (real) power setpoint for voltage-following mode.
	 * 
	 * @param power
	 *        the value to set, in watts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP1ActivePowerSetpoint(Integer power) throws IOException;

	/**
	 * Set the P1 line-to-line voltage setpoint for voltage-forming mode.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP1VoltageSetpoint(Integer voltage) throws IOException;

	/**
	 * Set the P1 AC frequency setpoint for voltage-forming mode.
	 * 
	 * @param frequency
	 *        the value to set, in hertz
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP1FrequencySetpoint(Float frequency) throws IOException;

	/**
	 * Set the P1 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP1CurrentLimit(Float current) throws IOException;

	/* === Port 2 === */

	/**
	 * Set the P2 control method.
	 * 
	 * @param method
	 *        the control method
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2ControlMethod(Stabiliti30cDcControlMethod method) throws IOException;

	/**
	 * Set the P2 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2CurrentSetpoint(Float current) throws IOException;

	/**
	 * Set the P2 power setpoint.
	 * 
	 * @param power
	 *        the value to set, in watts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2PowerSetpoint(Integer power) throws IOException;

	/**
	 * Set the P2 maximum DC operating voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2VoltageMaximumLimit(Integer voltage) throws IOException;

	/**
	 * Set the P2 minimum DC operating voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2VoltageMinimumLimit(Integer voltage) throws IOException;

	/**
	 * Set the P2 import (discharge) soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2ImportPowerLimit(Integer power) throws IOException;

	/**
	 * Set the P2 export (charge) soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2ExportPowerLimit(Integer power) throws IOException;

	/**
	 * Set the P2 current soft limit absolute value.
	 * 
	 * @param current
	 *        the value to set, in amps
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP2CurrentLimit(Float current) throws IOException;

	/* === Port 3 === */

	/**
	 * Set the P3 control method.
	 * 
	 * @param method
	 *        the control method
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3ControlMethod(Stabiliti30cDcControlMethod method) throws IOException;

	/**
	 * Set the P3 MPPT start time offset.
	 * 
	 * @param offset
	 *        the value to set, in minutes from midnight
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3MpptStartTimeOffsetSetpoint(Integer offset) throws IOException;

	/**
	 * Set the P3 MPPT stop time offset.
	 * 
	 * @param offset
	 *        the value to set, in minutes from midnight
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3MpptStopTimeOffsetSetpoint(Integer offset) throws IOException;

	/**
	 * Set the P3 MPPT stop time offset.
	 * 
	 * @param voltage
	 *        the voltage minimum, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3MpptVoltageMinimumSetpoint(Integer voltage) throws IOException;

	/**
	 * Set the P3 maximum voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3VoltageMaximum(Integer voltage) throws IOException;

	/**
	 * Set the P3 minimum voltage.
	 * 
	 * @param voltage
	 *        the voltage to set, in volts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3VoltageMinimum(Integer voltage) throws IOException;

	/**
	 * Get the P3 import soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3ImportPowerLimit(Integer power) throws IOException;

	/**
	 * Set the P3 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setP3CurrentLimit(Float current) throws IOException;

}
