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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.obr.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.BundlePlugin;
import net.solarnetwork.node.setup.Plugin;

/**
 * Task to install plugins.
 * 
 * @author matt
 * @version 2.1
 */
public class OBRProvisionTask implements Callable<OBRPluginProvisionStatus> {

	private static final Logger LOG = LoggerFactory.getLogger(OBRProvisionTask.class);

	private final BundleContext bundleContext;
	private final OBRPluginProvisionStatus status;
	private Future<OBRPluginProvisionStatus> future;
	private final File directory;
	private final BackupManager backupManager;
	private final SystemService systemService;

	/**
	 * Construct with a status.
	 * 
	 * @param bundleContext
	 *        the BundleContext to manipulate bundles with
	 * @param status
	 *        the status, which defines the plugins to install
	 * @param directory
	 *        the directory to download plugins to
	 * @param backupManager
	 *        if provided, then a backup will be performed before provisioning
	 *        any bundles
	 * @param systemService
	 *        if provided and
	 *        {@link OBRPluginProvisionStatus#isRestartRequired()} is
	 *        {@literal true} then perform a restart after the provision task
	 *        completes
	 */
	public OBRProvisionTask(BundleContext bundleContext, OBRPluginProvisionStatus status, File directory,
			BackupManager backupManager, SystemService systemService) {
		super();
		this.bundleContext = bundleContext;
		this.status = status;
		this.directory = directory;
		this.backupManager = backupManager;
		this.systemService = systemService;
		this.status.setBackupComplete(backupManager == null);
	}

	@Override
	public OBRPluginProvisionStatus call() throws Exception {
		try {
			status.setStatusMessage("Starting provisioning operation.");
			handleBackupBeforeProvisioningOperation();
			if ( status.getPluginsToInstall() != null && status.getPluginsToInstall().size() > 0 ) {
				downloadPlugins(status.getPluginsToInstall());
			}
			if ( status.getPluginsToRemove() != null && status.getPluginsToRemove().size() > 0 ) {
				removePlugins(status.getPluginsToRemove());
			}
			status.setStatusMessage("Provisioning operation complete.");
			return status;
		} catch ( Exception e ) {
			LOG.warn("Error in provision task: {}", e.getMessage(), e);
			status.setStatusMessage("Error in provisioning operation: " + e.getMessage());
			throw e;
		}
	}

	private void handleBackupBeforeProvisioningOperation() {
		// if we are actually going to provision something, let's make a backup
		if ( backupManager != null && status.getOverallProgress() < 1 ) {
			status.setStatusMessage("Creating backup before provisioning operation.");
			LOG.info("Creating backup before provisioning operation.");
			try {
				Backup backup = backupManager.createBackup();
				if ( backup != null ) {
					LOG.info("Created backup {} (size {})", backup.getKey(), backup.getSize());
					status.setStatusMessage("Backup complete.");
					status.setBackupComplete(Boolean.TRUE);
				}
			} catch ( RuntimeException e ) {
				status.setBackupComplete(Boolean.FALSE);
				LOG.warn("Error creating backup for provisioning operation {}", status.getProvisionID(),
						e);
			}
		}
	}

	/**
	 * Find all installed bundles for a specific ID that are less than or equal
	 * to a specific version.
	 * 
	 * @param symbolicName
	 *        The bundle ID to look for.
	 * @param maxVersion
	 *        The maximum version to include in the result.
	 * @return All found bundles whose symbolic name matches and has a version
	 *         less than {@code maxVersion}, in largest to smallest order, or
	 *         {@literal null} if none found.
	 */
	private List<Bundle> findBundlesOlderThanVersion(String symbolicName, Version maxVersion) {
		List<Bundle> olderBundles = null;
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle b : bundles ) {
			if ( b.getSymbolicName() != null && b.getSymbolicName().equals(symbolicName)
					&& b.getVersion().compareTo(maxVersion) < 1 ) {
				if ( olderBundles == null ) {
					olderBundles = new ArrayList<Bundle>(2);
				}
				olderBundles.add(b);
			}
		}
		if ( olderBundles != null ) {
			Collections.sort(olderBundles, new Comparator<Bundle>() {

				@Override
				public int compare(Bundle o1, Bundle o2) {
					return o2.getVersion().compareTo(o1.getVersion());
				}
			});
		}
		return olderBundles;
	}

	private void downloadPlugins(List<Plugin> plugins) throws InterruptedException {
		assert plugins != null;
		LOG.debug("Starting install of {} plugins", plugins.size());
		if ( !directory.exists() && !directory.mkdirs() ) {
			throw new RuntimeException("Unable to create plugin directory: " + directory.toString());
		}

		// This method will manually download the bundle for each resolved plugin, 
		// then install it and start it in the running OSGi platform. We don't
		// make use of the OBR RepositoryAdmin to do this because on SolarNode
		// the bundle's runtime area is held only in RAM (not persisted to disk)
		// but we want these downloaded bundles to be persisted to disk. Thus we
		// just do a bit of work here to download and start the bundles ourselves.

		List<Bundle> installedBundles = new ArrayList<Bundle>(plugins.size());

		// iterate backwards, to work our way up through deps to requested plugin
		for ( ListIterator<Plugin> itr = plugins.listIterator(plugins.size()); itr.hasPrevious(); ) {
			Plugin plugin = itr.previous();
			assert plugin instanceof OBRResourcePlugin;
			LOG.debug("Starting install of plugin: {}", plugin.getUID());
			status.setStatusMessage("Starting install of plugin " + plugin.getUID());

			OBRResourcePlugin obrPlugin = (OBRResourcePlugin) plugin;
			Resource resource = obrPlugin.getResource();
			URL resourceURL = resource.getURL();
			String pluginFileName = StringUtils.getFilename(resourceURL.getPath());
			File outputFile = new File(directory, pluginFileName);
			String bundleSymbolicName = resource.getSymbolicName();

			// download to tmp file first, then we'll rename
			File tmpOutputFile = new File(directory, "." + pluginFileName);
			LOG.debug("Downloading plugin {} => {}", resourceURL, tmpOutputFile);
			try {
				FileCopyUtils.copy(resourceURL.openStream(), new FileOutputStream(tmpOutputFile));
			} catch ( IOException e ) {
				throw new RuntimeException("Unable to download plugin " + bundleSymbolicName, e);
			}

			moveTemporaryDownloadedPluginFile(resource, outputFile, tmpOutputFile);

			installDownloadedPlugin(resource, outputFile, installedBundles);

			LOG.debug("Installed plugin: {}", plugin.getUID());
			status.markPluginInstalled(plugin);
		}
		if ( !installedBundles.isEmpty() ) {
			Set<Bundle> toRefresh = findFragmentHostsForBundles(installedBundles);
			toRefresh.addAll(installedBundles);
			status.setStatusMessage("Refreshing OSGi framework.");
			FrameworkWiring fw = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			fw.refreshBundles(toRefresh);

			for ( ListIterator<Bundle> itr = installedBundles.listIterator(); itr.hasNext(); ) {
				Bundle b = itr.next();
				boolean fragment = isFragment(b);
				status.setStatusMessage("Starting plugin: " + b.getSymbolicName());
				try {
					if ( !fragment
							&& !(b.getState() == Bundle.ACTIVE || b.getState() == Bundle.STARTING) ) {
						b.start();
					}
					// bundles are in reverse order of plugins
					Plugin p = plugins.get(plugins.size() - itr.nextIndex());
					status.markPluginStarted(p);
				} catch ( BundleException e ) {
					throw new RuntimeException("Unable to start plugin " + b.getSymbolicName()
							+ " version " + b.getVersion(), e);
				}
			}
		}
		if ( status.isRestartRequired() && systemService == null ) {
			LOG.debug("Install of {} plugins complete; manual restart required", plugins.size());
			status.setStatusMessage(
					"Install of " + plugins.size() + " plugins complete; manual restart required");
		} else if ( status.isRestartRequired() ) {
			LOG.debug("Install of {} plugins complete; restarting now", plugins.size());
			status.setStatusMessage(
					"Install of " + plugins.size() + " plugins complete; restarting now");
			performRestart();
		} else {
			LOG.debug("Install of {} plugins complete", plugins.size());
			status.setStatusMessage("Install of " + plugins.size() + " plugins complete");
		}
	}

	private void performRestart() {
		if ( systemService == null ) {
			return;
		}
		// restart after a delay, to give time for status query
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch ( InterruptedException e ) {
					// ignore
				}
				systemService.exit(true);
			}
		}).start();
	}

	private boolean isFragment(Bundle b) {
		BundleRevision r = b.adapt(BundleRevision.class);
		return (r != null && (r.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0);
	}

	private static final Pattern BUNDLE_VERSION_PATTERN = Pattern
			.compile("bundle-version\\s*=\\s*\"([^\"]+)", Pattern.CASE_INSENSITIVE);

	private Set<Bundle> findFragmentHostsForBundles(Collection<Bundle> toRefresh) {
		Set<Bundle> fragmentHosts = new HashSet<Bundle>();
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle b : toRefresh ) {
			if ( b.getState() == Bundle.UNINSTALLED ) {
				continue;
			}
			String hostHeader = b.getHeaders().get(Constants.FRAGMENT_HOST);
			if ( hostHeader == null ) {
				continue;
			}
			String[] clauses = StringUtils.delimitedListToStringArray(hostHeader, ";");
			if ( clauses == null || clauses.length < 1 ) {
				continue;
			}
			String hostSymbolicName = clauses[0];
			for ( Bundle hostBundle : bundles ) {
				if ( hostBundle.getSymbolicName() != null
						&& hostBundle.getSymbolicName().equals(hostSymbolicName) ) {
					VersionRange hostVersionRange = null;
					if ( clauses.length > 1 ) {
						for ( String clause : clauses ) {
							Matcher m = BUNDLE_VERSION_PATTERN.matcher(clause);
							if ( m.find() ) {
								String ver = m.group(1);
								try {
									hostVersionRange = new org.osgi.framework.VersionRange(ver);
								} catch ( IllegalArgumentException e ) {
									LOG.warn(
											"Ignoring fragment bundle {} version range syntax error: {}",
											hostSymbolicName, e.getMessage());
								}
								break;
							}
						}
					}
					if ( hostVersionRange == null
							|| hostVersionRange.includes(hostBundle.getVersion()) ) {
						LOG.debug("Found fragment {} host {} to refresh", b, hostBundle);
						fragmentHosts.add(hostBundle);
					}
					continue;
				}
			}
		}
		return fragmentHosts;
	}

	private void moveTemporaryDownloadedPluginFile(Resource resource, File outputFile,
			File tmpOutputFile) {
		if ( outputFile.exists() ) {
			// if the file has not changed, just delete tmp file
			InputStream outputFileInputStream = null;
			InputStream tmpOutputFileInputStream = null;
			try {
				outputFileInputStream = new FileInputStream(outputFile);
				tmpOutputFileInputStream = new FileInputStream(tmpOutputFile);
				String outputFileHash = DigestUtils.sha1Hex(outputFileInputStream);
				String tmpOutputFileHash = DigestUtils.sha1Hex(tmpOutputFileInputStream);
				if ( tmpOutputFileHash.equals(outputFileHash) ) {
					// file unchanged, so just delete tmp file
					tmpOutputFile.delete();
				} else {
					LOG.debug("Bundle {} version {} content updated", resource.getSymbolicName(),
							resource.getVersion());
					outputFile.delete();
					tmpOutputFile.renameTo(outputFile);
				}
			} catch ( IOException e ) {
				throw new RuntimeException("Error downloading plugin " + resource.getSymbolicName(), e);
			} finally {
				if ( outputFileInputStream != null ) {
					try {
						outputFileInputStream.close();
					} catch ( IOException e ) {
						// ignore;
					}
				}
				if ( tmpOutputFileInputStream != null ) {
					try {
						tmpOutputFileInputStream.close();
					} catch ( IOException e ) {
						// ignore
					}
				}
			}
		} else {
			// rename tmp file
			tmpOutputFile.renameTo(outputFile);
		}
	}

	private boolean installDownloadedPlugin(Resource resource, File outputFile,
			List<Bundle> installedBundles) {
		final String bundleSymbolicName = resource.getSymbolicName();
		boolean refreshNeeded = false;
		try {
			URL newBundleURL = outputFile.toURI().toURL();
			List<Bundle> oldBundles = findBundlesOlderThanVersion(bundleSymbolicName,
					resource.getVersion());
			Bundle oldBundle = (oldBundles != null && oldBundles.size() > 0 ? oldBundles.get(0) : null);
			Version oldVersion = (oldBundle != null ? oldBundle.getVersion() : null);
			if ( oldVersion != null && oldVersion.compareTo(resource.getVersion()) >= 0 ) {
				LOG.debug("Skipping install of plugin {} as version is unchanged at {}",
						bundleSymbolicName, oldVersion);
			} else if ( oldVersion != null ) {
				InputStream in = null;
				try {
					// only update bundles in runtime if restartRequired flag not set
					if ( !status.isRestartRequired() ) {
						LOG.debug("Upgrading plugin {} from {} to {}", bundleSymbolicName, oldVersion,
								resource.getVersion());
						in = new BufferedInputStream(new FileInputStream(outputFile));
						oldBundle.update(in);
					}

					// try to delete the old version
					File oldJar = new File(directory, bundleSymbolicName + "-" + oldVersion + ".jar");
					if ( !oldJar.delete() ) {
						LOG.warn("Error deleting old plugin " + oldJar.getName());
					}

					if ( status.isRestartRequired() ) {
						LOG.debug("Upgraded plugin {} {} will be available after restart",
								bundleSymbolicName, resource.getVersion());
					} else {
						installedBundles.add(oldBundle);
						LOG.info("Upgraded plugin {} from version {} to {}", bundleSymbolicName,
								oldVersion, resource.getVersion());
						refreshNeeded = true;
					}
				} catch ( BundleException e ) {
					throw new RuntimeException("Unable to upgrade plugin " + bundleSymbolicName, e);
				} catch ( FileNotFoundException e ) {
					throw new RuntimeException("Unable to upgrade plugin " + bundleSymbolicName, e);
				} finally {
					if ( in != null ) {
						try {
							in.close();
						} catch ( IOException e ) {
							// ignore
						}
					}
				}
			} else {
				if ( status.isRestartRequired() ) {
					LOG.debug("Downloaded plugin {} version {} will be installed after restart",
							newBundleURL, resource.getVersion());
				} else {
					LOG.debug("Installing plugin {} version {}", newBundleURL, resource.getVersion());
					Bundle newBundle = bundleContext.installBundle(newBundleURL.toString());
					LOG.info("Installed plugin {} version {}", newBundle.getSymbolicName(),
							newBundle.getVersion());
					installedBundles.add(newBundle);
				}
			}
		} catch ( BundleException e ) {
			throw new RuntimeException("Unable to install plugin " + bundleSymbolicName, e);
		} catch ( MalformedURLException e ) {
			throw new RuntimeException("Unable to install plugin " + bundleSymbolicName, e);
		}
		return refreshNeeded;
	}

	private void removePlugins(List<Plugin> plugins) {
		assert plugins != null;
		LOG.debug("Starting removal of {} plugins", plugins.size());

		final boolean restartRequired = status.isRestartRequired();
		boolean refreshNeeded = false;
		for ( Plugin plugin : plugins ) {
			assert plugin instanceof BundlePlugin;
			LOG.debug("Starting removal of plugin: {}", plugin.getUID());
			status.setStatusMessage("Starting removal of plugin " + plugin.getUID());
			BundlePlugin bundlePlugin = (BundlePlugin) plugin;
			Bundle oldBundle = bundlePlugin.getBundle();
			if ( oldBundle != null ) {
				Version oldVersion = oldBundle.getVersion();
				if ( !restartRequired ) {
					LOG.debug("Removing plugin {} version {}", oldBundle.getSymbolicName(), oldVersion);
					try {
						oldBundle.uninstall();
						refreshNeeded = true;
					} catch ( BundleException e ) {
						throw new RuntimeException(
								"Unable to uninstall plugin " + oldBundle.getSymbolicName(), e);
					}
				}
				File oldJar = new File(directory,
						oldBundle.getSymbolicName() + "-" + oldVersion + ".jar");
				if ( !oldJar.delete() ) {
					LOG.warn("Error deleting plugin JAR " + oldJar.getName());
				}
			}

			LOG.debug("Removed plugin: {}", plugin.getUID());
			status.setStatusMessage("Removed plugin " + plugin.getUID());
			status.markPluginRemoved(plugin);
		}
		if ( refreshNeeded ) {
			status.setStatusMessage("Refreshing OSGi framework.");
			FrameworkWiring fw = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			fw.refreshBundles(null);
		}
		LOG.debug("Removal of {} plugins complete", plugins.size());
		if ( restartRequired ) {
			performRestart();
		}
	}

	/**
	 * Get the status.
	 * 
	 * @return the status
	 */
	public OBRPluginProvisionStatus getStatus() {
		return status;
	}

	/**
	 * Get the status future.
	 * 
	 * @return the future
	 */
	Future<OBRPluginProvisionStatus> getFuture() {
		return future;
	}

	/**
	 * Set the status future.
	 * 
	 * @param future
	 *        the future to set
	 */
	void setFuture(Future<OBRPluginProvisionStatus> future) {
		this.future = future;
	}

	/**
	 * Get the directory.
	 * 
	 * @return the directory
	 */
	public File getDirectory() {
		return directory;
	}

}
