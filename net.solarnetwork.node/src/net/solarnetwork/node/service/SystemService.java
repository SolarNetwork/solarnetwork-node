/* ==================================================================
 * SystemService.java - 10/02/2017 8:31:04 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import java.net.InetAddress;
import org.springframework.util.MultiValueMap;

/**
 * API for node system services, such as restarting, rebooting, or making system
 * configuration changes.
 * 
 * @author matt
 * @version 1.3
 * @since 1.47
 */
public interface SystemService {

	/**
	 * The instruction topic for a request to restart the SolarNode application.
	 * 
	 * <p>
	 * In practice this could mean invoking the {@link #exit(boolean)} method,
	 * assuming the node OS automatically restarts the application when it
	 * exits.
	 * </p>
	 * 
	 * @since 1.1
	 */
	String TOPIC_RESTART = "SystemRestart";

	/**
	 * The instruction topic for a request to reboot the SolarNode device.
	 * 
	 * <p>
	 * In practice this could mean invoking the {@link #reboot()} method.
	 * </p>
	 * 
	 * @since 1.1
	 */
	String TOPIC_REBOOT = "SystemReboot";

	/**
	 * The instruction topic for a request to perform a factory reset of the
	 * SolarNode device.
	 * 
	 * <p>
	 * In practice this could mean invoking the {@link #reset(boolean)} method.
	 * </p>
	 * 
	 * @since 1.2
	 */
	String TOPIC_RESET = "SystemReset";

	/**
	 * Exit the node application, stopping the active process.
	 * 
	 * @param syncState
	 *        A flag to indicate (when {@code true}) that any transient data
	 *        should be persisted to permanent storage.
	 */
	void exit(boolean syncState);

	/**
	 * Reboot the device the application is running on.
	 */
	void reboot();

	/**
	 * Perform a factory reset.
	 * 
	 * @param applicationOnly
	 *        if {@literal true} then only reset SolarNode application settings;
	 *        if {@literal false} then also reset OS-level settings (such as
	 *        network passwords).
	 * @since 1.2
	 */
	void reset(boolean applicationOnly);

	/**
	 * Get the available host aliases.
	 * 
	 * @return the host aliases, never {@literal null}
	 * @since 1.3
	 */
	MultiValueMap<InetAddress, String> hostAliases();

	/**
	 * Add a host alias.
	 * 
	 * @param alias
	 *        the hostname to add
	 * @param address
	 *        the host address
	 */
	void addHostAlias(String alias, InetAddress address);

	/**
	 * Remove a host alias.
	 * 
	 * @param alias
	 *        the alias to remove
	 */
	void removeHostAlias(String alias);

}
