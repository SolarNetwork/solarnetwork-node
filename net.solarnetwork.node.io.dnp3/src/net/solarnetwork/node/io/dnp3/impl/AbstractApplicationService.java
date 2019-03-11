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

import java.util.LinkedHashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import com.automatak.dnp3.Channel;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.OutstationService;
import net.solarnetwork.util.OptionalService;

/**
 * Abstract implementation of {@link OutstationService}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractApplicationService implements OutstationService {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<ChannelService> dnp3Channel;

	private String uid = "DNP3 Outstation";
	private String groupUID;
	private MessageSource messageSource;
	private TaskExecutor taskExecutor;
	private TaskScheduler taskScheduler;

	/**
	 * Constructor.
	 * 
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public AbstractApplicationService(OptionalService<ChannelService> dnp3Channel) {
		super();
		this.dnp3Channel = dnp3Channel;
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
	 * Alias for the {@link #getUID()} method.
	 * 
	 * @return the unique ID
	 * @see #getUID()
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * Set the unique ID to identify this service with.
	 * 
	 * @param uid
	 *        the unique ID; defaults to {@literal Modbus Port}
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getUID() {
		return uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	/**
	 * Set the group unique ID to identify this service with.
	 * 
	 * @param groupUID
	 *        the group unique ID
	 */
	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

}
