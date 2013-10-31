/* ===================================================================
 * SmaChannelParam.java
 * 
 * Created Sep 7, 2009 10:26:48 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.hw.sma.protocol;

/**
 * An SMA channel parameter enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaChannelParam {

	/** The unit of measurement this channel reports with. */
	Unit,

	/** The gain. */
	Gain,

	/** The offset. */
	Offset,

	/** The "low" text value. */
	TextLow,

	/** The "high" text value. */
	TextHigh,

	/** A status message. */
	Status;

}
