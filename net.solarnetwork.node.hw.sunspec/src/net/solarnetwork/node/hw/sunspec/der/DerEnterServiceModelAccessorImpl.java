/* ==================================================================
 * DerEnterServiceModelAccessorImpl.java - 5/10/2026 9:32:29 am
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
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Implementation of {@link DerEnterServiceModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerEnterServiceModelAccessorImpl extends BaseModelAccessor
		implements DerEnterServiceModelAccessor {

	/** The DER enter service model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 17;

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
	public DerEnterServiceModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerEnterServiceModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Boolean isEnterServicePermitted() {
		return getBooleanValue(DerEnterServiceModelRegister.Permitted);
	}

	@Override
	public void setEnterServicePermitted(ModbusConnection conn, boolean permitted) throws IOException {
		writeValue(conn, DerEnterServiceModelRegister.Permitted, permitted ? 1 : 0);
	}

	@Override
	public @Nullable Float getVoltageHigh() {
		return getScaledFloatValue(DerEnterServiceModelRegister.VoltageHigh,
				DerEnterServiceModelRegister.ScaleFactorVoltage);
	}

	@Override
	public void setVoltageHigh(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, DerEnterServiceModelRegister.VoltageHigh,
				DerEnterServiceModelRegister.ScaleFactorVoltage, percent);
	}

	@Override
	public @Nullable Float getVoltageLow() {
		return getScaledFloatValue(DerEnterServiceModelRegister.VoltageLow,
				DerEnterServiceModelRegister.ScaleFactorVoltage);
	}

	@Override
	public void setVoltageLow(ModbusConnection conn, float percent) throws IOException {
		writeScaledValue(conn, DerEnterServiceModelRegister.VoltageLow,
				DerEnterServiceModelRegister.ScaleFactorVoltage, percent);
	}

	@Override
	public @Nullable Float getFrequencyHigh() {
		return getScaledFloatValue(DerEnterServiceModelRegister.FrequencyHigh,
				DerEnterServiceModelRegister.ScaleFactorFrequency);
	}

	@Override
	public void setFrequencyHigh(ModbusConnection conn, float hertz) throws IOException {
		writeScaledValue(conn, DerEnterServiceModelRegister.FrequencyHigh,
				DerEnterServiceModelRegister.ScaleFactorFrequency, hertz);
	}

	@Override
	public @Nullable Float getFrequencyLow() {
		return getScaledFloatValue(DerEnterServiceModelRegister.FrequencyLow,
				DerEnterServiceModelRegister.ScaleFactorFrequency);
	}

	@Override
	public void setFrequencyLow(ModbusConnection conn, float hertz) throws IOException {
		writeScaledValue(conn, DerEnterServiceModelRegister.FrequencyLow,
				DerEnterServiceModelRegister.ScaleFactorFrequency, hertz);
	}

	@Override
	public @Nullable Long getDelay() {
		return getLongValue(DerEnterServiceModelRegister.Delay);
	}

	@Override
	public void setDelay(ModbusConnection conn, long seconds) throws IOException {
		writeValue(conn, DerEnterServiceModelRegister.Delay, seconds);
	}

	@Override
	public @Nullable Long getRandomDelay() {
		return getLongValue(DerEnterServiceModelRegister.RandomDelay);
	}

	@Override
	public void setRandomDelay(ModbusConnection conn, long seconds) throws IOException {
		writeValue(conn, DerEnterServiceModelRegister.RandomDelay, seconds);
	}

	@Override
	public @Nullable Long getRampTime() {
		return getLongValue(DerEnterServiceModelRegister.RampTime);
	}

	@Override
	public void setRampTime(ModbusConnection conn, long seconds) throws IOException {
		writeValue(conn, DerEnterServiceModelRegister.RampTime, seconds);
	}

	@Override
	public @Nullable Long getDelayRemaining() {
		return getLongValue(DerEnterServiceModelRegister.DelayRemaining);
	}

}
