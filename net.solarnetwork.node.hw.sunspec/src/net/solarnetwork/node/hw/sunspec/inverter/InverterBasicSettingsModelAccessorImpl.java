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

import net.solarnetwork.domain.datum.AcPhase;
import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.ReactivePowerAction;

/**
 * Data access object for an inverter basic settings model.
 * 
 * @author matt
 * @version 2.0
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
	public Integer getActivePowerMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Float getPccVoltage() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltagePcc,
				InverterBasicSettingsRegister.ScaleFactorVoltagePcc);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPccVoltageOffset() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltagePccOffset,
				InverterBasicSettingsRegister.ScaleFactorVoltagePccOffset);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getVoltageMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltageMaximum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getVoltageMinimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.VoltageMinimum,
				InverterBasicSettingsRegister.ScaleFactorVoltageMinimumMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getApparentPowerMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ApparentPowerMaximum,
				InverterBasicSettingsRegister.ScaleFactorApparentPowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ1Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ1Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ2Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ2Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ3Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ3Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ4Maximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ReactivePowerQ4Maximum,
				InverterBasicSettingsRegister.ScaleFactorReactivePowerMaximum);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Float getActivePowerRampRate() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerRampRate,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRate);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ1Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ1Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ2Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ2Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ3Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ3Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ4Minimum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.PowerFactorQ4Minimum,
				InverterBasicSettingsRegister.ScaleFactorPowerFactorMinimum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public ReactivePowerAction getImportExportChangeReactivePowerAction() {
		Number n = getData().getNumber(
				InverterBasicSettingsRegister.ImportExportChangeReactivePowerAction, getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterReactivePowerAction.forCode(n.intValue());
	}

	@Override
	public ApparentPowerCalculationMethod getApparentPowerCalculationMethod() {
		Number n = getData().getNumber(InverterBasicSettingsRegister.ApparentPowerCalculationMethod,
				getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterApparentPowerCalculationMethod.forCode(n.intValue());
	}

	@Override
	public Float getActivePowerRampRateMaximum() {
		Number n = getScaledValue(InverterBasicSettingsRegister.ActivePowerRampRateMaximum,
				InverterBasicSettingsRegister.ScaleFactorActivePowerRampRateMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getEcpFrequency() {
		Number n = getScaledValue(InverterBasicSettingsRegister.FrequencyMaximum,
				InverterBasicSettingsRegister.ScaleFactorFrequencyMaximum);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public AcPhase getConnectedPhase() {
		Number n = getData().getNumber(InverterBasicSettingsRegister.ConnectedPhase);
		AcPhase phase = null;
		if ( n != null ) {
			int v = n.intValue();
			switch (v) {
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

}
