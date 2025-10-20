/* ==================================================================
 * BackupInfo.java - 2/11/2016 1:33:07 PM
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Metadata about a {@link Backup}.
 *
 * @author matt
 * @version 1.1
 * @since 1.46
 */
@JsonPropertyOrder({ "key", "nodeId", "date", "qualifier", "providerInfos", "resourceInfos" })
public interface BackupInfo extends BackupIdentity {

	/**
	 * Get a list of all providers included in the backup.
	 *
	 * @return The list of providers, or an empty list.
	 */
	Collection<BackupResourceProviderInfo> getProviderInfos();

	/**
	 * Get a list of all resources included in the backup.
	 *
	 * The resources should be ordered such that all resources for a given
	 * provider are together.
	 *
	 * @return The list of resources, or an empty list.
	 */
	Collection<BackupResourceInfo> getResourceInfos();

}
