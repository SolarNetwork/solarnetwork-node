/* ==================================================================
 * StompSetupServer.java - 4/08/2021 11:33:02 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.server;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * A STOMP protocol server for SolarNode Setup, using Netty.
 *
 * @author matt
 * @version 2.2
 */
public class StompSetupServer extends BaseIdentifiable
		implements SettingsChangeObserver, SettingSpecifierProvider {

	/** The default listen port. */
	public static final int DEFAULT_PORT = 8780;

	/** The default startup delay, in seconds. */
	public static final int DEFAULT_STARTUP_DELAY_SECS = 15;

	/** The default {@code bindAddress} property value. */
	public static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

	private static final Logger log = LoggerFactory.getLogger(StompSetupServer.class);

	private final StompSetupServerService serverService;
	private final ObjectMapper objectMapper;

	private TaskScheduler taskScheduler;
	private final Executor executor;
	private final int port = DEFAULT_PORT;
	private String bindAddress = DEFAULT_BIND_ADDRESS;
	private int startupDelay = DEFAULT_STARTUP_DELAY_SECS;

	private ScheduledFuture<?> startupFuture;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel channel;

	/**
	 * Constructor.
	 *
	 * @param serverService
	 *        the server service
	 * @param objectMapper
	 *        the object mapper
	 * @param executor
	 *        the executor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StompSetupServer(StompSetupServerService serverService, ObjectMapper objectMapper,
			Executor executor) {
		super();
		if ( serverService == null ) {
			throw new IllegalArgumentException("The serverService argument must not be null.");
		}
		this.serverService = serverService;
		if ( objectMapper == null ) {
			throw new IllegalArgumentException("The objectMapper argument must not be null.");
		}
		this.objectMapper = objectMapper;
		if ( executor == null ) {
			throw new IllegalArgumentException("The executor argument must not be null.");
		}
		this.executor = executor;
	}

	/**
	 * Startup the server.
	 */
	public void startup() {
		restartServer();
	}

	/**
	 * Shut the server down.
	 */
	public synchronized void shutdown() {
		if ( startupFuture != null && !startupFuture.isDone() ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( workerGroup != null && !workerGroup.isShuttingDown() ) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}
		if ( bossGroup != null && !bossGroup.isShuttingDown() ) {
			bossGroup.shutdownGracefully();
			bossGroup = null;
		}
		if ( channel != null ) {
			channel = null;
		}
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		restartServer();
	}

	private synchronized void restartServer() {
		shutdown();
		Runnable startupTask = new StartupTask();
		if ( taskScheduler != null ) {
			log.info("Will start STOMP setup server on port {} in {} seconds", port, startupDelay);
			startupFuture = taskScheduler.schedule(startupTask,
					Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(startupDelay));
		} else {
			startupTask.run();
		}
	}

	private class StartupTask implements Runnable {

		@Override
		public void run() {
			synchronized ( StompSetupServer.this ) {
				startupFuture = null;
				shutdown();
				final int port = StompSetupServer.this.port;
				final String bindAddress = StompSetupServer.this.bindAddress;
				final ThreadFactory tf = new DefaultThreadFactory("STOMP-Setup:" + port, true);

				bossGroup = new MultiThreadIoEventLoopGroup(tf, NioIoHandler.newFactory());
				workerGroup = new MultiThreadIoEventLoopGroup(tf, NioIoHandler.newFactory());
				try {
					ServerBootstrap b = new ServerBootstrap();
					// @formatter:off
					b.group(bossGroup, workerGroup)
							.channel(NioServerSocketChannel.class)
							.childHandler(new StompChannelInitializer())
							.option(ChannelOption.SO_BACKLOG, 128)
							.childOption(ChannelOption.SO_KEEPALIVE, true);
					// @formatter:on
					ChannelFuture future = b.bind(bindAddress, port).sync();
					StompSetupServer.this.channel = future.channel();
					log.info("STOMP setup server listening on {}:{}", bindAddress, port);
				} catch ( InterruptedException | RuntimeException e ) {
					shutdown();
					log.error("Error binding STOMP setup server {} to {}:{}: {}", StompSetupServer.this,
							bindAddress, port, e.toString());
					if ( taskScheduler != null ) {
						log.info("Will start STOMP setup server on port {} in {} seconds", port,
								startupDelay);
						startupFuture = taskScheduler.schedule(this, Instant
								.ofEpochMilli(System.currentTimeMillis()).plusSeconds(startupDelay));
					}
				}
			}
		}
	}

	private class StompChannelInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			if ( true ) { // FIXME add toggle
				ch.pipeline().addLast(
						new LoggingHandler("net.solarnetwork.node.setup.stomp.WIRE", LogLevel.TRACE));
			}
			// @formatter:off
			ch.pipeline().addLast(
					new StompSubframeDecoder(),
					new StompSubframeAggregator(4096),
					new StompSubframeEncoder(),
					new StompSetupServerHandler(serverService, objectMapper, executor));
			// @formatter:on
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.setup.stomp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true));

		result.addAll(baseIdentifiableSettings(null));
		result.add(new BasicTextFieldSettingSpecifier("bindAddress", DEFAULT_BIND_ADDRESS));
		result.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));

		return result;
	}

	private synchronized String getStatusMessage() {
		final EventLoopGroup g = this.bossGroup;
		if ( g == null || g.isShuttingDown() ) {
			return "Shutdown";
		}
		final Channel c = this.channel;
		if ( c != null && c.isActive() ) {
			return String.format("Listening on %s:%d", bindAddress, port);
		}
		return "Unknown";
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the startup delay.
	 *
	 * @return the startup delay, in seconds; defaults tO
	 *         {@link #DEFAULT_STARTUP_DELAY_SECS}
	 */
	public int getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Set the startup delay.
	 *
	 * @param startupDelay
	 *        the delay to set, in seconds
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Get the address to bind to.
	 *
	 * @return the address; defaults to {@link #DEFAULT_BIND_ADDRESS}
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the address to bind to.
	 *
	 * @param bindAddress
	 *        the address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

}
