/* ==================================================================
 * BackupResourceProvider.java - Mar 28, 2013 6:07:43 AM
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

import java.util.Locale;

/**
 * A provider of {@link BackupResource} instances.
 * 
 * <p>
 * Any system component can register {@link BackupResourceProvider} instances to
 * include additional resources in backups.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface BackupResourceProvider {

	/**
	 * Get a key, unique among all other {@link BackupResourceProvider}
	 * instances.
	 * 
	 * <p>
	 * The key should contain only alpha-numeric and/or the period characters. A
	 * good candidate is the full class name of the provider.
	 * </p>
	 * 
	 * @return the provider key
	 */
	String getKey();

	/**
	 * Get the resources that should be backed up.
	 * 
	 * @return the resources, never <em>null</em>
	 */
	Iterable<BackupResource> getBackupResources();

	/**
	 * Restore a {@link BackupResoruce}.
	 * 
	 * @param resource
	 *        the resource to restore
	 * @return <em>true</em> if successful, <em>false</em> otherwise
	 */
	boolean restoreBackupResource(BackupResource resource);

	/**
	 * Get info about the provider.
	 * 
	 * @param locale
	 *        The desired locale of the information, or {@code null} for the
	 *        system locale.
	 * @return The info.
	 * @since 1.1
	 */
	BackupResourceProviderInfo providerInfo(Locale locale);

	/**
	 * Get info about a particular resource.
	 * 
	 * @param resource
	 *        The resource to get the information for.
	 * @param locale
	 *        The desired locale of the information, or {@code null} for the
	 *        system locale.
	 * @return The info, or {@code null} if none available.
	 * @since 1.1
	 */
	BackupResourceInfo resourceInfo(BackupResource resource, Locale locale);
}
