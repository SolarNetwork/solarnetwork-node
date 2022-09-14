/* ==================================================================
 * ReversedInverterModelAccessor.java - 15/09/2022 9:26:08 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.OperatingState;

/**
 * * A "reversed" inverter model accessor that swaps import/export values.
 * 
 * 
 * @author matt
 * @version 1.0
 * @since 3.1
 */
public class ReversedInverterModelAccessor implements InverterModelAccessor {

	private final InverterModelAccessor delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the accessor to reverse
	 */
	public ReversedInverterModelAccessor(InverterModelAccessor delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return delegate.getDeviceInfo();
	}

	@Override
	public InverterModelAccessor accessorForPhase(AcPhase phase) {
		return new ReversedInverterModelAccessor(delegate.accessorForPhase(phase));
	}

	@Override
	public InverterModelAccessor reversed() {
		return delegate;
	}

	@Override
	public Instant getDataTimestamp() {
		return delegate.getDataTimestamp();
	}

	@Override
	public Float getFrequency() {
		return delegate.getFrequency();
	}

	@Override
	public Float getCurrent() {
		return delegate.getCurrent();
	}

	@Override
	public Float getNeutralCurrent() {
		return delegate.getNeutralCurrent();
	}

	@Override
	public Float getVoltage() {
		return delegate.getVoltage();
	}

	@Override
	public Float getLineVoltage() {
		return delegate.getLineVoltage();
	}

	@Override
	public Float getPowerFactor() {
		return delegate.getPowerFactor();
	}

	@Override
	public Integer getActivePower() {
		Integer n = delegate.getActivePower();
		return (n != null ? n * -1 : null);
	}

	@Override
	public Integer getApparentPower() {
		return delegate.getApparentPower();
	}

	@Override
	public Integer getReactivePower() {
		Integer n = delegate.getReactivePower();
		return (n != null ? n * -1 : null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return delegate.getActiveEnergyReceived();
	}

	@Override
	public Long getActiveEnergyReceived() {
		return delegate.getActiveEnergyDelivered();
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return delegate.getReactiveEnergyReceived();
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return delegate.getReactiveEnergyDelivered();
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return delegate.getApparentEnergyReceived();
	}

	@Override
	public Long getApparentEnergyReceived() {
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
	public ModelId getModelId() {
		return delegate.getModelId();
	}

	@Override
	public int getFixedBlockLength() {
		return delegate.getFixedBlockLength();
	}

	@Override
	public Long getActiveEnergyExported() {
		Long v = delegate.getActiveEnergyExported();
		return (v != null ? -v : null);
	}

	@Override
	public int getModelLength() {
		return delegate.getModelLength();
	}

	@Override
	public Float getDcCurrent() {
		Float v = delegate.getDcCurrent();
		return (v != null ? -v : null);
	}

	@Override
	public Float getDcVoltage() {
		return delegate.getDcVoltage();
	}

	@Override
	public Integer getDcPower() {
		Integer v = delegate.getDcPower();
		return (v != null ? -v : null);
	}

	@Override
	public Float getCabinetTemperature() {
		return delegate.getCabinetTemperature();
	}

	@Override
	public Float getHeatSinkTemperature() {
		return delegate.getHeatSinkTemperature();
	}

	@Override
	public Float getTransformerTemperature() {
		return delegate.getTransformerTemperature();
	}

	@Override
	public Float getOtherTemperature() {
		return delegate.getOtherTemperature();
	}

	@Override
	public OperatingState getOperatingState() {
		return delegate.getOperatingState();
	}

	@Override
	public Set<ModelEvent> getEvents() {
		return delegate.getEvents();
	}

}
