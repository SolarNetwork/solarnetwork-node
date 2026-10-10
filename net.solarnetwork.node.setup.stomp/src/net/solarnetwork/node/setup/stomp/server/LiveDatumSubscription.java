/* ==================================================================
 * LiveDatumSubscription.java - 10/10/2026 1:42:00 PM
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A single live datum subscription, for one source on one setup session.
 *
 * <p>
 * The mutable state is guarded by the instance monitor. Synchronize on the
 * instance for compound operations.
 * </p>
 *
 * @author elijah
 * @version 1.0
 * @since 4.1
 */
public class LiveDatumSubscription {

	/** The {@code lastSampleTs} value before any sample is sent. */
	private static final long NO_SAMPLE = Long.MIN_VALUE;

	private final SetupSession session;
	private final String subscriptionId;
	private final String sourceId;
	private final List<String> properties;
	private final long intervalMs;
	private final long created;
	private final long expires;

	private long lastSampleTs = NO_SAMPLE;
	private long lastDatumAt;
	private boolean confirmed;
	private boolean stalled;
	private boolean closed;

	/**
	 * Constructor.
	 *
	 * @param session
	 *        the session
	 * @param subscriptionId
	 *        the STOMP subscription ID
	 * @param sourceId
	 *        the source ID to stream
	 * @param properties
	 *        the property names to include, or an empty list for the default
	 *        set
	 * @param intervalMs
	 *        the minimum interval between messages, in milliseconds
	 * @param created
	 *        the creation date, as an epoch millisecond value
	 * @param expires
	 *        the expiration date, as an epoch millisecond value
	 * @throws IllegalArgumentException
	 *         if {@code session}, {@code subscriptionId}, or {@code sourceId}
	 *         are {@literal null}
	 */
	public LiveDatumSubscription(SetupSession session, String subscriptionId, String sourceId,
			List<String> properties, long intervalMs, long created, long expires) {
		super();
		this.session = requireNonNullArgument(session, "session");
		this.subscriptionId = requireNonNullArgument(subscriptionId, "subscriptionId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
		this.properties = (properties != null ? Collections.unmodifiableList(properties)
				: Collections.emptyList());
		this.intervalMs = intervalMs;
		this.created = created;
		this.expires = expires;
	}

	/**
	 * Get a registry key for a session and subscription ID.
	 *
	 * @param sessionId
	 *        the session ID
	 * @param subscriptionId
	 *        the subscription ID
	 * @return the key
	 */
	public static String key(UUID sessionId, String subscriptionId) {
		return sessionId + "/" + subscriptionId;
	}

	/**
	 * Get the registry key for this subscription.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key(session.getSessionId(), subscriptionId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LiveDatumSubscription{session=");
		builder.append(session.getSessionId());
		builder.append(", subscriptionId=");
		builder.append(subscriptionId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append(", intervalMs=");
		builder.append(intervalMs);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Test if a sample should be sent.
	 *
	 * <p>
	 * A sample is sent if this subscription is open, the sample is newer than
	 * the last one sent, and at least 80% of the interval has passed since the
	 * last sample sent, to allow for poll jitter.
	 * </p>
	 *
	 * @param sampleTs
	 *        the sample timestamp, as an epoch millisecond value
	 * @return {@literal true} if the sample should be sent
	 */
	public synchronized boolean shouldSend(long sampleTs) {
		if ( closed ) {
			return false;
		}
		if ( lastSampleTs == NO_SAMPLE ) {
			return true;
		}
		if ( sampleTs <= lastSampleTs ) {
			return false;
		}
		final long minElapsed = intervalMs - (intervalMs / 5);
		return (sampleTs - lastSampleTs) >= minElapsed;
	}

	/**
	 * Record that a sample has been sent.
	 *
	 * @param sampleTs
	 *        the sample timestamp, as an epoch millisecond value
	 */
	public synchronized void sampleSent(long sampleTs) {
		this.lastSampleTs = sampleTs;
	}

	/**
	 * Record that a datum for this subscription's source was received.
	 *
	 * <p>
	 * This confirms the subscription and clears any stalled state.
	 * </p>
	 *
	 * @param now
	 *        the current time, as an epoch millisecond value
	 */
	public synchronized void datumReceived(long now) {
		this.lastDatumAt = now;
		this.confirmed = true;
		this.stalled = false;
	}

	/**
	 * Close the subscription.
	 *
	 * @return {@literal true} if the subscription was open before this call
	 */
	public synchronized boolean close() {
		if ( closed ) {
			return false;
		}
		closed = true;
		return true;
	}

	/**
	 * Test if the subscription has been closed.
	 *
	 * @return {@literal true} if closed
	 */
	public synchronized boolean isClosed() {
		return closed;
	}

	/**
	 * Test if a datum for this subscription's source has been received.
	 *
	 * @return {@literal true} if confirmed
	 */
	public synchronized boolean isConfirmed() {
		return confirmed;
	}

	/**
	 * Test if the subscription has been marked as stalled.
	 *
	 * @return {@literal true} if stalled
	 */
	public synchronized boolean isStalled() {
		return stalled;
	}

	/**
	 * Mark the subscription as stalled.
	 *
	 * @return {@literal true} if the subscription was not already stalled
	 */
	public synchronized boolean markStalled() {
		if ( stalled ) {
			return false;
		}
		stalled = true;
		return true;
	}

	/**
	 * Get the date the last datum for this subscription's source was received.
	 *
	 * @return the date, as an epoch millisecond value, or {@literal 0} if none
	 *         received
	 */
	public synchronized long getLastDatumAt() {
		return lastDatumAt;
	}

	/**
	 * Get the session.
	 *
	 * @return the session, never {@literal null}
	 */
	public SetupSession getSession() {
		return session;
	}

	/**
	 * Get the STOMP subscription ID.
	 *
	 * @return the subscription ID, never {@literal null}
	 */
	public String getSubscriptionId() {
		return subscriptionId;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID, never {@literal null}
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Get the property names to include.
	 *
	 * @return the property names, never {@literal null}; empty means the
	 *         default set
	 */
	public List<String> getProperties() {
		return properties;
	}

	/**
	 * Get the minimum interval between messages.
	 *
	 * @return the interval, in milliseconds
	 */
	public long getIntervalMs() {
		return intervalMs;
	}

	/**
	 * Get the creation date.
	 *
	 * @return the creation date, as an epoch millisecond value
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * Get the expiration date.
	 *
	 * @return the expiration date, as an epoch millisecond value
	 */
	public long getExpires() {
		return expires;
	}

}
