/* ==================================================================
 * FilterMessage.java - 23/09/2019 11:23:43 am
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

/**
 * Specialized message for socketcand {@literal filter} commands.
 * 
 * @author matt
 * @version 1.0
 */
public interface FilterMessage extends Message, Addressed, Temporal, DataContainer {

	/**
	 * Get the data filter value.
	 * 
	 * <p>
	 * A data filter is a bitmask applied to the data contained in other
	 * messages. The {@link DataContainer#getData()} method returns the filter
	 * value as a byte array, while this method returns the filter encoded as a
	 * {@code long}.
	 * </p>
	 * 
	 * @return the data filter
	 */
	long getDataFilter();

}
