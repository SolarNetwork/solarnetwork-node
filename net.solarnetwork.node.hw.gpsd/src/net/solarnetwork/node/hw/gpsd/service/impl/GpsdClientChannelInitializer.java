/* ==================================================================
 * GpsdClientChannelInitializer.java - 11/11/2019 7:59:25 pm
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

package net.solarnetwork.node.hw.gpsd.service.impl;

import java.nio.charset.Charset;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Initialize a channel with a new pipeline.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final JsonObjectDecoder DECODER = new JsonObjectDecoder();
	private final StringEncoder ENCODER = new StringEncoder(Charset.forName("US-ASCII"));

	private final ChannelHandler handler;

	/**
	 * Constructor.
	 * 
	 * @param handler
	 *        the handler to add to the end of the channel pipeline.
	 */
	public GpsdClientChannelInitializer(ChannelHandler handler) {
		super();
		this.handler = handler;
	}

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(DECODER, ENCODER, handler);
	}

}
