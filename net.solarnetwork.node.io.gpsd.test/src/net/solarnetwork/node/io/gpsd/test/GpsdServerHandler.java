/* ==================================================================
 * GpsdServerHandler.java - 13/11/2019 7:02:28 am
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

package net.solarnetwork.node.io.gpsd.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageHandler;

/**
 * GPSd server handler.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdServerHandler extends SimpleChannelInboundHandler<String>
		implements GpsdMessagePublisher {

	private static final Logger log = LoggerFactory.getLogger(GpsdServerHandler.class);

	private final VersionMessage version;
	private final ObjectMapper mapper;
	private ChannelHandlerContext context;
	private GpsdMessageHandler messageHandler;

	/**
	 * Constructor.
	 * 
	 * @param version
	 *        the server version to use
	 * @param mapper
	 *        the mapper
	 */
	public GpsdServerHandler(VersionMessage version, ObjectMapper mapper) {
		super();
		this.version = version;
		this.mapper = mapper;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.context = ctx;
	}

	@Override
	public Future<Void> publishMessage(GpsdMessage message) {
		ChannelHandlerContext ctx = this.context;
		if ( ctx == null ) {
			CompletableFuture<Void> f = new CompletableFuture<>();
			f.completeExceptionally(new RuntimeException("Server channel not available."));
			return f;
		}
		return publishMessageInternal(ctx, message);
	}

	private ChannelFuture publishMessageInternal(ChannelHandlerContext ctx, GpsdMessage message) {
		String msg;
		try {
			msg = (message != null ? mapper.writeValueAsString(message) : "") + "\n";
		} catch ( JsonProcessingException e ) {
			return ctx.newFailedFuture(e);
		}
		log.debug("RES: " + msg);
		return ctx.writeAndFlush(msg);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		publishMessageInternal(ctx, version);
	}

	private static final Pattern CMD_PAT = Pattern.compile("\\?(\\w+)[;=](.*)");

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
		log.debug("REQ: {}", request);
		ObjectNode n = mapper.createObjectNode();
		Matcher m = CMD_PAT.matcher(request);
		if ( !m.matches() ) {
			n.put("class", "ERROR");
			n.put("message", "Invalid request syntax.");
		} else {
			String command = m.group(1);
			String args = m.group(2);
			GpsdMessageType type = GpsdMessageType.forName(command);
			GpsdMessage message = null;
			if ( args != null && !args.isEmpty() ) {
				message = mapper.readValue(args, GpsdMessage.class);
			}
			switch (type) {
				case Version:
					publishMessageInternal(ctx, version);
					return;

				default:
					n.put("class", "ERROR");
					n.put("message", "Unsupported command.");
					break;
			}
			if ( message != null ) {
				handleMessage(message);
			}
		}
		if ( n.size() > 0 ) {
			String resp = mapper.writeValueAsString(n);
			ctx.writeAndFlush(resp + "\n");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("GPSd server exception: {}", cause.getMessage(), cause);
		ctx.close();
	}

	private void handleMessage(GpsdMessage message) {
		GpsdMessageHandler h = getMessageHandler();
		if ( h != null ) {
			h.handleGpsdMessage(message);
		}
	}

	/**
	 * Get the message handler.
	 * 
	 * @return the messageHandler
	 */
	public GpsdMessageHandler getMessageHandler() {
		return messageHandler;
	}

	/**
	 * Set the message handler.
	 * 
	 * @param messageHandler
	 *        the messageHandler to set
	 */
	public void setMessageHandler(GpsdMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

}
