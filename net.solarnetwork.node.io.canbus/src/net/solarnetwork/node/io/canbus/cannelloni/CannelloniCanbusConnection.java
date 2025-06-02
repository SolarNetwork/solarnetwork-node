/* ==================================================================
 * CannelloniCanbusConnection.java - 21/11/2019 11:37:27 am
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * {@link CanbusConnection} that listens for UDP packets from a Cannalloni
 * server.
 *
 * @author matt
 * @version 2.0
 */
public class CannelloniCanbusConnection extends BasicIdentifiable
		implements CanbusConnection, SettingsChangeObserver {

	/** The default {@code host} property value. */
	public static final String DEFAULT_HOST = "localhost";

	/** The default {@code port} property value. */
	public static final int DEFAULT_PORT = 2947;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_RECONNECT_SECONDS = 60;

	/** The default {@code reconnectSeconds} property value. */
	public static final int DEFAULT_SHUTDOWN_SECONDS = 5;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TaskScheduler taskScheduler;
	private final Bootstrap bootstrap;
	private final String host;
	private final int port;
	private final String busName;
	private int reconnectSeconds;
	private int shutdownSeconds;
	private OptionalService<EventAdmin> eventAdmin;

	private boolean shutdown;
	private ScheduledFuture<?> connectFuture;
	private ChannelFuture startFuture;
	private Channel channel;

	/**
	 * Constructor.
	 *
	 * @param taskScheduler
	 *        the task scheduler
	 * @param busName
	 *        the bus name
	 */
	public CannelloniCanbusConnection(TaskScheduler taskScheduler, String busName) {
		this(taskScheduler, busName, DEFAULT_HOST, DEFAULT_PORT);
	}

	/**
	 * Constructor.
	 *
	 * @param taskScheduler
	 *        the task scheduler
	 * @param busName
	 *        the bus name
	 * @param host
	 *        the host name to connect to
	 * @param port
	 *        the port to connect to
	 */
	public CannelloniCanbusConnection(TaskScheduler taskScheduler, String busName, String host,
			int port) {
		super();
		this.taskScheduler = taskScheduler;
		this.bootstrap = createBootstrap();
		this.busName = busName;
		this.host = host;
		this.port = port;
		this.reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
		this.shutdownSeconds = DEFAULT_SHUTDOWN_SECONDS;
		this.shutdown = false;

	}

	private Bootstrap createBootstrap() {
		CustomizableThreadFactory tf = new CustomizableThreadFactory("cannalloni-");
		tf.setDaemon(true);

		Bootstrap b = new Bootstrap();
		EventLoopGroup group = new MultiThreadIoEventLoopGroup(0, tf, NioIoHandler.newFactory());
		b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
				.handler(new CannelloniChannelInitializer(new CanbusFrameHandler()));
		return b;
	}

	@Override
	public String getDisplayName() {
		return "Cannelloni Client";
	}

	private synchronized CannelloniConnectionStatus getConnectionStatus() {
		Channel c = this.channel;
		if ( c != null ) {
			if ( c.isActive() ) {
				return CannelloniConnectionStatus.Connected;
			}
		} else {
			ScheduledFuture<?> f = this.connectFuture;
			if ( f != null && !f.isDone() ) {
				return CannelloniConnectionStatus.ConnectionScheduled;
			}
		}
		return CannelloniConnectionStatus.Closed;
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		restart();
	}

	/**
	 * Call once after properties configured to initialize at a future date.
	 *
	 * @return initialization task future
	 */
	public Future<?> openLater() {
		synchronized ( this ) {
			shutdown = false;
		}
		return scheduleConnect();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("CannelloniCanbusConnection");
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
			log.info("Cannot start Cannelloni client: host not configured.");
		}
		if ( channel != null || startFuture != null ) {
			return restart();
		}
		log.info("Connecting to Cannelloni @ {}:{}", host, port);
		ChannelFuture f = bootstrap.bind(host, port);
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
					synchronized ( CannelloniCanbusConnection.this ) {
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
						log.warn("Unable to connect to Cannelloni @ {}:{}: {}", host, port,
								root.getMessage());
					} else {
						log.error("Error connecting to Cannelloni @ {}:{}: {}", host, port,
								root.toString(), t);
					}
				}
				if ( !shutdown ) {
					scheduleConnect();
				}
			} else {
				synchronized ( CannelloniCanbusConnection.this ) {
					Channel ch = future.channel();
					InetSocketAddress addr = (InetSocketAddress) ch.localAddress();
					assert addr != null;
					log.info("Connected to Cannelloni @ {}:{}", addr.getHostString(), addr.getPort());
					ch.closeFuture().addListener(new ReconnectFuture());
					channel = ch;
					startFuture = null;
					// TODO postClientStatusChangeEvent(CannelloniClientStatus.Connected);
				}
			}
		}

	}

	private class ReconnectFuture implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			synchronized ( CannelloniCanbusConnection.this ) {
				//TODO postClientStatusChangeEvent(CannelloniClientStatus.Closed);
				if ( !shutdown ) {
					log.info("Connection to Cannelloni @ {}:{} closed; will auto-reconnect.", host,
							port);
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
			if ( ch.localAddress() != null ) {
				final InetSocketAddress addr = (InetSocketAddress) ch.localAddress();
				host = addr.getHostString();
				port = addr.getPort();
			} else {
				host = this.host;
				port = this.port;
			}
			log.info("Closing connection to Cannelloni @ {}:{}", host, port);
			result = ch.close().addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					try {
						if ( future.isSuccess() ) {
							log.info("Closed connection to Cannelloni @ {}:{}", host, port);
						} else {
							Throwable root = future.cause();
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							if ( root instanceof IOException ) {
								log.warn("Unable to close connection to Cannelloni @ {}:{}: {}", host,
										port, root.getMessage());
							} else {
								log.error("Error closing connection to Cannelloni @ {}:{}: {}", host,
										port, root.toString(), future.cause());
							}
						}
					} finally {
						synchronized ( CannelloniCanbusConnection.this ) {
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
			log.warn("Error waiting for Cannelloni connection to close gracefully: {}", e.toString());
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
			log.info("Scheduling attempt to reconnect to Cannelloni @ {}:{} in {}s", host, port, delay);
			ScheduledFuture<?> f = taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					try {
						start();
					} finally {
						synchronized ( CannelloniCanbusConnection.this ) {
							connectFuture = null;
						}
					}
				}
			}, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delay)));
			connectFuture = f;
			//TODO postClientStatusChangeEvent(CannelloniClientStatus.ConnectionScheduled);
			return f;
		} else {
			return start();
		}
	}

	// CanbusConnection

	@Sharable
	private class CanbusFrameHandler extends SimpleChannelInboundHandler<CanbusFrame> {

		private CanbusFrameHandler() {
			super(CanbusFrame.class, false);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, CanbusFrame msg) throws Exception {
			log.debug("CAN {} <= {}", busName, msg);
		}

	}

	@Override
	public String getBusName() {
		return busName;
	}

	@Override
	public void open() throws IOException {
		Future<?> openFuture = null;
		synchronized ( this ) {
			if ( startFuture != null ) {
				openFuture = startFuture;
			} else {
				shutdown = false;
				openFuture = start();
			}
		}
		if ( openFuture != null ) {
			try {
				openFuture.get(shutdownSeconds, TimeUnit.SECONDS);
			} catch ( Exception e ) {
				throw new IOException("Failed to open connection to Cannelloni @ " + host + ":" + port,
						e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized ( this ) {
			shutdown = true;
		}
		try {
			stop().get(shutdownSeconds, TimeUnit.SECONDS);
		} catch ( ExecutionException | InterruptedException | TimeoutException e ) {
			log.warn("Error waiting for Cannelloni connection to close gracefully: {}", e.toString(), e);
		}
	}

	@Override
	public boolean isEstablished() {
		return getConnectionStatus() == CannelloniConnectionStatus.Connected;
	}

	@Override
	public boolean isClosed() {
		return getConnectionStatus() == CannelloniConnectionStatus.Closed;
	}

	@Override
	public Future<Boolean> verifyConnectivity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
			CanbusFrameListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long identifierMask,
			Iterable<Long> dataFilters, CanbusFrameListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unsubscribe(int address, boolean forceExtendedAddress) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void monitor(CanbusFrameListener listener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unmonitor() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isMonitoring() {
		// TODO Auto-generated method stub
		return false;
	}

	// Accessors

	/**
	 * Get the Cannelloni host to connect to.
	 *
	 * @return the host name or IP address; defaults to {@link #DEFAULT_HOST}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get the Cannelloni port to connect to.
	 *
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
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

}
