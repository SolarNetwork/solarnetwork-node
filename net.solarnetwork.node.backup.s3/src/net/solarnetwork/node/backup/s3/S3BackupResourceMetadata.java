/* ==================================================================
 * S3BackupResourceMetadata.java - 3/10/2017 7:53:06 PM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.backup.s3;

/**
 * Metadata on a single backup resource within a backup.
 *
 * @author matt
 * @version 1.0
 */
public class S3BackupResourceMetadata {

	private String backupPath;
	private long modificationDate;
	private String providerKey;
	private String objectKey;
	private String digest;

	/**
	 * Constructor.
	 */
	public S3BackupResourceMetadata() {
		super();
	}

	/**
	 * Get the backup path.
	 *
	 * @return the backup path
	 */
	public String getBackupPath() {
		return backupPath;
	}

	/**
	 * Get the modification date.
	 *
	 * @return the modification date
	 */
	public long getModificationDate() {
		return modificationDate;
	}

	/**
	 * Get the provider key.
	 *
	 * @return the provider key
	 */
	public String getProviderKey() {
		return providerKey;
	}

	/**
	 * Set the backup path.
	 *
	 * @param backupPath
	 *        the backup path to set
	 */
	public void setBackupPath(String backupPath) {
		this.backupPath = backupPath;
	}

	/**
	 * Set the modification date.
	 *
	 * @param modificationDate
	 *        the modification date to set
	 */
	public void setModificationDate(long modificationDate) {
		this.modificationDate = modificationDate;
	}

	/**
	 * Set the provider key.
	 *
	 * @param providerKey
	 *        the provider key to set
	 */
	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	/**
	 * Get the object key.
	 *
	 * @return the object key
	 */
	public String getObjectKey() {
		return objectKey;
	}

	/**
	 * Set the object key.
	 *
	 * @param objectKey
	 *        the object key to set
	 */
	public void setObjectKey(String objectKey) {
		this.objectKey = objectKey;
	}

	/**
	 * Get the digest.
	 *
	 * @return the digest
	 */
	public String getDigest() {
		return digest;
	}

	/**
	 * Set the digest.
	 *
	 * @param digest
	 *        the digest to set
	 */
	public void setDigest(String digest) {
		this.digest = digest;
	}

}
