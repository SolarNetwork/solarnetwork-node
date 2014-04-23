/* ==================================================================
 * PluginProvisionException.java - Apr 23, 2014 10:15:37 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
 * Exception thrown when a provisioning error occurs, such as a missing
 * dependency that prevents a plugin from being installed.
 * 
 * @author matt
 * @version 1.0
 */
public class PluginProvisionException extends RuntimeException {

	private static final long serialVersionUID = -2847282484880397704L;

	public PluginProvisionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PluginProvisionException(String message) {
		super(message);
	}

	public PluginProvisionException(Throwable cause) {
		super(cause);
	}

}
