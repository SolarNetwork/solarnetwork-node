/* ==================================================================
 * GpsdClient.java - 11/11/2019 5:09:34 pm
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

package net.solarnetwork.node.hw.gpsd.service;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * GSPd client component.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdClient extends BaseIdentifiable implements SettingsChangeObserver {

	/** The default {@code host} property value. */
	public static final String DEFAULT_HOST = "localhost";

	/** The default {@code port} property value. */
	public static final int DEFAULT_PORT = 2947;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_RECONNECT_SECONDS = 60;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_SHUTDOWN_SECONDS = 5;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectMapper mapper;
	private final TaskScheduler taskScheduler;
	private final Bootstrap bootstrap;
	private String host;
	private int port;
	private int reconnectSeconds;
	private int shutdownSeconds;
	private GpsdMessageHandler messageHandler;

	private boolean shutdown;
	private ScheduledFuture<?> connectFuture;
	private Channel channel;

	/**
	 * Constructor.
	 * 
	 * @param mapper
	 *        the JSON mapper to use for parsing messages
	 * @param taskScheduler
	 *        the task scheduler
	 */
	public GpsdClient(ObjectMapper mapper, TaskScheduler taskScheduler) {
		super();
		this.mapper = mapper;
		this.taskScheduler = taskScheduler;
		this.bootstrap = createBootstrap();
		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
		this.shutdownSeconds = DEFAULT_SHUTDOWN_SECONDS;
		this.shutdown = false;
	}

	private Bootstrap createBootstrap() {
		CustomizableThreadFactory tf = new CustomizableThreadFactory("gpsd-");
		//tf.setDaemon(true);

		Bootstrap b = new Bootstrap();
		b.group(new NioEventLoopGroup(0, tf)).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
						(int) TimeUnit.SECONDS.toMillis(DEFAULT_RECONNECT_SECONDS))
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new GpsdClientChannelInitializer(new ChannelHandler()));
		return b;
	}

	/**
	 * Get the client status.
	 * 
	 * @return the status
	 */
	public synchronized GpsdClientStatus getClientStatus() {
		Channel c = this.channel;
		if ( c != null ) {
			if ( c.isActive() ) {
				return GpsdClientStatus.Connected;
			}
		} else {
			ScheduledFuture<?> f = this.connectFuture;
			if ( f != null && !f.isDone() ) {
				return GpsdClientStatus.ConnectionScheduled;
			}
		}
		return GpsdClientStatus.Closed;
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		restart();
	}

	/**
	 * Call once after properties configured to initialize.
	 */
	public void startup() {
		synchronized ( this ) {
			shutdown = false;
		}
		start();
	}

	/**
	 * Call to close all connections and free resources.
	 */
	public void shutdown() {
		synchronized ( this ) {
			shutdown = true;
		}
		try {
			stop().get(shutdownSeconds, TimeUnit.SECONDS);
		} catch ( ExecutionException | InterruptedException | TimeoutException e ) {
			log.warn("Error waiting for GSPd connection to close gracefully: {}", e.toString(), e);
		}
	}

	private class ChannelHandler extends SimpleChannelInboundHandler<Object> {

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
					GpsdMessageHandler handler = getMessageHandler();
					if ( log.isDebugEnabled() ) {
						log.debug("Got GPSd message: " + message);
					}
					if ( handler != null ) {
						handler.handleGpsdMessage(message);
					}
				}
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			log.error("Error handling GPSd", cause);
		}

	}

	private synchronized void start() {
		final String host = getHost();
		final int port = getPort();
		if ( host == null || host.isEmpty() ) {
			log.info("Cannot start GPSd client");
		}
		if ( channel != null ) {
			stop();
		}

		log.info("Opening connection to GPSd @ {}:{}", host, port);

		ChannelFuture f = bootstrap.connect(host, port);
		f.addListener(new ConnectFuture());
	}

	private class ConnectFuture implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if ( !future.isSuccess() ) {
				future.channel().close();
				bootstrap.connect(host, port).addListener(this);
			} else {
				channel = future.channel();
				InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
				log.info("Connected to to GPSd @ {}:{}", addr.getHostString(), addr.getPort());
				channel.closeFuture().addListener(new ReconnectFuture());
			}
		}

	}

	private class ReconnectFuture implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			synchronized ( GpsdClient.this ) {
				if ( !shutdown ) {
					scheduleConnect();
				}
			}
		}
	}

	private synchronized Future<?> stop() {
		if ( connectFuture != null && !connectFuture.isDone() ) {
			connectFuture.cancel(true);
			connectFuture = null;
		}
		Future<?> result = null;
		if ( channel != null ) {
			InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
			log.info("Closing connection to GPSd @ {}:{}", addr.getHostString(), addr.getPort());
			try {
				result = channel.close();
			} finally {
				this.channel = null;
			}
		} else {
			CompletableFuture<Void> cf = new CompletableFuture<>();
			cf.complete(null);
			result = cf;
		}
		return result;
	}

	private synchronized void restart() {
		try {
			stop().get(shutdownSeconds, TimeUnit.SECONDS);
		} catch ( ExecutionException | InterruptedException | TimeoutException e ) {
			log.warn("Error waiting for GSPd connection to close gracefully: {}", e.toString());
		} finally {
			scheduleConnect();
		}
	}

	private synchronized void scheduleConnect() {
		final int delay = getReconnectSeconds();
		if ( delay > 0 ) {
			log.info("Will connect to GPSd @ {}:{} in {}s", host, port, delay);
			connectFuture = taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					start();
				}
			}, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delay)));
		} else {
			start();
		}
	}

	@Override
	public String getDisplayName() {
		return "GPSd Client";
	}

	// Accessors

	/**
	 * Get the GPSd host to connect to.
	 * 
	 * @return the host name or IP address; defaults to {@link #DEFAULT_HOST}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the GPSd host to connect to.
	 * 
	 * @param host
	 *        the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the GPSd port to connect to.
	 * 
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the GPSd port to connect to.
	 * 
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the delay in seconds before an automatic reconnection is attempted.
	 * 
	 * @return the seconds; defaults to {@link #DEFAULT_RECONNECT_SECONDS}
	 */
	public int getReconnectSeconds() {
		return reconnectSeconds;
	}

	/**
	 * Set the delay in seconds before an automatic reconnection is attempted.
	 * 
	 * @param reconnectSeconds
	 *        the seconds to set
	 */
	public void setReconnectSeconds(int reconnectSeconds) {
		this.reconnectSeconds = reconnectSeconds;
	}

	/**
	 * Get the maximum number of seconds to wait for the client to gracefully
	 * shutdown.
	 * 
	 * @return the seconds; defaults to {@link #DEFAULT_SHUTDOWN_SECONDS}
	 */
	public int getShutdownSeconds() {
		return shutdownSeconds;
	}

	/**
	 * Set the maximum number of seconds to wait for the client to gracefully
	 * shutdown.
	 * 
	 * @param shutdownSeconds
	 *        the seconds to set
	 */
	public void setShutdownSeconds(int shutdownSeconds) {
		this.shutdownSeconds = shutdownSeconds;
	}

	/**
	 * Get the message handler.
	 * 
	 * @return the handler, or {@literal null}
	 */
	public GpsdMessageHandler getMessageHandler() {
		return messageHandler;
	}

	/**
	 * Set the message handler.
	 * 
	 * @param messageHandler
	 *        the handler to set
	 */
	public void setMessageHandler(GpsdMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

}
