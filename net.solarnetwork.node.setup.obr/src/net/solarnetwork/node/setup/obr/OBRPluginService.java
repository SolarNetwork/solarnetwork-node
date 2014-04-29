/* ==================================================================
 * OBRPluginService.java - Apr 21, 2014 2:36:06 PM
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.node.setup.BundlePlugin;
import net.solarnetwork.node.setup.LocalizedPlugin;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginProvisionException;
import net.solarnetwork.node.setup.PluginProvisionStatus;
import net.solarnetwork.node.setup.PluginQuery;
import net.solarnetwork.node.setup.PluginService;
import net.solarnetwork.support.SearchFilter;
import net.solarnetwork.support.SearchFilter.CompareOperator;
import net.solarnetwork.support.SearchFilter.LogicOperator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OBR implementation of {@link PluginService}, using the Apache Felix OBR
 * implementation.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>repositoryAdmin</dt>
 * <dd>The {@link RepositoryAdmin} to manage all OBR actions with.</dd>
 * 
 * <dt>repositories</dt>
 * <dd>The collection of {@link OBRRepository} instances managed by SolarNode.
 * For each configured {@link OBRRepository} this service will register an
 * associated {@code org.osgi.service.obr.Repository} instance with the
 * {@code repositoryAdmin} service.</dd>
 * 
 * <dt>restrictingSymbolicNameFilter</dt>
 * <dd>An optional filter to include when
 * {@link #availablePlugins(PluginQuery, Locale)} is called, that restricts the
 * results to those <em>starting with</em> this value. The idea here is to
 * provide a way to focus the results on just a core subset of all plugins so
 * the results are more relevant to users.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class OBRPluginService implements PluginService {

	private static final String PREVIEW_PROVISION_ID = "preview";

	public static final String DEFAULT_RESTRICTING_SYMBOLIC_NAME_FILTER = "net.solarnetwork.node.";

	private static final String[] DEFAULT_EXCLUSION_SYMBOLIC_NAME_FILTERS = { "mock",
			"net.solarnetwork.node.dao." };

	private RepositoryAdmin repositoryAdmin;
	private BundleContext bundleContext;
	private List<OBRRepository> repositories;
	private String downloadPath = "app/main";
	private String restrictingSymbolicNameFilter = DEFAULT_RESTRICTING_SYMBOLIC_NAME_FILTER;
	private String[] exclusionSymbolicNameFilters = DEFAULT_EXCLUSION_SYMBOLIC_NAME_FILTERS;
	private TaskCleaner cleanerTask;
	private long provisionTaskStatusMinimumKeepSeconds = 60L * 10L; // 10min

	private final ConcurrentMap<URL, OBRRepositoryStatus> repoStatusMap = new ConcurrentHashMap<URL, OBRRepositoryStatus>(
			4);
	private final ConcurrentMap<String, OBRProvisionTask> provisionTaskMap = new ConcurrentHashMap<String, OBRProvisionTask>(
			4);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	// static, so we don't hang onto cyclic reference to self
	private class TaskCleaner implements Runnable {

		@Override
		public void run() {
			Iterator<OBRProvisionTask> itr;
			final long now = System.currentTimeMillis();
			final long min = provisionTaskStatusMinimumKeepSeconds * 1000L;
			for ( itr = provisionTaskMap.values().iterator(); itr.hasNext(); ) {
				OBRProvisionTask task = itr.next();
				if ( task.getFuture().isDone() && (now - task.getStatus().getCreationDate()) > min ) {
					log.debug("Cleaning out old provision task status {}", task.getStatus()
							.getProvisionID());
					itr.remove();
				}
			}

		}
	}

	@Override
	protected void finalize() throws Throwable {
		// just in case... clean up our timers
		destroy();
		super.finalize();
	}

	/**
	 * Call to initialize the service, after all properties have been
	 * configured.
	 */
	public void init() {
		if ( repositories != null && repositoryAdmin != null ) {
			for ( OBRRepository repo : repositories ) {
				configureOBRRepository(repo);
			}
		}
	}

	/**
	 * Call to destory this service, cleaning up any resources.
	 */
	public void destroy() {
		executorService.shutdownNow();
		scheduler.shutdownNow();
	}

	@Override
	public synchronized void refreshAvailablePlugins() {
		if ( repositoryAdmin == null ) {
			return;
		}
		// by examining the source, found that to "refresh" we really just
		// add the same URL again. To pick up changes applied to the repository URLs,
		// however, we'll just remove all currently configured URLs and then add
		// the currently configured ones.
		Repository[] repos = repositoryAdmin.listRepositories();
		if ( repos == null ) {
			return;
		}
		for ( Repository r : repos ) {
			try {
				repositoryAdmin.removeRepository(r.getURL());
			} catch ( Exception e ) {
				log.warn("Unable to refresh OBR repository {}", r.getURL());
			}
		}
		repoStatusMap.clear();
		if ( repositories != null ) {
			for ( OBRRepository r : repositories ) {
				configureOBRRepository(r);
			}
		}
	}

	private OBRRepositoryStatus getOrCreateStatus(URL url) {
		synchronized ( repoStatusMap ) {
			OBRRepositoryStatus status = repoStatusMap.get(url);
			if ( status == null ) {
				status = new OBRRepositoryStatus();
				status.setRepositoryURL(url);
				repoStatusMap.put(url, status);
			}
			return status;
		}
	}

	private synchronized void configureOBRRepository(OBRRepository repository) {
		if ( repository == null || repositoryAdmin == null ) {
			return;
		}
		Set<URL> configuredURLs = new HashSet<URL>();
		for ( Repository repo : repositoryAdmin.listRepositories() ) {
			configuredURLs.add(repo.getURL());
		}
		URL repoURL = repository.getURL();
		if ( repoURL != null && !configuredURLs.contains(repoURL) ) {
			try {
				log.info("Adding OBR plugin repository {}", repoURL);
				repositoryAdmin.addRepository(repoURL);
				OBRRepositoryStatus status = getOrCreateStatus(repoURL);
				status.setConfigured(true);
				status.setException(null);
			} catch ( Exception e ) {
				OBRRepositoryStatus status = getOrCreateStatus(repoURL);
				status.setConfigured(false);
				status.setException(e);
			}
		}
	}

	@Override
	public List<Plugin> availablePlugins(PluginQuery query, Locale locale) {
		if ( repositoryAdmin == null ) {
			return Collections.emptyList();
		}
		Resource[] resources = repositoryAdmin.discoverResources(getOBRFilter(query));
		if ( resources == null || resources.length < 1 ) {
			return Collections.emptyList();
		}

		if ( query.isLatestVersionOnly() ) {
			resources = getLatestVersions(resources);
		}

		List<Plugin> plugins = new ArrayList<Plugin>(resources.length);
		for ( Resource r : resources ) {
			Plugin p = new OBRResourcePlugin(r);
			if ( locale != null ) {
				p = new LocalizedPlugin(p, locale);
			}
			plugins.add(p);
		}

		// sort the results lexically by their names
		Collections.sort(plugins, new Comparator<Plugin>() {

			@Override
			public int compare(Plugin o1, Plugin o2) {
				return o1.getInfo().getName().compareToIgnoreCase(o2.getInfo().getName());
			}

		});

		return plugins;
	}

	@Override
	public List<Plugin> installedPlugins(Locale locale) {
		// we limit returned results to those that are also available via OBR
		if ( repositoryAdmin == null || bundleContext == null ) {
			return Collections.emptyList();
		}
		Resource[] resources = repositoryAdmin.discoverResources(getOBRFilter(null));
		if ( resources == null || resources.length < 1 ) {
			return Collections.emptyList();
		}
		Set<String> availableUIDs = new HashSet<String>(resources.length);
		for ( Resource r : resources ) {
			availableUIDs.add(r.getSymbolicName());
		}
		Map<String, Bundle> installedBundles = new HashMap<String, Bundle>(availableUIDs.size());
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle b : bundles ) {
			String uid = b.getSymbolicName();
			if ( availableUIDs.contains(uid) ) {
				Bundle installedBundle = installedBundles.get(uid);
				if ( installedBundle == null
						|| b.getVersion().compareTo(installedBundle.getVersion()) > 0 ) {
					installedBundles.put(uid, b);
				}
			}
		}
		List<Plugin> results = new ArrayList<Plugin>(installedBundles.size());
		for ( Bundle b : installedBundles.values() ) {
			Plugin p = new BundlePlugin(b);
			if ( locale != null ) {
				p = new LocalizedPlugin(p, locale);
			}
			results.add(p);
		}
		return results;
	}

	private String getOBRFilter(final PluginQuery query) {
		Map<String, Object> filter = new LinkedHashMap<String, Object>(4);
		if ( query != null && query.getSimpleQuery() != null && query.getSimpleQuery().length() > 0 ) {
			SearchFilter id = new SearchFilter(Resource.SYMBOLIC_NAME, query.getSimpleQuery(),
					CompareOperator.SUBSTRING);
			filter.put("id", id);
		}
		if ( restrictingSymbolicNameFilter != null ) {
			SearchFilter restrict = new SearchFilter(Resource.SYMBOLIC_NAME,
					restrictingSymbolicNameFilter, CompareOperator.SUBSTRING_AT_START);
			filter.put("restrict", restrict);
		}
		if ( exclusionSymbolicNameFilters != null && exclusionSymbolicNameFilters.length > 0 ) {
			Map<String, Object> exMap = new LinkedHashMap<String, Object>(
					exclusionSymbolicNameFilters.length);
			for ( String ex : exclusionSymbolicNameFilters ) {
				exMap.put(ex, new SearchFilter(Resource.SYMBOLIC_NAME, ex, CompareOperator.SUBSTRING));
			}
			SearchFilter exOrs = new SearchFilter(exMap, LogicOperator.OR);
			filter.put("exclusion", new SearchFilter(Collections.singletonMap("exors", exOrs),
					LogicOperator.NOT));
		}
		return new SearchFilter(filter, LogicOperator.AND).asLDAPSearchFilterString();
	}

	private String generateProvisionID() {
		return UUID.randomUUID().toString();
	}

	private void saveProvisionTask(OBRProvisionTask task) {
		provisionTaskMap.put(task.getStatus().getProvisionID(), task);
	}

	@Override
	public PluginProvisionStatus removePlugins(Collection<String> uids, Locale locale) {
		List<Plugin> pluginsToRemove = new ArrayList<Plugin>(uids.size());
		Bundle[] bundles = bundleContext.getBundles();
		for ( Bundle b : bundles ) {
			String uid = b.getSymbolicName();
			if ( uids.contains(uid) ) {
				Plugin p = new BundlePlugin(b);
				pluginsToRemove.add(p);
			}
		}
		OBRPluginProvisionStatus status = new OBRPluginProvisionStatus(generateProvisionID());
		status.setPluginsToRemove(pluginsToRemove);
		OBRProvisionTask task = new OBRProvisionTask(bundleContext, status, new File(downloadPath));
		saveProvisionTask(task);
		Future<OBRPluginProvisionStatus> future = executorService.submit(task);
		task.setFuture(future);

		startCleanerTaskIfNeeded();

		return new OBRPluginProvisionStatus(status);
	}

	@Override
	public synchronized PluginProvisionStatus installPlugins(Collection<String> uids, Locale locale) {
		OBRPluginProvisionStatus status = resolveInstall(uids, locale, generateProvisionID());
		OBRProvisionTask task = new OBRProvisionTask(bundleContext, status, new File(downloadPath));
		saveProvisionTask(task);
		Future<OBRPluginProvisionStatus> future = executorService.submit(task);
		task.setFuture(future);

		startCleanerTaskIfNeeded();

		// return a copy, like a snapshot, so we don't deal with threading
		return new OBRPluginProvisionStatus(status);
	}

	private void startCleanerTaskIfNeeded() {
		if ( cleanerTask == null ) {
			cleanerTask = new TaskCleaner();
			log.debug("Scheduling TaskCleaner thread at fixed delay {} seconds",
					provisionTaskStatusMinimumKeepSeconds);
			scheduler.scheduleWithFixedDelay(cleanerTask, provisionTaskStatusMinimumKeepSeconds,
					provisionTaskStatusMinimumKeepSeconds, TimeUnit.SECONDS);
		}
	}

	@Override
	public PluginProvisionStatus statusForProvisioningOperation(String provisionID, Locale locale) {
		OBRProvisionTask task = provisionTaskMap.get(provisionID);
		return (task == null ? null : new OBRPluginProvisionStatus(task.getStatus()));
	}

	private SearchFilter filterForPluginUIDs(Collection<String> uids) {
		assert uids != null;
		Map<String, Object> f = new LinkedHashMap<String, Object>(uids.size());
		for ( String uid : uids ) {
			f.put(uid, new SearchFilter(Resource.SYMBOLIC_NAME, uid, CompareOperator.EQUAL));
		}
		return new SearchFilter(f, LogicOperator.OR);
	}

	@Override
	public PluginProvisionStatus previewInstallPlugins(Collection<String> uids, Locale locale) {
		return resolveInstall(uids, locale, PREVIEW_PROVISION_ID);
	}

	private Resource[] getLatestVersions(Resource[] resources) {
		Map<String, Resource> latestVersions = new LinkedHashMap<String, Resource>(resources.length);
		for ( Resource r : resources ) {
			Resource seenResource = latestVersions.get(r.getSymbolicName());
			Version seenVersion = (seenResource == null ? null : seenResource.getVersion());
			if ( seenVersion != null && seenVersion.compareTo(r.getVersion()) < 0 ) {
				// newer version... so remove older one from results
				latestVersions.remove(r.getSymbolicName());
			} else if ( seenVersion != null ) {
				// skip older version
				continue;
			}
			latestVersions.put(r.getSymbolicName(), r);
		}
		return latestVersions.values().toArray(new Resource[latestVersions.size()]);
	}

	private OBRPluginProvisionStatus resolveInstall(Collection<String> uids, Locale locale,
			String provisionID) {
		if ( uids == null || uids.size() < 1 || repositoryAdmin == null ) {
			return new OBRPluginProvisionStatus(PREVIEW_PROVISION_ID);
		}

		// get a list of the Resources we want to install
		SearchFilter filter = filterForPluginUIDs(uids);
		Resource[] resources = repositoryAdmin.discoverResources(filter.asLDAPSearchFilterString());
		if ( resources == null || resources.length < 1 ) {
			return new OBRPluginProvisionStatus(PREVIEW_PROVISION_ID);
		}

		// filter out duplicate, older versions
		resources = getLatestVersions(resources);

		// resolve the complete list of resources we need
		Resolver resolver = repositoryAdmin.resolver();
		for ( Resource r : resources ) {
			resolver.add(r);
		}
		final boolean success = resolver.resolve();
		if ( !success ) {
			StringBuilder buf = new StringBuilder();
			Requirement[] failures = resolver.getUnsatisfiedRequirements();
			// TODO: l10n
			if ( failures != null && failures.length > 0 ) {
				for ( Requirement r : failures ) {
					if ( buf.length() > 0 ) {
						buf.append(", ");
					}
					buf.append(r.getName());
					if ( r.getComment() != null && r.getComment().length() > 0 ) {
						buf.append(" (").append(r.getComment()).append(")");
					}
				}
				if ( failures.length == 1 ) {
					buf.insert(0, "The following requirement is not satisfied: ");
				} else {
					buf.insert(0, "The following requirements are not satisfied: ");
				}
			} else {
				buf.append("Unknown error");
			}
			throw new PluginProvisionException(buf.toString());
		}
		Resource[] requiredResources = resolver.getRequiredResources();
		List<Plugin> toInstall = new ArrayList<Plugin>(resources.length + requiredResources.length);
		for ( Resource r : resources ) {
			toInstall.add(new OBRResourcePlugin(r));
		}
		for ( Resource r : requiredResources ) {
			toInstall.add(new OBRResourcePlugin(r));
		}
		OBRPluginProvisionStatus result = new OBRPluginProvisionStatus(provisionID);
		result.setPluginsToInstall(toInstall);
		return result;
	}

	/**
	 * Call when an {@link OBRRepository} becomes available.
	 * 
	 * @param repository
	 *        the repository
	 */
	public void onBind(OBRRepository repository) {
		configureOBRRepository(repository);
	}

	/**
	 * Call when an {@link OBRRepository} is no longer available.
	 * 
	 * @param repository
	 *        the repository
	 */
	public void onUnbind(OBRRepository repository) {
		if ( repository == null || repository.getURL() == null || repositoryAdmin == null ) {
			return;
		}
		for ( Repository repo : repositoryAdmin.listRepositories() ) {
			URL repoURL = repo.getURL();
			if ( repoURL != null && repoURL.equals(repository.getURL()) ) {
				repositoryAdmin.removeRepository(repoURL);
				repoStatusMap.remove(repoURL);
				return;
			}
		}
	}

	public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
		this.repositoryAdmin = repositoryAdmin;
	}

	public void setRepositories(List<OBRRepository> repositories) {
		this.repositories = repositories;
	}

	public void setRestrictingSymbolicNameFilter(String restrictingSymbolicNameFilter) {
		this.restrictingSymbolicNameFilter = restrictingSymbolicNameFilter;
	}

	public void setExclusionSymbolicNameFilters(String[] exclusionSymbolicNameFilters) {
		this.exclusionSymbolicNameFilters = exclusionSymbolicNameFilters;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setDownloadPath(String downloadPath) {
		this.downloadPath = downloadPath;
	}

	public void setProvisionTaskStatusMinimumKeepSeconds(long provisionTaskStatusMinimumKeepSeconds) {
		this.provisionTaskStatusMinimumKeepSeconds = provisionTaskStatusMinimumKeepSeconds;
	}

}
