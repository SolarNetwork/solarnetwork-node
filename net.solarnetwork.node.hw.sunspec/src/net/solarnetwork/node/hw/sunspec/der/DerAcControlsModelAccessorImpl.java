/* ==================================================================
 * DerAcControlsModelAccessorImpl.java - 5/10/2026 2:52:06 pm
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
import java.util.Collection;
import java.util.EnumSet;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link DerAcControlsModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerAcControlsModelAccessorImpl extends BaseModelAccessor
		implements DerAcControlsModelAccessor {

	/**
	 * The DER AC controls model fixed block length, including the power factor
	 * groups.
	 */
	public static final int FIXED_BLOCK_LENGTH = 65;

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
	public DerAcControlsModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link DerModelId} class will be used as the {@code ModelId}
	 * instance.
	 * </p>
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public DerAcControlsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(DerAcControlsModelRegister.class);
	}

	/**
	 * Write a power factor synchronization group.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactorRef
	 *        the power factor register, which the excitation register follows
	 * @param excitationRef
	 *        the excitation register
	 * @param powerFactor
	 *        the power factor
	 * @param excitation
	 *        the excitation
	 * @throws IOException
	 *         if any communication error occurs
	 */
	private void writePowerFactor(ModbusConnection conn, DerAcControlsModelRegister powerFactorRef,
			DerAcControlsModelRegister excitationRef, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException {
		final short[] pf = encodeScaledValue(powerFactorRef,
				DerAcControlsModelRegister.ScaleFactorPowerFactor, getBlockAddress(), powerFactor);
		final short[] ext = encodeValue(excitationRef, excitation.getCode());
		writeWords(conn, getBlockAddress() + powerFactorRef.getAddress(), new short[] { pf[0], ext[0] });
	}

	@Override
	public @Nullable Boolean isPowerFactorWhenInjectingEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.PowerFactorWhenInjectingEnabled);
	}

	@Override
	public void setPowerFactorWhenInjectingEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenInjectingEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Float getPowerFactorWhenInjecting() {
		return getScaledFloatValue(DerAcControlsModelRegister.PowerFactorWhenInjecting,
				DerAcControlsModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable DerPowerFactorExcitation getPowerFactorExcitationWhenInjecting() {
		return getCodedValue(DerAcControlsModelRegister.PowerFactorExcitationWhenInjecting,
				DerPowerFactorExcitation.class);
	}

	@Override
	public void setPowerFactorWhenInjecting(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException {
		writePowerFactor(conn, DerAcControlsModelRegister.PowerFactorWhenInjecting,
				DerAcControlsModelRegister.PowerFactorExcitationWhenInjecting, powerFactor, excitation);
	}

	@Override
	public @Nullable Boolean isPowerFactorWhenInjectingReversionEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.PowerFactorWhenInjectingReversionEnabled);
	}

	@Override
	public void setPowerFactorWhenInjectingReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenInjectingReversionEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Long getPowerFactorWhenInjectingReversionTime() {
		return getLongValue(DerAcControlsModelRegister.PowerFactorWhenInjectingReversionTime);
	}

	@Override
	public void setPowerFactorWhenInjectingReversionTime(ModbusConnection conn, long seconds)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenInjectingReversionTime, seconds);
	}

	@Override
	public @Nullable Long getPowerFactorWhenInjectingReversionTimeRemaining() {
		return getLongValue(DerAcControlsModelRegister.PowerFactorWhenInjectingReversionTimeRemaining);
	}

	@Override
	public @Nullable Float getReversionPowerFactorWhenInjecting() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReversionPowerFactorWhenInjecting,
				DerAcControlsModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable DerPowerFactorExcitation getReversionPowerFactorExcitationWhenInjecting() {
		return getCodedValue(DerAcControlsModelRegister.ReversionPowerFactorExcitationWhenInjecting,
				DerPowerFactorExcitation.class);
	}

	@Override
	public void setReversionPowerFactorWhenInjecting(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException {
		writePowerFactor(conn, DerAcControlsModelRegister.ReversionPowerFactorWhenInjecting,
				DerAcControlsModelRegister.ReversionPowerFactorExcitationWhenInjecting, powerFactor,
				excitation);
	}

	@Override
	public @Nullable Boolean isPowerFactorWhenAbsorbingEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.PowerFactorWhenAbsorbingEnabled);
	}

	@Override
	public void setPowerFactorWhenAbsorbingEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenAbsorbingEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Float getPowerFactorWhenAbsorbing() {
		return getScaledFloatValue(DerAcControlsModelRegister.PowerFactorWhenAbsorbing,
				DerAcControlsModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable DerPowerFactorExcitation getPowerFactorExcitationWhenAbsorbing() {
		return getCodedValue(DerAcControlsModelRegister.PowerFactorExcitationWhenAbsorbing,
				DerPowerFactorExcitation.class);
	}

	@Override
	public void setPowerFactorWhenAbsorbing(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException {
		writePowerFactor(conn, DerAcControlsModelRegister.PowerFactorWhenAbsorbing,
				DerAcControlsModelRegister.PowerFactorExcitationWhenAbsorbing, powerFactor, excitation);
	}

	@Override
	public @Nullable Boolean isPowerFactorWhenAbsorbingReversionEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.PowerFactorWhenAbsorbingReversionEnabled);
	}

	@Override
	public void setPowerFactorWhenAbsorbingReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenAbsorbingReversionEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Long getPowerFactorWhenAbsorbingReversionTime() {
		return getLongValue(DerAcControlsModelRegister.PowerFactorWhenAbsorbingReversionTime);
	}

	@Override
	public void setPowerFactorWhenAbsorbingReversionTime(ModbusConnection conn, long seconds)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.PowerFactorWhenAbsorbingReversionTime, seconds);
	}

	@Override
	public @Nullable Long getPowerFactorWhenAbsorbingReversionTimeRemaining() {
		return getLongValue(DerAcControlsModelRegister.PowerFactorWhenAbsorbingReversionTimeRemaining);
	}

	@Override
	public @Nullable Float getReversionPowerFactorWhenAbsorbing() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReversionPowerFactorWhenAbsorbing,
				DerAcControlsModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable DerPowerFactorExcitation getReversionPowerFactorExcitationWhenAbsorbing() {
		return getCodedValue(DerAcControlsModelRegister.ReversionPowerFactorExcitationWhenAbsorbing,
				DerPowerFactorExcitation.class);
	}

	@Override
	public void setReversionPowerFactorWhenAbsorbing(ModbusConnection conn, float powerFactor,
			DerPowerFactorExcitation excitation) throws IOException {
		writePowerFactor(conn, DerAcControlsModelRegister.ReversionPowerFactorWhenAbsorbing,
				DerAcControlsModelRegister.ReversionPowerFactorExcitationWhenAbsorbing, powerFactor,
				excitation);
	}

	@Override
	public @Nullable Boolean isActivePowerLimitEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ActivePowerLimitEnabled);
	}

	@Override
	public void setActivePowerLimitEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerLimitEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Float getActivePowerLimitPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ActivePowerLimitPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerLimitPercent);
	}

	@Override
	public void setActivePowerLimitPercent(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ActivePowerLimitPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerLimitPercent, percent);
	}

	@Override
	public @Nullable Boolean isActivePowerLimitReversionEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ActivePowerLimitReversionEnabled);
	}

	@Override
	public void setActivePowerLimitReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerLimitReversionEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Long getActivePowerLimitReversionTime() {
		return getLongValue(DerAcControlsModelRegister.ActivePowerLimitReversionTime);
	}

	@Override
	public void setActivePowerLimitReversionTime(ModbusConnection conn, long seconds)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerLimitReversionTime, seconds);
	}

	@Override
	public @Nullable Long getActivePowerLimitReversionTimeRemaining() {
		return getLongValue(DerAcControlsModelRegister.ActivePowerLimitReversionTimeRemaining);
	}

	@Override
	public @Nullable Float getReversionActivePowerLimitPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReversionActivePowerLimitPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerLimitPercent);
	}

	@Override
	public void setReversionActivePowerLimitPercent(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReversionActivePowerLimitPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerLimitPercent, percent);
	}

	@Override
	public @Nullable Boolean isActivePowerSetpointEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ActivePowerSetpointEnabled);
	}

	@Override
	public void setActivePowerSetpointEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerSetpointEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable DerActivePowerSetpointMode getActivePowerSetpointMode() {
		return getCodedValue(DerAcControlsModelRegister.ActivePowerSetpointMode,
				DerActivePowerSetpointMode.class);
	}

	@Override
	public void setActivePowerSetpointMode(ModbusConnection conn, DerActivePowerSetpointMode mode)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerSetpointMode, mode.getCode());
	}

	@Override
	public @Nullable Integer getActivePowerSetpoint() {
		return getScaledIntegerValue(DerAcControlsModelRegister.ActivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpoint);
	}

	@Override
	public void setActivePowerSetpoint(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ActivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpoint, watts);
	}

	@Override
	public @Nullable Float getActivePowerSetpointPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ActivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpointPercent);
	}

	@Override
	public void setActivePowerSetpointPercent(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ActivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpointPercent, percent);
	}

	@Override
	public @Nullable Boolean isActivePowerSetpointReversionEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ActivePowerSetpointReversionEnabled);
	}

	@Override
	public void setActivePowerSetpointReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerSetpointReversionEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Long getActivePowerSetpointReversionTime() {
		return getLongValue(DerAcControlsModelRegister.ActivePowerSetpointReversionTime);
	}

	@Override
	public void setActivePowerSetpointReversionTime(ModbusConnection conn, long seconds)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerSetpointReversionTime, seconds);
	}

	@Override
	public @Nullable Long getActivePowerSetpointReversionTimeRemaining() {
		return getLongValue(DerAcControlsModelRegister.ActivePowerSetpointReversionTimeRemaining);
	}

	@Override
	public @Nullable Integer getReversionActivePowerSetpoint() {
		return getScaledIntegerValue(DerAcControlsModelRegister.ReversionActivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpoint);
	}

	@Override
	public void setReversionActivePowerSetpoint(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReversionActivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpoint, watts);
	}

	@Override
	public @Nullable Float getReversionActivePowerSetpointPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReversionActivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpointPercent);
	}

	@Override
	public void setReversionActivePowerSetpointPercent(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReversionActivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorActivePowerSetpointPercent, percent);
	}

	@Override
	public @Nullable Boolean isReactivePowerSetpointEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ReactivePowerSetpointEnabled);
	}

	@Override
	public void setReactivePowerSetpointEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerSetpointEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable DerReactivePowerSetpointMode getReactivePowerSetpointMode() {
		return getCodedValue(DerAcControlsModelRegister.ReactivePowerSetpointMode,
				DerReactivePowerSetpointMode.class);
	}

	@Override
	public void setReactivePowerSetpointMode(ModbusConnection conn, DerReactivePowerSetpointMode mode)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerSetpointMode, mode.getCode());
	}

	@Override
	public @Nullable DerReactivePowerPriority getReactivePowerPriority() {
		return getCodedValue(DerAcControlsModelRegister.ReactivePowerPriority,
				DerReactivePowerPriority.class);
	}

	@Override
	public void setReactivePowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerPriority, priority.getCode());
	}

	@Override
	public @Nullable Integer getReactivePowerSetpoint() {
		return getScaledIntegerValue(DerAcControlsModelRegister.ReactivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpoint);
	}

	@Override
	public void setReactivePowerSetpoint(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReactivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpoint, vars);
	}

	@Override
	public @Nullable Float getReactivePowerSetpointPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReactivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpointPercent);
	}

	@Override
	public void setReactivePowerSetpointPercent(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReactivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpointPercent, percent);
	}

	@Override
	public @Nullable Boolean isReactivePowerSetpointReversionEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.ReactivePowerSetpointReversionEnabled);
	}

	@Override
	public void setReactivePowerSetpointReversionEnabled(ModbusConnection conn, boolean enabled)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerSetpointReversionEnabled,
				enabled ? 1 : 0);
	}

	@Override
	public @Nullable Long getReactivePowerSetpointReversionTime() {
		return getLongValue(DerAcControlsModelRegister.ReactivePowerSetpointReversionTime);
	}

	@Override
	public void setReactivePowerSetpointReversionTime(ModbusConnection conn, long seconds)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerSetpointReversionTime, seconds);
	}

	@Override
	public @Nullable Long getReactivePowerSetpointReversionTimeRemaining() {
		return getLongValue(DerAcControlsModelRegister.ReactivePowerSetpointReversionTimeRemaining);
	}

	@Override
	public @Nullable Integer getReversionReactivePowerSetpoint() {
		return getScaledIntegerValue(DerAcControlsModelRegister.ReversionReactivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpoint);
	}

	@Override
	public void setReversionReactivePowerSetpoint(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReversionReactivePowerSetpoint,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpoint, vars);
	}

	@Override
	public @Nullable Float getReversionReactivePowerSetpointPercent() {
		return getScaledFloatValue(DerAcControlsModelRegister.ReversionReactivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpointPercent);
	}

	@Override
	public void setReversionReactivePowerSetpointPercent(ModbusConnection conn, float percent)
			throws IOException {
		writeScaledValue(conn, DerAcControlsModelRegister.ReversionReactivePowerSetpointPercent,
				DerAcControlsModelRegister.ScaleFactorReactivePowerSetpointPercent, percent);
	}

	@Override
	public @Nullable Integer getActivePowerRampRate() {
		return getIntegerValue(DerAcControlsModelRegister.ActivePowerRampRate);
	}

	@Override
	public void setActivePowerRampRate(ModbusConnection conn, int percentPerSecond) throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerRampRate, percentPerSecond);
	}

	@Override
	public @Nullable DerRampRateReference getActivePowerRampRateReference() {
		return getCodedValue(DerAcControlsModelRegister.ActivePowerRampRateReference,
				DerRampRateReference.class);
	}

	@Override
	public void setActivePowerRampRateReference(ModbusConnection conn, DerRampRateReference reference)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ActivePowerRampRateReference, reference.getCode());
	}

	@Override
	public @Nullable Integer getReactivePowerRampRate() {
		return getIntegerValue(DerAcControlsModelRegister.ReactivePowerRampRate);
	}

	@Override
	public void setReactivePowerRampRate(ModbusConnection conn, int percentPerSecond)
			throws IOException {
		writeValue(conn, DerAcControlsModelRegister.ReactivePowerRampRate, percentPerSecond);
	}

	@Override
	public @Nullable Boolean isAntiIslandingEnabled() {
		return getBooleanValue(DerAcControlsModelRegister.AntiIslandingEnabled);
	}

	@Override
	public void setAntiIslandingEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, DerAcControlsModelRegister.AntiIslandingEnabled, enabled ? 1 : 0);
	}

}
