/* ==================================================================
 * ZipEntryBackupResource.java - Mar 27, 2013 4:18:59 PM
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@link BackupResource} for a file in a zip archive.
 * 
 * @author matt
 * @version 1.1
 */
public class ZipEntryBackupResource implements BackupResource {

	private final ZipFile archiveFile;
	private final ZipEntry entry;
	private final String providerKey;

	/**
	 * Constructor.
	 * 
	 * The {@code providerKey} will be set to {@code null}.
	 * 
	 * @param archiveFile
	 *        the archive file
	 * @param entry
	 *        the entry previously obtained from the zip archive
	 */
	public ZipEntryBackupResource(ZipFile archiveFile, ZipEntry entry) {
		this(archiveFile, entry, null);
	}

	/**
	 * Construct with values.
	 * 
	 * @param archiveFile
	 *        the archive file
	 * @param entry
	 *        the entry previously obtained from the zip archive
	 * @param providerKey
	 *        the provider key
	 * @since 1.1
	 */
	public ZipEntryBackupResource(ZipFile archiveFile, ZipEntry entry, String providerKey) {
		super();
		this.archiveFile = archiveFile;
		this.entry = entry;
		this.providerKey = providerKey;
	}

	@Override
	public String getProviderKey() {
		return providerKey;
	}

	@Override
	public String getBackupPath() {
		return entry.getName();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return archiveFile.getInputStream(entry);
	}

	@Override
	public long getModificationDate() {
		return entry.getTime();
	}

}
