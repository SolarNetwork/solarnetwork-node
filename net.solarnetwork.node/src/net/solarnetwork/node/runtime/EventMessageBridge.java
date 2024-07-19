/* ==================================================================
 * EventMessageBridge.java - 24/09/2017 5:59:19 PM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.support.GenericMessage;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.EventMessageRegistrar;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.PlatformService;
import net.solarnetwork.node.service.UploadService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Bridge between OSGi EventAdmin events and a Spring Messaging.
 *
 * @author matt
 * @version 2.2
 */
public class EventMessageBridge implements EventHandler, EventMessageRegistrar {

	/** A prefix automatically stripped from event topic values. */
	public static final String NODE_EVENT_PREFIX = "net/solarnetwork/node/";

	/** A prefix automatically stripped from event topic values. */
	public static final String SN_EVENT_PREFIX = "net/solarnetwork/";

	/** The prefix automatically added to every message topic value. */
	public static final String MESSAGE_TOPIC_PREFIX = "/topic/";

	/**
	 * The prefix automatically added to every public message topic value.
	 *
	 * @since 1.1
	 */
	public static final String PUBLIC_MESSAGE_TOPIC_PREFIX = "/pub/topic/";

	private final OptionalService<SimpMessageSendingOperations> messageSendingOps;
	private final Executor executor;

	private Map<String, String> topicMapping;
	private Map<String, String> publicTopicMapping;
	private Map<String, Set<Function<Event, Map<String, ?>>>> eventExtractors;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static Map<String, String> defaultTopicMapping() {
		Map<String, String> map = new HashMap<>();
		map.put(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, "datum/captured/{sourceId}");
		map.put(DatumDao.EVENT_TOPIC_DATUM_STORED, "datum/stored/{sourceId}");
		map.put(UploadService.EVENT_TOPIC_DATUM_UPLOADED, "datum/uploaded/{sourceId}");
		map.put(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, "control/captured/{controlId}");
		map.put(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, "control/changed/{controlId}");
		return Collections.unmodifiableMap(map);
	}

	private static Map<String, String> defaultPublicTopicMapping() {
		Map<String, String> map = new HashMap<>();
		map.put(PlatformService.EVENT_TOPIC_PLATFORM_STATE_CHANGED, "platform/state");
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Constructor.
	 *
	 * @param messageSendingOps
	 *        the optional message sending service to use
	 * @throws IllegalArgumentException
	 *         if {@code messageSendingOps} is {@literal null}
	 */
	public EventMessageBridge(OptionalService<SimpMessageSendingOperations> messageSendingOps) {
		this(messageSendingOps, null);
	}

	/**
	 * Constructor.
	 *
	 * @param messageSendingOps
	 *        the optional message sending service to use
	 * @param executor
	 *        the optional executor to use
	 * @throws IllegalArgumentException
	 *         if {@code messageSendingOps} is {@literal null}
	 * @since 2.1
	 */
	public EventMessageBridge(OptionalService<SimpMessageSendingOperations> messageSendingOps,
			Executor executor) {
		super();
		this.messageSendingOps = ObjectUtils.requireNonNullArgument(messageSendingOps,
				"messageSendingOps");
		this.executor = executor;
		topicMapping = new HashMap<>(defaultTopicMapping());
		publicTopicMapping = new HashMap<>(defaultPublicTopicMapping());
		eventExtractors = new HashMap<>(8);
	}

	@Override
	public void registerTopicMapping(String eventTopic, String messageTopic) {
		requireNonNullArgument(eventTopic, "eventTopic");
		requireNonNullArgument(messageTopic, "messageTopic");
		synchronized ( topicMapping ) {
			final String exisiting = topicMapping.get(eventTopic);
			if ( exisiting != null ) {
				if ( exisiting.equals(messageTopic) ) {

					// nothing to do, already registered
					return;
				}
				throw new IllegalStateException(
						"Event topic [" + eventTopic + "] already mapped to topic [" + exisiting
								+ "], cannot map to [" + messageTopic + "]");
			}
			topicMapping.put(eventTopic, messageTopic);
		}
	}

	@Override
	public void unregisterTopicMapping(String eventTopic, String messageTopic) {
		requireNonNullArgument(eventTopic, "eventTopic");
		requireNonNullArgument(messageTopic, "messageTopic");
		synchronized ( topicMapping ) {
			topicMapping.remove(eventTopic, messageTopic);
		}
	}

	@Override
	public void registerEventDataExtractor(String eventTopic,
			Function<Event, Map<String, ?>> extractor) {
		requireNonNullArgument(eventTopic, "eventTopic");
		requireNonNullArgument(extractor, "extractor");
		synchronized ( eventExtractors ) {
			eventExtractors.computeIfAbsent(eventTopic, k -> new HashSet<>(4)).add(extractor);
		}
	}

	@Override
	public void unregisterEventDataExtractor(String eventTopic,
			Function<Event, Map<String, ?>> extractor) {
		requireNonNullArgument(eventTopic, "eventTopic");
		requireNonNullArgument(extractor, "extractor");
		synchronized ( eventExtractors ) {
			Set<Function<Event, Map<String, ?>>> set = eventExtractors.get(eventTopic);
			if ( set != null ) {
				set.remove(extractor);
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		final Set<Function<Event, Map<String, ?>>> extractors = eventExtractors.get(event.getTopic());
		Map<String, ?> data;
		if ( extractors != null && !extractors.isEmpty() ) {
			Map<String, Object> d = new LinkedHashMap<>(8);
			for ( Function<Event, Map<String, ?>> fn : extractors ) {
				Map<String, ?> ed = fn.apply(event);
				if ( ed != null ) {
					d.putAll(ed);
				}
			}
			data = d;
		} else {
			data = DatumEvents.datumEventMap(event);
		}
		String topic = messageTopicForEvent(event, data);
		if ( topic == null ) {
			return;
		}
		log.debug("Posting event {} to message topic {} with data {}", event.getTopic(), topic, data);
		if ( executor != null ) {
			executor.execute(() -> postMessage(topic, data));
		} else {
			postMessage(topic, data);
		}
	}

	private String messageTopicForEvent(Event event, Map<String, ?> data) {
		boolean pubTopic = false;
		String topic = event.getTopic();
		if ( topicMapping != null ) {
			String val = topicMapping.get(topic);
			if ( val != null ) {
				topic = val;
			}
		}
		if ( publicTopicMapping != null ) {
			String val = publicTopicMapping.get(topic);
			if ( val != null ) {
				topic = val;
				pubTopic = true;
			}
		}
		topic = StringUtils.expandTemplateString(topic, data);
		if ( topic.startsWith(NODE_EVENT_PREFIX) ) {
			topic = topic.substring(NODE_EVENT_PREFIX.length());
		} else if ( topic.startsWith(SN_EVENT_PREFIX) ) {
			topic = topic.substring(SN_EVENT_PREFIX.length());
		}

		// remove double-slashes
		topic = topic.replaceAll("\\/\\/", "/");

		return (pubTopic ? PUBLIC_MESSAGE_TOPIC_PREFIX : MESSAGE_TOPIC_PREFIX) + topic;
	}

	/**
	 * Post a message without any headers, converting it first.
	 *
	 * <p>
	 * Will silently ignore the event if no {@link MessageSendingOperations} is
	 * available.
	 * </p>
	 *
	 * @param dest
	 *        The destination to post to.
	 * @param body
	 *        The message body to post. This will be wrapped in a {@link Result}
	 *        object if it is not one already.
	 * @see #postMessage(String, Object, Map, boolean)
	 */
	protected void postMessage(String dest, Object body) {
		postMessage(dest, body, null, true);
	}

	/**
	 * Post a message. Will silently ignore the event if no
	 * {@link MessageSendingOperations} is available.
	 *
	 * <p>
	 * If {@code convert} is {@literal true} the message will be sent via the
	 * {@link MessageSendingOperations#convertAndSend(Object, Object, Map)}
	 * method. Otherwise the
	 * {@link MessageSendingOperations#send(Object, Message)} method will be
	 * used to send the body as-is.
	 * </p>
	 *
	 * @param dest
	 *        The destination to post to.
	 * @param body
	 *        The message body to post. If {@code convert} is {@literal true}
	 *        then this will be wrapped in a {@link Result} object if it is not
	 *        one already.
	 * @param headers
	 *        an optional set of message headers to include
	 * @param convert
	 *        {@literal true} to convert the message before sending,
	 *        {@literal false} to send without any conversion
	 * @since 1.1
	 */
	protected void postMessage(String dest, Object body, Map<String, Object> headers, boolean convert) {
		SimpMessageSendingOperations ops = (messageSendingOps != null ? messageSendingOps.service()
				: null);
		if ( ops == null ) {
			return;
		}
		if ( convert ) {
			Result<?> r = (body instanceof Result ? (Result<?>) body : Result.result(body));
			ops.convertAndSend(dest, r, headers);
		} else {
			Message<Object> msg = new GenericMessage<Object>(body, headers);
			ops.send(dest, msg);
		}
	}

	/**
	 * Set a mapping of event topic values to corresponding message topic
	 * values.
	 *
	 * <p>
	 * The event topic values are first stripped of any
	 * {@link #NODE_EVENT_PREFIX}, and the resulting value used as a key to
	 * lookup a message topic value to use from this map. The message topics can
	 * use template variables as defined in
	 * {@link StringUtils#expandTemplateString(String, Map)}; all the event
	 * properties are made available to the expansion.
	 * </p>
	 *
	 * @param topicMapping
	 *        the topicMapping to set
	 */
	public void setTopicMapping(Map<String, String> topicMapping) {
		this.topicMapping = topicMapping;
	}

	/**
	 * Set the mapping of event topic values to corresponding public message
	 * topic values.
	 *
	 * <p>
	 * The same messaging handling rules as documented in
	 * {@link #setTopicMapping(Map)} apply here, except that the resulting topic
	 * will have the {@link #PUBLIC_MESSAGE_TOPIC_PREFIX} instead of the
	 * {@link #MESSAGE_TOPIC_PREFIX}.
	 * </p>
	 *
	 * @param publicTopicMapping
	 *        the public topic mapping to set
	 * @since 1.1
	 * @see #setTopicMapping(Map)
	 */
	public void setPublicTopicMapping(Map<String, String> publicTopicMapping) {
		this.publicTopicMapping = publicTopicMapping;
	}

	/**
	 * Get the event extractors.
	 *
	 * @return the extractors
	 * @since 2.2
	 */
	public final Map<String, Set<Function<Event, Map<String, ?>>>> getEventExtractors() {
		return eventExtractors;
	}

	/**
	 * Set the event extractors.
	 *
	 * @param eventExtractors
	 *        the extractors to set
	 * @since 2.2
	 */
	public final void setEventExtractors(
			Map<String, Set<Function<Event, Map<String, ?>>>> eventExtractors) {
		this.eventExtractors = (eventExtractors != null ? eventExtractors : new HashMap<>(8));
	}

}
