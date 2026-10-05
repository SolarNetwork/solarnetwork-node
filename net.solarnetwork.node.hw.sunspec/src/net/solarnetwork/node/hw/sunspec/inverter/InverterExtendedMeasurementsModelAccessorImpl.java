/* ==================================================================
 * InverterExtendedMeasurementsModelAccessorImpl.java - 6/10/2026 7:52:30 am
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

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link InverterExtendedMeasurementsModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class InverterExtendedMeasurementsModelAccessorImpl extends BaseModelAccessor
		implements InverterExtendedMeasurementsModelAccessor {

	/** The inverter extended measurements model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 44;

	/** The SunSpec time epoch, 1 January 2000 00:00 UTC. */
	private static final Instant DEVICE_TIME_EPOCH = Instant.parse("2000-01-01T00:00:00Z");

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
	public InverterExtendedMeasurementsModelAccessorImpl(ModelData data, int baseAddress,
			ModelId modelId) {
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
	public InverterExtendedMeasurementsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(InverterExtendedMeasurementsModelRegister.class);
	}

	@Override
	public Set<InverterConnectionStatus> getPvConnectionStatus() {
		return getBitmaskableValues(InverterExtendedMeasurementsModelRegister.PvConnectionStatus,
				InverterConnectionStatus.class);
	}

	@Override
	public Set<InverterConnectionStatus> getStorageConnectionStatus() {
		return getBitmaskableValues(InverterExtendedMeasurementsModelRegister.StorageConnectionStatus,
				InverterConnectionStatus.class);
	}

	@Override
	public @Nullable Boolean isEcpConnected() {
		return getBitfieldBit(InverterExtendedMeasurementsModelRegister.EcpConnectionStatus, 0);
	}

	@Override
	public @Nullable Long getActiveEnergyExported() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ActiveEnergyExported);
	}

	@Override
	public @Nullable Long getApparentEnergyExported() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ApparentEnergyExported);
	}

	@Override
	public @Nullable Long getReactiveEnergyQ1() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ReactiveEnergyQ1);
	}

	@Override
	public @Nullable Long getReactiveEnergyQ2() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ReactiveEnergyQ2);
	}

	@Override
	public @Nullable Long getReactiveEnergyQ3() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ReactiveEnergyQ3);
	}

	@Override
	public @Nullable Long getReactiveEnergyQ4() {
		return getLongValue(InverterExtendedMeasurementsModelRegister.ReactiveEnergyQ4);
	}

	@Override
	public @Nullable Integer getReactivePowerAvailable() {
		return getScaledIntegerValue(InverterExtendedMeasurementsModelRegister.ReactivePowerAvailable,
				InverterExtendedMeasurementsModelRegister.ScaleFactorReactivePowerAvailable);
	}

	@Override
	public @Nullable Integer getActivePowerAvailable() {
		return getScaledIntegerValue(InverterExtendedMeasurementsModelRegister.ActivePowerAvailable,
				InverterExtendedMeasurementsModelRegister.ScaleFactorActivePowerAvailable);
	}

	@Override
	public Set<InverterSetpointLimit> getSetpointLimitsReached() {
		return getBitmaskableValues(InverterExtendedMeasurementsModelRegister.SetpointLimitsReached,
				InverterSetpointLimit.class);
	}

	@Override
	public Set<InverterControlFunction> getActiveControls() {
		return getBitmaskableValues(InverterExtendedMeasurementsModelRegister.ActiveControls,
				InverterControlFunction.class);
	}

	@Override
	public @Nullable String getTimeSource() {
		return getStringValue(InverterExtendedMeasurementsModelRegister.TimeSource);
	}

	@Override
	public @Nullable Instant getDeviceTime() {
		Long secs = getLongValue(InverterExtendedMeasurementsModelRegister.DeviceTime);
		return (secs != null ? DEVICE_TIME_EPOCH.plusSeconds(secs) : null);
	}

	@Override
	public Set<InverterRideThrough> getActiveRideThroughs() {
		return getBitmaskableValues(InverterExtendedMeasurementsModelRegister.ActiveRideThroughs,
				InverterRideThrough.class);
	}

	@Override
	public @Nullable Float getIsolationResistance() {
		return getScaledFloatValue(InverterExtendedMeasurementsModelRegister.IsolationResistance,
				InverterExtendedMeasurementsModelRegister.ScaleFactorIsolationResistance);
	}

}
