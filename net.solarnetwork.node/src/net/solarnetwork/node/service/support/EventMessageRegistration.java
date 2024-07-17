/* ==================================================================
 * EventMessageRegistration.java - 17/07/2024 2:36:11 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.service.EventMessageRegistrar;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.ObjectUtils;

/**
 * Dynamically register event message mappings.
 *
 * @author matt
 * @version 1.0
 */
public class EventMessageRegistration implements ServiceLifecycleObserver {

	private static final Logger log = LoggerFactory.getLogger(EventMessageRegistration.class);

	private final EventMessageRegistrar registrar;
	private Map<String, String> topicMappings;
	private Map<String, Function<Event, Map<String, ?>>> eventExtractors;

	/**
	 * Constructor.
	 *
	 * @param registrar
	 *        the registrar
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EventMessageRegistration(EventMessageRegistrar registrar) {
		super();
		this.registrar = ObjectUtils.requireNonNullArgument(registrar, "registrar");
	}

	@Override
	public void serviceDidStartup() {
		final Map<String, String> mappings = getTopicMappings();
		if ( mappings != null ) {
			for ( Entry<String, String> entry : mappings.entrySet() ) {
				try {
					registrar.registerTopicMapping(entry.getKey(), entry.getValue());
				} catch ( IllegalStateException e ) {
					log.warn(e.getMessage());
				}
			}
		}
		final Map<String, Function<Event, Map<String, ?>>> extractors = getEventExtractors();
		if ( extractors != null ) {
			for ( Entry<String, Function<Event, Map<String, ?>>> entry : extractors.entrySet() ) {
				registrar.registerEventDataExtractor(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void serviceDidShutdown() {
		final Map<String, String> mappings = getTopicMappings();
		if ( mappings != null ) {
			for ( Entry<String, String> entry : mappings.entrySet() ) {
				registrar.unregisterTopicMapping(entry.getKey(), entry.getValue());
			}
		}
		final Map<String, Function<Event, Map<String, ?>>> extractors = getEventExtractors();
		if ( extractors != null ) {
			for ( Entry<String, Function<Event, Map<String, ?>>> entry : extractors.entrySet() ) {
				registrar.unregisterEventDataExtractor(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Get the topic mappings.
	 *
	 * @return the topic mappings
	 */
	public final Map<String, String> getTopicMappings() {
		return topicMappings;
	}

	/**
	 * Set the topic mappings.
	 *
	 * @param topicMappings
	 *        the topic mappings to set
	 */
	public final void setTopicMappings(Map<String, String> topicMappings) {
		this.topicMappings = topicMappings;
	}

	/**
	 * Get the event extractors.
	 *
	 * @return the event extractors
	 */
	public final Map<String, Function<Event, Map<String, ?>>> getEventExtractors() {
		return eventExtractors;
	}

	/**
	 * Set the event extractors.
	 *
	 * @param eventExtractor
	 *        the event extractors to set
	 */
	public final void setEventExtractors(Map<String, Function<Event, Map<String, ?>>> eventExtractors) {
		this.eventExtractors = eventExtractors;
	}

}
