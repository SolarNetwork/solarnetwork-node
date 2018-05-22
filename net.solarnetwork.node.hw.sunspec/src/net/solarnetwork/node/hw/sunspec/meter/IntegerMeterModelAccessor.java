/* ==================================================================
 * IntegerMeterModelAccessor.java - 22/05/2018 6:31:57 AM
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

package net.solarnetwork.node.hw.sunspec.meter;

import java.util.Set;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Data object for an integer meter model.
 * 
 * @author matt
 * @version 1.0
 */
public class IntegerMeterModelAccessor extends BaseModelAccessor implements MeterModelAccessor {

	/** The integer meter model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 105;

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
	public IntegerMeterModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	public Float getFrequencyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorFrequency);
		return (n != null ? n.floatValue() : null);
	}

	public Float getCurrentValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorCurrent);
		return (n != null ? n.floatValue() : null);
	}

	public Float getVoltageValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorVoltage);
		return (n != null ? n.floatValue() : null);
	}

	public Float getPowerFactorValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorPowerFactor);
		return (n != null ? n.floatValue() : null);
	}

	public Integer getActivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorActivePower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getApparentPowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorApparentPower);
		return (n != null ? n.intValue() : null);
	}

	public Integer getReactivePowerValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorReactivePower);
		return (n != null ? n.intValue() : null);
	}

	public Long getActiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorActiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	public Long getApparentEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorApparentEnergy);
		return (n != null ? n.longValue() : null);
	}

	public Long getReactiveEnergyValue(ModbusReference ref) {
		Number n = getScaledValue(ref, IntegerMeterModelRegister.ScaleFactorReactiveEnergy);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Float getFrequency() {
		return getFrequencyValue(IntegerMeterModelRegister.Frequency);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(IntegerMeterModelRegister.CurrentTotal);
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(IntegerMeterModelRegister.VoltageLineNeutralAverage);
	}

	@Override
	public Float getPowerFactor() {
		return getPowerFactorValue(IntegerMeterModelRegister.PowerFactorAverage);
	}

	@Override
	public Integer getActivePower() {
		return getActivePowerValue(IntegerMeterModelRegister.ActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getApparentPowerValue(IntegerMeterModelRegister.ApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getReactivePowerValue(IntegerMeterModelRegister.ReactivePowerTotal);
	}

	@Override
	public Long getActiveEnergyImported() {
		return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyImportedTotal);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getActiveEnergyValue(IntegerMeterModelRegister.ActiveEnergyExportedTotal);
	}

	@Override
	public Long getReactiveEnergyImported() {
		Long q1 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyImportedQ1Total);
		Long q2 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyImportedQ2Total);
		return (q1 != null ? q1.longValue() : 0) + (q2 != null ? q2.longValue() : 0);
	}

	@Override
	public Long getReactiveEnergyExported() {
		Long q3 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyExportedQ3Total);
		Long q4 = getReactiveEnergyValue(IntegerMeterModelRegister.ReactiveEnergyExportedQ4Total);
		return (q3 != null ? q3.longValue() : 0) + (q4 != null ? q4.longValue() : 0);
	}

	@Override
	public Long getApparentEnergyImported() {
		return getApparentEnergyValue(IntegerMeterModelRegister.ApparentEnergyImportedTotal);
	}

	@Override
	public Long getApparentEnergyExported() {
		return getApparentEnergyValue(IntegerMeterModelRegister.ApparentEnergyExportedTotal);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		// TODO Auto-generated method stub
		return null;
	}

}
