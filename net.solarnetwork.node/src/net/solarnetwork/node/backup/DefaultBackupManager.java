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
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.OptionalService;
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
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultBackupManager implements BackupManager {

	private static final MessageSource MESSAGE_SOURCE = getMessageSourceInstance();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private OptionalService<BackupService> backupServiceTracker;

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
		results.add(new BasicTextFieldSettingSpecifier("dataCollectorFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFilterValue", ""));
		results.add(new BasicToggleSettingSpecifier("collectAllSourceIds", Boolean.TRUE));
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
			log.info("BackupService {} in {} state, can't find resources for backup", service.getInfo()
					.getKey(), service.getInfo().getStatus());
			return Collections.emptyList();
		}

		//final Date lastBackupDate = service.getInfo().getMostRecentBackupDate();

		// TODO see if any settings have been changed and back those up
		final List<BackupResource> resources = new ArrayList<BackupResource>(50);
		return resources;
	}

	public void setBackupServiceTracker(OptionalService<BackupService> backupServiceTracker) {
		this.backupServiceTracker = backupServiceTracker;
	}

}
