/* ==================================================================
 * BatteryBaseModelAccessorImpl.java - 5/10/2026 7:58:02 pm
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

package net.solarnetwork.node.hw.sunspec.storage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.BitSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.der.DerLocalRemoteControl;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Implementation of {@link BatteryBaseModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class BatteryBaseModelAccessorImpl extends BaseModelAccessor implements BatteryBaseModelAccessor {

	/** The battery base model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 62;

	/** The date the warranty date is counted in days from. */
	private static final LocalDate WARRANTY_DATE_EPOCH = LocalDate.of(2000, 1, 1);

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
	public BatteryBaseModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link StorageModelId} class will be used as the {@code ModelId}
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
	public BatteryBaseModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, StorageModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Float getChargeCapacityRating() {
		return getScaledFloatValue(BatteryBaseModelRegister.ChargeCapacityRating,
				BatteryBaseModelRegister.ScaleFactorChargeCapacity);
	}

	@Override
	public @Nullable Long getEnergyCapacityRating() {
		return getScaledLongValue(BatteryBaseModelRegister.EnergyCapacityRating,
				BatteryBaseModelRegister.ScaleFactorEnergyCapacity);
	}

	@Override
	public @Nullable Integer getChargeRateMaximumRating() {
		return getScaledIntegerValue(BatteryBaseModelRegister.ChargeRateMaximumRating,
				BatteryBaseModelRegister.ScaleFactorChargeDischargeRate);
	}

	@Override
	public @Nullable Integer getDischargeRateMaximumRating() {
		return getScaledIntegerValue(BatteryBaseModelRegister.DischargeRateMaximumRating,
				BatteryBaseModelRegister.ScaleFactorChargeDischargeRate);
	}

	@Override
	public @Nullable Float getSelfDischargeRate() {
		return getScaledFloatValue(BatteryBaseModelRegister.SelfDischargeRate,
				BatteryBaseModelRegister.ScaleFactorSelfDischargeRate);
	}

	@Override
	public @Nullable Float getStateOfChargeMaximumRating() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfChargeMaximumRating,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getStateOfChargeMinimumRating() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfChargeMinimumRating,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getStateOfChargeReserveMaximum() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfChargeReserveMaximum,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public void setStateOfChargeReserveMaximum(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, BatteryBaseModelRegister.StateOfChargeReserveMaximum,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge, percent);
	}

	@Override
	public @Nullable Float getStateOfChargeReserveMinimum() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfChargeReserveMinimum,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public void setStateOfChargeReserveMinimum(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, BatteryBaseModelRegister.StateOfChargeReserveMinimum,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge, percent);
	}

	@Override
	public @Nullable Float getStateOfCharge() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfCharge,
				BatteryBaseModelRegister.ScaleFactorStateOfCharge);
	}

	@Override
	public @Nullable Float getDepthOfDischarge() {
		return getScaledFloatValue(BatteryBaseModelRegister.DepthOfDischarge,
				BatteryBaseModelRegister.ScaleFactorDepthOfDischarge);
	}

	@Override
	public @Nullable Float getStateOfHealth() {
		return getScaledFloatValue(BatteryBaseModelRegister.StateOfHealth,
				BatteryBaseModelRegister.ScaleFactorStateOfHealth);
	}

	@Override
	public @Nullable Long getCycleCount() {
		return getLongValue(BatteryBaseModelRegister.CycleCount);
	}

	@Override
	public @Nullable BatteryChargeStatus getChargeStatus() {
		return getCodedValue(BatteryBaseModelRegister.ChargeStatus, BatteryChargeStatus.class);
	}

	@Override
	public @Nullable DerLocalRemoteControl getLocalRemoteControl() {
		return getCodedValue(BatteryBaseModelRegister.LocalRemoteControl, DerLocalRemoteControl.class);
	}

	@Override
	public @Nullable Integer getBatteryHeartbeat() {
		return getIntegerValue(BatteryBaseModelRegister.BatteryHeartbeat);
	}

	@Override
	public @Nullable Integer getControllerHeartbeat() {
		return getIntegerValue(BatteryBaseModelRegister.ControllerHeartbeat);
	}

	@Override
	public void setControllerHeartbeat(ModbusConnection conn, int heartbeat) throws IOException {
		writeValue(conn, BatteryBaseModelRegister.ControllerHeartbeat, heartbeat);
	}

	@Override
	public @Nullable Boolean isAlarmResetInProgress() {
		return getBooleanValue(BatteryBaseModelRegister.AlarmReset);
	}

	@Override
	public void resetAlarms(ModbusConnection conn) throws IOException {
		writeValue(conn, BatteryBaseModelRegister.AlarmReset, 1);
	}

	@Override
	public @Nullable BatteryType getBatteryType() {
		return getCodedValue(BatteryBaseModelRegister.BatteryType, BatteryType.class);
	}

	@Override
	public @Nullable BatteryState getBatteryState() {
		return getCodedValue(BatteryBaseModelRegister.BatteryState, BatteryState.class);
	}

	@Override
	public @Nullable Integer getVendorBatteryState() {
		return getIntegerValue(BatteryBaseModelRegister.VendorBatteryState);
	}

	@Override
	public @Nullable LocalDate getWarrantyDate() {
		final Long days = getLongValue(BatteryBaseModelRegister.WarrantyDate);
		return (days != null ? WARRANTY_DATE_EPOCH.plusDays(days) : null);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		Number n = getBitfield(BatteryBaseModelRegister.EventsBitmask);
		return BatteryEvent.forBitmask(n != null ? n.longValue() : 0L);
	}

	@Override
	public BitSet getVendorEvents() {
		final BitSet result = new BitSet(64);
		addBitfield(result, getBitfield(BatteryBaseModelRegister.VendorEventsBitmask), 0);
		addBitfield(result, getBitfield(BatteryBaseModelRegister.VendorEvents2Bitmask), 32);
		return result;
	}

	/**
	 * Add the bits of a SunSpec 32-bit bitfield to a bit set.
	 *
	 * <p>
	 * If the most significant bit is set, the bitfield is not implemented and
	 * no bits are added.
	 * </p>
	 *
	 * @param set
	 *        the set to add the bits to
	 * @param bitfield
	 *        the bitfield value
	 * @param offset
	 *        the index in {@code set} of the first bit of the bitfield
	 */
	private static void addBitfield(BitSet set, @Nullable Number bitfield, int offset) {
		final long bits = (bitfield != null ? bitfield.longValue() : 0L);
		if ( bits == 0 || (bits & 0x80000000L) != 0 ) {
			return;
		}
		for ( int i = 0; i < 31; i++ ) {
			if ( ((bits >> i) & 0x1) == 1 ) {
				set.set(offset + i);
			}
		}
	}

	@Override
	public @Nullable Float getDCVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.DcVoltage,
				BatteryBaseModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMaximumVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.MaximumVoltage,
				BatteryBaseModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMinimumVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.MinimumVoltage,
				BatteryBaseModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getMaximumCellVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.MaximumCellVoltage,
				BatteryBaseModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMaximumCellVoltageStringIndex() {
		return getIntegerValue(BatteryBaseModelRegister.MaximumCellVoltageStringIndex);
	}

	@Override
	public @Nullable Integer getMaximumCellVoltageModuleIndex() {
		return getIntegerValue(BatteryBaseModelRegister.MaximumCellVoltageModuleIndex);
	}

	@Override
	public @Nullable Float getMinimumCellVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.MinimumCellVoltage,
				BatteryBaseModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Integer getMinimumCellVoltageStringIndex() {
		return getIntegerValue(BatteryBaseModelRegister.MinimumCellVoltageStringIndex);
	}

	@Override
	public @Nullable Integer getMinimumCellVoltageModuleIndex() {
		return getIntegerValue(BatteryBaseModelRegister.MinimumCellVoltageModuleIndex);
	}

	@Override
	public @Nullable Float getAverageCellVoltage() {
		return getScaledFloatValue(BatteryBaseModelRegister.AverageCellVoltage,
				BatteryBaseModelRegister.ScaleFactorCellVoltage);
	}

	@Override
	public @Nullable Float getDCCurrent() {
		return getScaledFloatValue(BatteryBaseModelRegister.DcCurrent,
				BatteryBaseModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Float getMaximumChargeCurrent() {
		return getScaledFloatValue(BatteryBaseModelRegister.MaximumChargeCurrent,
				BatteryBaseModelRegister.ScaleFactorCurrentMaximum);
	}

	@Override
	public @Nullable Float getMaximumDischargeCurrent() {
		return getScaledFloatValue(BatteryBaseModelRegister.MaximumDischargeCurrent,
				BatteryBaseModelRegister.ScaleFactorCurrentMaximum);
	}

	@Override
	public @Nullable Integer getDCPower() {
		return getScaledIntegerValue(BatteryBaseModelRegister.DcPower,
				BatteryBaseModelRegister.ScaleFactorPower);
	}

	@Override
	public @Nullable BatteryInverterStateRequest getInverterStateRequest() {
		return getCodedValue(BatteryBaseModelRegister.InverterStateRequest,
				BatteryInverterStateRequest.class);
	}

	@Override
	public @Nullable Integer getPowerRequest() {
		return getScaledIntegerValue(BatteryBaseModelRegister.PowerRequest,
				BatteryBaseModelRegister.ScaleFactorPower);
	}

	@Override
	public @Nullable BatteryOperation getOperation() {
		return getCodedValue(BatteryBaseModelRegister.Operation, BatteryOperation.class);
	}

	@Override
	public void setOperation(ModbusConnection conn, BatteryOperation operation) throws IOException {
		writeValue(conn, BatteryBaseModelRegister.Operation, operation.getCode());
	}

	@Override
	public @Nullable BatteryInverterState getInverterState() {
		return getCodedValue(BatteryBaseModelRegister.InverterState, BatteryInverterState.class);
	}

	@Override
	public void setInverterState(ModbusConnection conn, BatteryInverterState state) throws IOException {
		writeValue(conn, BatteryBaseModelRegister.InverterState, state.getCode());
	}

}
