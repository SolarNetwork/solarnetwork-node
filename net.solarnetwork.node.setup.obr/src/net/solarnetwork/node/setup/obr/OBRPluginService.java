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

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginService;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;

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
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class OBRPluginService implements PluginService {

	private RepositoryAdmin repositoryAdmin;
	private List<OBRRepository> repositories;

	private final Map<URL, OBRRepositoryStatus> statusMap = new ConcurrentHashMap<URL, OBRRepositoryStatus>(
			4);

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

	private OBRRepositoryStatus getOrCreateStatus(URL url) {
		synchronized ( statusMap ) {
			OBRRepositoryStatus status = statusMap.get(url);
			if ( status == null ) {
				status = new OBRRepositoryStatus();
				status.setRepositoryURL(url);
				statusMap.put(url, status);
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
	public List<Plugin> availablePlugins(String filter) {
		// TODO Auto-generated method stub
		return null;
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
				statusMap.remove(repoURL);
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

}
