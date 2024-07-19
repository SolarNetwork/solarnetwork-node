/* ==================================================================
 * EventMessageRegistrar.java - 17/07/2024 2:22:25 pm
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

package net.solarnetwork.node.service;

import java.util.Map;
import java.util.function.Function;
import org.osgi.service.event.Event;

/**
 * API for maintaining event message registrations.
 *
 * @author matt
 * @version 1.0
 */
public interface EventMessageRegistrar {

	/**
	 * Register a topic mapping.
	 *
	 * @param eventTopic
	 *        the event topic
	 * @param messageTopic
	 *        the message topic
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws IllegalStateException
	 *         if the given {@code eventTopic} is already mapped to a different
	 *         message topic
	 */
	void registerTopicMapping(String eventTopic, String messageTopic);

	/**
	 * Unregister a topic mapping.
	 *
	 * <p>
	 * If the topic is not registered, this method does nothing.
	 * </p>
	 *
	 * @param eventTopic
	 *        the event topic
	 * @param messageTopic
	 *        the message topic
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void unregisterTopicMapping(String eventTopic, String messageTopic);

	/**
	 * Register an event data extractor.
	 *
	 * @param eventTopic
	 *        the event topic to register for
	 * @param extractor
	 *        the extractor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void registerEventDataExtractor(String eventTopic, Function<Event, Map<String, ?>> extractor);

	/**
	 * Unregister an event data extractor.
	 *
	 * @param eventTopic
	 *        the event topic to unregister from
	 * @param extractor
	 *        the extractor to unregister
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void unregisterEventDataExtractor(String eventTopic, Function<Event, Map<String, ?>> extractor);
}
