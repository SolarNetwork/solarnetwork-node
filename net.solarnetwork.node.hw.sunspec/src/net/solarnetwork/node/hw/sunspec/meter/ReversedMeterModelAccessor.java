/* ==================================================================
 * ReversedMeterModelAccessor.java - 14/03/2019 10:33:42 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * A "reversed" meter model accessor that swaps import/export values.
 *
 * @author matt
 * @version 2.0
 * @since 1.3
 */
public class ReversedMeterModelAccessor implements MeterModelAccessor {

	private final MeterModelAccessor delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the accessor to reverse
	 */
	public ReversedMeterModelAccessor(MeterModelAccessor delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return delegate.getDeviceInfo();
	}

	@Override
	public MeterModelAccessor accessorForPhase(AcPhase phase) {
		return new ReversedMeterModelAccessor(delegate.accessorForPhase(phase));
	}

	@Override
	public MeterModelAccessor reversed() {
		return delegate;
	}

	@Override
	public @Nullable Instant getDataTimestamp() {
		return delegate.getDataTimestamp();
	}

	@Override
	public @Nullable Float getFrequency() {
		return delegate.getFrequency();
	}

	@Override
	public @Nullable Float getCurrent() {
		return delegate.getCurrent();
	}

	@Override
	public @Nullable Float getNeutralCurrent() {
		return delegate.getNeutralCurrent();
	}

	@Override
	public @Nullable Float getVoltage() {
		return delegate.getVoltage();
	}

	@Override
	public @Nullable Float getLineVoltage() {
		return delegate.getLineVoltage();
	}

	@Override
	public @Nullable Float getPowerFactor() {
		return delegate.getPowerFactor();
	}

	@Override
	public @Nullable Integer getActivePower() {
		Integer n = delegate.getActivePower();
		return (n != null ? n * -1 : null);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		return delegate.getApparentPower();
	}

	@Override
	public @Nullable Integer getReactivePower() {
		Integer n = delegate.getReactivePower();
		return (n != null ? n * -1 : null);
	}

	@Override
	public @Nullable Long getActiveEnergyDelivered() {
		return delegate.getActiveEnergyReceived();
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		return delegate.getActiveEnergyDelivered();
	}

	@Override
	public @Nullable Long getReactiveEnergyDelivered() {
		return delegate.getReactiveEnergyReceived();
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
		return delegate.getReactiveEnergyDelivered();
	}

	@Override
	public @Nullable Long getApparentEnergyDelivered() {
		return delegate.getApparentEnergyReceived();
	}

	@Override
	public @Nullable Long getApparentEnergyReceived() {
		return delegate.getApparentEnergyDelivered();
	}

	@Override
	public int getBaseAddress() {
		return delegate.getBaseAddress();
	}

	@Override
	public int getBlockAddress() {
		return delegate.getBlockAddress();
	}

	@Override
	public @Nullable Long getActiveEnergyImported() {
		return delegate.getActiveEnergyExported();
	}

	@Override
	public ModelId getModelId() {
		return delegate.getModelId();
	}

	@Override
	public int getFixedBlockLength() {
		return delegate.getFixedBlockLength();
	}

	@Override
	public @Nullable Long getActiveEnergyExported() {
		return delegate.getActiveEnergyImported();
	}

	@Override
	public @Nullable Long getReactiveEnergyImported() {
		return delegate.getReactiveEnergyExported();
	}

	@Override
	public int getModelLength() {
		return delegate.getModelLength();
	}

	@Override
	public @Nullable Long getReactiveEnergyExported() {
		return delegate.getReactiveEnergyImported();
	}

	@Override
	public @Nullable Long getApparentEnergyImported() {
		return delegate.getApparentEnergyExported();
	}

	@Override
	public @Nullable Long getApparentEnergyExported() {
		return delegate.getApparentEnergyImported();
	}

	@Override
	public Set<ModelEvent> getEvents() {
		return delegate.getEvents();
	}

}
