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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	private Set<Plugin> pluginsInstalled = Collections.emptySet();
	private Set<Plugin> pluginsStarted = Collections.emptySet();
	private List<Plugin> pluginsToRemove = Collections.emptyList();
	private Set<Plugin> pluginsRemoved = Collections.emptySet();

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
		pluginsInstalled = new HashSet<Plugin>(other.pluginsInstalled);
		pluginsStarted = new HashSet<Plugin>(other.pluginsStarted);
		pluginsToRemove = new ArrayList<Plugin>(other.pluginsToRemove);
		pluginsRemoved = new HashSet<Plugin>(other.pluginsRemoved);
	}

	@Override
	public String getProvisionID() {
		return provisionID;
	}

	@Override
	public String getStatusMessage() {
		return statusMessage;
	}

	/**
	 * Mark a given Plugin from the {@link #getPluginsToInstall()} list as
	 * installed.
	 * 
	 * @param plugin
	 *        the plugin to mark as installed
	 */
	public void markPluginInstalled(Plugin plugin) {
		if ( pluginsToInstall == null || !pluginsToInstall.contains(plugin) ) {
			return;
		}
		if ( pluginsInstalled != null && !pluginsInstalled.contains(plugin) ) {
			try {
				pluginsInstalled.add(plugin);
			} catch ( UnsupportedOperationException e ) {
				// read-only list, so convert to writable one
				Set<Plugin> installed = new HashSet<Plugin>(pluginsToInstall.size());
				installed.add(plugin);
				pluginsInstalled = installed;
			}
		} else {
			pluginsInstalled = Collections.singleton(plugin);
		}
	}

	/**
	 * Mark a given Plugin from the {@link #getPluginsToInstall()} list as
	 * started.
	 * 
	 * @param plugin
	 *        the plugin to mark as started
	 */
	public void markPluginStarted(Plugin plugin) {
		if ( pluginsToInstall == null || !pluginsToInstall.contains(plugin) ) {
			return;
		}
		if ( pluginsStarted != null && !pluginsStarted.contains(plugin) ) {
			try {
				pluginsStarted.add(plugin);
			} catch ( UnsupportedOperationException e ) {
				// read-only list, so convert to writable one
				Set<Plugin> installed = new HashSet<Plugin>(pluginsToInstall.size());
				installed.add(plugin);
				pluginsStarted = installed;
			}
		} else {
			pluginsStarted = Collections.singleton(plugin);
		}
	}

	/**
	 * Mark a given Plugin from the {@link #getPluginsToRemove()} list as
	 * removed.
	 * 
	 * @param plugin
	 *        the plugin to mark as removed
	 */
	public void markPluginRemoved(Plugin plugin) {
		if ( pluginsToRemove == null || !pluginsToRemove.contains(plugin) ) {
			return;
		}
		if ( pluginsRemoved != null && !pluginsRemoved.contains(plugin) ) {
			try {
				pluginsInstalled.add(plugin);
			} catch ( UnsupportedOperationException e ) {
				// read-only list, so convert to writable one
				Set<Plugin> installed = new HashSet<Plugin>(pluginsToRemove.size());
				installed.add(plugin);
				pluginsRemoved = installed;
			}
		} else {
			pluginsRemoved = Collections.singleton(plugin);
		}
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
			steps += 2;
			progress += (float) ((double) pluginsInstalled.size() / (double) pluginsToInstall.size());
			progress += (float) ((double) pluginsStarted.size() / (double) pluginsToInstall.size());
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

	public Set<Plugin> getPluginsInstalled() {
		return pluginsInstalled;
	}

	public Set<Plugin> getPluginsRemoved() {
		return pluginsRemoved;
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
