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

import static net.solarnetwork.service.OptionalService.service;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.regex.Pattern;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.BundlePlugin;
import net.solarnetwork.node.setup.LocalizedPlugin;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginProvisionException;
import net.solarnetwork.node.setup.PluginProvisionStatus;
import net.solarnetwork.node.setup.PluginQuery;
import net.solarnetwork.node.setup.PluginService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.SearchFilter.CompareOperator;
import net.solarnetwork.util.SearchFilter.LogicOperator;
import net.solarnetwork.util.StringUtils;

/**
 * OBR implementation of {@link PluginService}, using the Apache Felix OBR
 * implementation.
 * 
 * @author matt
 * @version 2.2
 */
public class OBRPluginService implements PluginService, SettingSpecifierProvider {

	private static final String FRAGMENT_CAPABILITY = "fragment";

	private static final String PREVIEW_PROVISION_ID = "preview";

	/** The {@code restrictingSymbolicNameFilters} property default value. */
	public static final String[] DEFAULT_RESTRICTING_SYMBOLIC_NAME_FILTER = { "net.solarnetwork.node" };

	private static final String[] DEFAULT_EXCLUSION_SYMBOLIC_NAME_FILTERS = { ".mock", ".test",
			"net.solarnetwork.node.dao.", "net.solarnetwork.node.hw." };

	private static final String[] DEFAULT_CORE_FEATURE_EXPRESSIONS = { "net\\.solarnetwork\\.node",
			"net\\.solarnetwork\\.node\\.dao(?:\\..*)*", "net\\.solarnetwork\\.node\\.setup(?:\\..*)*",
			"net\\.solarnetwork\\.node\\.settings(?:\\..*)*" };

	private final BundleContext bundleContext;
	private RepositoryAdmin repositoryAdmin;
	private List<OBRRepository> repositories;
	private String downloadPath = "app/main";
	private String[] restrictingSymbolicNameFilters = DEFAULT_RESTRICTING_SYMBOLIC_NAME_FILTER;
	private String[] exclusionSymbolicNameFilters = DEFAULT_EXCLUSION_SYMBOLIC_NAME_FILTERS;
	private Pattern[] coreFeatureSymbolicNamePatterns = StringUtils
			.patterns(DEFAULT_CORE_FEATURE_EXPRESSIONS, 0);
	private OptionalService<BackupManager> backupManager;
	private OptionalService<SystemService> systemService;
	private long provisionTaskStatusMinimumKeepSeconds = 60L * 10L; // 10min

	private MessageSource messageSource;

	private TaskCleaner cleanerTask;

	private final ConcurrentMap<URL, OBRRepositoryStatus> repoStatusMap = new ConcurrentHashMap<URL, OBRRepositoryStatus>(
			4);
	private final ConcurrentMap<String, OBRProvisionTask> provisionTaskMap = new ConcurrentHashMap<String, OBRProvisionTask>(
			4);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private class TaskCleaner implements Runnable {

		@Override
		public void run() {
			Iterator<OBRProvisionTask> itr;
			final long now = System.currentTimeMillis();
			final long min = provisionTaskStatusMinimumKeepSeconds * 1000L;
			for ( itr = provisionTaskMap.values().iterator(); itr.hasNext(); ) {
				OBRProvisionTask task = itr.next();
				if ( task.getFuture().isDone() && (now - task.getStatus().getCreationDate()) > min ) {
					log.debug("Cleaning out old provision task status {}",
							task.getStatus().getProvisionID());
					itr.remove();
				}
			}

		}
	}

	/**
	 * Constructor.
	 * 
	 * @param bundleContext
	 *        the bundle context
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OBRPluginService(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	@Override
	protected void finalize() throws Throwable {
		// just in case... clean up our timers
		destroy();
		super.finalize();
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

		// we need to know if we should includes a (normally) excluded plugin that is upgradable
		final Map<String, Bundle> installedBundles = installedBundles();

		final List<Plugin> plugins = new ArrayList<Plugin>(resources.length);
		RLOOP: for ( Resource r : resources ) {
			final String uid = r.getSymbolicName();
			if ( uid == null || uid.isEmpty() ) {
				continue;
			}
			if ( exclusionSymbolicNameFilters != null && exclusionSymbolicNameFilters.length > 0 ) {
				for ( String exclude : exclusionSymbolicNameFilters ) {
					if ( uid.contains(exclude) ) {
						// this plugin is normally excluded... but is it actually installed, and upgradable?
						Bundle installed = installedBundles.get(uid);
						if ( installed == null
								|| r.getVersion().compareTo(installed.getVersion()) < 1 ) {
							// it's not installed, or the installed version is up to date, so exclude it
							continue RLOOP;
						}
					}
				}
			}
			Plugin p = new OBRResourcePlugin(r,
					(StringUtils.matches(coreFeatureSymbolicNamePatterns, uid) != null));
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

	private Map<String, Bundle> installedBundles() {
		Bundle[] bundles = bundleContext.getBundles();
		if ( bundles == null || bundles.length < 1 ) {
			return Collections.emptyMap();
		}
		Map<String, Bundle> installedBundles = new HashMap<String, Bundle>(bundles.length);
		for ( Bundle b : bundles ) {
			String uid = b.getSymbolicName();
			if ( uid == null || uid.isEmpty() ) {
				continue;
			}
			if ( restrictingSymbolicNameFilters != null && restrictingSymbolicNameFilters.length > 0 ) {
				boolean allowed = false;
				for ( String filter : restrictingSymbolicNameFilters ) {
					if ( uid.startsWith(filter) ) {
						allowed = true;
						break;
					}
				}
				if ( !allowed ) {
					continue;
				}
			}
			installedBundles.put(uid, b);
		}
		return installedBundles;
	}

	@Override
	public List<Plugin> installedPlugins(Locale locale) {
		if ( bundleContext == null ) {
			return Collections.emptyList();
		}
		Map<String, Bundle> installedBundles = installedBundles();
		List<Plugin> results = new ArrayList<Plugin>(installedBundles.size());
		for ( Bundle b : installedBundles.values() ) {
			Plugin p = new BundlePlugin(b,
					(StringUtils.matches(coreFeatureSymbolicNamePatterns, b.getSymbolicName()) != null));
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
		if ( restrictingSymbolicNameFilters != null && restrictingSymbolicNameFilters.length > 0 ) {
			Map<String, Object> restrictions = new LinkedHashMap<String, Object>(
					restrictingSymbolicNameFilters.length);
			for ( String oneFilter : restrictingSymbolicNameFilters ) {
				SearchFilter restrict = new SearchFilter(Resource.SYMBOLIC_NAME, oneFilter,
						CompareOperator.SUBSTRING_AT_START);
				restrictions.put(oneFilter, restrict);
			}
			if ( restrictions.size() > 1 ) {
				filter.put("restrict", new SearchFilter(restrictions, LogicOperator.OR));
			} else {
				filter.putAll(restrictions);
			}
		}
		String result = new SearchFilter(filter, LogicOperator.AND).asLDAPSearchFilterString();
		if ( result == null || result.length() < 1 ) {
			return null;
		}
		return result;
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
				Plugin p = new BundlePlugin(b, (StringUtils.matches(coreFeatureSymbolicNamePatterns,
						b.getSymbolicName()) != null));
				pluginsToRemove.add(p);
			}
		}
		OBRPluginProvisionStatus status = new OBRPluginProvisionStatus(generateProvisionID());
		status.setPluginsToRemove(pluginsToRemove);
		status.setRestartRequired(true);
		OBRProvisionTask task = new OBRProvisionTask(bundleContext, status, new File(downloadPath),
				service(backupManager), service(systemService));
		saveProvisionTask(task);

		Future<OBRPluginProvisionStatus> future = executorService.submit(task);
		task.setFuture(future);

		startCleanerTaskIfNeeded();

		return new OBRPluginProvisionStatus(status);
	}

	@Override
	public synchronized PluginProvisionStatus installPlugins(Collection<String> uids, Locale locale) {
		OBRPluginProvisionStatus status = resolveInstall(uids, locale, generateProvisionID());
		OBRProvisionTask task = new OBRProvisionTask(bundleContext, status, new File(downloadPath),
				(backupManager != null ? backupManager.service() : null),
				(systemService != null ? systemService.service() : null));
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
					buf.insert(0,
							messageSource.getMessage("resolve.failed.unsatisfied.requierment.intro",
									null, "The following requirement is not satisfied: ", locale));
				} else {
					buf.insert(0,
							messageSource.getMessage("resolve.failed.unsatisfied.requierments.intro",
									null, "The following requirements are not satisfied: ", locale));
				}
			} else {
				buf.append("Unknown error");
			}
			throw new PluginProvisionException(buf.toString());
		}

		// require a restart if we're upgrading anything, or installing any core feature,
		// or installing a fragment
		Set<String> installed = installedBundles().keySet();
		boolean requireRestart = false;

		Resource[] requiredResources = resolver.getRequiredResources();

		// combine requested resources with required resources
		Resource[] installResources = new Resource[resources.length + requiredResources.length];
		System.arraycopy(resources, 0, installResources, 0, resources.length);
		System.arraycopy(requiredResources, 0, installResources, resources.length,
				requiredResources.length);

		List<Plugin> toInstall = new ArrayList<Plugin>(installResources.length);
		for ( Resource r : installResources ) {
			boolean coreFeature = StringUtils.matches(coreFeatureSymbolicNamePatterns,
					r.getSymbolicName()) != null;
			if ( !requireRestart && r.getCapabilities() != null ) {
				// look for a fragment capability, which will force a restart
				for ( Capability capibility : r.getCapabilities() ) {
					if ( FRAGMENT_CAPABILITY.equals(capibility.getName()) ) {
						requireRestart = true;
						break;
					}
				}
			}
			toInstall.add(new OBRResourcePlugin(r, coreFeature));
			if ( !requireRestart && (coreFeature || installed.contains(r.getSymbolicName())) ) {
				requireRestart = true;
			}
		}
		OBRPluginProvisionStatus result = new OBRPluginProvisionStatus(provisionID);
		result.setPluginsToInstall(toInstall);
		result.setRestartRequired(requireRestart);
		return result;
	}

	/**
	 * Call when an {@link OBRRepository} becomes available.
	 * 
	 * @param repository
	 *        the repository
	 */
	public void onBind(OBRRepository repository) {
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				configureOBRRepository(repository);
			}
		});
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
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				for ( Repository repo : repositoryAdmin.listRepositories() ) {
					URL repoURL = repo.getURL();
					if ( repoURL != null && repoURL.equals(repository.getURL()) ) {
						repositoryAdmin.removeRepository(repoURL);
						repoStatusMap.remove(repoURL);
						return;
					}
				}
			}
		});
	}

	// Settings

	@Override
	public String getSettingUid() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "OBR Plugin Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(new BasicTextFieldSettingSpecifier("restrictingSymbolicNameFilter",
				StringUtils.commaDelimitedStringFromCollection(
						Arrays.asList(DEFAULT_RESTRICTING_SYMBOLIC_NAME_FILTER))));
		return result;
	}

	// Accessors

	/**
	 * Set the {@link RepositoryAdmin} to manage all OBR actions with.
	 * 
	 * @param repositoryAdmin
	 *        the admin to use
	 */
	public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
		this.repositoryAdmin = repositoryAdmin;
	}

	/**
	 * Set the collection of {@link OBRRepository} instances managed by
	 * SolarNode.
	 * 
	 * <p>
	 * For each configured {@link OBRRepository} this service will register an
	 * associated {@code org.osgi.service.obr.Repository} instance with the
	 * {@code repositoryAdmin} service.
	 * </p>
	 * 
	 * @param repositories
	 *        the repositories to use
	 */
	public void setRepositories(List<OBRRepository> repositories) {
		this.repositories = repositories;
	}

	/**
	 * Get the restricting symbol name filter.
	 * 
	 * @return the filter
	 */
	public String getRestrictingSymbolicNameFilter() {
		if ( this.restrictingSymbolicNameFilters == null ) {
			return null;
		}
		return StringUtils
				.commaDelimitedStringFromCollection(Arrays.asList(this.restrictingSymbolicNameFilters));
	}

	/**
	 * Set an optional comma-delimited list of filters to include when
	 * {@link #availablePlugins(PluginQuery, Locale)} or
	 * {@link #installedPlugins(Locale)} are called that restricts the results
	 * to those <em>starting with</em> this value.
	 * 
	 * <p>
	 * The idea here is to provide a way to focus the results on just a core
	 * subset of all plugins so the results are more relevant to users. The
	 * filters are <b>or-ed</b> together, so any filter that matches allows the
	 * matching bundle to be used.
	 * </p>
	 * 
	 * @param restrictingSymbolicNameFilter
	 *        the comma-delimited list of filters to use
	 */
	public void setRestrictingSymbolicNameFilter(String restrictingSymbolicNameFilter) {
		Set<String> set = StringUtils.commaDelimitedStringToSet(restrictingSymbolicNameFilter);
		this.restrictingSymbolicNameFilters = (set.size() > 0 ? set.toArray(new String[set.size()])
				: null);
	}

	/**
	 * An optional list of symbolic bundle name <em>substrings</em> to exclude
	 * from the results of {@link #availablePlugins(PluginQuery, Locale)}.
	 * 
	 * <p>
	 * The idea here is to hide low-level plugins that would automatically be
	 * included by user-facing plugins, making the results more relevant to
	 * users.
	 * </p>
	 * 
	 * @param exclusionSymbolicNameFilters
	 *        the list of filters to use
	 */
	public void setExclusionSymbolicNameFilters(String[] exclusionSymbolicNameFilters) {
		this.exclusionSymbolicNameFilters = exclusionSymbolicNameFilters;
	}

	/**
	 * Set the download path.
	 * 
	 * @param downloadPath
	 *        the path to set
	 */
	public void setDownloadPath(String downloadPath) {
		this.downloadPath = downloadPath;
	}

	/**
	 * set the minimum number of seconds to hold provision tasks in memory after
	 * the task has completed, to support the
	 * {@link #statusForProvisioningOperation(String, Locale)} method.
	 * 
	 * @param provisionTaskStatusMinimumKeepSeconds
	 *        the seconds to use; defaults to 10 minutes
	 */
	public void setProvisionTaskStatusMinimumKeepSeconds(long provisionTaskStatusMinimumKeepSeconds) {
		this.provisionTaskStatusMinimumKeepSeconds = provisionTaskStatusMinimumKeepSeconds;
	}

	/**
	 * Set an optional {@link BackupManager} service.
	 * 
	 * <p>
	 * If configured, then automatic backups will be initiated before any
	 * provisioning operation.
	 * </p>
	 * 
	 * @param backupManager
	 *        the manager to use
	 */
	public void setBackupManager(OptionalService<BackupManager> backupManager) {
		this.backupManager = backupManager;
	}

	/**
	 * Set the {@code coreFeatureSymbolicNamePatterns} property via string
	 * expressions. The expressions will be compiled into {@link Pattern}
	 * objects and thus must be valid expressions according to that class.
	 * 
	 * @param expressions
	 *        the expressions to use for {@code coreFeatureSymbolicNamePatterns}
	 */
	public void setCoreFeatureSymbolicNameExpressions(String[] expressions) {
		setCoreFeatureSymbolicNamePatterns(StringUtils.patterns(expressions, 0));
	}

	/**
	 * Get the {@code coreFeatureSymbolicNamePatterns} property as string
	 * values.
	 * 
	 * @return {@code coreFeatureSymbolicNamePatterns} as strings, or
	 *         {@literal null}
	 */
	public String[] getCoreFeatureSymbolicNameExpressions() {
		return StringUtils.expressions(coreFeatureSymbolicNamePatterns);
	}

	/**
	 * Set a list of regular expressions to use to determine if a plugin is a
	 * "core feature" or not. The expressions will be matched against bundle
	 * symbolic names; if a match is found the plugin will have its
	 * {@link Plugin#isCoreFeature()} flag set to {@literal true}.
	 * 
	 * @param coreFeatureSymbolicNamePatterns
	 *        patterns to match against bundle symbolic names
	 */
	public void setCoreFeatureSymbolicNamePatterns(Pattern[] coreFeatureSymbolicNamePatterns) {
		this.coreFeatureSymbolicNamePatterns = coreFeatureSymbolicNamePatterns;
	}

	/**
	 * Set a message source for resolving status messages with.
	 * 
	 * @param messageSource
	 *        the msessage source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set a system service to use for restarting the platform after
	 * provisioning tasks that require a restart.
	 * 
	 * @param systemService
	 *        the service to set
	 * @since 1.1
	 */
	public void setSystemService(OptionalService<SystemService> systemService) {
		this.systemService = systemService;
	}

}
