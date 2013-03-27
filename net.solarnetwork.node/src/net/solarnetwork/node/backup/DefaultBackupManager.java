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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.util.DynamicServiceTracker;
import net.solarnetwork.util.UnionIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Default implementation of {@link BackupManager}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>backupServiceTracker</dt>
 * <dd>A tracker for the desired backup service to use.</dd>
 * 
 * <dt>resourceProviders</dt>
 * <dd>The collection of {@link BackupResourceProvider} instances that provide
 * the resources to be backed up.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultBackupManager implements BackupManager {

	private static final MessageSource MESSAGE_SOURCE = getMessageSourceInstance();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Collection<BackupService> backupServices;
	private DynamicServiceTracker<BackupService> backupServiceTracker;
	private Collection<BackupResourceProvider> resourceProviders;

	private static MessageSource getMessageSourceInstance() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBundleClassLoader(DefaultBackupManager.class.getClassLoader());
		source.setBasename(DefaultBackupManager.class.getName());
		return source;
	}

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "Backup Manager";
	}

	@Override
	public MessageSource getMessageSource() {
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		BasicRadioGroupSettingSpecifier serviceSpec = new BasicRadioGroupSettingSpecifier(
				"backupServiceTracker.propertyFilters['key']", FileSystemBackupService.KEY);
		Map<String, String> serviceSpecValues = new TreeMap<String, String>();
		for ( BackupService service : backupServices ) {
			serviceSpecValues.put(service.getKey(), service.getSettingSpecifierProvider()
					.getDisplayName());
		}
		serviceSpec.setValueTitles(serviceSpecValues);
		results.add(serviceSpec);
		return results;
	}

	@Override
	public BackupService activeBackupService() {
		return backupServiceTracker.service();
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
			resources.add(provider.getBackupResources());
		}
		return new Iterable<BackupResource>() {

			@Override
			public Iterator<BackupResource> iterator() {
				return new UnionIterator<BackupResource>(resources);
			}

		};
	}

	public void setBackupServiceTracker(DynamicServiceTracker<BackupService> backupServiceTracker) {
		this.backupServiceTracker = backupServiceTracker;
	}

	public void setBackupServices(Collection<BackupService> backupServices) {
		this.backupServices = backupServices;
	}

	public void setResourceProviders(Collection<BackupResourceProvider> resourceProviders) {
		this.resourceProviders = resourceProviders;
	}

}
