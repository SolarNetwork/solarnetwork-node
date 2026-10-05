/* ==================================================================
 * DerAcControlsModelAccessor.java - 5/10/2026 2:52:06 pm
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

package net.solarnetwork.node.hw.sunspec.der;

import java.io.IOException;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing DER AC controls model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>704</b>. It provides the
 * power factor when injecting active power, power factor when absorbing active
 * power, limit maximum active power, set active power, and set reactive power
 * functions. Each function has an enable setting, and a reversion timer: if the
 * timer is enabled and expires without the function settings being updated, the
 * function reverts to its reversion settings.
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
public interface DerAcControlsModelAccessor extends ModelAccessor {

	/**
	 * Get the power factor when injecting active power enable setting.
	 *
	 * @return {@literal true} if the power factor when injecting active power
	 *         function is enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isPowerFactorWhenInjectingEnabled();

	/**
	 * Set the power factor when injecting active power enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the power factor when injecting active
	 *        power function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenInjectingEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the power factor setpoint when injecting active power.
	 *
	 * @return the power factor, or {@code null} if not available
	 */
	@Nullable
	Float getPowerFactorWhenInjecting();

	/**
	 * Get the power factor excitation setpoint when injecting active power.
	 *
	 * @return the excitation, or {@code null} if not available
	 */
	@Nullable
	DerPowerFactorExcitation getPowerFactorExcitationWhenInjecting();

	/**
	 * Set the power factor setpoint when injecting active power.
	 *
	 * <p>
	 * The power factor and excitation are written together in a single request,
	 * as SunSpec requires.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor
	 * @param excitation
	 *        the excitation
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenInjecting(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException;

	/**
	 * Get the power factor when injecting active power reversion timer enable
	 * setting.
	 *
	 * @return {@literal true} if the power factor when injecting active power
	 *         reversion timer is enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isPowerFactorWhenInjectingReversionEnabled();

	/**
	 * Set the power factor when injecting active power reversion timer enable
	 * setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the power factor when injecting active
	 *        power reversion timer
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenInjectingReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException;

	/**
	 * Get the power factor when injecting active power reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getPowerFactorWhenInjectingReversionTime();

	/**
	 * Set the power factor when injecting active power reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenInjectingReversionTime(ModbusConnection conn, long seconds)
			throws IOException;

	/**
	 * Get the power factor when injecting active power reversion time
	 * remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getPowerFactorWhenInjectingReversionTimeRemaining();

	/**
	 * Get the reversion power factor setpoint when injecting active power.
	 *
	 * @return the power factor, or {@code null} if not available
	 */
	@Nullable
	Float getReversionPowerFactorWhenInjecting();

	/**
	 * Get the reversion power factor excitation setpoint when injecting active
	 * power.
	 *
	 * @return the excitation, or {@code null} if not available
	 */
	@Nullable
	DerPowerFactorExcitation getReversionPowerFactorExcitationWhenInjecting();

	/**
	 * Set the reversion power factor setpoint when injecting active power.
	 *
	 * <p>
	 * The power factor and excitation are written together in a single request,
	 * as SunSpec requires.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor
	 * @param excitation
	 *        the excitation
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionPowerFactorWhenInjecting(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException;

	/**
	 * Get the power factor when absorbing active power enable setting.
	 *
	 * @return {@literal true} if the power factor when absorbing active power
	 *         function is enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isPowerFactorWhenAbsorbingEnabled();

	/**
	 * Set the power factor when absorbing active power enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the power factor when absorbing active
	 *        power function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenAbsorbingEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the power factor setpoint when absorbing active power.
	 *
	 * @return the power factor, or {@code null} if not available
	 */
	@Nullable
	Float getPowerFactorWhenAbsorbing();

	/**
	 * Get the power factor excitation setpoint when absorbing active power.
	 *
	 * @return the excitation, or {@code null} if not available
	 */
	@Nullable
	DerPowerFactorExcitation getPowerFactorExcitationWhenAbsorbing();

	/**
	 * Set the power factor setpoint when absorbing active power.
	 *
	 * <p>
	 * The power factor and excitation are written together in a single request,
	 * as SunSpec requires.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor
	 * @param excitation
	 *        the excitation
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenAbsorbing(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException;

	/**
	 * Get the power factor when absorbing active power reversion timer enable
	 * setting.
	 *
	 * @return {@literal true} if the power factor when absorbing active power
	 *         reversion timer is enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isPowerFactorWhenAbsorbingReversionEnabled();

	/**
	 * Set the power factor when absorbing active power reversion timer enable
	 * setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the power factor when absorbing active
	 *        power reversion timer
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenAbsorbingReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException;

	/**
	 * Get the power factor when absorbing active power reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getPowerFactorWhenAbsorbingReversionTime();

	/**
	 * Set the power factor when absorbing active power reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPowerFactorWhenAbsorbingReversionTime(ModbusConnection conn, long seconds)
			throws IOException;

	/**
	 * Get the power factor when absorbing active power reversion time
	 * remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getPowerFactorWhenAbsorbingReversionTimeRemaining();

	/**
	 * Get the reversion power factor setpoint when absorbing active power.
	 *
	 * @return the power factor, or {@code null} if not available
	 */
	@Nullable
	Float getReversionPowerFactorWhenAbsorbing();

	/**
	 * Get the reversion power factor excitation setpoint when absorbing active
	 * power.
	 *
	 * @return the excitation, or {@code null} if not available
	 */
	@Nullable
	DerPowerFactorExcitation getReversionPowerFactorExcitationWhenAbsorbing();

	/**
	 * Set the reversion power factor setpoint when absorbing active power.
	 *
	 * <p>
	 * The power factor and excitation are written together in a single request,
	 * as SunSpec requires.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor
	 * @param excitation
	 *        the excitation
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionPowerFactorWhenAbsorbing(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException;

	/**
	 * Get the limit maximum active power enable setting.
	 *
	 * @return {@literal true} if the limit maximum active power function is
	 *         enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isActivePowerLimitEnabled();

	/**
	 * Set the limit maximum active power enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the limit maximum active power function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the limit maximum active power setpoint.
	 *
	 * @return the limit, as a percentage of the maximum active power, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getActivePowerLimitPercent();

	/**
	 * Set the limit maximum active power setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the limit, as a percentage of the maximum active power
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the limit maximum active power reversion timer enable setting.
	 *
	 * @return {@literal true} if the limit maximum active power reversion timer
	 *         is enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isActivePowerLimitReversionEnabled();

	/**
	 * Set the limit maximum active power reversion timer enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the limit maximum active power reversion
	 *        timer
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitReversionEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the limit maximum active power reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getActivePowerLimitReversionTime();

	/**
	 * Set the limit maximum active power reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerLimitReversionTime(ModbusConnection conn, long seconds) throws IOException;

	/**
	 * Get the limit maximum active power reversion time remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getActivePowerLimitReversionTimeRemaining();

	/**
	 * Get the reversion limit maximum active power setpoint.
	 *
	 * @return the limit, as a percentage of the maximum active power, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getReversionActivePowerLimitPercent();

	/**
	 * Set the reversion limit maximum active power setpoint.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the limit, as a percentage of the maximum active power
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionActivePowerLimitPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the set active power enable setting.
	 *
	 * @return {@literal true} if the set active power function is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isActivePowerSetpointEnabled();

	/**
	 * Set the set active power enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the set active power function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpointEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the set active power mode.
	 *
	 * @return the mode, or {@code null} if not available
	 */
	@Nullable
	DerActivePowerSetpointMode getActivePowerSetpointMode();

	/**
	 * Set the set active power mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param mode
	 *        the mode to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpointMode(ModbusConnection conn, DerActivePowerSetpointMode mode)
			throws IOException;

	/**
	 * Get the active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#Watts} mode.
	 *
	 * @return the setpoint, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerSetpoint();

	/**
	 * Set the active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#Watts} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setpoint, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpoint(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#MaximumActivePowerPercent} mode.
	 *
	 * @return the setpoint, as a percentage of the maximum active power, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getActivePowerSetpointPercent();

	/**
	 * Set the active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#MaximumActivePowerPercent} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the setpoint, as a percentage of the maximum active power
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpointPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the set active power reversion timer enable setting.
	 *
	 * @return {@literal true} if the set active power reversion timer is
	 *         enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isActivePowerSetpointReversionEnabled();

	/**
	 * Set the set active power reversion timer enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the set active power reversion timer
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpointReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException;

	/**
	 * Get the set active power reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getActivePowerSetpointReversionTime();

	/**
	 * Set the set active power reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerSetpointReversionTime(ModbusConnection conn, long seconds) throws IOException;

	/**
	 * Get the set active power reversion time remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getActivePowerSetpointReversionTimeRemaining();

	/**
	 * Get the reversion active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#Watts} mode.
	 *
	 * @return the setpoint, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getReversionActivePowerSetpoint();

	/**
	 * Set the reversion active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#Watts} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setpoint, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionActivePowerSetpoint(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the reversion active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#MaximumActivePowerPercent} mode.
	 *
	 * @return the setpoint, as a percentage of the maximum active power, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getReversionActivePowerSetpointPercent();

	/**
	 * Set the reversion active power setpoint, used in the
	 * {@link DerActivePowerSetpointMode#MaximumActivePowerPercent} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the setpoint, as a percentage of the maximum active power
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionActivePowerSetpointPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the set reactive power enable setting.
	 *
	 * @return {@literal true} if the set reactive power function is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isReactivePowerSetpointEnabled();

	/**
	 * Set the set reactive power enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the set reactive power function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpointEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the set reactive power mode.
	 *
	 * @return the mode, or {@code null} if not available
	 */
	@Nullable
	DerReactivePowerSetpointMode getReactivePowerSetpointMode();

	/**
	 * Set the set reactive power mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param mode
	 *        the mode to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpointMode(ModbusConnection conn, DerReactivePowerSetpointMode mode)
			throws IOException;

	/**
	 * Get the reactive power priority.
	 *
	 * @return the priority, or {@code null} if not available
	 */
	@Nullable
	DerReactivePowerPriority getReactivePowerPriority();

	/**
	 * Set the reactive power priority.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param priority
	 *        the priority to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
			throws IOException;

	/**
	 * Get the reactive power setpoint, used in the
	 * {@link DerReactivePowerSetpointMode#Vars} mode.
	 *
	 * @return the setpoint, in var, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerSetpoint();

	/**
	 * Set the reactive power setpoint, used in the
	 * {@link DerReactivePowerSetpointMode#Vars} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the setpoint, in var
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpoint(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the reactive power setpoint, used in the percentage modes.
	 *
	 * @return the setpoint, as a percentage of the reference given by the mode,
	 *         or {@code null} if not available
	 */
	@Nullable
	Float getReactivePowerSetpointPercent();

	/**
	 * Set the reactive power setpoint, used in the percentage modes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the setpoint, as a percentage of the reference given by the mode
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpointPercent(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the set reactive power reversion timer enable setting.
	 *
	 * @return {@literal true} if the set reactive power reversion timer is
	 *         enabled, or {@code null} if not available
	 */
	@Nullable
	Boolean isReactivePowerSetpointReversionEnabled();

	/**
	 * Set the set reactive power reversion timer enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the set reactive power reversion timer
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpointReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException;

	/**
	 * Get the set reactive power reversion timeout.
	 *
	 * @return the timeout, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getReactivePowerSetpointReversionTime();

	/**
	 * Set the set reactive power reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerSetpointReversionTime(ModbusConnection conn, long seconds) throws IOException;

	/**
	 * Get the set reactive power reversion time remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getReactivePowerSetpointReversionTimeRemaining();

	/**
	 * Get the reversion reactive power setpoint, used in the
	 * {@link DerReactivePowerSetpointMode#Vars} mode.
	 *
	 * @return the setpoint, in var, or {@code null} if not available
	 */
	@Nullable
	Integer getReversionReactivePowerSetpoint();

	/**
	 * Set the reversion reactive power setpoint, used in the
	 * {@link DerReactivePowerSetpointMode#Vars} mode.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the setpoint, in var
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionReactivePowerSetpoint(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the reversion reactive power setpoint, used in the percentage modes.
	 *
	 * @return the setpoint, as a percentage of the reference given by the mode,
	 *         or {@code null} if not available
	 */
	@Nullable
	Float getReversionReactivePowerSetpointPercent();

	/**
	 * Set the reversion reactive power setpoint, used in the percentage modes.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the setpoint, as a percentage of the reference given by the mode
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionReactivePowerSetpointPercent(ModbusConnection conn, float percent)
			throws IOException;

	/**
	 * Get the ramp rate for increases in active power during normal generation.
	 *
	 * @return the ramp rate, as a percentage of the ramp rate reference per
	 *         second, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePowerRampRate();

	/**
	 * Set the ramp rate for increases in active power during normal generation.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percentPerSecond
	 *        the ramp rate, as a percentage of the ramp rate reference per
	 *        second
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerRampRate(ModbusConnection conn, int percentPerSecond) throws IOException;

	/**
	 * Get the active power ramp rate reference.
	 *
	 * @return the reference, or {@code null} if not available
	 */
	@Nullable
	DerRampRateReference getActivePowerRampRateReference();

	/**
	 * Set the active power ramp rate reference.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param reference
	 *        the reference to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerRampRateReference(ModbusConnection conn, DerRampRateReference reference)
			throws IOException;

	/**
	 * Get the reactive power ramp rate.
	 *
	 * @return the ramp rate, as a percentage of the maximum reactive power per
	 *         second, or {@code null} if not available
	 */
	@Nullable
	Integer getReactivePowerRampRate();

	/**
	 * Set the reactive power ramp rate.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percentPerSecond
	 *        the ramp rate, as a percentage of the maximum reactive power per
	 *        second
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerRampRate(ModbusConnection conn, int percentPerSecond) throws IOException;

	/**
	 * Get the anti-islanding enable setting.
	 *
	 * @return {@literal true} if the anti-islanding function is enabled, or
	 *         {@code null} if not available
	 */
	@Nullable
	Boolean isAntiIslandingEnabled();

	/**
	 * Set the anti-islanding enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the anti-islanding function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setAntiIslandingEnabled(ModbusConnection conn, boolean enabled) throws IOException;

}
