/* ==================================================================
 * TestOperationalModesService.java - 10/10/2026 1:42:00 PM
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

package net.solarnetwork.node.setup.stomp.server.test;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.solarnetwork.node.service.OperationalModesService;

/**
 * In-memory {@link OperationalModesService} that records calls.
 *
 * <p>
 * Modes enabled without an expiration are excluded from
 * {@link #activeOperationalModesWithExpirations()}, and enabling a mode
 * replaces any existing expiration, as in the default implementation.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public class TestOperationalModesService implements OperationalModesService {

	private static final Long NO_EXPIRATION = Long.MAX_VALUE;

	private final Clock clock;
	private final Map<String, Long> modes = new HashMap<>();

	/** Recorded enable calls, as "mode@expiration" strings. */
	public final List<String> enableCalls = new ArrayList<>();

	/** Recorded disable calls. */
	public final List<String> disableCalls = new ArrayList<>();

	/** Recorded calling thread names for enable and disable calls. */
	public final List<String> callingThreads = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 */
	public TestOperationalModesService(Clock clock) {
		super();
		this.clock = clock;
	}

	/**
	 * Get the expiration of a mode.
	 *
	 * @param mode
	 *        the mode
	 * @return the expiration, or {@literal null} if not active or no expiration
	 */
	public synchronized Long expiration(String mode) {
		Long exp = modes.get(mode);
		return (exp == null || NO_EXPIRATION.equals(exp) ? null : exp);
	}

	@Override
	public synchronized boolean isOperationalModeActive(String mode) {
		Long exp = modes.get(mode);
		return (exp != null && (NO_EXPIRATION.equals(exp) || exp > clock.millis()));
	}

	@Override
	public synchronized Set<String> activeOperationalModes() {
		return modes.keySet().stream().filter(this::isOperationalModeActive).collect(Collectors.toSet());
	}

	@Override
	public synchronized Map<String, Long> activeOperationalModesWithExpirations() {
		final long now = clock.millis();
		return modes.entrySet().stream()
				.filter(e -> !NO_EXPIRATION.equals(e.getValue()) && e.getValue() > now)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Set<String> enableOperationalModes(Set<String> modes) {
		return enableOperationalModes(modes, null);
	}

	@Override
	public synchronized Set<String> enableOperationalModes(Set<String> modes, Instant expire) {
		callingThreads.add(Thread.currentThread().getName());
		for ( String m : modes ) {
			this.modes.put(m, expire != null ? expire.toEpochMilli() : NO_EXPIRATION);
			enableCalls.add(m + "@" + (expire != null ? expire.toEpochMilli() : "never"));
		}
		return activeOperationalModes();
	}

	@Override
	public synchronized Set<String> disableOperationalModes(Set<String> modes) {
		callingThreads.add(Thread.currentThread().getName());
		for ( String m : modes ) {
			this.modes.remove(m);
			disableCalls.add(m);
		}
		return activeOperationalModes();
	}

	@Override
	public UUID registerOperationalModeInfo(OperationalModeInfo info) {
		return UUID.randomUUID();
	}

	@Override
	public Stream<OperationalModeInfo> registeredOperationalModes() {
		return Stream.empty();
	}

	@Override
	public boolean unregisterOperationalModeInfo(UUID id) {
		return false;
	}

}
