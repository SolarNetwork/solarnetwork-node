/* ==================================================================
 * DefaultBackupManager.java - Mar 27, 2013 9:17:24 AM
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

package net.solarnetwork.node.backup;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.service.DynamicServiceUnavailableException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.support.PrefixedMessageSource;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.util.UnionIterator;

/**
 * Default implementation of {@link BackupManager}.
 *
 * @author matt
 * @version 2.1
 */
public class DefaultBackupManager implements BackupManager {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Collection<BackupService> backupServices;
	private Collection<BackupResourceProvider> resourceProviders;
	private ExecutorService executorService = defaultExecutorService();
	private int backupRestoreDelaySeconds = 15;
	private String preferredBackupServiceKey = FileSystemBackupService.KEY;

	private static HierarchicalMessageSource getMessageSourceInstance() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBundleClassLoader(DefaultBackupManager.class.getClassLoader());
		source.setBasename(DefaultBackupManager.class.getName());
		return source;
	}

	private static ExecutorService defaultExecutorService() {
		// we want at most one backup happening at a time by default
		return new ThreadPoolExecutor(0, 1, 5, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(3, true));
	}

	/**
	 * Default constructor.
	 */
	public DefaultBackupManager() {
		super();
	}

	/**
	 * Initialize after all properties set.
	 */
	public void init() {
		// look for marked backup to restore
		scheduleRestore();
	}

	private void scheduleRestore() {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				boolean retry = true;
				try {
					// sleep for just a bit here
					Thread.sleep(backupRestoreDelaySeconds * 1000L);
				} catch ( InterruptedException e ) {
					return;
				}
				log.debug("Looking to see if there is a marked backup to restore");
				BackupService backupService = activeBackupService();
				if ( backupService != null ) {
					Map<String, String> props = new HashMap<String, String>();
					Backup backup = backupService.markedBackupForRestore(props);
					if ( backup != null ) {
						if ( restoreBackupInternal(backup, props) ) {
							// clear marked backup
							if ( backupService.markBackupForRestore(null, null) ) {
								retry = false;
								finishRestore(backup);
							}
						}
					} else {
						// no marked backup to restore
						retry = false;
					}
				}
				if ( retry ) {
					log.debug(
							"Will retry looking to see if there is a marked backup to restore in {} seconds",
							backupRestoreDelaySeconds);
					scheduleRestore();
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private void finishRestore(Backup backup) {
		log.info("Restore from backup {} complete", backup.getKey());
	}

	@Override
	public String getSettingUid() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "Backup Manager";
	}

	@Override
	public MessageSource getMessageSource() {
		HierarchicalMessageSource source = getMessageSourceInstance();
		Map<String, MessageSource> delegates = new HashMap<String, MessageSource>();
		for ( BackupService backupService : backupServices ) {
			delegates.put(backupService.getKey() + ".",
					backupService.getSettingSpecifierProvider().getMessageSource());
		}
		PrefixedMessageSource ps = new PrefixedMessageSource();
		ps.setDelegates(delegates);
		source.setParentMessageSource(ps);
		return source;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		BasicRadioGroupSettingSpecifier serviceSpec = new BasicRadioGroupSettingSpecifier(
				"preferredBackupServiceKey", FileSystemBackupService.KEY);
		Map<String, String> serviceSpecValues = new TreeMap<String, String>();
		for ( BackupService service : backupServices ) {
			serviceSpecValues.put(service.getKey(),
					service.getSettingSpecifierProvider().getDisplayName());
		}
		serviceSpec.setValueTitles(serviceSpecValues);
		results.add(serviceSpec);
		return results;
	}

	@Override
	public BackupService activeBackupService() {
		BackupService fallback = null;
		for ( BackupService service : backupServices ) {
			if ( preferredBackupServiceKey.equals(service.getKey()) ) {
				return service;
			}
			if ( FileSystemBackupService.KEY.equals(service.getKey()) ) {
				fallback = service;
			}
		}
		return fallback;
	}

	@Override
	public Iterable<BackupResource> resourcesForBackup() {
		BackupService service = activeBackupService();
		if ( service == null ) {
			log.debug("No BackupService available, can't find resources for backup");
			return Collections.emptyList();
		}
		if ( service.getInfo().getStatus() != BackupStatus.Configured ) {
			log.info("BackupService {} in {} state, can't find resources for backup", service.getKey(),
					service.getInfo().getStatus());
			return Collections.emptyList();
		}

		final List<Iterator<BackupResource>> resources = new ArrayList<Iterator<BackupResource>>(10);
		for ( BackupResourceProvider provider : resourceProviders ) {
			// map each resource into a sub directory
			Iterator<BackupResource> itr = provider.getBackupResources().iterator();
			resources.add(new PrefixedBackupResourceIterator(itr, provider.getKey()));

		}
		return new Iterable<BackupResource>() {

			@Override
			public Iterator<BackupResource> iterator() {
				return new UnionIterator<BackupResource>(resources);
			}

		};
	}

	@Override
	public Backup createBackup() {
		return createBackup(null);
	}

	@Override
	public Backup createBackup(final Map<String, String> props) {
		final BackupService service = activeBackupService();
		if ( service == null ) {
			log.info("No active backup service available, cannot perform backup");
			return null;
		}
		final BackupServiceInfo info = service.getInfo();
		final BackupStatus status = info.getStatus();
		if ( !(status == BackupStatus.Configured || status == BackupStatus.Error) ) {
			log.info("BackupService {} is in the {} state; cannot perform backup", service.getKey(),
					status);
			return null;
		}

		log.info("Initiating backup to service {}", service.getKey());
		final Backup backup = service.performBackup(resourcesForBackup());
		if ( backup != null ) {
			log.info("Backup {} {} with service {}", backup.getKey(),
					(backup.isComplete() ? "completed" : "initiated"), service.getKey());
		}
		return backup;
	}

	@Override
	public Future<Backup> createAsynchronousBackup() {
		return createAsynchronousBackup(null);
	}

	@Override
	public Future<Backup> createAsynchronousBackup(final Map<String, String> props) {
		assert executorService != null;
		return executorService.submit(new Callable<Backup>() {

			@Override
			public Backup call() throws Exception {
				return createBackup(props);
			}

		});
	}

	@Override
	public void exportBackupArchive(String backupKey, OutputStream out) throws IOException {
		exportBackupArchive(backupKey, out, null);
	}

	@Override
	public void exportBackupArchive(String backupKey, OutputStream out, Map<String, String> props)
			throws IOException {
		final BackupService service = activeBackupService();
		if ( service == null ) {
			return;
		}

		final Backup backup = service.backupForKey(backupKey);
		if ( backup == null ) {
			return;
		}

		// create the zip archive for the backup files
		ZipOutputStream zos = new ZipOutputStream(out);
		try {
			BackupResourceIterable resources = service.getBackupResources(backup);
			for ( BackupResource r : resources ) {
				zos.putNextEntry(new ZipEntry(r.getBackupPath()));
				FileCopyUtils.copy(r.getInputStream(), new FilterOutputStream(zos) {

					@Override
					public void close() throws IOException {
						// FileCopyUtils closed the stream, which we don't want here
					}

				});
			}
			resources.close();
		} finally {
			zos.flush();
			zos.finish();
			zos.close();
		}
	}

	@Override
	public Future<Backup> importBackupArchive(InputStream archive) throws IOException {
		return importBackupArchive(archive, null);
	}

	@Override
	public Future<Backup> importBackupArchive(InputStream archive, final Map<String, String> props)
			throws IOException {
		final BackupService service = activeBackupService();
		if ( service == null ) {
			throw new DynamicServiceUnavailableException(
					"No BackupService available to import backup with");
		}
		final ZipInputStream zin = new ZipInputStream(archive);
		return executorService.submit(new Callable<Backup>() {

			@Override
			public Backup call() throws Exception {
				final BackupResourceIterable itr = new ZipStreamBackupResourceIterable(zin, props);
				return service.importBackup(null, itr, props);
			}
		});
	}

	@Override
	public void restoreBackup(Backup backup) {
		restoreBackup(backup, null);
	}

	@Override
	public void restoreBackup(Backup backup, Map<String, String> props) {
		restoreBackupInternal(backup, props);
	}

	private boolean restoreBackupInternal(Backup backup, Map<String, String> props) {
		BackupService service = activeBackupService();
		if ( service == null ) {
			log.warn("No BackupService available to restore backup with");
			return false;
		}
		final Set<String> providerKeySet = (props == null ? null
				: StringUtils.commaDelimitedStringToSet(props.get(RESOURCE_PROVIDER_FILTER)));
		BackupResourceIterable resources = service.getBackupResources(backup);
		boolean result = true;
		try {
			for ( final BackupResource r : resources ) {
				// top-level dir is the  key of the provider
				final String path = r.getBackupPath();
				log.debug("Inspecting backup {} resource {}", backup.getKey(), path);
				final int providerIndex = path.indexOf('/');
				if ( providerIndex != -1 ) {
					final String providerKey = path.substring(0, providerIndex);
					if ( providerKeySet != null && !providerKeySet.isEmpty()
							&& !providerKeySet.contains(providerKey) ) {
						log.debug("Skipping backup {} resource {} (provider filtered)", backup.getKey(),
								path);
						continue;
					}
					boolean resourceHandled = false;
					for ( BackupResourceProvider provider : resourceProviders ) {
						if ( providerKey.equals(provider.getKey()) ) {
							log.debug("Restoring backup {} resource {}", backup.getKey(), path);
							resourceHandled = provider.restoreBackupResource(new BackupResource() {

								@Override
								public String getProviderKey() {
									return providerKey;
								}

								@Override
								public String getBackupPath() {
									return path.substring(providerIndex + 1);
								}

								@Override
								public InputStream getInputStream() throws IOException {
									return r.getInputStream();
								}

								@Override
								public long getModificationDate() {
									return r.getModificationDate();
								}

								@Override
								public String getSha256Digest() {
									return r.getSha256Digest();
								}

							});
							if ( resourceHandled ) {
								break;
							}
						}
					}
					if ( !resourceHandled ) {
						result = false;
						log.warn(
								"Backup {} resource {} could not be restored because no resource provider handled the resource.",
								backup.getKey(), path);
					}
				}
			}
		} catch ( RuntimeException e ) {
			log.error("Error restoring backup {}", backup.getKey(), e);
		} finally {
			try {
				resources.close();
			} catch ( IOException e ) {
				// ignore
			}
		}
		return result;
	}

	@Override
	public BackupInfo infoForBackup(final String key, final Locale locale) {
		BackupService service = activeBackupService();
		if ( service == null ) {
			log.debug("No BackupService available, can't find resources for backup");
			return null;
		}
		Backup backup = service.backupForKey(key);
		if ( backup == null ) {
			log.debug("No backup avaialble from service {} for key {}", service.getKey(), key);
			return null;
		}

		Map<String, BackupResourceProviderInfo> providerInfos = new LinkedHashMap<String, BackupResourceProviderInfo>();
		List<BackupResourceInfo> resourceInfos = new ArrayList<BackupResourceInfo>();

		BackupResourceIterable resources = null;
		try {
			resources = service.getBackupResources(backup);
			for ( BackupResource r : resources ) {
				final String providerKey = r.getProviderKey();
				BackupResourceProvider provider = providerForKey(providerKey);
				if ( provider == null ) {
					continue;
				}
				if ( !providerInfos.containsKey(providerKey) ) {
					providerInfos.put(providerKey, provider.providerInfo(locale));
				}
				BackupResourceInfo info = provider.resourceInfo(r, locale);
				if ( info != null ) {
					String name = info.getName();
					if ( name != null && name.startsWith(providerKey) ) {
						// strip provider key + '/' from name
						name = name.substring(providerKey.length() + 1);
					}
					resourceInfos
							.add(new SimpleBackupResourceInfo(providerKey, name, info.getDescription()));
				}
			}
		} finally {
			if ( resources != null ) {
				try {
					resources.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		}

		return new SimpleBackupInfo(key, backup.getDate(), providerInfos.values(), resourceInfos);
	}

	private BackupResourceProvider providerForKey(String key) {
		Collection<BackupResourceProvider> providers = resourceProviders;
		if ( providers == null || providers.isEmpty() ) {
			return null;
		}
		for ( BackupResourceProvider provider : providers ) {
			if ( key.equals(provider.getKey()) ) {
				return provider;
			}
		}
		return null;
	}

	/**
	 * Set a collection of {@link BackupService} instances to allow backing up
	 * and restoring with.
	 *
	 * @param backupServices
	 *        the backup services to use
	 */
	public void setBackupServices(Collection<BackupService> backupServices) {
		this.backupServices = backupServices;
	}

	/**
	 * Set a collection of {@link BackupResourceProvider} instances that provide
	 * the resources to be backed up.
	 *
	 * @param resourceProviders
	 *        the resource providers to backup resources from
	 */
	public void setResourceProviders(Collection<BackupResourceProvider> resourceProviders) {
		this.resourceProviders = resourceProviders;
	}

	/**
	 * Set the executor service to use for tasks.
	 *
	 * @param executorService
	 *        the service to use
	 */
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * Set a number of seconds to delay the attempt of restoring a backup, when
	 * a backup has been previously marked for restoration. This delay gives the
	 * platform time to boot up and register the backup resource providers and
	 * other services required to perform the restore.
	 *
	 * @param backupRestoreDelaySeconds
	 *        The number of seconds to delay attempting to restore from backup.
	 * @since 1.1
	 */
	public void setBackupRestoreDelaySeconds(int backupRestoreDelaySeconds) {
		this.backupRestoreDelaySeconds = backupRestoreDelaySeconds;
	}

	/**
	 * Set the key of the preferred backup service to use.
	 *
	 * @param preferredBackupServiceKey
	 *        the service key to set
	 * @since 1.4
	 */
	public void setPreferredBackupServiceKey(String preferredBackupServiceKey) {
		this.preferredBackupServiceKey = preferredBackupServiceKey;
	}

}
