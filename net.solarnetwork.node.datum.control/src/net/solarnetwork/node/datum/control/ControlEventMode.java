/* ==================================================================
 * ControlEventMode.java - 9/04/2021 11:30:25 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.control;

/**
 * An enumeration of control event modes.
 * 
 * @author matt
 * @version 1.0
 */
public enum ControlEventMode {

	/** Only generate datum via polling, not in response to control events. */
	None,

	/** Generate datum only when control info is captured. */
	Capture,

	/** Generate datum only when control info changes. */
	Change,

	/** Generate datum when both control info is captured and changes. */
	CaptureAndChange,

}
