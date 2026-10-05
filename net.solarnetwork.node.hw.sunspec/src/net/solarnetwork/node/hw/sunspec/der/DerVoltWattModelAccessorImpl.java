/* ==================================================================
 * DerVoltWattModelAccessorImpl.java - 5/10/2026 4:58:20 pm
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
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link DerVoltWattModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerVoltWattModelAccessorImpl extends BaseDerCurveModelAccessor
		implements DerVoltWattModelAccessor {

	/** The DER volt-watt model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 13;

	/** The DER volt-watt model curve settings length. */
	public static final int CURVE_SETTINGS_LENGTH = 5;

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
	public DerVoltWattModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerVoltWattModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected int getCurveSettingsLength() {
		return CURVE_SETTINGS_LENGTH;
	}

	@Override
	protected SunspecModbusReference getCurveReadOnlyRegister() {
		return DerVoltWattModelRegister.CurveReadOnly;
	}

	@Override
	protected SunspecModbusReference getPointXRegister() {
		return DerVoltWattModelRegister.PointVoltage;
	}

	@Override
	protected ModbusReference getPointXScaleFactorRegister() {
		return DerVoltWattModelRegister.ScaleFactorVoltage;
	}

	@Override
	protected SunspecModbusReference getPointYRegister() {
		return DerVoltWattModelRegister.PointActivePower;
	}

	@Override
	protected ModbusReference getPointYScaleFactorRegister() {
		return DerVoltWattModelRegister.ScaleFactorActivePower;
	}

	@Override
	public List<VoltWattCurve> getCurves() {
		return curves(VoltWattCurveImpl::new);
	}

	private final class VoltWattCurveImpl extends BaseDerCurve implements VoltWattCurve {

		private VoltWattCurveImpl(int index) {
			super(index);
		}

		@Override
		public @Nullable DerActivePowerReference getDependentReference() {
			return getCodedValue(DerVoltWattModelRegister.CurveDependentReference, curveAddress,
					DerActivePowerReference.class);
		}

		@Override
		public void setDependentReference(ModbusConnection conn, DerActivePowerReference reference)
				throws IOException {
			requireWritable();
			writeValue(conn, DerVoltWattModelRegister.CurveDependentReference, curveAddress,
					reference.getCode());
		}

		@Override
		public @Nullable Float getOpenLoopResponseTime() {
			return getScaledFloatValue(DerVoltWattModelRegister.CurveOpenLoopResponseTime,
					DerVoltWattModelRegister.ScaleFactorResponseTime, curveAddress, getBlockAddress());
		}

		@Override
		public void setOpenLoopResponseTime(ModbusConnection conn, float seconds) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerVoltWattModelRegister.CurveOpenLoopResponseTime,
					DerVoltWattModelRegister.ScaleFactorResponseTime, curveAddress, getBlockAddress(),
					seconds);
		}

	}

}
