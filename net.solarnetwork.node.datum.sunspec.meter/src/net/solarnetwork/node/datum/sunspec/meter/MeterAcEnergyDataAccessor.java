/* ==================================================================
 * MeterAcEnergyDataAccessor.java - 10 Oct 2026 4:12:18 pm
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

package net.solarnetwork.node.datum.sunspec.meter;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.sunspec.api.meter.MeterModelAccessor;
import net.solarnetwork.util.ObjectUtils;

/**
 * Adapt a SunSpec {@link MeterModelAccessor} into an
 * {@link AcEnergyDataAccessor}.
 *
 * <p>
 * The meter <i>imported</i> energy values are treated as <i>delivered</i>, and
 * <i>exported</i> as <i>received</i>.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 6.0
 */
public class MeterAcEnergyDataAccessor implements AcEnergyDataAccessor {

	private final MeterModelAccessor delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public MeterAcEnergyDataAccessor(MeterModelAccessor delegate) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public @Nullable Instant getDataTimestamp() {
		return delegate.getDataTimestamp();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		// device info is not available from the model accessor
		return Map.of();
	}

	@Override
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		return new MeterAcEnergyDataAccessor(
				delegate.accessorForPhase(net.solarnetwork.sunspec.api.AcPhase.forKey(phase.getKey())));
	}

	@Override
	public AcEnergyDataAccessor reversed() {
		return new MeterAcEnergyDataAccessor(delegate.reversed());
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
		// not supported in SunSpec
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
		var n = delegate.getActiveEnergyImported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Long getActiveEnergyReceived() {
		var n = delegate.getActiveEnergyExported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Integer getApparentPower() {
		var n = delegate.getApparentPower();
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getApparentEnergyDelivered() {
		var n = delegate.getApparentEnergyImported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Long getApparentEnergyReceived() {
		var n = delegate.getApparentEnergyExported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Integer getReactivePower() {
		var n = delegate.getReactivePower();
		return (n != null ? n.intValue() : null);
	}

	@Override
	public @Nullable Long getReactiveEnergyDelivered() {
		var n = delegate.getReactiveEnergyImported();
		return (n != null ? n.longValue() : null);
	}

	@Override
	public @Nullable Long getReactiveEnergyReceived() {
		var n = delegate.getReactiveEnergyExported();
		return (n != null ? n.longValue() : null);
	}

}
