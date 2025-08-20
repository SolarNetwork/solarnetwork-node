/* ==================================================================
 * BaseApplication.java - 8/08/2025 7:52:47 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.InstantSource;
import com.automatak.dnp3.LinkStatusListener;
import com.automatak.dnp3.enums.LinkStatus;

/**
 * Base DNP3 application.
 *
 * @author matt
 * @version 1.0
 */
public class BaseApplication implements LinkStatusListener {

	/** The clock. */
	protected final InstantSource clock;

	private LinkStatus linkStatus = LinkStatus.UNRESET;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The system UTC clock will be used.
	 * </p>
	 */
	public BaseApplication() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BaseApplication(InstantSource clock) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
	}

	@Override
	public void onStateChange(LinkStatus value) {
		linkStatus = value;
	}

	/**
	 * Get the current link status.
	 *
	 * @return the status
	 */
	protected LinkStatus getLinkStatus() {
		return linkStatus;
	}

}
