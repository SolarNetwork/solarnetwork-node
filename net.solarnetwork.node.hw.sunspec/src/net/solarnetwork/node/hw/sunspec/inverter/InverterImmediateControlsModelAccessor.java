/* ==================================================================
 * InverterImmediateControlsModelAccessor.java - 6/10/2026 8:12:08 am
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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing SunSpec immediate inverter controls model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>123</b>. Each control has
 * a time window for changes and a reversion timeout, and all but the connection
 * control have a ramp time, all in seconds.
 * </p>
 *
 * <p>
 * Setter methods write to the device immediately, and throw
 * {@link IllegalArgumentException} if the value is not valid for the point, or
 * {@link IllegalStateException} if the model scale factors have not been read
 * from the device or are not implemented.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface InverterImmediateControlsModelAccessor extends ModelAccessor {

	/**
	 * Get the time window for connection control changes.
	 *
	 * @return the time window, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getConnectionTimeWindow();

	/**
	 * Set the time window for connection control changes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the time window, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setConnectionTimeWindow(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the connection control reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getConnectionReversionTime();

	/**
	 * Set the connection control reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setConnectionReversionTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the connection control.
	 *
	 * @return the control, or {@code null} if not available
	 */
	@Nullable
	InverterConnectionControl getConnectionControl();

	/**
	 * Set the connection control.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param control
	 *        the control
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setConnectionControl(ModbusConnection conn, InverterConnectionControl control)
			throws IOException;

	/**
	 * Get the active power limit setpoint.
	 *
	 * @return the setpoint, as a percentage of the maximum active power, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getActivePowerLimitPercent();

	/**
	 * Set the active power limit setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the setpoint, as a percentage of the maximum active power
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the time window for active power limit changes.
	 *
	 * @return the time window, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerLimitTimeWindow();

	/**
	 * Set the time window for active power limit changes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the time window, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitTimeWindow(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the active power limit reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerLimitReversionTime();

	/**
	 * Set the active power limit reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitReversionTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the active power limit ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @return the ramp time, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerLimitRampTime();

	/**
	 * Set the active power limit ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the ramp time, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitRampTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the active power limit enable setting.
	 *
	 * @return {@literal true} if the active power limit is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isActivePowerLimitEnabled();

	/**
	 * Set the active power limit enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the active power limit
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the fixed power factor setpoint.
	 *
	 * @return the setpoint, as a decimal from -1.0 to 1.0, or {@code null} if
	 *         not available
	 */
	@Nullable
	Float getFixedPowerFactor();

	/**
	 * Set the fixed power factor setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the setpoint, as a decimal from -1.0 to 1.0
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setFixedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the time window for fixed power factor changes.
	 *
	 * @return the time window, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getFixedPowerFactorTimeWindow();

	/**
	 * Set the time window for fixed power factor changes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the time window, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setFixedPowerFactorTimeWindow(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the fixed power factor reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getFixedPowerFactorReversionTime();

	/**
	 * Set the fixed power factor reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setFixedPowerFactorReversionTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the fixed power factor ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @return the ramp time, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getFixedPowerFactorRampTime();

	/**
	 * Set the fixed power factor ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the ramp time, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setFixedPowerFactorRampTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the fixed power factor enable setting.
	 *
	 * @return {@literal true} if the fixed power factor is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isFixedPowerFactorEnabled();

	/**
	 * Set the fixed power factor enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the fixed power factor
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setFixedPowerFactorEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the reactive power, as a percentage of the maximum active power, used
	 * in the {@link InverterReactivePowerPercentMode#MaximumActivePowerPercent}
	 * mode.
	 *
	 * @return the percentage, or {@code null} if not available
	 */
	@Nullable
	Float getReactivePowerPercentOfMaximumActivePower();

	/**
	 * Set the reactive power, as a percentage of the maximum active power, used
	 * in the {@link InverterReactivePowerPercentMode#MaximumActivePowerPercent}
	 * mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the percentage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentOfMaximumActivePower(ModbusConnection conn, float percent)
			throws IOException;

	/**
	 * Get the reactive power, as a percentage of the maximum reactive power,
	 * used in the
	 * {@link InverterReactivePowerPercentMode#MaximumReactivePowerPercent}
	 * mode.
	 *
	 * @return the percentage, or {@code null} if not available
	 */
	@Nullable
	Float getReactivePowerPercentOfMaximumReactivePower();

	/**
	 * Set the reactive power, as a percentage of the maximum reactive power,
	 * used in the
	 * {@link InverterReactivePowerPercentMode#MaximumReactivePowerPercent}
	 * mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the percentage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentOfMaximumReactivePower(ModbusConnection conn, float percent)
			throws IOException;

	/**
	 * Get the reactive power, as a percentage of the available reactive power,
	 * used in the
	 * {@link InverterReactivePowerPercentMode#AvailableReactivePowerPercent}
	 * mode.
	 *
	 * @return the percentage, or {@code null} if not available
	 */
	@Nullable
	Float getReactivePowerPercentOfAvailableReactivePower();

	/**
	 * Set the reactive power, as a percentage of the available reactive power,
	 * used in the
	 * {@link InverterReactivePowerPercentMode#AvailableReactivePowerPercent}
	 * mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the percentage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentOfAvailableReactivePower(ModbusConnection conn, float percent)
			throws IOException;

	/**
	 * Get the time window for reactive power percentage changes.
	 *
	 * @return the time window, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerPercentTimeWindow();

	/**
	 * Set the time window for reactive power percentage changes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the time window, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentTimeWindow(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the reactive power percentage reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerPercentReversionTime();

	/**
	 * Set the reactive power percentage reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentReversionTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the reactive power percentage ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @return the ramp time, in seconds, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerPercentRampTime();

	/**
	 * Set the reactive power percentage ramp time, for moving from the current
	 * setpoint to a new setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the ramp time, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentRampTime(ModbusConnection conn, int seconds) throws IOException;

	/**
	 * Get the reactive power percentage mode.
	 *
	 * @return the mode, or {@code null} if not available
	 */
	@Nullable
	InverterReactivePowerPercentMode getReactivePowerPercentMode();

	/**
	 * Set the reactive power percentage mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param mode
	 *        the mode
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentMode(ModbusConnection conn, InverterReactivePowerPercentMode mode)
			throws IOException;

	/**
	 * Get the reactive power percentage enable setting.
	 *
	 * @return {@literal true} if the reactive power percentage is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isReactivePowerPercentEnabled();

	/**
	 * Set the reactive power percentage enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the reactive power percentage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPercentEnabled(ModbusConnection conn, boolean enabled) throws IOException;

}
