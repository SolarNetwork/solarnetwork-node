/* ==================================================================
 * ResourceBackupResource.java - Mar 27, 2013 3:56:48 PM
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
import org.springframework.core.io.Resource;

/**
 * {@link BackupResource} implementation using a Spring {@link Resource}.
 * 
 * @author matt
 * @version 1.1
 */
public class ResourceBackupResource implements BackupResource {

	private final Resource resource;
	private final String backupPath;
	private final String providerKey;

	/**
	 * Constructor.
	 * 
	 * The {@code providerKey} will be set to
	 * {@code net.solarnetwork.node.backup.FileBackupResourceProvider}.
	 * 
	 * @param resource
	 *        The resource.
	 * @param backupPath
	 *        The backup path.
	 */
	public ResourceBackupResource(Resource resource, String backupPath) {
		this(resource, backupPath, FileBackupResourceProvider.class.getName());
	}

	/**
	 * Construct with a specific provider key.
	 * 
	 * @param resource
	 *        The resource.
	 * @param backupPath
	 *        The backup path.
	 * @param providerKey
	 *        The provider key.
	 * @since 1.1
	 */
	public ResourceBackupResource(Resource resource, String backupPath, String providerKey) {
		super();
		this.resource = resource;
		this.backupPath = backupPath;
		this.providerKey = providerKey;
	}

	@Override
	public String getProviderKey() {
		return providerKey;
	}

	@Override
	public String getBackupPath() {
		return backupPath;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return resource.getInputStream();
	}

	@Override
	public long getModificationDate() {
		try {
			return resource.getFile().lastModified();
		} catch ( IOException e ) {
			// ignore
		}
		return -1;
	}

}
