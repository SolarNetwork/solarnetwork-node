/* ==================================================================
 * ADAM411xDataAccessor.java - 21/11/2018 7:03:25 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.advantech.adam;

import java.math.BigDecimal;
import java.util.Set;

/**
 * API for reading ADAM 411x data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ADAM411xDataAccessor {

	/**
	 * Get the device model name.
	 * 
	 * @return the model name
	 */
	String getModelName();

	/**
	 * Get the device revision.
	 * 
	 * @return the revision number
	 */
	String getFirmwareRevision();

	/**
	 * Get the complete set of enabled channel numbers.
	 * 
	 * <p>
	 * Channel numbers start at {@literal 0}.
	 * </p>
	 * 
	 * @return the enabled channel numbers
	 */
	Set<Integer> getEnabledChannelNumbers();

	/**
	 * Get the input range type of a given channel.
	 * 
	 * @param channelNumber
	 *        the channel number to get the type of, starting at {@literal 0}
	 * @return the input range type of the specified channel, or
	 *         {@link InputRangeType#Unknown} if not known
	 * @throws IllegalArgumentException
	 *         if {@code channelNumber} is outside the allowed range for the
	 *         device (e.g. {@literal 0} to {@literal 7})
	 */
	InputRangeType getChannelType(int channelNumber);

	/**
	 * Get the value of a given channel.
	 * 
	 * @param channelNumber
	 *        the channel number to get the value of, starting at {@literal 0}
	 * @return the value of the specified channel, or {@literal null} if the
	 *         channel is not enabled
	 * @throws IllegalArgumentException
	 *         if {@code channelNumber} is outside the allowed range for the
	 *         device (e.g. {@literal 0} to {@literal 7})
	 */
	BigDecimal getChannelValue(int channelNumber);

}
