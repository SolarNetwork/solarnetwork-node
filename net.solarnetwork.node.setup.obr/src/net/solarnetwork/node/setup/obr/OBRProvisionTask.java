/* ==================================================================
 * OBRProvisionTask.java - Apr 24, 2014 8:09:12 PM
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

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import net.solarnetwork.node.setup.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to install plugins.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRProvisionTask implements Callable<OBRPluginProvisionStatus> {

	private static final Logger LOG = LoggerFactory.getLogger(OBRProvisionTask.class);

	private final OBRPluginProvisionStatus status;
	private Future<OBRPluginProvisionStatus> future;
	private final File directory;

	/**
	 * Construct with a status.
	 * 
	 * @param status
	 *        the status, which defines the plugins to install
	 * @param directory
	 *        the directory to download plugins to
	 */
	public OBRProvisionTask(OBRPluginProvisionStatus status, File directory) {
		super();
		this.status = status;
		this.directory = directory;
	}

	@Override
	public OBRPluginProvisionStatus call() throws Exception {
		if ( status.getPluginsToInstall() != null && status.getPluginsToInstall().size() > 0 ) {
			downloadPlugins(status.getPluginsToInstall());
		}
		return status;
	}

	private void downloadPlugins(List<Plugin> plugins) throws InterruptedException {
		assert plugins != null;
		LOG.debug("Starting install of {} plugins", plugins.size());
		for ( Plugin plugin : plugins ) {
			LOG.debug("Starting install of plugin: {}", plugin.getUID());

			// TODO: download bundle, start it
			Thread.sleep(3000);

			LOG.debug("Installed plugin: {}", plugin.getUID());
			status.markPluginInstalled(plugin);
		}
		LOG.debug("Install of {} plugins complete", plugins.size());
	}

	public OBRPluginProvisionStatus getStatus() {
		return status;
	}

	Future<OBRPluginProvisionStatus> getFuture() {
		return future;
	}

	void setFuture(Future<OBRPluginProvisionStatus> future) {
		this.future = future;
	}

	public File getDirectory() {
		return directory;
	}

}
