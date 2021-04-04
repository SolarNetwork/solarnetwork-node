/* ==================================================================
 * MuxFilterMessage.java - 23/09/2019 2:25:51 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.socketcand;

import net.solarnetwork.node.io.canbus.Addressed;
import net.solarnetwork.node.io.canbus.Temporal;

/**
 * Specialized message for socketcand {@literal muxfilter} commands.
 * 
 * @author matt
 * @version 1.0
 */
public interface MuxFilterMessage extends Message, Addressed, Temporal, DataContainer {

	/**
	 * Get the multiplex identifier bitmask.
	 * 
	 * @return the multiplex identifier bitmask
	 */
	long getMultiplexIdentifierBitmask();

	/**
	 * Get the list of multiplex data filters.
	 * 
	 * <p>
	 * Each value in the list represents a single data filter where the
	 * multiplex identifier is encoded in the bits specified by
	 * {@link #getMultiplexIdentifierBitmask()}.
	 * </p>
	 * 
	 * @return the multiplex data filters, never {@literal null}
	 */
	Iterable<Long> getMultiplexDataFilters();

}
