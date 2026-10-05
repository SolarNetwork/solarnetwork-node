/* ==================================================================
 * InverterBasicSettingsModelAccessorImpl.java - 15/10/2018 3:03:04 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.ReactivePowerAction;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Data access object for an inverter basic settings model.
 *
 * @author matt
 * @version 2.1
 * @since 1.2
 */
public class InverterBasicSettingsModelAccessorImpl extends BaseModelAccessor
		implements InverterBasicSettingsModelAccessor {

	/** The model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 30;

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
	public InverterBasicSettingsModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public InverterBasicSettingsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(InverterBasicSettingsRegister.class);
	}

	@Override
	public @Nullable Integer getActivePowerMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setActivePowerMaximum(ModbusConnection conn, int watts) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ActivePowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerMaximum, watts);
	}

	@Override
	public @Nullable Float getPccVoltage() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltagePcc,
				InverterBasicSettingsRegister.ScaleFactorVoltagePcc);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPccVoltage(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.VoltagePcc,
				InverterBasicSettingsRegister.ScaleFactorVoltagePcc, volts);
	}

	@Override
	public @Nullable Float getPccVoltageOffset() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltagePccOffset,
				InverterBasicSettingsRegister.ScaleFactorVoltagePccOffset);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPccVoltageOffset(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.VoltagePccOffset,
				InverterBasicSettingsRegister.ScaleFactorVoltagePccOffset, volts);
	}

	@Override
	public @Nullable Float getVoltageMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltageMaximum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setVoltageMaximum(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.VoltageMaximum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum, volts);
	}

	@Override
	public @Nullable Float getVoltageMinimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltageMinimum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setVoltageMinimum(ModbusConnection conn, float volts) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.VoltageMinimum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum, volts);
	}

	@Override
	public @Nullable Integer getApparentPowerMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ApparentPowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorApparentPowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setApparentPowerMaximum(ModbusConnection conn, int voltAmps) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ApparentPowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorApparentPowerMaximum, voltAmps);
	}

	@Override
	public @Nullable Integer getReactivePowerQ1Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ1Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setReactivePowerQ1Maximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ReactivePowerQ1Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum, vars);
	}

	@Override
	public @Nullable Integer getReactivePowerQ2Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ2Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setReactivePowerQ2Maximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ReactivePowerQ2Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum, vars);
	}

	@Override
	public @Nullable Integer getReactivePowerQ3Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ3Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setReactivePowerQ3Maximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ReactivePowerQ3Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum, vars);
	}

	@Override
	public @Nullable Integer getReactivePowerQ4Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ4Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public void setReactivePowerQ4Maximum(ModbusConnection conn, int vars) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ReactivePowerQ4Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum, vars);
	}

	@Override
	public @Nullable Float getActivePowerRampRate() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerRampRate,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRate);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setActivePowerRampRate(ModbusConnection conn, float percentPerSecond)
			throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ActivePowerRampRate,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRate, percentPerSecond);
	}

	@Override
	public @Nullable Float getPowerFactorQ1Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ1Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPowerFactorQ1Minimum(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.PowerFactorQ1Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum, powerFactor);
	}

	@Override
	public @Nullable Float getPowerFactorQ2Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ2Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPowerFactorQ2Minimum(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.PowerFactorQ2Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum, powerFactor);
	}

	@Override
	public @Nullable Float getPowerFactorQ3Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ3Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPowerFactorQ3Minimum(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.PowerFactorQ3Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum, powerFactor);
	}

	@Override
	public @Nullable Float getPowerFactorQ4Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ4Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setPowerFactorQ4Minimum(ModbusConnection conn, float powerFactor) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.PowerFactorQ4Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum, powerFactor);
	}

	@Override
	public @Nullable ReactivePowerAction getImportExportChangeReactivePowerAction() {
		return getCodedValue(InverterBasicSettingsRegister.ImportExportChangeReactivePowerAction,
				InverterReactivePowerAction.class);
	}

	@Override
	public void setImportExportChangeReactivePowerAction(ModbusConnection conn,
			ReactivePowerAction action) throws IOException {
		writeValue(conn, InverterBasicSettingsRegister.ImportExportChangeReactivePowerAction,
				action.getCode());
	}

	@Override
	public @Nullable ApparentPowerCalculationMethod getApparentPowerCalculationMethod() {
		return getCodedValue(InverterBasicSettingsRegister.ApparentPowerCalculationMethod,
				InverterApparentPowerCalculationMethod.class);
	}

	@Override
	public void setApparentPowerCalculationMethod(ModbusConnection conn,
			ApparentPowerCalculationMethod method) throws IOException {
		writeValue(conn, InverterBasicSettingsRegister.ApparentPowerCalculationMethod, method.getCode());
	}

	@Override
	public @Nullable Float getActivePowerRampRateMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerRampRateMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRateMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setActivePowerRampRateMaximum(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.ActivePowerRampRateMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRateMaximum, percent);
	}

	@Override
	public @Nullable Float getEcpFrequency() {
		Number n = getScaledValue(InverterBasicSettingsRegister.EcpNominalFrequency,
				InverterBasicSettingsRegister.ScaleFactorEcpNominalFrequency);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public void setEcpFrequency(ModbusConnection conn, float hertz) throws IOException {
		writeScaledValue(conn, InverterBasicSettingsRegister.EcpNominalFrequency,
				InverterBasicSettingsRegister.ScaleFactorEcpNominalFrequency, hertz);
	}

	@Override
	public @Nullable AcPhase getConnectedPhase() {
		Integer n = getIntegerValue(InverterBasicSettingsRegister.ConnectedPhase);
		AcPhase phase = null;
		if ( n != null ) {
			switch (n) {
				case 1:
					phase = AcPhase.PhaseA;
					break;

				case 2:
					phase = AcPhase.PhaseB;
					break;

				case 3:
					phase = AcPhase.PhaseC;
					break;

				default:
					break;
			}
		}
		return phase;
	}

	@Override
	public void setConnectedPhase(ModbusConnection conn, AcPhase phase) throws IOException {
		switch (phase) {
			case PhaseA:
			case PhaseB:
			case PhaseC:
				// the SunSpec phase codes match the AcPhase numbers
				writeValue(conn, InverterBasicSettingsRegister.ConnectedPhase, phase.getNumber());
				break;

			default:
				throw new IllegalArgumentException(
						String.format("The %s phase is not a connected phase.", phase));
		}
	}

}
