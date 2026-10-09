/* ==================================================================
 * InverterAcEnergyDataAccessor.java - 9 Oct 2026 6:40:30 pm
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

package net.solarnetwork.node.datum.sunspec.inverter;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.sunspec.api.inverter.InverterModelAccessor;
import net.solarnetwork.util.ObjectUtils;

/**
 * Adapt a SunSpec {@link InverterModelAccessor} into an
 * {@link AcEnergyDataAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 6.0
 */
public class InverterAcEnergyDataAccessor implements AcEnergyDataAccessor {

	private final InverterModelAccessor delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public InverterAcEnergyDataAccessor(InverterModelAccessor delegate) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public @Nullable Instant getDataTimestamp() {
		return delegate.getDataTimestamp();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		return new InverterAcEnergyDataAccessor(
				delegate.accessorForPhase(net.solarnetwork.sunspec.api.AcPhase.forKey(phase.getKey())));
	}

	@Override
	public AcEnergyDataAccessor reversed() {
		return new InverterAcEnergyDataAccessor(delegate.reversed());
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
		return null;
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
		var n = delegate.getActivePower();
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getActiveEnergyDelivered() {
		var n = delegate.getActiveEnergyExported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		var n = delegate.getActiveEnergyImported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		var n = delegate.getApparentPower();
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public @Nullable Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public @Nullable Integer getReactivePower() {
		var n = delegate.getReactivePower();
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getReactiveEnergyDelivered() {
		var n = delegate.getReactiveEnergyExported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
		var n = delegate.getReactiveEnergyImported();
		return (n != null ? n.longValue() : null);
	}

}
