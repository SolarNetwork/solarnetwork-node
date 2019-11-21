/* ==================================================================
 * CannelloniChannelInitializer.java - 21/11/2019 11:37:54 am
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

package net.solarnetwork.node.io.canbus.cannelloni;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.DatagramPacketDecoder;

/**
 * Initialize a Cannalloni channel with a new pipeline.
 * 
 * @author matt
 * @version 1.0
 */
public class CannelloniChannelInitializer extends ChannelInitializer<DatagramChannel> {

	private final ChannelHandler handler;

	/**
	 * Constructor.
	 * 
	 * @param handler
	 *        the handler to add to the end of the channel pipeline.
	 */
	public CannelloniChannelInitializer(ChannelHandler handler) {
		super();
		this.handler = handler;
	}

	@Override
	protected void initChannel(DatagramChannel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new DatagramPacketDecoder(new CannelloniCanbusFrameDecoder()), handler);
	}

}
