/* ==================================================================
 * OBRPluginProvisionStatus.java - Apr 23, 2014 9:17:51 AM
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

package net.solarnetwork.node.setup.obr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginProvisionStatus;

/**
 * OBR implementation of {@link PluginProvisionStatus}.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRPluginProvisionStatus implements PluginProvisionStatus {

	private final long creationDate;
	private final String provisionID;
	private String statusMessage;
	private Long overallDownloadSize;
	private Long overallDownloadedSize;
	private List<Plugin> pluginsToInstall = Collections.emptyList();
	private List<Plugin> pluginsInstalled = Collections.emptyList();
	private List<Plugin> pluginsToRemove = Collections.emptyList();
	private List<Plugin> pluginsRemoved = Collections.emptyList();

	/**
	 * Construct with an ID.
	 * 
	 * @param provisionID
	 *        the provisionID
	 * @throws IllegalArgumentException
	 *         if the provisionID is <em>null</em>
	 */
	public OBRPluginProvisionStatus(String provisionID) {
		super();
		if ( provisionID == null ) {
			throw new IllegalArgumentException("The provision ID must not be null");
		}
		this.creationDate = System.currentTimeMillis();
		this.provisionID = provisionID;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the status to copy
	 */
	public OBRPluginProvisionStatus(OBRPluginProvisionStatus other) {
		super();
		creationDate = other.creationDate;
		provisionID = other.provisionID;
		statusMessage = other.statusMessage;
		overallDownloadedSize = other.overallDownloadedSize;
		overallDownloadSize = other.overallDownloadSize;
		pluginsToInstall = new ArrayList<Plugin>(other.pluginsToInstall);
		pluginsInstalled = new ArrayList<Plugin>(other.pluginsInstalled);
		pluginsToRemove = new ArrayList<Plugin>(other.pluginsToRemove);
		pluginsRemoved = new ArrayList<Plugin>(other.pluginsRemoved);
	}

	@Override
	public String getProvisionID() {
		return provisionID;
	}

	@Override
	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public float getOverallProgress() {
		int steps = 0;
		float progress = 0;
		if ( overallDownloadSize != null && overallDownloadSize.longValue() > 0
				&& overallDownloadedSize != null ) {
			steps++;
			progress += (float) (overallDownloadedSize.doubleValue() / overallDownloadSize.doubleValue());
		}
		if ( pluginsToInstall != null && pluginsToInstall.size() > 0 && pluginsInstalled != null ) {
			steps++;
			progress += (float) ((double) pluginsInstalled.size() / (double) pluginsToInstall.size());
		}
		if ( pluginsToRemove != null && pluginsToRemove.size() > 0 && pluginsRemoved != null ) {
			steps++;
			progress += (float) ((double) pluginsRemoved.size() / (double) pluginsToRemove.size());
		}
		return (steps == 0 ? 0f : (progress / steps));
	}

	@Override
	public Long getOverallDownloadSize() {
		return overallDownloadSize;
	}

	@Override
	public Long getOverallDownloadedSize() {
		return overallDownloadedSize;
	}

	@Override
	public List<Plugin> getPluginsToInstall() {
		return pluginsToInstall;
	}

	@Override
	public List<Plugin> getPluginsToRemove() {
		return pluginsToRemove;
	}

	public List<Plugin> getPluginsInstalled() {
		return pluginsInstalled;
	}

	public void setPluginsInstalled(List<Plugin> pluginsInstalled) {
		this.pluginsInstalled = pluginsInstalled;
	}

	public List<Plugin> getPluginsRemoved() {
		return pluginsRemoved;
	}

	public void setPluginsRemoved(List<Plugin> pluginsRemoved) {
		this.pluginsRemoved = pluginsRemoved;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public void setOverallDownloadSize(Long overallDownloadSize) {
		this.overallDownloadSize = overallDownloadSize;
	}

	public void setOverallDownloadedSize(Long overallDownloadedSize) {
		this.overallDownloadedSize = overallDownloadedSize;
	}

	public void setPluginsToInstall(List<Plugin> pluginsToInstall) {
		this.pluginsToInstall = pluginsToInstall;
	}

	public void setPluginsToRemove(List<Plugin> pluginsToRemove) {
		this.pluginsToRemove = pluginsToRemove;
	}

	public long getCreationDate() {
		return creationDate;
	}

}
