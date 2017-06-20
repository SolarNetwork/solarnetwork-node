/* ==================================================================
 * RemoteSshConfig.java - 9/06/2017 4:48:21 PM
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

package net.solarnetwork.node.system.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Remote SSH configuration model object.
 * 
 * @author matt
 * @version 1.0
 */
public class RemoteSshConfig implements Comparable<RemoteSshConfig> {

	private final String user;
	private final String host;
	private final Integer port;
	private final Integer reversePort;
	private final Boolean error;
	private final List<String> messages;

	/**
	 * Parse a configuration key, in the form returned by
	 * {@link #toConfigKey()}.
	 * 
	 * <p>
	 * Messages are supported as well, as additional list items after the
	 * required configuration items. If an additional message is provided with
	 * the value {@literal error} then
	 * 
	 * @param key
	 *        the key to parse
	 * @return the configuration object
	 * @throws IllegalArgumentException
	 *         if the key cannot be parsed
	 */
	public static final RemoteSshConfig parseConfigKey(String key) {
		String[] components = key.split(",");
		if ( components.length < 4 ) {
			throw new IllegalArgumentException("The config key does not appear valid.");
		}
		try {
			Boolean error = false;
			List<String> messages = null;
			if ( components.length > 4 ) {
				int msgStart = 4;
				if ( "error".equalsIgnoreCase(components[4]) ) {
					msgStart = 5;
					error = true;
				}
				if ( msgStart < components.length ) {
					String[] msgs = new String[components.length - msgStart];
					System.arraycopy(components, msgStart, msgs, 0, msgs.length);
					messages = Arrays.asList(msgs);
				}
			}
			return new RemoteSshConfig(components[0], components[1], Integer.valueOf(components[2]),
					Integer.valueOf(components[3]), error, messages);
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("Invalid port value in key [" + key + "]", e);
		}
	}

	/**
	 * Construct with values.
	 * 
	 * @param user
	 *        the user
	 * @param host
	 *        the host
	 * @param port
	 *        the port
	 * @param reversePort
	 *        the reverse port
	 */
	public RemoteSshConfig(String user, String host, Integer port, Integer reversePort) {
		this(user, host, port, reversePort, Boolean.FALSE, null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param user
	 *        the user
	 * @param host
	 *        the host
	 * @param port
	 *        the port
	 * @param reversePort
	 *        the reverse port
	 * @param error
	 *        an error flag ({@literal true} if the configuration is in an error
	 *        state)
	 * @param messages
	 *        any additional messages associated with the configuration
	 */
	public RemoteSshConfig(String user, String host, Integer port, Integer reversePort, Boolean error,
			Collection<String> messages) {
		super();
		this.user = user;
		this.host = host;
		this.port = port;
		this.reversePort = reversePort;
		this.error = error;
		this.messages = (messages == null || messages.isEmpty() ? Collections.<String> emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(messages)));
	}

	/**
	 * Encode the configuration as a "key" value.
	 * 
	 * <p>
	 * The returned value is a comma-delimited list of all configuration
	 * properties, in {@code user}, {@code host}, {@code port},
	 * {@code reversePort} order.
	 * </p>
	 * 
	 * @return the configuration key
	 */
	public String toConfigKey() {
		StringBuilder buf = new StringBuilder(user);
		buf.append(',').append(host).append(',').append(port).append(',').append(reversePort);
		return buf.toString();
	}

	/**
	 * Encode the configuration as a display-friendly info value.
	 * 
	 * @return the display info
	 */
	public String toDisplayInfo() {
		StringBuilder buf = new StringBuilder();
		buf.append(user).append('@').append(host).append(':').append(port).append(":")
				.append(reversePort);
		return buf.toString();
	}

	/**
	 * Returns the same value as {@link #toConfigKey()}.
	 */
	@Override
	public String toString() {
		return toConfigKey();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((port == null) ? 0 : port.hashCode());
		result = prime * result + ((reversePort == null) ? 0 : reversePort.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof RemoteSshConfig) ) {
			return false;
		}
		RemoteSshConfig other = (RemoteSshConfig) obj;
		if ( host == null ) {
			if ( other.host != null ) {
				return false;
			}
		} else if ( !host.equals(other.host) ) {
			return false;
		}
		if ( port == null ) {
			if ( other.port != null ) {
				return false;
			}
		} else if ( !port.equals(other.port) ) {
			return false;
		}
		if ( reversePort == null ) {
			if ( other.reversePort != null ) {
				return false;
			}
		} else if ( !reversePort.equals(other.reversePort) ) {
			return false;
		}
		if ( user == null ) {
			if ( other.user != null ) {
				return false;
			}
		} else if ( !user.equals(other.user) ) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(RemoteSshConfig o) {
		if ( !host.equals(o.host) ) {
			return host.compareToIgnoreCase(o.host);
		}
		if ( !user.equals(o.user) ) {
			return user.compareToIgnoreCase(o.user);
		}
		if ( !port.equals(o.port) ) {
			return port.compareTo(o.port);
		}
		return reversePort.compareTo(o.reversePort);
	}

	/**
	 * Get the connection user.
	 * 
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Get the connection host.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get the connection port.
	 * 
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Get the reverse connection port.
	 * 
	 * @return the reversePort
	 */
	public Integer getReversePort() {
		return reversePort;
	}

	/**
	 * Get the error flag.
	 * 
	 * @return {@literal true} if the configuration represents an error state
	 */
	public Boolean getError() {
		return error;
	}

	/**
	 * Any additional messages, such as error information.
	 * 
	 * @return the messages
	 */
	public List<String> getMessages() {
		return messages;
	}

}
