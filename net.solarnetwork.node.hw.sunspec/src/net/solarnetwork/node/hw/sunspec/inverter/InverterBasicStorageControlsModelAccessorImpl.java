/* ==================================================================
 * InverterBasicStorageControlsModelAccessorImpl.java - 6/10/2026 9:03:44 am
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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.storage.BatteryChargeStatus;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link InverterBasicStorageControlsModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class InverterBasicStorageControlsModelAccessorImpl extends BaseModelAccessor
		implements InverterBasicStorageControlsModelAccessor {

	/** The basic storage controls model fixed block length. */
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
	public InverterBasicStorageControlsModelAccessorImpl(ModelData data, int baseAddress,
			ModelId modelId) {
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
	public InverterBasicStorageControlsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(InverterBasicStorageControlsModelRegister.class);
	}

	@Override
	public @Nullable Integer getActivePowerChargeRateMaximum() {
		return getScaledIntegerValue(
				InverterBasicStorageControlsModelRegister.ActivePowerChargeRateMaximum,
				InverterBasicStorageControlsModelRegister.ScaleFactorActivePowerChargeRateMaximum);
	}

	@Override
	public void setActivePowerChargeRateMaximum(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.ActivePowerChargeRateMaximum,
				InverterBasicStorageControlsModelRegister.ScaleFactorActivePowerChargeRateMaximum,
				watts);
	}

	@Override
	public @Nullable Float getChargeRampRate() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.ChargeRampRate,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRampRate);
	}

	@Override
	public void setChargeRampRate(ModbusConnection conn, float percentPerSecond) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.ChargeRampRate,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRampRate,
				percentPerSecond);
	}

	@Override
	public @Nullable Float getDischargeRampRate() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.DischargeRampRate,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRampRate);
	}

	@Override
	public void setDischargeRampRate(ModbusConnection conn, float percentPerSecond) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.DischargeRampRate,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRampRate,
				percentPerSecond);
	}

	@Override
	public Set<InverterStorageControlMode> getStorageControlModes() {
		return getBitmaskableValues(InverterBasicStorageControlsModelRegister.StorageControlModes,
				InverterStorageControlMode.class);
	}

	@Override
	public void setStorageControlModes(ModbusConnection conn, Set<InverterStorageControlMode> modes)
			throws IOException {
		writeValue(conn, InverterBasicStorageControlsModelRegister.StorageControlModes,
				Bitmaskable.bitmaskValue(modes));
	}

	@Override
	public @Nullable Integer getApparentPowerChargeRateMaximum() {
		return getScaledIntegerValue(
				InverterBasicStorageControlsModelRegister.ApparentPowerChargeRateMaximum,
				InverterBasicStorageControlsModelRegister.ScaleFactorApparentPowerChargeRateMaximum);
	}

	@Override
	public void setApparentPowerChargeRateMaximum(ModbusConnection conn, int voltAmps)
			throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.ApparentPowerChargeRateMaximum,
				InverterBasicStorageControlsModelRegister.ScaleFactorApparentPowerChargeRateMaximum,
				voltAmps);
	}

	@Override
	public @Nullable Float getStateOfChargeReserveMinimum() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.StateOfChargeReserveMinimum,
				InverterBasicStorageControlsModelRegister.ScaleFactorStateOfChargeReserveMinimum);
	}

	@Override
	public void setStateOfChargeReserveMinimum(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.StateOfChargeReserveMinimum,
				InverterBasicStorageControlsModelRegister.ScaleFactorStateOfChargeReserveMinimum,
				percent);
	}

	@Override
	public @Nullable Float getStateOfCharge() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.StateOfCharge,
				InverterBasicStorageControlsModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getStorageAvailable() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.StorageAvailable,
				InverterBasicStorageControlsModelRegister.ScaleFactorStorageAvailable);
	}

	@Override
	public @Nullable Float getBatteryVoltage() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.BatteryVoltage,
				InverterBasicStorageControlsModelRegister.ScaleFactorBatteryVoltage);
	}

	@Override
	public @Nullable BatteryChargeStatus getChargeStatus() {
		return getCodedValue(InverterBasicStorageControlsModelRegister.ChargeStatus,
				BatteryChargeStatus.class);
	}

	@Override
	public @Nullable Float getDischargeRatePercent() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.DischargeRatePercent,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRatePercent);
	}

	@Override
	public void setDischargeRatePercent(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.DischargeRatePercent,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRatePercent,
				percent);
	}

	@Override
	public @Nullable Float getChargeRatePercent() {
		return getScaledFloatValue(InverterBasicStorageControlsModelRegister.ChargeRatePercent,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRatePercent);
	}

	@Override
	public void setChargeRatePercent(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, InverterBasicStorageControlsModelRegister.ChargeRatePercent,
				InverterBasicStorageControlsModelRegister.ScaleFactorChargeDischargeRatePercent,
				percent);
	}

	@Override
	public @Nullable Integer getChargeDischargeRateTimeWindow() {
		return getIntegerValue(InverterBasicStorageControlsModelRegister.ChargeDischargeRateTimeWindow);
	}

	@Override
	public void setChargeDischargeRateTimeWindow(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterBasicStorageControlsModelRegister.ChargeDischargeRateTimeWindow,
				seconds);
	}

	@Override
	public @Nullable Integer getChargeDischargeRateReversionTime() {
		return getIntegerValue(
				InverterBasicStorageControlsModelRegister.ChargeDischargeRateReversionTime);
	}

	@Override
	public void setChargeDischargeRateReversionTime(ModbusConnection conn, int seconds)
			throws IOException {
		writeValue(conn, InverterBasicStorageControlsModelRegister.ChargeDischargeRateReversionTime,
				seconds);
	}

	@Override
	public @Nullable Integer getChargeDischargeRateRampTime() {
		return getIntegerValue(InverterBasicStorageControlsModelRegister.ChargeDischargeRateRampTime);
	}

	@Override
	public void setChargeDischargeRateRampTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterBasicStorageControlsModelRegister.ChargeDischargeRateRampTime, seconds);
	}

	@Override
	public @Nullable InverterChargeSource getChargeSource() {
		return getCodedValue(InverterBasicStorageControlsModelRegister.ChargeSource,
				InverterChargeSource.class);
	}

	@Override
	public void setChargeSource(ModbusConnection conn, InverterChargeSource source) throws IOException {
		writeValue(conn, InverterBasicStorageControlsModelRegister.ChargeSource, source.getCode());
	}

}
