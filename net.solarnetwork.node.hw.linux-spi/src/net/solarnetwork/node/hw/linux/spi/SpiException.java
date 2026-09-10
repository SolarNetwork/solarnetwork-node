/* ==================================================================
 * SpiException.java - 10 Sept 2026 8:10:45 pm
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

package net.solarnetwork.node.hw.linux.spi;

import java.io.Serial;

/**
 * Runtime exception thrown when a SPI operation fails.
 *
 * @author matt
 * @version 1.0
 */
public class SpiException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -7374514855356195574L;

	/**
	 * Construct with a message.
	 *
	 * @param message
	 *        the message
	 */
	public SpiException(String message) {
		super(message);
	}

	/**
	 * Construct with a message and cause.
	 *
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public SpiException(String message, Throwable cause) {
		super(message, cause);
	}

}
