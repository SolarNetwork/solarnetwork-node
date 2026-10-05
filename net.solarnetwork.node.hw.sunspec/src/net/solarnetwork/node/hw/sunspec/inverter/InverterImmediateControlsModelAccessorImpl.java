/* ==================================================================
 * InverterImmediateControlsModelAccessorImpl.java - 6/10/2026 8:24:40 am
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
import java.util.Collection;
import java.util.EnumSet;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link InverterImmediateControlsModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class InverterImmediateControlsModelAccessorImpl extends BaseModelAccessor
		implements InverterImmediateControlsModelAccessor {

	/** The immediate inverter controls model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 24;

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public InverterImmediateControlsModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link InverterControlModelId} class will be used as the
	 * {@code ModelId} instance.
	 * </p>
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public InverterImmediateControlsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(InverterImmediateControlsModelRegister.class);
	}

	@Override
	public @Nullable Integer getConnectionTimeWindow() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ConnectionTimeWindow);
	}

	@Override
	public void setConnectionTimeWindow(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ConnectionTimeWindow, seconds);
	}

	@Override
	public @Nullable Integer getConnectionReversionTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ConnectionReversionTime);
	}

	@Override
	public void setConnectionReversionTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ConnectionReversionTime, seconds);
	}

	@Override
	public @Nullable InverterConnectionControl getConnectionControl() {
		return getCodedValue(InverterImmediateControlsModelRegister.ConnectionControl,
				InverterConnectionControl.class);
	}

	@Override
	public void setConnectionControl(ModbusConnection conn, InverterConnectionControl control)
			throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ConnectionControl, control.getCode());
	}

	@Override
	public @Nullable Float getActivePowerLimitPercent() {
		return getScaledFloatValue(InverterImmediateControlsModelRegister.ActivePowerLimitPercent,
				InverterImmediateControlsModelRegister.ScaleFactorActivePowerLimitPercent);
	}

	@Override
	public void setActivePowerLimitPercent(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, InverterImmediateControlsModelRegister.ActivePowerLimitPercent,
				InverterImmediateControlsModelRegister.ScaleFactorActivePowerLimitPercent, percent);
	}

	@Override
	public @Nullable Integer getActivePowerLimitTimeWindow() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ActivePowerLimitTimeWindow);
	}

	@Override
	public void setActivePowerLimitTimeWindow(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ActivePowerLimitTimeWindow, seconds);
	}

	@Override
	public @Nullable Integer getActivePowerLimitReversionTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ActivePowerLimitReversionTime);
	}

	@Override
	public void setActivePowerLimitReversionTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ActivePowerLimitReversionTime, seconds);
	}

	@Override
	public @Nullable Integer getActivePowerLimitRampTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ActivePowerLimitRampTime);
	}

	@Override
	public void setActivePowerLimitRampTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ActivePowerLimitRampTime, seconds);
	}

	@Override
	public @Nullable Boolean isActivePowerLimitEnabled() {
		return getBooleanValue(InverterImmediateControlsModelRegister.ActivePowerLimitEnabled);
	}

	@Override
	public void setActivePowerLimitEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ActivePowerLimitEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Float getFixedPowerFactor() {
		return getScaledFloatValue(InverterImmediateControlsModelRegister.FixedPowerFactor,
				InverterImmediateControlsModelRegister.ScaleFactorFixedPowerFactor);
	}

	@Override
	public void setFixedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, InverterImmediateControlsModelRegister.FixedPowerFactor,
				InverterImmediateControlsModelRegister.ScaleFactorFixedPowerFactor, powerFactor);
	}

	@Override
	public @Nullable Integer getFixedPowerFactorTimeWindow() {
		return getIntegerValue(InverterImmediateControlsModelRegister.FixedPowerFactorTimeWindow);
	}

	@Override
	public void setFixedPowerFactorTimeWindow(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.FixedPowerFactorTimeWindow, seconds);
	}

	@Override
	public @Nullable Integer getFixedPowerFactorReversionTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.FixedPowerFactorReversionTime);
	}

	@Override
	public void setFixedPowerFactorReversionTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.FixedPowerFactorReversionTime, seconds);
	}

	@Override
	public @Nullable Integer getFixedPowerFactorRampTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.FixedPowerFactorRampTime);
	}

	@Override
	public void setFixedPowerFactorRampTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.FixedPowerFactorRampTime, seconds);
	}

	@Override
	public @Nullable Boolean isFixedPowerFactorEnabled() {
		return getBooleanValue(InverterImmediateControlsModelRegister.FixedPowerFactorEnabled);
	}

	@Override
	public void setFixedPowerFactorEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.FixedPowerFactorEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Float getReactivePowerPercentOfMaximumActivePower() {
		return getScaledFloatValue(
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfMaximumActivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent);
	}

	@Override
	public void setReactivePowerPercentOfMaximumActivePower(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn,
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfMaximumActivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent, percent);
	}

	@Override
	public @Nullable Float getReactivePowerPercentOfMaximumReactivePower() {
		return getScaledFloatValue(
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfMaximumReactivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent);
	}

	@Override
	public void setReactivePowerPercentOfMaximumReactivePower(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn,
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfMaximumReactivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent, percent);
	}

	@Override
	public @Nullable Float getReactivePowerPercentOfAvailableReactivePower() {
		return getScaledFloatValue(
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfAvailableReactivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent);
	}

	@Override
	public void setReactivePowerPercentOfAvailableReactivePower(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn,
				InverterImmediateControlsModelRegister.ReactivePowerPercentOfAvailableReactivePower,
				InverterImmediateControlsModelRegister.ScaleFactorReactivePowerPercent, percent);
	}

	@Override
	public @Nullable Integer getReactivePowerPercentTimeWindow() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ReactivePowerPercentTimeWindow);
	}

	@Override
	public void setReactivePowerPercentTimeWindow(ModbusConnection conn, int seconds)
			throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ReactivePowerPercentTimeWindow, seconds);
	}

	@Override
	public @Nullable Integer getReactivePowerPercentReversionTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ReactivePowerPercentReversionTime);
	}

	@Override
	public void setReactivePowerPercentReversionTime(ModbusConnection conn, int seconds)
			throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ReactivePowerPercentReversionTime,
				seconds);
	}

	@Override
	public @Nullable Integer getReactivePowerPercentRampTime() {
		return getIntegerValue(InverterImmediateControlsModelRegister.ReactivePowerPercentRampTime);
	}

	@Override
	public void setReactivePowerPercentRampTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ReactivePowerPercentRampTime, seconds);
	}

	@Override
	public @Nullable InverterReactivePowerPercentMode getReactivePowerPercentMode() {
		return getCodedValue(InverterImmediateControlsModelRegister.ReactivePowerPercentMode,
				InverterReactivePowerPercentMode.class);
	}

	@Override
	public void setReactivePowerPercentMode(ModbusConnection conn, InverterReactivePowerPercentMode mode)
			throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ReactivePowerPercentMode,
				mode.getCode());
	}

	@Override
	public @Nullable Boolean isReactivePowerPercentEnabled() {
		return getBooleanValue(InverterImmediateControlsModelRegister.ReactivePowerPercentEnabled);
	}

	@Override
	public void setReactivePowerPercentEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, InverterImmediateControlsModelRegister.ReactivePowerPercentEnabled,
				enabled ? 1 : 0);
	}

}
