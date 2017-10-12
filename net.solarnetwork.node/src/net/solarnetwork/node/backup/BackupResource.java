/* ==================================================================
 * BackupResource.java - Mar 27, 2013 2:56:38 PM
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

import java.io.IOException;
import java.io.InputStream;

/**
 * API for a resource to take part in the backup system.
 * 
 * @author matt
 * @version 1.2
 */
public interface BackupResource {

	/**
	 * Get a relative path to save this resource to in the backup.
	 * 
	 * <p>
	 * This must be a URL-like path, using a forward slash to represent
	 * directories. For example, the a path could be {@code some/path/here.txt}.
	 * </p>
	 * 
	 * @return the relative path
	 */
	String getBackupPath();

	/**
	 * Get an {@link InputStream} to the resource.
	 * 
	 * @return an InputStream to the data for the resource
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * Get the modification date of the resource, in milliseconds since the
	 * epoch.
	 * 
	 * @return the modification date, or <em>-1</em> if not known
	 */
	long getModificationDate();

	/**
	 * Get the key of the {@link BackupResourceProvider} that provided this
	 * resource.
	 * 
	 * @return The provider key.
	 * @since 1.1
	 */
	String getProviderKey();

	/**
	 * Get the expected SHA-256 digest of the resource content.
	 * 
	 * <p>
	 * This value is meant to be used to verify the integrity of the content of
	 * the resource when read from {@link #getInputStream()}.
	 * </p>
	 * 
	 * @return the SHA-256 digest as a hex-encoded string, or {@literal null} if
	 *         not known
	 * @since 1.2
	 */
	String getSha256Digest();

}
