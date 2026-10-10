/* ==================================================================
 * MutableClock.java - 10/10/2026 1:42:00 PM
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock whose time can be set by tests.
 *
 * @author elijah
 * @version 1.0
 */
public class MutableClock extends Clock {

	private volatile Instant instant;

	/**
	 * Constructor.
	 *
	 * @param instant
	 *        the starting instant
	 */
	public MutableClock(Instant instant) {
		super();
		this.instant = instant;
	}

	/**
	 * Advance the clock.
	 *
	 * @param d
	 *        the amount to advance by
	 * @return the new instant
	 */
	public Instant advance(Duration d) {
		instant = instant.plus(d);
		return instant;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return instant;
	}

}
