/* ==================================================================
 * AbstractNiftyModbusNetwork.java - 19/12/2022 10:11:26 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.nifty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import io.netty.channel.EventLoopGroup;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.ModbusClientConfig;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Base class for Nifty Modbus implementations of {@link ModbusNetwork}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractNiftyModbusNetwork<C extends ModbusClientConfig>
		extends AbstractModbusNetwork implements SettingSpecifierProvider, SettingsChangeObserver,
		ServiceLifecycleObserver, ThreadFactory {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 0;

	/** The {@code eventLoopGroupMaxThreadCount} property default value. */
	public static final int DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT = 4;

	/** The {@code replyTimeout} property default value. */
	public static final long DEFAULT_REPLY_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

	private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

	/** The client configuration. */
	protected final C config;

	/** The Modbus client. */
	protected ModbusClient controller;

	private EventLoopGroup eventLoopGroup;
	private int eventLoopGroupMaxThreadCount;
	private int keepOpenSeconds = DEFAULT_KEEP_OPEN_SECONDS;
	private long replyTimeout = DEFAULT_REPLY_TIMEOUT;
	private boolean wireLogging = false;

	private NiftyCachedModbusConnection cachedConnection;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AbstractNiftyModbusNetwork(C config) {
		super();
		this.config = ObjectUtils.requireNonNullArgument(config, "config");
		setUid(null);
		setEventLoopGroupMaxThreadCount(DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT);
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( controller != null ) {
			controller.stop();
			controller = null;
		}
		if ( cachedConnection != null ) {
			cachedConnection.forceClose();
		}
		if ( eventLoopGroup != null ) {
			eventLoopGroup.shutdownGracefully();
			eventLoopGroup = null;
		}
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		try {
			if ( eventLoopGroup != null ) {
				eventLoopGroup.shutdownGracefully();
				eventLoopGroup = null;
			}
			if ( cachedConnection != null ) {
				cachedConnection.forceClose();
				cachedConnection = null;
			}
			if ( controller != null ) {
				controller.stop();
				controller = null;
			}
			if ( isConfigured() ) {
				controller = createController();
				configureController(controller);
			}
		} catch ( Exception e ) {
			log.error("Error applying configuration change: {}", e.toString(), e);
		}
	}

	@Override
	protected String getNetworkDescription() {
		return config.getDescription();
	}

	/**
	 * Get the event loop group.
	 * 
	 * @return the event loop group, or {@literal null}
	 */
	protected EventLoopGroup eventLoopGroup() {
		return eventLoopGroup;
	}

	/**
	 * Get the event loop group, creating it if it does not already exist.
	 * 
	 * @param factory
	 *        the factory for creating a new event loop group
	 * @return the event loop group, never {@literal null}
	 */
	protected synchronized EventLoopGroup getOrCreateEventLoopGroup(Supplier<EventLoopGroup> factory) {
		EventLoopGroup g = eventLoopGroup();
		if ( g != null ) {
			return g;
		}
		g = factory.get();
		this.eventLoopGroup = g;
		return g;
	}

	/**
	 * Test if the network is fully configured.
	 * 
	 * @return {@literal true} if the configuration of the network is complete,
	 *         and can be used
	 */
	protected abstract boolean isConfigured();

	/**
	 * Create a new controller instance.
	 * 
	 * @return the new controller instance
	 */
	protected abstract ModbusClient createController();

	/**
	 * Configure a new controller instance.
	 * 
	 * @param controller
	 *        the instance to configure
	 */
	protected void configureController(ModbusClient controller) {
		// extending classes can do something here
	}

	@Override
	public synchronized ModbusConnection createConnection(int unitId) {
		if ( !isConfigured() || controller == null ) {
			return null;
		}

		if ( keepOpenSeconds > 0 ) {
			if ( cachedConnection == null ) {
				cachedConnection = new NiftyCachedModbusConnection(unitId, isHeadless(), controller,
						this::getNetworkDescription, keepOpenSeconds);
			}

			return createLockingConnection(cachedConnection);
		}

		return createLockingConnection(new NiftyModbusConnection(unitId, isHeadless(), controller,
				this::getNetworkDescription));
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r,
				"ModbusClient-" + config.getDescription() + "-" + THREAD_COUNT.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

	/**
	 * Get basic network settings.
	 * 
	 * @param keepOpenSeconds
	 *        the default keep open seconds value
	 * @return the settings
	 */
	public static List<SettingSpecifier> baseNiftyModbusNetworkSettings(int keepOpenSeconds) {
		List<SettingSpecifier> results = new ArrayList<>(1);
		results.add(
				new BasicTextFieldSettingSpecifier("keepOpenSeconds", String.valueOf(keepOpenSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("replyTimeout",
				String.valueOf(DEFAULT_REPLY_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("eventLoopGroupMaxThreadCount",
				String.valueOf(DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT)));
		results.add(new BasicToggleSettingSpecifier("wireLogging", Boolean.FALSE));
		return results;
	}

	/**
	 * Get the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @return the number of seconds; defaults to
	 *         {@link #DEFAULT_KEEP_OPEN_SECONDS}
	 */
	public int getKeepOpenSeconds() {
		return keepOpenSeconds;
	}

	/**
	 * Set the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @param keepOpenSeconds
	 *        the number of seconds, or anything less than {@literal 1} to not
	 *        keep connections open
	 */
	public void setKeepOpenSeconds(int keepOpenSeconds) {
		this.keepOpenSeconds = keepOpenSeconds;
	}

	/**
	 * Get the wire logging setting.
	 * 
	 * @return {@literal true} if wire-level logging is supported
	 */
	public boolean isWireLogging() {
		return wireLogging;
	}

	/**
	 * Set the wire logging setting.
	 * 
	 * <p>
	 * Wire-level logging causes raw Modbus frames to be logged using a logger
	 * name starting with <code>net.solarnetwork.io.modbus.</code> followed by
	 * an implementation-specific value. The <code>TRACE</code> log level is
	 * used, and must be enabled for the log messages to be actually generated.
	 * <b>Note</b> there is a small performance penalty for generating the
	 * wire-level log messages.
	 * </p>
	 * 
	 * @param wireLogging
	 *        {@literal true} if wire-level logging is supported
	 */
	public void setWireLogging(boolean wireLogging) {
		this.wireLogging = wireLogging;
	}

	/**
	 * Get the event loop group maximum thread count.
	 * 
	 * @return the maximum thread count; defaults to
	 *         {@link #DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT}
	 */
	public int getEventLoopGroupMaxThreadCount() {
		return eventLoopGroupMaxThreadCount;
	}

	/**
	 * Set the event loop group maximum thread count.
	 * 
	 * @param eventLoopGroupMaxThreadCount
	 *        the maximum thread count to set
	 */
	public void setEventLoopGroupMaxThreadCount(int eventLoopGroupMaxThreadCount) {
		this.eventLoopGroupMaxThreadCount = eventLoopGroupMaxThreadCount;
	}

	/**
	 * Get the message reply timeout.
	 * 
	 * @return the reply timeout, in milliseconds; defaults to
	 */
	public long getReplyTimeout() {
		return replyTimeout;
	}

	/**
	 * Set the message reply timeout.
	 * 
	 * @param replyTimeout
	 *        the reply timeout to set, in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

}
