/* ==================================================================
 * ChannelService.java - 21/02/2019 5:11:11 pm
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

package net.solarnetwork.node.io.dnp3;

import com.automatak.dnp3.Channel;
import com.automatak.dnp3.enums.ChannelState;
import net.solarnetwork.service.Identifiable;

/**
 * A managed DNP3 channel.
 *
 * @author matt
 * @version 3.0
 */
public interface ChannelService extends Identifiable {

	/**
	 * Get the channel.
	 *
	 * @return the channel
	 */
	Channel dnp3Channel();

	/**
	 * Get the channel state.
	 *
	 * @return the channel state
	 * @since 3.0
	 */
	ChannelState getChannelState();

}
