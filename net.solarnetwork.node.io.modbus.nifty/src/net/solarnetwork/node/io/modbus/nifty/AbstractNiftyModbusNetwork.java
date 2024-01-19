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

import static java.util.Collections.singleton;
import static net.solarnetwork.node.service.OperationalModesService.hasActiveOperationalMode;
import static net.solarnetwork.service.OptionalService.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import io.netty.channel.EventLoopGroup;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.ModbusClientConfig;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.OperationalModesService.OperationalModeInfo;
import net.solarnetwork.service.OptionalService;
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
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.2
 */
public abstract class AbstractNiftyModbusNetwork<C extends ModbusClientConfig>
		extends AbstractModbusNetwork implements SettingSpecifierProvider, SettingsChangeObserver,
		ServiceLifecycleObserver, ThreadFactory, EventHandler {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 90;

	/** The {@code eventLoopGroupMaxThreadCount} property default value. */
	public static final int DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT = 4;

	/** The {@code replyTimeout} property default value. */
	public static final long DEFAULT_REPLY_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

	/**
	 * An operational mode to enable publishing Modbus CLI commands.
	 * 
	 * <p>
	 * If this mode is enabled, it overrides whatever value the
	 * {@code publishCliCommandMessages} setting has.
	 * </p>
	 * 
	 * @since 1.1
	 */
	public static final String PUBLISH_MODBUS_CLI_COMMANDS_MODE = "cli-commands/modbus";

	/**
	 * An operational mode tag for Modbus CLI commands.
	 * 
	 * @since 1.1
	 */
	public static final String CLI_COMMANDS_MODE_MODBUS_TAG = "modbus";

	/**
	 * A message topic for Modbus CLI commands to be published to.
	 * 
	 * @since 1.1
	 */
	public static final String PUBLISH_MODBUS_CLI_COMMANDS_TOPIC = "/topic/cli/modbus";

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

	private OptionalService<OperationalModesService> opModesService;
	private boolean publishCliCommandsMode;
	private boolean publishCliCommandMessages;
	private OptionalService<SimpMessageSendingOperations> messageSendingOps;

	private UUID opModeRegistrationId;
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
	public synchronized void serviceDidStartup() {
		OperationalModesService opModesService = service(this.opModesService);
		if ( opModesService != null ) {
			this.opModeRegistrationId = opModesService.registerOperationalModeInfo(
					new OperationalModeInfo(PUBLISH_MODBUS_CLI_COMMANDS_MODE,
							singleton(CLI_COMMANDS_MODE_MODBUS_TAG)));
			this.publishCliCommandsMode = opModesService
					.isOperationalModeActive(PUBLISH_MODBUS_CLI_COMMANDS_MODE);
		}
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( opModeRegistrationId != null ) {
			OperationalModesService opModesService = service(this.opModesService);
			if ( opModesService != null ) {
				opModesService.unregisterOperationalModeInfo(opModeRegistrationId);
			}
			this.opModeRegistrationId = null;
		}
		closeCachedConnection();
		if ( controller != null ) {
			controller.stop();
			controller = null;
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
			closeCachedConnection();
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

	private synchronized void closeCachedConnection() {
		if ( cachedConnection != null ) {
			if ( controller != null ) {
				controller.setConnectionObserver(null);
			}
			cachedConnection.forceClose();
			cachedConnection = null;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if ( !OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equals(event.getTopic()) ) {
			return;
		}
		boolean cliModeActive = hasActiveOperationalMode(event, PUBLISH_MODBUS_CLI_COMMANDS_MODE);
		if ( cliModeActive != this.publishCliCommandsMode ) {
			this.publishCliCommandsMode = cliModeActive;
			closeCachedConnection();
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
				cachedConnection = new NiftyCachedModbusConnection(isHeadless(), controller,
						this::getNetworkDescription, keepOpenSeconds);
				controller.setConnectionObserver(cachedConnection);
			}

			NiftyModbusConnection conn = cachedConnection.connection(unitId);
			conn.setPublishCliCommandMessages(
					this.publishCliCommandMessages || this.publishCliCommandsMode);
			conn.setMessageSendingOps(messageSendingOps);

			return createLockingConnection(conn);
		}

		NiftyModbusConnection conn = new NiftyModbusConnection(unitId, isHeadless(), controller,
				this::getNetworkDescription);
		conn.setPublishCliCommandMessages(this.publishCliCommandMessages || this.publishCliCommandsMode);
		conn.setMessageSendingOps(messageSendingOps);
		return createLockingConnection(conn);
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
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(DEFAULT_TIMEOUT_SECS)));
		results.add(
				new BasicTextFieldSettingSpecifier("keepOpenSeconds", String.valueOf(keepOpenSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("replyTimeout",
				String.valueOf(DEFAULT_REPLY_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("eventLoopGroupMaxThreadCount",
				String.valueOf(DEFAULT_EVENT_LOOP_MAX_THREAD_COUNT)));
		results.add(new BasicToggleSettingSpecifier("wireLogging", Boolean.FALSE));
		results.add(new BasicToggleSettingSpecifier("publishCliCommandMessages", Boolean.FALSE));
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

	/**
	 * Get the operational modes service.
	 * 
	 * @return the opModesService
	 */
	public OptionalService<OperationalModesService> getOpModesService() {
		return opModesService;
	}

	/**
	 * Set the operational modes service.
	 * 
	 * @param opModesService
	 *        the opModesService to set
	 */
	public void setOpModesService(OptionalService<OperationalModesService> opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Get the "publish CLI command messages" setting.
	 * 
	 * @return {@literal true} to publish CLI command messages
	 * @since 1.1
	 */
	public boolean isPublishCliCommandMessages() {
		return publishCliCommandMessages;
	}

	/**
	 * Set the "publish CLI command messages" setting.
	 * 
	 * @param publishCliCommandMessages
	 *        {@literal true} to publish CLI command messages; requires the
	 *        {@link #setMessageSendingOps(OptionalService)} property also be
	 *        configured
	 * @since 1.1
	 */
	public void setPublishCliCommandMessages(boolean publishCliCommandMessages) {
		this.publishCliCommandMessages = publishCliCommandMessages;
	}

	/**
	 * Get the message sending operations.
	 * 
	 * @return the message sending operations
	 * @since 1.1
	 */
	public OptionalService<SimpMessageSendingOperations> getMessageSendingOps() {
		return messageSendingOps;
	}

	/**
	 * Set the message sending operations.
	 * 
	 * @param messageSendingOps
	 *        the message sending operations to set; required by the
	 *        {@link #setPublishCliCommandMessages(boolean)} setting
	 * @since 1.1
	 */
	public void setMessageSendingOps(OptionalService<SimpMessageSendingOperations> messageSendingOps) {
		this.messageSendingOps = messageSendingOps;
	}

}
