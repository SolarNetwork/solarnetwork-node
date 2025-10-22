/* ==================================================================
 * S3BackupResource.java - 4/10/2017 6:40:35 AM
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

import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.common.s3.S3Client;
import net.solarnetwork.common.s3.S3Object;
import net.solarnetwork.node.backup.BackupResource;

/**
 * {@link BackupResource} for a S3 object.
 *
 * <p>
 * The {@link #getInputStream()} method will return new InputStream instances
 * each time, each stream will fetch from S3 anew.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class S3BackupResource implements BackupResource {

	private final S3Client client;
	private final S3BackupResourceMetadata metadata;

	/**
	 * Constructor.
	 *
	 * @param client
	 *        the S3 client
	 * @param metadata
	 *        the metadata
	 */
	public S3BackupResource(S3Client client, S3BackupResourceMetadata metadata) {
		super();
		this.client = client;
		this.metadata = metadata;
	}

	@Override
	public String getBackupPath() {
		return metadata.getBackupPath();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		S3Object obj = client.getObject(metadata.getObjectKey(), null, null);
		return obj.getInputStream();
	}

	@Override
	public long getModificationDate() {
		return metadata.getModificationDate();
	}

	@Override
	public String getProviderKey() {
		return metadata.getProviderKey();
	}

	@Override
	public String getSha256Digest() {
		return metadata.getDigest();
	}

}
