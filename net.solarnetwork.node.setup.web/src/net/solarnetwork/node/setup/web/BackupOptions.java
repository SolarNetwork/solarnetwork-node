/* ==================================================================
 * BackupOptions.java - 3/11/2016 7:12:14 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.backup.BackupManager;

/**
 * Options to pass to backup operations.
 * 
 * @author matt
 * @version 1.0
 */
public class BackupOptions {

	private String key;
	private String[] providers;

	/**
	 * Get a property map suitable for passing into various
	 * {@link BackupManager} methods.
	 * 
	 * @return A property map, or {@literal null}.
	 */
	public Map<String, String> asBackupManagerProperties() {
		if ( providers == null || providers.length < 1 ) {
			return null;
		}
		Map<String, String> props = new HashMap<String, String>(1);
		props.put(BackupManager.RESOURCE_PROVIDER_FILTER,
				StringUtils.arrayToCommaDelimitedString(providers));
		return props;
	}

	/**
	 * Get the {@link net.solarnetwork.node.backup.Backup} key.
	 * 
	 * @return The backup key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the {@link net.solarnetwork.node.backup.Backup} key.
	 * 
	 * @param key
	 *        The backup key.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the list of
	 * {@link net.solarnetwork.node.backup.BackupResourceProvider} keys to limit
	 * the backup to.
	 * 
	 * @return The list of provider keys to limit the backup to, or {@literal null}
	 *         for all providers.
	 */
	public String[] getProviders() {
		return providers;
	}

	/**
	 * Set the list of
	 * {@link net.solarnetwork.node.backup.BackupResourceProvider} keys to limit
	 * the backup to.
	 * 
	 * @param providers
	 *        The list of provider keys to limit the backup to, or {@literal null}
	 *        for all providers.
	 */
	public void setProviders(String[] providers) {
		this.providers = providers;
	}

}
