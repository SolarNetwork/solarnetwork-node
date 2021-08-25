/* ==================================================================
 * StompSession.java - 11/08/2021 4:51:35 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.springframework.security.core.Authentication;
import org.springframework.util.PathMatcher;
import io.netty.channel.Channel;

/**
 * Details about a single setup session (i.e. connection).
 * 
 * @author matt
 * @version 1.0
 */
public class SetupSession {

	private final ConcurrentNavigableMap<String, String> subscriptions = new ConcurrentSkipListMap<>();
	private final UUID sessionId;
	private final String login;
	private final Channel channel;
	private final long created;
	private long lastActivity;
	private Authentication authentication;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A {@code sessionId} will be assigned during construction to a new random
	 * value.
	 * </p>
	 * 
	 * @param login
	 *        the username to associate with the session
	 * @param channel
	 *        the channel
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SetupSession(String login, Channel channel) {
		super();
		if ( login == null ) {
			throw new IllegalArgumentException("The login argument must not be null.");
		}
		this.login = login;
		if ( channel == null ) {
			throw new IllegalArgumentException("The channel argument must not be null.");
		}
		this.channel = channel;
		this.sessionId = UUID.randomUUID();
		this.created = System.currentTimeMillis();
		this.lastActivity = this.created;
	}

	/**
	 * Get the login.
	 * 
	 * @return the login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Get the session ID.
	 * 
	 * @return the session ID (never {@literal null}
	 */
	public UUID getSessionId() {
		return sessionId;
	}

	/**
	 * Get the channel.
	 * 
	 * @return the channel, never {@literal null}
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the created date
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * Get the last activity date.
	 * 
	 * @return the last activity date
	 */
	public long getLastActivity() {
		return lastActivity;
	}

	/**
	 * Mark the session as active as of "now".
	 * 
	 * <p>
	 * This updates the {@code lastActivity} date.
	 * </p>
	 */
	public void activity() {
		this.lastActivity = System.currentTimeMillis();
	}

	/**
	 * Test if the session has successfully authenticated.
	 * 
	 * @return {@literal true} if an {@code Authentication} has been set and is
	 *         authenticated
	 */
	public boolean isAuthenticated() {
		final Authentication auth = getAuthentication();
		return (auth != null && auth.isAuthenticated());
	}

	/**
	 * Get the session authentication.
	 * 
	 * @return the authentication
	 */
	public Authentication getAuthentication() {
		return authentication;
	}

	/**
	 * Set the session authentication.
	 * 
	 * @param authentication
	 *        the authentication to set
	 */
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	/**
	 * Add a subscription to topic mapping.
	 * 
	 * @param id
	 *        the subscription ID
	 * @param topic
	 *        the topic
	 */
	public void addSubscription(String id, String topic) {
		subscriptions.put(id, topic);
	}

	/**
	 * Remove a subscription to topic mapping.
	 * 
	 * @param id
	 *        the subscription ID to remove
	 * @return the removed topic, or {@literal null} if the subscription ID was
	 *         not mapped
	 */
	public String removeSubscription(String id) {
		return subscriptions.remove(id);
	}

	/**
	 * Get all subscription IDs for a given topic.
	 * 
	 * @param topic
	 *        the topic to get subscription IDs for
	 * @param pathMatcher
	 *        an optional path patcher to interpret subscription topics with
	 * @return the matching subscription IDs, never {@literal null}
	 */
	public Collection<String> subscriptionIdsForTopic(String topic, PathMatcher pathMatcher) {
		SortedSet<String> result = new TreeSet<>();
		for ( Entry<String, String> e : subscriptions.entrySet() ) {
			String subTopic = e.getValue();
			if ( pathMatcher != null && pathMatcher.isPattern(subTopic) ) {
				if ( pathMatcher.match(subTopic, topic) ) {
					result.add(e.getKey());
				}
			} else if ( subTopic.equals(topic) ) {
				result.add(e.getKey());
			}
		}
		return result;
	}

}
