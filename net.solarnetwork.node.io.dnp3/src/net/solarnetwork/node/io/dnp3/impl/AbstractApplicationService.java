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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import com.automatak.dnp3.Channel;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.OutstationService;
import net.solarnetwork.node.io.dnp3.domain.LinkLayerConfig;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Abstract implementation of {@link OutstationService}.
 *
 * @author matt
 * @version 2.1
 */
public abstract class AbstractApplicationService extends BasicIdentifiable implements OutstationService {

	/**
	 * The {@code uid} property default value.
	 *
	 * @since 2.0
	 */
	public static final String DEFAULT_UID = "DNP3 Outstation";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<ChannelService> dnp3Channel;

	private TaskExecutor taskExecutor;
	private TaskScheduler taskScheduler;
	private final LinkLayerConfig linkLayerConfig = new LinkLayerConfig(false);

	/**
	 * Constructor.
	 *
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public AbstractApplicationService(OptionalService<ChannelService> dnp3Channel) {
		super();
		this.dnp3Channel = dnp3Channel;
		setUid(DEFAULT_UID);
	}

	/**
	 * Configure and start the service.
	 *
	 * <p>
	 * This method calls {@link #configurationChanged(Map)} with a
	 * {@literal null} argument.
	 * </p>
	 */
	public synchronized void startup() {
		configurationChanged(null);
	}

	/**
	 * Callback after properties have been changed.
	 *
	 * <p>
	 * This method calls {@link #shutdown()}.
	 * </p>
	 *
	 * @param properties
	 *        the changed properties
	 */
	public synchronized void configurationChanged(Map<String, Object> properties) {
		shutdown();
	}

	/**
	 * Shutdown this service when no longer needed.
	 */
	public synchronized void shutdown() {
		// extending classes can override
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
	 * Copy the link layer configuration from one object to another.
	 *
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 * @since 1.1
	 */
	public static void copySettings(com.automatak.dnp3.LinkLayerConfig from,
			com.automatak.dnp3.LinkLayerConfig to) {
		to.isMaster = from.isMaster;
		to.keepAliveTimeout = from.keepAliveTimeout;
		to.localAddr = from.localAddr;
		to.remoteAddr = from.remoteAddr;
		to.responseTimeout = from.responseTimeout;
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
		results.add(new BasicTextFieldSettingSpecifier(prefix + "remoteAddr",
				String.valueOf(defaults.remoteAddr)));

		return results;
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

}
