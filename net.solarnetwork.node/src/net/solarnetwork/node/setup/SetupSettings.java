/* ==================================================================
 * SetupSettings.java - Jun 1, 2010 12:09:59 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

/**
 * Constants for setup related settings.
 * 
 * @author matt
 * @version 1.0
 */
public final class SetupSettings {

	/** A type key for setup settings. */
	public static final String SETUP_TYPE_KEY = "solarnode.setup";

	/** The node ID. */
	public static final String KEY_NODE_ID = "solarnode.id";

	/** The SolarNetwork server host name. */
	public static final String KEY_SOLARNETWORK_HOST_NAME = "solarnode.solarnet.host";

	/** The SolarNetwork server host port. */
	public static final String KEY_SOLARNETWORK_HOST_PORT = "solarnode.solarnet.port";

	/** The SolarNetwork force TLS status. */
	public static final String KEY_SOLARNETWORK_FORCE_TLS = "solarnode.solarnet.forceTLS";

	/** The confirmation code supplied by the SolarNetwork server. */
	public static final String KEY_CONFIRMATION_CODE = "solarnode.solarnet.confirmation";

}
