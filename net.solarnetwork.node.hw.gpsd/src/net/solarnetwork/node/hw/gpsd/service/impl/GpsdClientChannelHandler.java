/* ==================================================================
 * GpsdClientChannelHandler.java - 14/11/2019 11:20:38 am
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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.GenericFutureListener;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.hw.gpsd.service.GpsdCommandSender;
import net.solarnetwork.node.hw.gpsd.service.GpsdMessageBroker;
import net.solarnetwork.node.hw.gpsd.service.GpsdMessageHandler;
import net.solarnetwork.node.hw.gpsd.service.GpsdMessageListener;
import net.solarnetwork.util.OptionalService;

/**
 * Channel handler for GPSd protocol.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdClientChannelHandler extends SimpleChannelInboundHandler<Object>
		implements GpsdCommandSender, GpsdMessageBroker {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Queue<MessageResponseHolder> responseQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentMap<Class<? extends GpsdMessage>, Set<GpsdMessageListener<GpsdMessage>>> messageListeners = new ConcurrentHashMap<>(
			8, 0.9f, 1);

	private final ObjectMapper mapper;
	private final OptionalService<GpsdMessageHandler> messageHandler;

	private int responseTimeoutSeconds;

	private ChannelHandlerContext context;

	/**
	 * Constructor.
	 * 
	 * @param mapper
	 *        the mapper
	 * @param messageHandler
	 *        the optional message handler
	 */
	public GpsdClientChannelHandler(ObjectMapper mapper,
			OptionalService<GpsdMessageHandler> messageHandler) {
		super();
		this.mapper = mapper;
		this.messageHandler = messageHandler;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <M extends GpsdMessage> void addMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener) {
		messageListeners.computeIfAbsent(messageType, k -> new CopyOnWriteArraySet<>())
				.add((GpsdMessageListener<GpsdMessage>) listener);
	}

	@Override
	public <M extends GpsdMessage> void removeMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener) {
		messageListeners.compute(messageType, (k, v) -> {
			if ( v != null && v.remove(listener) ) {
				if ( v.isEmpty() ) {
					return null;
				}
			}
			return v;
		});
	}

	@Override
	public <T extends GpsdMessage> Future<T> sendCommand(GpsdMessageType command, Object argument) {
		final ChannelHandlerContext ctx = this.context;
		final CompletableFuture<T> result = new CompletableFuture<T>();
		if ( ctx == null ) {
			result.completeExceptionally(new IOException("Connection not configured."));
			return result;
		}
		@SuppressWarnings("unchecked")
		MessageResponseHolder h = createResponseHolder(command, (CompletableFuture<GpsdMessage>) result,
				TimeUnit.SECONDS.toMillis(responseTimeoutSeconds));
		if ( h != null ) {
			responseQueue.add(h);
		}
		String argJson = null;
		if ( argument != null ) {
			try {
				argJson = mapper.writeValueAsString(argument);
			} catch ( JsonProcessingException e ) {
				result.completeExceptionally(new IOException("Error serializing " + command
						+ " argument [" + argument + "] to JSON: " + e.getMessage(), e));
				return result;
			}
		}
		ChannelFuture f = publishMessageInternal(ctx, command, argJson);
		f.addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {

			@Override
			public void operationComplete(io.netty.util.concurrent.Future<? super Void> future)
					throws Exception {
				try {
					future.await(responseTimeoutSeconds, TimeUnit.SECONDS);
					if ( !future.isSuccess() ) {
						result.completeExceptionally(future.cause());
					}
				} catch ( Exception e ) {
					result.completeExceptionally(e);
				}
			}
		});
		return result;
	}

	private MessageResponseHolder createResponseHolder(GpsdMessageType command,
			CompletableFuture<GpsdMessage> future, long timeout) {
		GpsdMessageType responseType = null;
		switch (command) {
			case Version:
				responseType = command;
				break;

			case Watch:
				responseType = command;
				break;

			default:
				// nothing
		}
		return (responseType != null ? new MessageResponseHolder(responseType, future, timeout) : null);
	}

	private ChannelFuture publishMessageInternal(ChannelHandlerContext ctx, GpsdMessageType command,
			String argument) {
		String msg;
		if ( argument != null && !argument.isEmpty() ) {
			msg = String.format("?%s=%s\n", command.getName(), argument);
		} else {
			msg = String.format("?%s;\n", command.getName());
		}
		log.debug("Sending GPSd command: " + msg);
		return ctx.writeAndFlush(msg);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.context = ctx;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.trace("Got GPSd raw message: " + msg);
		ByteBuf buf = (ByteBuf) msg;
		int len = buf.readableBytes();
		if ( len > 0 ) {
			byte[] b = new byte[len];
			buf.readBytes(b);
			GpsdMessage message = mapper.readValue(b, GpsdMessage.class);
			if ( message != null ) {
				synchronized ( responseQueue ) {
					final long now = System.currentTimeMillis();
					for ( Iterator<MessageResponseHolder> itr = responseQueue.iterator(); itr
							.hasNext(); ) {
						MessageResponseHolder h = itr.next();
						if ( h.type == message.getMessageType() ) {
							h.future.complete(message);
							itr.remove();
							break;
						} else if ( h.expireDate < now ) {
							h.future.completeExceptionally(new TimeoutException("The expected " + h.type
									+ " response has not been recieved within the expected time."));
							itr.remove();
						}
					}
				}
				if ( log.isDebugEnabled() ) {
					log.debug("Got GPSd message: " + message);
				}
				GpsdMessageHandler handler = messageHandler();
				if ( handler != null ) {
					handler.handleGpsdMessage(message);
				}
				Class<? extends GpsdMessage> messageType = message.getClass();
				for ( Entry<Class<? extends GpsdMessage>, Set<GpsdMessageListener<GpsdMessage>>> me : messageListeners
						.entrySet() ) {
					if ( me.getKey().isAssignableFrom(messageType) ) {
						Set<GpsdMessageListener<GpsdMessage>> listeners = me.getValue();
						if ( listeners != null ) {
							for ( GpsdMessageListener<GpsdMessage> listener : listeners ) {
								listener.onGpsdMessage(message);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("Error handling GPSd", cause);
	}

	private static class MessageResponseHolder {

		private final GpsdMessageType type;
		private final CompletableFuture<GpsdMessage> future;
		private final long expireDate;

		private MessageResponseHolder(GpsdMessageType type, CompletableFuture<GpsdMessage> future,
				long timeout) {
			super();
			this.type = type;
			this.future = future;
			this.expireDate = System.currentTimeMillis() + timeout;
		}
	}

	private GpsdMessageHandler messageHandler() {
		return (messageHandler != null ? messageHandler.service() : null);
	}

	/**
	 * Get the maximum number of seconds to wait for a command response.
	 * 
	 * @return the seconds; defaults to
	 *         {@link #DEFAULT_RESPONSE_TIMEOUT_SECONDS}
	 */
	public int getResponseTimeoutSeconds() {
		return responseTimeoutSeconds;
	}

	/**
	 * Set the maximum number of seconds to wait for a command response.
	 * 
	 * @param responseTimeoutSeconds
	 *        the seconds to set
	 */
	public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
		this.responseTimeoutSeconds = responseTimeoutSeconds;
	}

}
