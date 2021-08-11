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

package net.solarnetwork.node.setup.stomp;

import java.util.UUID;
import io.netty.channel.Channel;

/**
 * Details about a single setup session (i.e. connection).
 * 
 * @author matt
 * @version 1.0
 */
public class SetupSession {

	private final UUID sessionId;
	private final Channel channel;
	private final long created;
	private long lastActivity;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A {@code sessionId} will be assigned during construction to a new random
	 * value.
	 * </p>
	 */
	public SetupSession(Channel channel) {
		super();
		if ( channel == null ) {
			throw new IllegalArgumentException("The channel argument must not be null.");
		}
		this.sessionId = UUID.randomUUID();
		this.channel = channel;
		this.created = System.currentTimeMillis();
		this.lastActivity = this.created;
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

}
