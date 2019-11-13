/* ==================================================================
 * GpsdServerInitializer.java - 13/11/2019 6:59:30 am
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

package net.solarnetwork.node.hw.gpsd.test;

import java.nio.charset.Charset;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringEncoder;

/**
 * GPSd server channel initializer.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final StringEncoder DECODER = new StringEncoder(Charset.forName("US-ASCII"));
	private static final StringEncoder ENCODER = new StringEncoder(Charset.forName("US-ASCII"));

	private final GpsdServerHandler serverHandler;

	/**
	 * Constructor.
	 * 
	 * @param handler
	 *        the handler
	 */
	public GpsdServerInitializer(GpsdServerHandler handler) {
		super();
		this.serverHandler = handler;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new DelimiterBasedFrameDecoder(128, Delimiters.lineDelimiter()));
		pipeline.addLast(DECODER);
		pipeline.addLast(ENCODER);
		pipeline.addLast(serverHandler);
	}

}
