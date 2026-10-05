/* ==================================================================
 * DerVoltVarModelAccessorImpl.java - 5/10/2026 4:58:20 pm
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
 * Implementation of {@link DerVoltVarModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerVoltVarModelAccessorImpl extends BaseDerCurveModelAccessor
		implements DerVoltVarModelAccessor {

	/** The DER volt-var model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 13;

	/** The DER volt-var model curve settings length. */
	public static final int CURVE_SETTINGS_LENGTH = 10;

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
	public DerVoltVarModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerVoltVarModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
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
		return DerVoltVarModelRegister.CurveReadOnly;
	}

	@Override
	protected SunspecModbusReference getPointXRegister() {
		return DerVoltVarModelRegister.PointVoltage;
	}

	@Override
	protected ModbusReference getPointXScaleFactorRegister() {
		return DerVoltVarModelRegister.ScaleFactorVoltage;
	}

	@Override
	protected SunspecModbusReference getPointYRegister() {
		return DerVoltVarModelRegister.PointReactivePower;
	}

	@Override
	protected ModbusReference getPointYScaleFactorRegister() {
		return DerVoltVarModelRegister.ScaleFactorReactivePower;
	}

	@Override
	public List<VoltVarCurve> getCurves() {
		return curves(VoltVarCurveImpl::new);
	}

	private final class VoltVarCurveImpl extends BaseDerCurve implements VoltVarCurve {

		private VoltVarCurveImpl(int index) {
			super(index);
		}

		@Override
		public @Nullable DerReactivePowerReference getDependentReference() {
			return getCodedValue(DerVoltVarModelRegister.CurveDependentReference, curveAddress,
					DerReactivePowerReference.class);
		}

		@Override
		public void setDependentReference(ModbusConnection conn, DerReactivePowerReference reference)
				throws IOException {
			requireWritable();
			writeValue(conn, DerVoltVarModelRegister.CurveDependentReference, curveAddress,
					reference.getCode());
		}

		@Override
		public @Nullable DerReactivePowerPriority getPowerPriority() {
			return getCodedValue(DerVoltVarModelRegister.CurvePowerPriority, curveAddress,
					DerReactivePowerPriority.class);
		}

		@Override
		public void setPowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
				throws IOException {
			requireWritable();
			writeValue(conn, DerVoltVarModelRegister.CurvePowerPriority, curveAddress,
					priority.getCode());
		}

		@Override
		public @Nullable Float getVoltageReference() {
			return getScaledFloatValue(DerVoltVarModelRegister.CurveVoltageReference,
					DerVoltVarModelRegister.ScaleFactorVoltage, curveAddress, getBlockAddress());
		}

		@Override
		public void setVoltageReference(ModbusConnection conn, float percent) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerVoltVarModelRegister.CurveVoltageReference,
					DerVoltVarModelRegister.ScaleFactorVoltage, curveAddress, getBlockAddress(),
					percent);
		}

		@Override
		public @Nullable Float getAutonomousVoltageReference() {
			return getScaledFloatValue(DerVoltVarModelRegister.CurveAutonomousVoltageReference,
					DerVoltVarModelRegister.ScaleFactorVoltage, curveAddress, getBlockAddress());
		}

		@Override
		public @Nullable Boolean isAutonomousVoltageReferenceEnabled() {
			return getBooleanValue(DerVoltVarModelRegister.CurveAutonomousVoltageReferenceEnabled,
					curveAddress);
		}

		@Override
		public void setAutonomousVoltageReferenceEnabled(ModbusConnection conn, boolean enabled)
				throws IOException {
			requireWritable();
			writeValue(conn, DerVoltVarModelRegister.CurveAutonomousVoltageReferenceEnabled,
					curveAddress, enabled ? 1 : 0);
		}

		@Override
		public @Nullable Integer getAutonomousVoltageReferenceTimeConstant() {
			return getIntegerValue(DerVoltVarModelRegister.CurveAutonomousVoltageReferenceTimeConstant,
					curveAddress);
		}

		@Override
		public void setAutonomousVoltageReferenceTimeConstant(ModbusConnection conn, int seconds)
				throws IOException {
			requireWritable();
			writeValue(conn, DerVoltVarModelRegister.CurveAutonomousVoltageReferenceTimeConstant,
					curveAddress, seconds);
		}

		@Override
		public @Nullable Float getOpenLoopResponseTime() {
			return getScaledFloatValue(DerVoltVarModelRegister.CurveOpenLoopResponseTime,
					DerVoltVarModelRegister.ScaleFactorResponseTime, curveAddress, getBlockAddress());
		}

		@Override
		public void setOpenLoopResponseTime(ModbusConnection conn, float seconds) throws IOException {
			requireWritable();
			writeScaledValue(conn, DerVoltVarModelRegister.CurveOpenLoopResponseTime,
					DerVoltVarModelRegister.ScaleFactorResponseTime, curveAddress, getBlockAddress(),
					seconds);
		}

	}

}
