/* ==================================================================
 * PluginProvisionStatus.java - Apr 23, 2014 8:22:42 AM
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

import java.util.List;

/**
 * API for status information and feedback during plugin provisioning
 * operations, that is, installing or removing plugins.
 * 
 * @author matt
 * @version 1.1
 */
public interface PluginProvisionStatus {

	/**
	 * Get a unique provisioning operation ID for this status, so clients of the
	 * {@link PluginService} API can track progress.
	 * 
	 * @return a unique provisioning operation ID
	 */
	String getProvisionID();

	/**
	 * Get a status message on the provision progress. This might include
	 * information on which plugin was being downloaded, installed, removed,
	 * etc.
	 * 
	 * @return a status message
	 */
	String getStatusMessage();

	/**
	 * Get an overall progress amount, as a percentage between 0 and 1.
	 * 
	 * @return percent complete
	 */
	float getOverallProgress();

	/**
	 * Get an overall number of bytes that must be downloaded to complete the
	 * provisioning operation.
	 * 
	 * @return number of bytes, or <em>null</em> if not known
	 */
	Long getOverallDownloadSize();

	/**
	 * Get the number of bytes that have been downloaded so far while executing
	 * this provisioning operation.
	 * 
	 * @return number of bytes, or <em>null</em> if not known
	 */
	Long getOverallDownloadedSize();

	/**
	 * Get a list of plugins to be installed as part of this provisioning
	 * operation.
	 * 
	 * @return a list of plugins to intall, or an empty list if none
	 */
	List<Plugin> getPluginsToInstall();

	/**
	 * Get a list of plugins to be removed as part of this provisioning
	 * operation.
	 * 
	 * @return a list of plugins to remove, or an empty list if none
	 */
	List<Plugin> getPluginsToRemove();

	/**
	 * Flag indicating a restart will be required after the provision task
	 * completes.
	 * 
	 * @return {@literal true} if a restart is required after the provision task
	 *         completes
	 * @since 1.1
	 */
	boolean isRestartRequired();

}
