/* ==================================================================
 * GpsdClientService.java - 11/11/2019 5:09:34 pm
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

package net.solarnetwork.node.io.gpsd.service.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.GpsdReportMessage;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdClientConnection;
import net.solarnetwork.node.io.gpsd.service.GpsdClientStatus;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageHandler;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageListener;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * GPSd client component.
 *
 * @author matt
 * @version 2.1
 */
public class GpsdClientService extends BasicIdentifiable implements GpsdClientConnection,
		SettingsChangeObserver, SettingSpecifierProvider, GpsdMessageHandler {

	/** The default {@code host} property value. */
	public static final String DEFAULT_HOST = "localhost";

	/** The default {@code port} property value. */
	public static final int DEFAULT_PORT = 2947;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_RECONNECT_SECONDS = 60;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_SHUTDOWN_SECONDS = 5;

	/** The default {@code responseTimeoutSeconds} property value. */
	public static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 5;

	/** The default {@code autoWatch} property value. */
	public static final boolean DEFAULT_AUTO_WATCH = false;

	/** The default {@code gpsRolloverCompensation} property value. */
	public static final boolean DEFAULT_GPS_ROLLOVER_COMPENSATION = true;

	private final ConcurrentMap<Class<? extends GpsdMessage>, Set<GpsdMessageListener<GpsdMessage>>> messageListeners = new ConcurrentHashMap<>(
			8, 0.9f, 1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TaskScheduler taskScheduler;
	private final Bootstrap bootstrap;
	private final GpsdClientChannelHandler handler;
	private String host;
	private int port;
	private int reconnectSeconds;
	private int shutdownSeconds;
	private boolean gpsRolloverCompensation;
	private GpsdMessageHandler messageHandler;
	private OptionalService<EventAdmin> eventAdmin;

	private boolean shutdown;
	private ScheduledFuture<?> connectFuture;
	private ChannelFuture startFuture;
	private Channel channel;

	/**
	 * Constructor.
	 *
	 * @param mapper
	 *        the JSON mapper to use for parsing messages
	 * @param taskScheduler
	 *        the task scheduler
	 */
	public GpsdClientService(ObjectMapper mapper, TaskScheduler taskScheduler) {
		super();
		this.taskScheduler = taskScheduler;

		GpsdClientChannelHandler handler = new GpsdClientChannelHandler(mapper, this);
		this.handler = handler;
		this.bootstrap = createBootstrap(handler);

		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
		this.shutdownSeconds = DEFAULT_SHUTDOWN_SECONDS;
		this.gpsRolloverCompensation = DEFAULT_GPS_ROLLOVER_COMPENSATION;
		setResponseTimeoutSeconds(DEFAULT_RESPONSE_TIMEOUT_SECONDS);
		setAutoWatch(DEFAULT_AUTO_WATCH);
		this.shutdown = false;
	}

	private Bootstrap createBootstrap(ChannelHandler handler) {
		CustomizableThreadFactory tf = new CustomizableThreadFactory("gpsd-");
		tf.setDaemon(true);

		Bootstrap b = new Bootstrap();
		EventLoopGroup group = new MultiThreadIoEventLoopGroup(0, tf, NioIoHandler.newFactory());
		b.group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
						(int) TimeUnit.SECONDS.toMillis(DEFAULT_RECONNECT_SECONDS))
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new GpsdClientChannelInitializer(handler));
		return b;
	}

	@Override
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
	 * Call once after properties configured to initialize at a future date.
	 *
	 * @return a future that completes when connected
	 */
	public Future<?> startupLater() {
		synchronized ( this ) {
			shutdown = false;
		}
		return scheduleConnect();
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
			log.warn("Error waiting for GPSd connection to close gracefully: {}", e.toString(), e);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("GpsdClient");
		final String host = getHost();
		final int port = getPort();
		if ( host != null ) {
			buf.append("@");
			buf.append(host);
			buf.append(":");
			buf.append(port);
		} else {
			buf.append("{unconfigured}");
		}
		return buf.toString();
	}

	private synchronized Future<?> start() {
		final String host = getHost();
		final int port = getPort();
		if ( host == null || host.isEmpty() ) {
			log.info("Cannot start GPSd client: host not configured.");
		}
		if ( channel != null || startFuture != null ) {
			return restart();
		}
		log.info("Connecting to GPSd @ {}:{}", host, port);
		ChannelFuture f = bootstrap.connect(host, port);
		f.addListener(new ConnectFuture());
		this.startFuture = f;
		return f;
	}

	private class ConnectFuture implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if ( !future.isSuccess() ) {
				try {
					future.channel().close().sync();
				} finally {
					synchronized ( GpsdClientService.this ) {
						startFuture = null;
					}
				}
				Throwable t = future.cause();
				Throwable root = t;
				if ( root != null ) {
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IOException ) {
						log.warn("Unable to connect to GPSd @ {}:{}: {}", host, port, root.getMessage());
					} else {
						log.error("Error connecting to GPSd @ {}:{}: {}", host, port, root.toString(),
								t);
					}
				}
				if ( !shutdown ) {
					scheduleConnect();
				}
			} else {
				synchronized ( GpsdClientService.this ) {
					Channel ch = future.channel();
					InetSocketAddress addr = (InetSocketAddress) ch.remoteAddress();
					log.info("Connected to to GPSd @ {}:{}", addr.getHostString(), addr.getPort());
					ch.closeFuture().addListener(new ReconnectFuture());
					channel = ch;
					startFuture = null;
					postClientStatusChangeEvent(GpsdClientStatus.Connected);
				}
			}
		}

	}

	private class ReconnectFuture implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			synchronized ( GpsdClientService.this ) {
				postClientStatusChangeEvent(GpsdClientStatus.Closed);
				if ( !shutdown ) {
					log.info("Connection to GPSd @ {}:{} closed; will auto-reconnect.", host, port);
					scheduleConnect();
				}
			}
		}
	}

	private synchronized Future<?> stop() {
		if ( connectFuture != null && !connectFuture.isDone() ) {
			connectFuture.cancel(false);
			connectFuture = null;
		}
		Future<?> result = null;
		Channel ch = this.channel;
		if ( startFuture != null ) {
			ch = startFuture.channel();
			startFuture.cancel(false);
			startFuture = null;
		}
		if ( ch != null ) {
			final String host;
			final int port;
			if ( ch.remoteAddress() != null ) {
				final InetSocketAddress addr = (InetSocketAddress) ch.remoteAddress();
				host = addr.getHostString();
				port = addr.getPort();
			} else {
				host = this.host;
				port = this.port;
			}
			log.info("Closing connection to GPSd @ {}:{}", host, port);
			result = ch.close().addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					try {
						if ( future.isSuccess() ) {
							log.info("Closed connection to GPSd @ {}:{}", host, port);
						} else {
							Throwable root = future.cause();
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							if ( root instanceof IOException ) {
								log.warn("Unable to close connection to GPSd @ {}:{}: {}", host, port,
										root.getMessage());
							} else {
								log.error("Error closing connection to GPSd @ {}:{}: {}", host, port,
										root.toString(), future.cause());
							}
						}
					} finally {
						synchronized ( GpsdClientService.this ) {
							channel = null;
						}
					}
				}
			});
		} else {
			CompletableFuture<Void> cf = new CompletableFuture<>();
			cf.complete(null);
			result = cf;
		}
		return result;
	}

	private synchronized Future<?> restart() {
		Future<?> f = null;
		try {
			stop().get(shutdownSeconds, TimeUnit.SECONDS);
		} catch ( ExecutionException | InterruptedException | TimeoutException e ) {
			log.warn("Error waiting for GPSd connection to close gracefully: {}", e.toString());
		} finally {
			f = scheduleConnect();
		}
		return f;
	}

	private synchronized Future<?> scheduleConnect() {
		if ( connectFuture != null && !connectFuture.isDone() ) {
			// already scheduled
			return connectFuture;
		}
		final int delay = getReconnectSeconds();
		if ( delay > 0 ) {
			log.info("Scheduling attempt to reconnect to GPSd @ {}:{} in {}s", host, port, delay);
			ScheduledFuture<?> f = taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					try {
						start();
					} finally {
						synchronized ( GpsdClientService.this ) {
							connectFuture = null;
						}
					}
				}
			}, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delay)));
			connectFuture = f;
			postClientStatusChangeEvent(GpsdClientStatus.ConnectionScheduled);
			return f;
		} else {
			return start();
		}
	}

	private void postClientStatusChangeEvent(GpsdClientStatus status) {
		String uid = getUid();
		if ( uid == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(UID_PROPERTY, uid);
		if ( getGroupUid() != null ) {
			props.put(GROUP_UID_PROPERTY, getGroupUid());
		}
		props.put(STATUS_PROPERTY, status);
		postEvent(new Event(EVENT_TOPIC_CLIENT_STATUS_CHANGE, props));
	}

	private void postReportMessageCapturedEvent(GpsdReportMessage message) {
		String uid = getUid();
		if ( uid == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(UID_PROPERTY, uid);
		if ( getGroupUid() != null ) {
			props.put(GROUP_UID_PROPERTY, getGroupUid());
		}
		props.put(MESSAGE_PROPERTY, message);
		postEvent(new Event(EVENT_TOPIC_REPORT_MESSAGE_CAPTURED, props));
	}

	private void postEvent(Event event) {
		EventAdmin ea = (eventAdmin != null ? eventAdmin.service() : null);
		if ( ea != null ) {
			ea.postEvent(event);
		}
	}

	@Override
	public final void handleGpsdMessage(GpsdMessage message) {
		if ( message instanceof GpsdReportMessage ) {
			if ( gpsRolloverCompensation && ((GpsdReportMessage) message).getTimestamp() != null ) {
				OffsetDateTime localTime = Instant.now().atOffset(ZoneOffset.UTC);
				OffsetDateTime reportTime = ((GpsdReportMessage) message).getTimestamp()
						.atOffset(ZoneOffset.UTC);
				// this is a quick and dirty check: if local time hour  > 1 hour ahead of report time hour,
				// use local time value instead of report time
				if ( localTime.get(ChronoField.YEAR) - reportTime.get(ChronoField.YEAR) > 10 ) {
					OffsetDateTime newTime = reportTime.withYear(localTime.get(ChronoField.YEAR))
							.withDayOfYear(localTime.get(ChronoField.DAY_OF_YEAR));
					log.debug("GPS rollover time compensation: replacing {} with {}", reportTime,
							newTime);
					message = ((GpsdReportMessage) message).withTimestamp(newTime.toInstant());
				}
			}
			postReportMessageCapturedEvent((GpsdReportMessage) message);
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
		GpsdMessageHandler delegate = getMessageHandler();
		if ( delegate != null ) {
			delegate.handleGpsdMessage(message);
		}
	}

	@Override
	public String getDisplayName() {
		return "GPSd Client";
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
	public Future<VersionMessage> requestGpsdVersion() {
		return handler.sendCommand(GpsdMessageType.Version, null);
	}

	@Override
	public Future<WatchMessage> configureWatchMode(WatchMessage config) {
		return handler.sendCommand(GpsdMessageType.Watch, config);
	}

	// SettingsSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.gpsd.client";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTitleSettingSpecifier("status", getClientStatus().toString(), true));
		results.add(new BasicTextFieldSettingSpecifier("host", DEFAULT_HOST));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));
		results.add(new BasicTextFieldSettingSpecifier("reconnectSeconds",
				String.valueOf(DEFAULT_RECONNECT_SECONDS)));
		results.add(new BasicToggleSettingSpecifier("autoWatch", DEFAULT_AUTO_WATCH));
		results.add(new BasicToggleSettingSpecifier("gpsRolloverCompensation",
				DEFAULT_GPS_ROLLOVER_COMPENSATION));
		return results;
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
	 * Get the maximum number of seconds to wait for a command response.
	 *
	 * @return the seconds; defaults to
	 *         {@link #DEFAULT_RESPONSE_TIMEOUT_SECONDS}
	 */
	public int getResponseTimeoutSeconds() {
		return this.handler.getResponseTimeoutSeconds();
	}

	/**
	 * Set the maximum number of seconds to wait for a command response.
	 *
	 * @param responseTimeoutSeconds
	 *        the seconds to set
	 */
	public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
		this.handler.setResponseTimeoutSeconds(responseTimeoutSeconds);
	}

	/**
	 * Get the "auto watch" mode flag.
	 *
	 * @return {@literal true} to automatically issue a {@literal ?WATCH}
	 *         command when connecting to GPSd; default is
	 *         {@link #DEFAULT_AUTO_WATCH}
	 */
	public boolean isAutoWatch() {
		return handler.isAutoWatch();
	}

	/**
	 * Set the "auto watch" mode flag.
	 *
	 * @param autoWatch
	 *        the mode to set
	 */
	public void setAutoWatch(boolean autoWatch) {
		handler.setAutoWatch(autoWatch);
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

	/**
	 * Get the {@link EventAdmin} service.
	 *
	 * @return the EventAdmin service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} service to use.
	 *
	 * @param eventAdmin
	 *        the EventAdmin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the GPS week rollover compensation mode.
	 *
	 * @return the mode; defaults to {@link #DEFAULT_GPS_ROLLOVER_COMPENSATION}
	 */
	public boolean isGpsRolloverCompensation() {
		return gpsRolloverCompensation;
	}

	/**
	 * Set the GPS week rollover compensation mode.
	 *
	 * @param gpsRolloverCompensation
	 *        {@literal true} if the GPS date should be compensated for hardware
	 *        that does not correctly handle week rollover peiods (every 20
	 *        years)
	 */
	public void setGpsRolloverCompensation(boolean gpsRolloverCompensation) {
		this.gpsRolloverCompensation = gpsRolloverCompensation;
	}

}
