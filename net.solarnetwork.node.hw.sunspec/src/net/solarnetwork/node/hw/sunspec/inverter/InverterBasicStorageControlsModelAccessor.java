/* ==================================================================
 * InverterBasicStorageControlsModelAccessor.java - 6/10/2026 8:52:31 am
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

import java.io.IOException;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.storage.BatteryChargeStatus;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing SunSpec basic storage controls model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>124</b>. Setter methods
 * write to the device immediately, and throw {@link IllegalArgumentException}
 * if the value is not valid for the point, or {@link IllegalStateException} if
 * the model scale factors have not been read from the device or are not
 * implemented.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface InverterBasicStorageControlsModelAccessor extends ModelAccessor {

	/**
	 * Get the maximum charge rate setpoint.
	 *
	 * @return the maximum charge rate, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerChargeRateMaximum();

	/**
	 * Set the maximum charge rate setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the maximum charge rate, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerChargeRateMaximum(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the maximum charging ramp rate setpoint.
	 *
	 * @return the ramp rate, as a percentage of the maximum charge rate per
	 *         second, or {@code null} if not available
	 */
	@Nullable
	Float getChargeRampRate();

	/**
	 * Set the maximum charging ramp rate setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percentPerSecond
	 *        the ramp rate, as a percentage of the maximum charge rate per
	 *        second
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeRampRate(ModbusConnection conn, float percentPerSecond) throws IOException;

	/**
	 * Get the maximum discharging ramp rate setpoint.
	 *
	 * @return the ramp rate, as a percentage of the maximum charge rate per
	 *         second, or {@code null} if not available
	 */
	@Nullable
	Float getDischargeRampRate();

	/**
	 * Set the maximum discharging ramp rate setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percentPerSecond
	 *        the ramp rate, as a percentage of the maximum charge rate per
	 *        second
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setDischargeRampRate(ModbusConnection conn, float percentPerSecond) throws IOException;

	/**
	 * Get the active storage control modes.
	 *
	 * @return the modes, never {@code null}
	 */
	Set<InverterStorageControlMode> getStorageControlModes();

	/**
	 * Set the active storage control modes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param modes
	 *        the modes to activate; all other modes are deactivated
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setStorageControlModes(ModbusConnection conn, Set<InverterStorageControlMode> modes)
			throws IOException;

	/**
	 * Get the maximum charging apparent power setpoint.
	 *
	 * @return the maximum apparent power, in VA, or {@code null} if not
	 *         available
	 */
	@Nullable
	Integer getApparentPowerChargeRateMaximum();

	/**
	 * Set the maximum charging apparent power setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param voltAmps
	 *        the maximum apparent power, in VA
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setApparentPowerChargeRateMaximum(ModbusConnection conn, int voltAmps) throws IOException;

	/**
	 * Get the minimum reserve setpoint.
	 *
	 * @return the reserve, as a percentage of the nominal maximum storage, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getStateOfChargeReserveMinimum();

	/**
	 * Set the minimum reserve setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the reserve, as a percentage of the nominal maximum storage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setStateOfChargeReserveMinimum(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the state of charge.
	 *
	 * @return the currently available energy, as a percentage of the capacity
	 *         rating, or {@code null} if not available
	 */
	@Nullable
	Float getStateOfCharge();

	/**
	 * Get the storage available: the state of charge less the minimum reserve,
	 * times the capacity rating.
	 *
	 * @return the storage available, in Ah, or {@code null} if not available
	 */
	@Nullable
	Float getStorageAvailable();

	/**
	 * Get the internal battery voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getBatteryVoltage();

	/**
	 * Get the charge status.
	 *
	 * @return the status, or {@code null} if not available
	 */
	@Nullable
	BatteryChargeStatus getChargeStatus();

	/**
	 * Get the discharge rate.
	 *
	 * @return the discharge rate, as a percentage of the maximum discharge
	 *         rate, or {@code null} if not available
	 */
	@Nullable
	Float getDischargeRatePercent();

	/**
	 * Set the discharge rate.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the discharge rate, as a percentage of the maximum discharge rate
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setDischargeRatePercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the charge rate.
	 *
	 * @return the charge rate, as a percentage of the maximum charge rate, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getChargeRatePercent();

	/**
	 * Set the charge rate.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the charge rate, as a percentage of the maximum charge rate
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeRatePercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the time window for charge and discharge rate changes.
	 *
	 * @return the time window, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getChargeDischargeRateTimeWindow();

	/**
	 * Set the time window for charge and discharge rate changes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the time window, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeDischargeRateTimeWindow(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the charge and discharge rate reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getChargeDischargeRateReversionTime();

	/**
	 * Set the charge and discharge rate reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeDischargeRateReversionTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the charge and discharge rate ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @return the ramp time, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getChargeDischargeRateRampTime();

	/**
	 * Set the charge and discharge rate ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the ramp time, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeDischargeRateRampTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the charge source setting.
	 *
	 * @return the source, or {@code null} if not available
	 */
	@Nullable
	InverterChargeSource getChargeSource();

	/**
	 * Set the charge source setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param source
	 *        the source
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setChargeSource(ModbusConnection conn, InverterChargeSource source) throws IOException;

}
