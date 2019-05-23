/* ==================================================================
 * Constants.java - Mar 30, 2013 11:07:00 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

/**
 * SolarNode constants.
 * 
 * @author matt
 * @version 1.2
 */
public final class Constants {

	/** The system property for the node's home directory. */
	public static final String SYSTEM_PROP_NODE_HOME = "sn.home";

	/**
	 * An event topic to post when a significant change has occurred to the
	 * system's configuration. An example of a listener interested in such an
	 * event would be an automatic backup service.
	 * 
	 * @since 1.1
	 */
	public static final String EVENT_TOPIC_CONFIGURATION_CHANGED = "net/solarnetwork/node/CONFIGURATION_CHANGED";

	/**
	 * Get the configured SolarNode home directory.
	 * 
	 * <p>
	 * This returns the {@link #SYSTEM_PROP_NODE_HOME} system property value if
	 * available, or else {@literal /home/solar}.
	 * </p>
	 * 
	 * @return the home directory, never {@literal null}
	 * @since 1.2
	 */
	public static String solarNodeHome() {
		return System.getProperty(SYSTEM_PROP_NODE_HOME, "/home/solar");
	}

	private Constants() {
		// don't construct me
	}

}
