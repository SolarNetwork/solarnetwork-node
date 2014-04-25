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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import net.solarnetwork.node.setup.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.obr.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Task to install plugins.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRProvisionTask implements Callable<OBRPluginProvisionStatus> {

	private static final Logger LOG = LoggerFactory.getLogger(OBRProvisionTask.class);

	private final BundleContext bundleContext;
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
	public OBRProvisionTask(BundleContext bundleContext, OBRPluginProvisionStatus status, File directory) {
		super();
		this.bundleContext = bundleContext;
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

	private Bundle findBundle(String symbolicName) {
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle b : bundles ) {
			if ( b.getSymbolicName().equals(symbolicName) ) {
				return b;
			}
		}
		return null;
	}

	private void downloadPlugins(List<Plugin> plugins) throws InterruptedException {
		assert plugins != null;
		LOG.debug("Starting install of {} plugins", plugins.size());
		if ( !directory.exists() && !directory.mkdirs() ) {
			throw new RuntimeException("Unable to create plugin directory: " + directory.toString());
		}

		boolean refreshNeeded = false;
		List<Bundle> installedBundles = new ArrayList<Bundle>(plugins.size());

		// iterate backwards, to work our way up through deps to requested plugin
		for ( ListIterator<Plugin> itr = plugins.listIterator(plugins.size()); itr.hasPrevious(); ) {
			Plugin plugin = itr.previous();
			assert plugin instanceof OBRResourcePlugin;
			LOG.debug("Starting install of plugin: {}", plugin.getUID());

			OBRResourcePlugin obrPlugin = (OBRResourcePlugin) plugin;
			Resource resource = obrPlugin.getResource();
			URL resourceURL = resource.getURL();
			String pluginFileName = StringUtils.getFilename(resourceURL.getPath());
			File outputFile = new File(directory, pluginFileName);
			String bundleSymbolicName = resource.getSymbolicName();
			Bundle oldBundle = findBundle(bundleSymbolicName);
			if ( oldBundle != null ) {
				try {
					oldBundle.uninstall();
					if ( !refreshNeeded ) {
						refreshNeeded = true;
					}
				} catch ( BundleException e ) {
					throw new RuntimeException("Unable to uninstall plugin " + bundleSymbolicName, e);
				}
			}
			LOG.debug("Downloading plugin {} => {}", resourceURL, outputFile);
			try {
				FileCopyUtils.copy(resourceURL.openStream(), new FileOutputStream(outputFile));
			} catch ( IOException e ) {
				throw new RuntimeException("Unable to download plugin " + bundleSymbolicName, e);
			}

			try {
				URL newBundleURL = outputFile.toURI().toURL();
				LOG.debug("Installing plugin {}", newBundleURL);
				Bundle newBundle = bundleContext.installBundle(newBundleURL.toString());
				LOG.info("Installed plugin {} version {}", newBundle.getSymbolicName(),
						newBundle.getVersion());
				installedBundles.add(newBundle);
			} catch ( BundleException e ) {
				throw new RuntimeException("Unable to install plugin " + bundleSymbolicName, e);
			} catch ( MalformedURLException e ) {
				throw new RuntimeException("Unable to install plugin " + bundleSymbolicName, e);
			}

			LOG.debug("Installed plugin: {}", plugin.getUID());
			status.markPluginInstalled(plugin);
		}
		for ( ListIterator<Bundle> itr = installedBundles.listIterator(); itr.hasNext(); ) {
			Bundle b = itr.next();
			try {
				b.start();
				// bundles are in reverse order of plugins
				Plugin p = plugins.get(plugins.size() - itr.nextIndex());
				status.markPluginStarted(p);
			} catch ( BundleException e ) {
				throw new RuntimeException("Unable to start plugin " + b.getSymbolicName() + " version "
						+ b.getVersion(), e);
			}
		}
		if ( refreshNeeded ) {
			FrameworkWiring fw = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			fw.refreshBundles(null);
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
