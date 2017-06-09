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

/**
 * Remote SSH configuration model object.
 * 
 * @author matt
 * @version 1.0
 */
public class RemoteSshConfig {

	private final String user;
	private final String host;
	private final Integer port;
	private final Integer reversePort;

	/**
	 * Parse a configuration key, in the form returned by
	 * {@link #toConfigKey()}.
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
			return new RemoteSshConfig(components[0], components[1], Integer.valueOf(components[2]),
					Integer.valueOf(components[3]));
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
		super();
		this.user = user;
		this.host = host;
		this.port = port;
		this.reversePort = reversePort;
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

}
