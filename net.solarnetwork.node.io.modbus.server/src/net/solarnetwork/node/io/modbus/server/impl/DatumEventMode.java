/* ==================================================================
 * DatumEventMode.java - 17/03/2026 9:19:01 am
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

package net.solarnetwork.node.io.modbus.server.impl;

/**
 * Enumeration of datum event modes.
 *
 * @author matt
 * @version 1.0
 * @since 5.6
 */
public enum DatumEventMode {

	/**
	 * Populate properties on the capture phase, before filters have been
	 * applied.
	 */
	Capture,

	/**
	 * Populate properties on the acquire phase, after filters have been
	 * applied.
	 */
	Acquire,

	/** Populate properties on both capture and acquire phases. */
	Both,

	;

}
