/* ==================================================================
 * DerCapacityModelAccessorImpl.java - 5/10/2026 9:32:29 am
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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Implementation of {@link DerCapacityModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerCapacityModelAccessorImpl extends BaseModelAccessor implements DerCapacityModelAccessor {

	/** The DER capacity model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 50;

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
	public DerCapacityModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerCapacityModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Integer getActivePowerMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerMaximumRating,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Integer getActivePowerOverExcitedRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerOverExcitedRating,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Float getOverExcitedPowerFactorRating() {
		return getScaledFloatValue(DerCapacityModelRegister.OverExcitedPowerFactorRating,
				DerCapacityModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable Integer getActivePowerUnderExcitedRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerUnderExcitedRating,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Float getUnderExcitedPowerFactorRating() {
		return getScaledFloatValue(DerCapacityModelRegister.UnderExcitedPowerFactorRating,
				DerCapacityModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public @Nullable Integer getApparentPowerMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerMaximumRating,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public @Nullable Integer getReactivePowerInjectedMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ReactivePowerInjectedMaximumRating,
				DerCapacityModelRegister.ScaleFactorReactivePower);
	}

	@Override
	public @Nullable Integer getReactivePowerAbsorbedMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ReactivePowerAbsorbedMaximumRating,
				DerCapacityModelRegister.ScaleFactorReactivePower);
	}

	@Override
	public @Nullable Integer getActivePowerChargeRateMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerChargeRateMaximumRating,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Integer getActivePowerDischargeRateMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerDischargeRateMaximumRating,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public @Nullable Integer getApparentPowerChargeRateMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerChargeRateMaximumRating,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public @Nullable Integer getApparentPowerDischargeRateMaximumRating() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerDischargeRateMaximumRating,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public @Nullable Float getVoltageNominalRating() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageNominalRating,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getVoltageMaximumRating() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageMaximumRating,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getVoltageMinimumRating() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageMinimumRating,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public @Nullable Float getCurrentMaximumRating() {
		return getScaledFloatValue(DerCapacityModelRegister.CurrentMaximumRating,
				DerCapacityModelRegister.ScaleFactorCurrent);
	}

	@Override
	public @Nullable Float getReactiveSusceptanceRating() {
		return getScaledFloatValue(DerCapacityModelRegister.ReactiveSusceptanceRating,
				DerCapacityModelRegister.ScaleFactorSusceptance);
	}

	@Override
	public @Nullable DerNormalOperatingCategory getNormalOperatingCategory() {
		return getCodedValue(DerCapacityModelRegister.NormalOperatingCategoryRating,
				DerNormalOperatingCategory.class);
	}

	@Override
	public @Nullable DerAbnormalOperatingCategory getAbnormalOperatingCategory() {
		return getCodedValue(DerCapacityModelRegister.AbnormalOperatingCategoryRating,
				DerAbnormalOperatingCategory.class);
	}

	@Override
	public Set<DerControlMode> getSupportedControlModes() {
		return getBitmaskableValues(DerCapacityModelRegister.ControlModesBitmask, DerControlMode.class);
	}

	@Override
	public Set<DerIntentionalIslandCategory> getIntentionalIslandCategoriesRating() {
		return getBitmaskableValues(DerCapacityModelRegister.IntentionalIslandCategoriesRatingBitmask,
				DerIntentionalIslandCategory.class);
	}

	@Override
	public @Nullable Integer getActivePowerMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public void setActivePowerMaximum(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ActivePowerMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower, watts);
	}

	@Override
	public @Nullable Integer getActivePowerOverExcited() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerOverExcited,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public void setActivePowerOverExcited(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ActivePowerOverExcited,
				DerCapacityModelRegister.ScaleFactorActivePower, watts);
	}

	@Override
	public @Nullable Float getOverExcitedPowerFactor() {
		return getScaledFloatValue(DerCapacityModelRegister.OverExcitedPowerFactor,
				DerCapacityModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public void setOverExcitedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.OverExcitedPowerFactor,
				DerCapacityModelRegister.ScaleFactorPowerFactor, powerFactor);
	}

	@Override
	public @Nullable Integer getActivePowerUnderExcited() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerUnderExcited,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public void setActivePowerUnderExcited(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ActivePowerUnderExcited,
				DerCapacityModelRegister.ScaleFactorActivePower, watts);
	}

	@Override
	public @Nullable Float getUnderExcitedPowerFactor() {
		return getScaledFloatValue(DerCapacityModelRegister.UnderExcitedPowerFactor,
				DerCapacityModelRegister.ScaleFactorPowerFactor);
	}

	@Override
	public void setUnderExcitedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.UnderExcitedPowerFactor,
				DerCapacityModelRegister.ScaleFactorPowerFactor, powerFactor);
	}

	@Override
	public @Nullable Integer getApparentPowerMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public void setApparentPowerMaximum(ModbusConnection conn, int voltAmps) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ApparentPowerMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower, voltAmps);
	}

	@Override
	public @Nullable Integer getReactivePowerInjectedMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ReactivePowerInjectedMaximum,
				DerCapacityModelRegister.ScaleFactorReactivePower);
	}

	@Override
	public void setReactivePowerInjectedMaximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ReactivePowerInjectedMaximum,
				DerCapacityModelRegister.ScaleFactorReactivePower, vars);
	}

	@Override
	public @Nullable Integer getReactivePowerAbsorbedMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ReactivePowerAbsorbedMaximum,
				DerCapacityModelRegister.ScaleFactorReactivePower);
	}

	@Override
	public void setReactivePowerAbsorbedMaximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ReactivePowerAbsorbedMaximum,
				DerCapacityModelRegister.ScaleFactorReactivePower, vars);
	}

	@Override
	public @Nullable Integer getActivePowerChargeRateMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerChargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public void setActivePowerChargeRateMaximum(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ActivePowerChargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower, watts);
	}

	@Override
	public @Nullable Integer getActivePowerDischargeRateMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ActivePowerDischargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower);
	}

	@Override
	public void setActivePowerDischargeRateMaximum(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ActivePowerDischargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorActivePower, watts);
	}

	@Override
	public @Nullable Integer getApparentPowerChargeRateMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerChargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public void setApparentPowerChargeRateMaximum(ModbusConnection conn, int voltAmps)
			throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ApparentPowerChargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower, voltAmps);
	}

	@Override
	public @Nullable Integer getApparentPowerDischargeRateMaximum() {
		return getScaledIntegerValue(DerCapacityModelRegister.ApparentPowerDischargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower);
	}

	@Override
	public void setApparentPowerDischargeRateMaximum(ModbusConnection conn, int voltAmps)
			throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.ApparentPowerDischargeRateMaximum,
				DerCapacityModelRegister.ScaleFactorApparentPower, voltAmps);
	}

	@Override
	public @Nullable Float getVoltageNominal() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageNominal,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public void setVoltageNominal(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.VoltageNominal,
				DerCapacityModelRegister.ScaleFactorVoltage, volts);
	}

	@Override
	public @Nullable Float getVoltageMaximum() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageMaximum,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public void setVoltageMaximum(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.VoltageMaximum,
				DerCapacityModelRegister.ScaleFactorVoltage, volts);
	}

	@Override
	public @Nullable Float getVoltageMinimum() {
		return getScaledFloatValue(DerCapacityModelRegister.VoltageMinimum,
				DerCapacityModelRegister.ScaleFactorVoltage);
	}

	@Override
	public void setVoltageMinimum(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.VoltageMinimum,
				DerCapacityModelRegister.ScaleFactorVoltage, volts);
	}

	@Override
	public @Nullable Float getCurrentMaximum() {
		return getScaledFloatValue(DerCapacityModelRegister.CurrentMaximum,
				DerCapacityModelRegister.ScaleFactorCurrent);
	}

	@Override
	public void setCurrentMaximum(ModbusConnection conn, float amps) throws IOException {
		writeScaledValue(conn, DerCapacityModelRegister.CurrentMaximum,
				DerCapacityModelRegister.ScaleFactorCurrent, amps);
	}

	@Override
	public Set<DerIntentionalIslandCategory> getIntentionalIslandCategories() {
		return getBitmaskableValues(DerCapacityModelRegister.IntentionalIslandCategoriesBitmask,
				DerIntentionalIslandCategory.class);
	}

	@Override
	public void setIntentionalIslandCategories(ModbusConnection conn,
			Set<DerIntentionalIslandCategory> categories) throws IOException {
		writeValue(conn, DerCapacityModelRegister.IntentionalIslandCategoriesBitmask,
				Bitmaskable.bitmaskValue(categories));
	}

}
