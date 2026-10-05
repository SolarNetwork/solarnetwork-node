/* ==================================================================
 * InverterPricingSignalModelAccessorImpl.java - 6/10/2026 9:27:15 am
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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Implementation of {@link InverterPricingSignalModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class InverterPricingSignalModelAccessorImpl extends BaseModelAccessor
		implements InverterPricingSignalModelAccessor {

	/** The pricing signal model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 8;

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
	public InverterPricingSignalModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public InverterPricingSignalModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public @Nullable Boolean isPricingEnabled() {
		return getBitfieldBit(InverterPricingSignalModelRegister.PricingEnabled, 0);
	}

	@Override
	public void setPricingEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, InverterPricingSignalModelRegister.PricingEnabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable InverterPricingSignalType getPricingSignalType() {
		return getCodedValue(InverterPricingSignalModelRegister.PricingSignalType,
				InverterPricingSignalType.class);
	}

	@Override
	public void setPricingSignalType(ModbusConnection conn, InverterPricingSignalType type)
			throws IOException {
		writeValue(conn, InverterPricingSignalModelRegister.PricingSignalType, type.getCode());
	}

	@Override
	public @Nullable Float getPricingSignal() {
		return getScaledFloatValue(InverterPricingSignalModelRegister.PricingSignal,
				InverterPricingSignalModelRegister.ScaleFactorPricingSignal);
	}

	@Override
	public void setPricingSignal(ModbusConnection conn, float signal) throws IOException {
		writeScaledValue(conn, InverterPricingSignalModelRegister.PricingSignal,
				InverterPricingSignalModelRegister.ScaleFactorPricingSignal, signal);
	}

	@Override
	public @Nullable Integer getPricingTimeWindow() {
		return getIntegerValue(InverterPricingSignalModelRegister.PricingTimeWindow);
	}

	@Override
	public void setPricingTimeWindow(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterPricingSignalModelRegister.PricingTimeWindow, seconds);
	}

	@Override
	public @Nullable Integer getPricingReversionTime() {
		return getIntegerValue(InverterPricingSignalModelRegister.PricingReversionTime);
	}

	@Override
	public void setPricingReversionTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterPricingSignalModelRegister.PricingReversionTime, seconds);
	}

	@Override
	public @Nullable Integer getPricingRampTime() {
		return getIntegerValue(InverterPricingSignalModelRegister.PricingRampTime);
	}

	@Override
	public void setPricingRampTime(ModbusConnection conn, int seconds) throws IOException {
		writeValue(conn, InverterPricingSignalModelRegister.PricingRampTime, seconds);
	}

}
