/* ==================================================================
 * LiveDatumPublisher.java - 10/10/2026 1:42:00 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.server;

import static net.solarnetwork.node.setup.stomp.StompUtils.JSON_UTF8_CONTENT_TYPE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompHeaders;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.setup.stomp.LiveHeader;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.SetupTopic;

/**
 * Publish live datum {@literal MESSAGE} frames to setup sessions.
 *
 * <p>
 * Every frame is published to the {@link SetupTopic#DatumLive} destination for
 * a single subscription ID, with a {@link SetupHeader#Status} header. Datum
 * frames have a JSON object body with a {@link #TIMESTAMP_PROPERTY} property
 * plus the requested datum properties.
 * </p>
 *
 * @author elijah
 * @version 1.0
 * @since 4.1
 */
public class LiveDatumPublisher {

	/** The message body property used for the sample timestamp. */
	public static final String TIMESTAMP_PROPERTY = "t";

	private static final Logger log = LoggerFactory.getLogger(LiveDatumPublisher.class);

	private static final DatumSamplesType[] DEFAULT_SAMPLE_TYPES = new DatumSamplesType[] {
			DatumSamplesType.Instantaneous, DatumSamplesType.Accumulating };

	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the object mapper to encode message bodies with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LiveDatumPublisher(ObjectMapper objectMapper) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
	}

	/**
	 * Publish a datum message to a subscription.
	 *
	 * <p>
	 * Nothing is published if the subscription's channel is not active and
	 * writable, or the body cannot be encoded.
	 * </p>
	 *
	 * @param sub
	 *        the subscription
	 * @param datum
	 *        the datum
	 * @param sampleTs
	 *        the datum timestamp to publish, as an epoch millisecond value
	 * @return {@literal true} if a message was published
	 */
	public boolean publishDatum(LiveDatumSubscription sub, NodeDatum datum, long sampleTs) {
		final Channel channel = sub.getSession().getChannel();
		if ( !channel.isActive() || !channel.isWritable() ) {
			log.trace("Dropping live datum for {}: channel not writable", sub);
			return false;
		}
		final byte[] body = encodeBody(sub, datum, sampleTs);
		if ( body == null ) {
			return false;
		}
		writeFrame(sub.getSession(), sub.getSubscriptionId(), sub.getSourceId(), SetupStatus.Ok, null,
				body);
		return true;
	}

	/**
	 * Publish a status message to a subscription.
	 *
	 * @param sub
	 *        the subscription
	 * @param status
	 *        the status
	 * @param message
	 *        the message, or {@literal null}
	 */
	public static void publishStatus(LiveDatumSubscription sub, SetupStatus status, String message) {
		publishStatus(sub.getSession(), sub.getSubscriptionId(), sub.getSourceId(), status, message);
	}

	/**
	 * Publish a status message to a subscription ID.
	 *
	 * @param session
	 *        the session
	 * @param subscriptionId
	 *        the subscription ID
	 * @param sourceId
	 *        the source ID, or {@literal null} if not known
	 * @param status
	 *        the status
	 * @param message
	 *        the message, or {@literal null}
	 */
	public static void publishStatus(SetupSession session, String subscriptionId, String sourceId,
			SetupStatus status, String message) {
		writeFrame(session, subscriptionId, sourceId, status, message, null);
	}

	private byte[] encodeBody(LiveDatumSubscription sub, NodeDatum datum, long sampleTs) {
		final Map<String, Object> body = new LinkedHashMap<>(16);
		body.put(TIMESTAMP_PROPERTY, sampleTs);
		final DatumSamplesOperations ops = datum.asSampleOperations();
		if ( ops != null ) {
			final List<String> props = sub.getProperties();
			if ( props.isEmpty() ) {
				for ( DatumSamplesType type : DEFAULT_SAMPLE_TYPES ) {
					Map<String, ?> data = ops.getSampleData(type);
					if ( data != null ) {
						for ( Map.Entry<String, ?> e : data.entrySet() ) {
							if ( e.getValue() != null ) {
								body.putIfAbsent(e.getKey(), e.getValue());
							}
						}
					}
				}
			} else {
				for ( String p : props ) {
					if ( TIMESTAMP_PROPERTY.equals(p) ) {
						continue;
					}
					Object v = ops.findSampleValue(p);
					if ( v != null ) {
						body.put(p, v);
					}
				}
			}
		}
		try {
			return objectMapper.writeValueAsBytes(body);
		} catch ( JsonProcessingException e ) {
			log.warn("Error encoding live datum for {} as JSON: {}", sub, e.toString());
			return null;
		}
	}

	private static void writeFrame(SetupSession session, String subscriptionId, String sourceId,
			SetupStatus status, String message, byte[] body) {
		final Channel channel = session.getChannel();
		if ( !channel.isActive() ) {
			return;
		}
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.MESSAGE);
		f.headers().set(StompHeaders.DESTINATION, SetupTopic.DatumLive.getValue());
		// Netty escapes header values
		f.headers().set(StompHeaders.SUBSCRIPTION, subscriptionId);
		f.headers().set(StompHeaders.MESSAGE_ID, String.valueOf(session.nextMessageId()));
		if ( sourceId != null ) {
			f.headers().set(LiveHeader.SourceId.getValue(), sourceId);
		}
		f.headers().set(SetupHeader.Status.getValue(), String.valueOf(status.getCode()));
		if ( message != null ) {
			f.headers().set(StompHeaders.MESSAGE, message);
		}
		if ( body != null && body.length > 0 ) {
			f.headers().set(StompHeaders.CONTENT_TYPE, JSON_UTF8_CONTENT_TYPE);
			f.headers().set(StompHeaders.CONTENT_LENGTH, String.valueOf(body.length));
			f.content().writeBytes(body);
		}
		channel.writeAndFlush(f);
	}

}
