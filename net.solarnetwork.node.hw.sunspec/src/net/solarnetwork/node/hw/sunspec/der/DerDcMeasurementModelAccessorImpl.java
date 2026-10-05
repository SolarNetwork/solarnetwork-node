/* ==================================================================
 * DerDcMeasurementModelAccessorImpl.java - 5/10/2026 10:24:03 am
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link DerDcMeasurementModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerDcMeasurementModelAccessorImpl extends BaseModelAccessor
		implements DerDcMeasurementModelAccessor {

	/** The DER DC measurement model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 18;

	/** The DER DC measurement model port repeating block length. */
	public static final int REPEATING_BLOCK_LENGTH = 25;

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
	public DerDcMeasurementModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public DerDcMeasurementModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return REPEATING_BLOCK_LENGTH;
	}

	@Override
	public Set<Integer> getAlarmedPortIndexes() {
		return getBitfieldIndexes(DerDcMeasurementModelRegister.AlarmedPortsBitmask);
	}

	@Override
	public @Nullable Integer getPortCount() {
		return getIntegerValue(DerDcMeasurementModelRegister.NumberOfPorts);
	}

	@Override
	public @Nullable Float getDCCurrent() {
		return getScaledFloatValue(DerDcMeasurementModelRegister.DcCurrent,
				DerDcMeasurementModelRegister.ScaleFactorDcCurrent);
	}

	@Override
	public @Nullable Integer getDCPower() {
		return getScaledIntegerValue(DerDcMeasurementModelRegister.DcPower,
				DerDcMeasurementModelRegister.ScaleFactorDcPower);
	}

	@Override
	public @Nullable Long getDCEnergyInjected() {
		return getScaledLongValue(DerDcMeasurementModelRegister.DcEnergyInjected,
				DerDcMeasurementModelRegister.ScaleFactorDcEnergy);
	}

	@Override
	public @Nullable Long getDCEnergyAbsorbed() {
		return getScaledLongValue(DerDcMeasurementModelRegister.DcEnergyAbsorbed,
				DerDcMeasurementModelRegister.ScaleFactorDcEnergy);
	}

	@Override
	public List<DcPort> getDcPorts() {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer portCount = getPortCount();
		final int count = (portCount != null ? Math.min(portCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<DcPort> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			result.add(new DerDcPort(i));
		}
		return result;
	}

	private final class DerDcPort implements DcPort {

		private final int portAddress;

		private DerDcPort(int index) {
			super();
			this.portAddress = getBlockAddress() + FIXED_BLOCK_LENGTH + index * REPEATING_BLOCK_LENGTH;
		}

		@Override
		public @Nullable DerDcPortType getPortType() {
			return getCodedValue(DerDcMeasurementModelRegister.PortType, portAddress,
					DerDcPortType.class);
		}

		@Override
		public @Nullable Integer getPortId() {
			return getIntegerValue(DerDcMeasurementModelRegister.PortId, portAddress);
		}

		@Override
		public @Nullable String getPortName() {
			return getStringValue(DerDcMeasurementModelRegister.PortName, portAddress);
		}

		@Override
		public @Nullable Float getDCCurrent() {
			return getScaledFloatValue(DerDcMeasurementModelRegister.PortDcCurrent,
					DerDcMeasurementModelRegister.ScaleFactorDcCurrent, portAddress, getBlockAddress());
		}

		@Override
		public @Nullable Float getDCVoltage() {
			return getScaledFloatValue(DerDcMeasurementModelRegister.PortDcVoltage,
					DerDcMeasurementModelRegister.ScaleFactorDcVoltage, portAddress, getBlockAddress());
		}

		@Override
		public @Nullable Integer getDCPower() {
			return getScaledIntegerValue(DerDcMeasurementModelRegister.PortDcPower,
					DerDcMeasurementModelRegister.ScaleFactorDcPower, portAddress, getBlockAddress());
		}

		@Override
		public @Nullable Long getDCEnergyInjected() {
			return getScaledLongValue(DerDcMeasurementModelRegister.PortDcEnergyInjected,
					DerDcMeasurementModelRegister.ScaleFactorDcEnergy, portAddress, getBlockAddress());
		}

		@Override
		public @Nullable Long getDCEnergyAbsorbed() {
			return getScaledLongValue(DerDcMeasurementModelRegister.PortDcEnergyAbsorbed,
					DerDcMeasurementModelRegister.ScaleFactorDcEnergy, portAddress, getBlockAddress());
		}

		@Override
		public @Nullable Float getTemperature() {
			return getScaledFloatValue(DerDcMeasurementModelRegister.PortTemperature,
					DerDcMeasurementModelRegister.ScaleFactorTemperature, portAddress,
					getBlockAddress());
		}

		@Override
		public @Nullable DerDcPortStatus getPortStatus() {
			return getCodedValue(DerDcMeasurementModelRegister.PortStatus, portAddress,
					DerDcPortStatus.class);
		}

		@Override
		public Set<ModelEvent> getEvents() {
			Number n = getBitfield(DerDcMeasurementModelRegister.PortAlarmsBitmask, portAddress);
			return DerDcPortAlarm.forBitmask(n != null ? n.longValue() : 0L);
		}

	}

}
