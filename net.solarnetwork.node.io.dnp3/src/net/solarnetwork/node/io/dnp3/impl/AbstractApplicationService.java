/* ==================================================================
 * AbstractApplicationService.java - 22/02/2019 9:22:59 am
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

package net.solarnetwork.node.io.dnp3.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.Stack;
import com.automatak.dnp3.StackStatistics;
import com.automatak.dnp3.TransportStatistics;
import com.automatak.dnp3.enums.ChannelState;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.domain.LinkLayerConfig;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Abstract implementation of DNP3 application service.
 *
 * @param <T>
 *        the application stack type
 * @author matt
 * @version 3.0
 */
public abstract class AbstractApplicationService<T extends Stack> extends BaseIdentifiable
		implements ServiceLifecycleObserver, SettingsChangeObserver {

	/**
	 * The default startup delay.
	 *
	 * @since 3.0
	 */
	private static final int DEFAULT_STARTUP_DELAY_SECONDS = 5;

	private final OptionalService<ChannelService> dnp3Channel;
	private final LinkLayerConfig linkLayerConfig;

	private TaskExecutor taskExecutor;
	private TaskScheduler taskScheduler;
	private int startupDelaySecs = DEFAULT_STARTUP_DELAY_SECONDS;

	private Runnable initTask;
	private T dnp3Stack;

	/**
	 * Constructor.
	 *
	 * @param dnp3Channel
	 *        the channel to use
	 * @param linkLayerConfig
	 *        the link layer config to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public AbstractApplicationService(OptionalService<ChannelService> dnp3Channel,
			LinkLayerConfig linkLayerConfig) {
		super();
		this.dnp3Channel = requireNonNullArgument(dnp3Channel, "dnp3Channel");
		this.linkLayerConfig = requireNonNullArgument(linkLayerConfig, "linkLayerConfig");
	}

	@Override
	public synchronized void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		serviceDidShutdown();
		if ( initTask != null ) {
			// init task already underway
			return;
		}
		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			initTask = new Runnable() {

				@Override
				public void run() {
					try {
						log.info("Waiting {}s to start {} [{}]", getStartupDelaySecs(), getDisplayName(),
								getUid());
						Thread.sleep(getStartupDelaySecs() * 1000L);
					} catch ( InterruptedException e ) {
						// ignore
					} finally {
						synchronized ( AbstractApplicationService.this ) {
							initTask = null;
							dnp3Stack();
						}
					}
				}
			};
			executor.execute(initTask);
		} else {
			// no executor; init immediately
			dnp3Stack();
		}
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( dnp3Stack != null ) {
			final String uid = getUid();
			final String displayName = getDisplayName();
			log.info("Shutting down {} [{}]", displayName, uid);
			dnp3Stack.shutdown();
			this.dnp3Stack = null;
			log.info("{} [{}] shutdown", displayName, uid);
		}
	}

	/**
	 * Get the configured {@link ChannelService}.
	 *
	 * @return the service, or {@literal null} if not available
	 */
	protected ChannelService channelService() {
		return (dnp3Channel != null ? dnp3Channel.service() : null);
	}

	/**
	 * Get the configured {@link Channel}.
	 *
	 * @return the channel, or {@literal null} if not available
	 */
	protected Channel channel() {
		ChannelService service = channelService();
		return (service != null ? service.dnp3Channel() : null);
	}

	/**
	 * Get a map from the properties of an event.
	 *
	 * @param event
	 *        the event
	 * @return the map, or {@literal null} if {@code event} is {@literal null}
	 *         or has no properties
	 */
	protected Map<String, Object> mapForEvent(Event event) {
		if ( event == null ) {
			return null;
		}
		String[] propNames = event.getPropertyNames();
		if ( propNames == null || propNames.length < 1 ) {
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>(propNames.length);
		for ( String propName : propNames ) {
			if ( EventConstants.EVENT_TOPIC.equals(propName) ) {
				// exclude event topic
				continue;
			}
			map.put(propName, event.getProperty(propName));
		}
		return map;
	}

	/**
	 * Get settings suitable for configuring an instance of
	 * {@link LinkLayerConfig}.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @param defaults
	 *        the default settings
	 * @return the settings, never {@literal null}
	 * @since 1.1
	 */
	public static List<SettingSpecifier> linkLayerSettings(String prefix, LinkLayerConfig defaults) {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "localAddr",
				String.valueOf(defaults.localAddr)));
		if ( !defaults.isMaster() ) {
			results.add(new BasicTextFieldSettingSpecifier(prefix + "remoteAddr",
					String.valueOf(defaults.remoteAddr)));
		}

		return results;
	}

	/**
	 * Get a DNP3 stack status message.
	 *
	 * @param stackStats
	 *        the stack statistics
	 * @param channelState
	 *        the channel state
	 * @return the message, never {@code null}
	 * @since 3.0
	 */
	public static String stackStatusMessage(StackStatistics stackStats, ChannelState channelState) {
		if ( stackStats == null ) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		buf.append(channelState != null ? channelState : ChannelState.CLOSED);
		TransportStatistics stats = stackStats.transport;
		if ( stats != null ) {
			buf.append("; ").append(stats.numTransportRx).append(" in");
			buf.append("; ").append(stats.numTransportTx).append(" out");
			buf.append("; ").append(stats.numTransportErrorRx).append(" in errors");
			buf.append("; ").append(stats.numTransportBufferOverflow).append(" buffer overflows");
			buf.append("; ").append(stats.numTransportDiscard).append(" discarded");
			buf.append("; ").append(stats.numTransportIgnore).append(" ignored");
		}
		return buf.toString();
	}

	/**
	 * Get (or create and return) the DNP3 stack.
	 *
	 * @return the existing or newly created DNP3 stack
	 * @see #createDnp3Stack()
	 */
	protected synchronized T dnp3Stack() {
		final T stack = getDnp3Stack();
		if ( stack != null || initTask != null ) {
			return stack;
		}
		T newStack = createDnp3Stack();
		if ( newStack != null ) {
			newStack.enable();
			setDnp3Stack(newStack);
			log.info("DNP3 outstation [{}] enabled", getUid());
		}
		return newStack;
	}

	/**
	 * Create a new instance of the DNP3 stack.
	 *
	 * @return the new stack, or {@code null} if unable to create
	 */
	protected abstract T createDnp3Stack();

	/**
	 * Get the DNP3 stack.
	 *
	 * @return the stack
	 */
	protected synchronized T getDnp3Stack() {
		return dnp3Stack;
	}

	/**
	 * Set the DNP3 stack.
	 *
	 * @param dnp3Stack
	 *        the stack to set
	 */
	protected synchronized void setDnp3Stack(T dnp3Stack) {
		this.dnp3Stack = dnp3Stack;
	}

	/**
	 * Get the task executor.
	 *
	 * @return the task executor
	 */
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 *
	 * @param taskExecutor
	 *        the task executor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Get the task scheduler
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
	 * Get the channel service.
	 *
	 * @return the channel service
	 * @since 1.1
	 */
	public OptionalService<ChannelService> getDnp3Channel() {
		return dnp3Channel;
	}

	/**
	 * Get the link layer configuration
	 *
	 * @return the configuration
	 * @since 1.1
	 */
	public LinkLayerConfig getLinkLayerConfig() {
		return linkLayerConfig;
	}

	/**
	 * Get the startup delay, in seconds.
	 *
	 * @return the delay; defaults to {@link #DEFAULT_STARTUP_DELAY_SECONDS}
	 * @since 3.0
	 */
	public int getStartupDelaySecs() {
		return startupDelaySecs;
	}

	/**
	 * Set the startup delay, in seconds.
	 *
	 * <p>
	 * This delay is used to allow the class to be configured fully before
	 * starting.
	 * </p>
	 *
	 * @param startupDelaySecs
	 *        the delay
	 * @since 3.0
	 */
	public void setStartupDelaySecs(int startupDelaySecs) {
		this.startupDelaySecs = startupDelaySecs;
	}

}
