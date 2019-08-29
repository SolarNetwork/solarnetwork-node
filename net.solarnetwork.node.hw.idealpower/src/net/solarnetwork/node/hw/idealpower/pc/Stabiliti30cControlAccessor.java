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

/**
 * API for controlling Stabiliti 30C series power control systems.
 * 
 * @author matt
 * @version 1.0
 */
public interface Stabiliti30cControlAccessor {

	/* === Port 1 === */

	/**
	 * Set the P1 control method.
	 * 
	 * @param method
	 *        the control method
	 */
	void setP1ControlMethod(Stabiliti30cAcControlMethod method);

	/**
	 * Set the P1 active (real) power setpoint for voltage-following mode.
	 * 
	 * @param power
	 *        the value to set, in watts
	 */
	void setP1ActivePowerSetpoint(Integer power);

	/**
	 * Set the P1 line-to-line voltage setpoint for voltage-forming mode.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 */
	void setP1VoltageSetpoint(Integer voltage);

	/**
	 * Set the P1 AC frequency setpoint for voltage-forming mode.
	 * 
	 * @param frequency
	 *        the value to set, in hertz
	 */
	void setP1FrequencySetpoint(Float frequency);

	/**
	 * Set the P1 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 */
	void setP1CurrentLimit(Float current);

	/* === Port 2 === */

	/**
	 * Set the P2 control method.
	 * 
	 * @param method
	 *        the control method
	 */
	void setP2ControlMethod(Stabiliti30cDcControlMethod method);

	/**
	 * Set the P2 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 */
	void setP2CurrentSetpoint(Float current);

	/**
	 * Set the P2 power setpoint.
	 * 
	 * @param power
	 *        the value to set, in watts
	 */
	void setP2PowerSetpoint(Integer power);

	/**
	 * Set the P2 maximum DC operating voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 */
	void setP2VoltageMaximumLimit(Integer voltage);

	/**
	 * Set the P2 minimum DC operating voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 */
	void setP2VoltageMinimumLimit(Integer voltage);

	/**
	 * Set the P2 import (discharge) soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 */
	void setP2ImportPowerLimit(Integer power);

	/**
	 * Set the P2 export (charge) soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 */
	void setP2ExportPowerLimit(Integer power);

	/**
	 * Set the P2 current soft limit absolute value.
	 * 
	 * @param current
	 *        the value to set, in amps
	 */
	void setP2CurrentLimit(Float current);

	/* === Port 3 === */

	/**
	 * Set the P3 control method.
	 * 
	 * @param method
	 *        the control method
	 */
	void setP3ControlMethod(Stabiliti30cDcControlMethod method);

	/**
	 * Set the P3 MPPT start time offset.
	 * 
	 * @param offset
	 *        the value to set, in minutes from midnight
	 */
	void setP3MpptStartTimeOffsetSetpoint(Integer offset);

	/**
	 * Set the P3 MPPT stop time offset.
	 * 
	 * @param offset
	 *        the value to set, in minutes from midnight
	 */
	void setP3MpptStopTimeOffsetSetpoint(Integer offset);

	/**
	 * Set the P3 MPPT stop time offset.
	 * 
	 * @param voltage
	 *        the voltage minimum, in volts
	 */
	void setP3MpptVoltageMinimumSetpoint(Integer voltage);

	/**
	 * Set the P3 maximum voltage.
	 * 
	 * @param voltage
	 *        the value to set, in volts
	 */
	void setP3VoltageMaximum(Integer voltage);

	/**
	 * Set the P3 minimum voltage.
	 * 
	 * @param voltage
	 *        the voltage to set, in volts
	 */
	void setP3VoltageMinimum(Integer voltage);

	/**
	 * Get the P3 import soft power limit.
	 * 
	 * @param power
	 *        the value to set, in watts
	 */
	void setP3ImportPowerLimit(Integer power);

	/**
	 * Set the P3 current soft limit.
	 * 
	 * @param current
	 *        the value to set, in amps
	 */
	void setP3CurrentLimit(Float current);

}
