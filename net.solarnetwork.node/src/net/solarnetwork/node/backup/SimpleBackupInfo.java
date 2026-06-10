/* ==================================================================
 * SimpleBackupInfo.java - 2/11/2016 1:36:53 PM
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

package net.solarnetwork.node.backup;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Basic implementation of {@link BackupInfo}.
 *
 * @author matt
 * @version 1.1
 * @since 1.46
 */
public class SimpleBackupInfo extends SimpleBackupIdentity implements BackupInfo {

	private final Collection<BackupResourceProviderInfo> providerInfos;
	private final Collection<BackupResourceInfo> resourceInfos;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        The backup key.
	 * @param date
	 *        The backup date.
	 * @param providerInfos
	 *        The providers.
	 * @param resourceInfos
	 *        The resources.
	 */
	public SimpleBackupInfo(String key, Date date,
			@Nullable Collection<BackupResourceProviderInfo> providerInfos,
			@Nullable Collection<BackupResourceInfo> resourceInfos) {
		this(key, date, null, null, providerInfos, resourceInfos);
	}

	/**
	 * Constructor.
	 *
	 * @param key
	 *        The backup key.
	 * @param date
	 *        The backup date.
	 * @param nodeId
	 *        The node ID.
	 * @param qualifier
	 *        The qualifier.
	 * @param providerInfos
	 *        The providers.
	 * @param resourceInfos
	 *        The resources.
	 */
	public SimpleBackupInfo(String key, Date date, @Nullable Long nodeId, @Nullable String qualifier,
			@Nullable Collection<BackupResourceProviderInfo> providerInfos,
			@Nullable Collection<BackupResourceInfo> resourceInfos) {
		super(key, date, null, null);
		this.providerInfos = (providerInfos != null ? providerInfos : List.of());
		this.resourceInfos = (resourceInfos != null ? resourceInfos : List.of());
	}

	@Override
	public Collection<BackupResourceProviderInfo> getProviderInfos() {
		return providerInfos;
	}

	@Override
	public Collection<BackupResourceInfo> getResourceInfos() {
		return resourceInfos;
	}

}
