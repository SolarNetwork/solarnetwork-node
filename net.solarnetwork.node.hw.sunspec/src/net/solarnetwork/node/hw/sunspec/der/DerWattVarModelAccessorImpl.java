/* ==================================================================
 * DerWattVarModelAccessorImpl.java - 5/10/2026 4:58:20 pm
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
 * Implementation of {@link DerWattVarModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerWattVarModelAccessorImpl extends BaseDerCurveModelAccessor
		implements DerWattVarModelAccessor {

	/** The DER watt-var model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 12;

	/** The DER watt-var model curve settings length. */
	public static final int CURVE_SETTINGS_LENGTH = 4;

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
	public DerWattVarModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerWattVarModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
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
		return DerWattVarModelRegister.CurveReadOnly;
	}

	@Override
	protected SunspecModbusReference getPointXRegister() {
		return DerWattVarModelRegister.PointActivePower;
	}

	@Override
	protected ModbusReference getPointXScaleFactorRegister() {
		return DerWattVarModelRegister.ScaleFactorActivePower;
	}

	@Override
	protected SunspecModbusReference getPointYRegister() {
		return DerWattVarModelRegister.PointReactivePower;
	}

	@Override
	protected ModbusReference getPointYScaleFactorRegister() {
		return DerWattVarModelRegister.ScaleFactorReactivePower;
	}

	@Override
	public List<WattVarCurve> getCurves() {
		return curves(WattVarCurveImpl::new);
	}

	private final class WattVarCurveImpl extends BaseDerCurve implements WattVarCurve {

		private WattVarCurveImpl(int index) {
			super(index);
		}

		@Override
		public @Nullable DerReactivePowerReference getDependentReference() {
			return getCodedValue(DerWattVarModelRegister.CurveDependentReference, curveAddress,
					DerReactivePowerReference.class);
		}

		@Override
		public void setDependentReference(ModbusConnection conn, DerReactivePowerReference reference)
				throws IOException {
			requireWritable();
			writeValue(conn, DerWattVarModelRegister.CurveDependentReference, curveAddress,
					reference.getCode());
		}

		@Override
		public @Nullable DerReactivePowerPriority getPowerPriority() {
			return getCodedValue(DerWattVarModelRegister.CurvePowerPriority, curveAddress,
					DerReactivePowerPriority.class);
		}

		@Override
		public void setPowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
				throws IOException {
			if ( priority == DerReactivePowerPriority.Vendor ) {
				throw new IllegalArgumentException(
						"The watt-var model does not support the vendor power priority.");
			}
			requireWritable();
			writeValue(conn, DerWattVarModelRegister.CurvePowerPriority, curveAddress,
					priority.getCode());
		}

	}

}
