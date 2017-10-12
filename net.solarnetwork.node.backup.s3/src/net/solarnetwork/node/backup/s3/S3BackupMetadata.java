/* ==================================================================
 * S3BackupMetadata.java - 3/10/2017 5:53:20 PM
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupIdentity;
import net.solarnetwork.node.backup.BackupResource;

/**
 * S3 implementation of {@link Backup}.
 * 
 * @author matt
 * @version 1.0
 */
public class S3BackupMetadata implements Backup {

	private Long nodeId;
	private String key;
	private Date date;
	private String qualifier;
	private boolean complete;

	private List<S3BackupResourceMetadata> resourceMetadata;

	public S3BackupMetadata() {
		this(null);
	}

	public S3BackupMetadata(S3ObjectReference objRef) {
		super();
		if ( objRef != null ) {
			this.date = objRef.getModified();
			setKey(objRef.getKey());
			this.complete = true;
		} else {
			this.nodeId = null;
			this.key = null;
			this.date = null;
			this.complete = false;
		}
	}

	@Override
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
		BackupIdentity ident = S3BackupService.identityFromBackupKey(key);
		if ( ident != null ) {
			if ( this.nodeId == null ) {
				setNodeId(ident.getNodeId());
			}
			if ( this.date == null ) {
				setDate(ident.getDate());
			}
			if ( this.qualifier == null ) {
				setQualifier(ident.getQualifier());
			}
		}
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public Long getSize() {
		return null;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * Add a resource to the resource list.
	 * 
	 * @param resource
	 *        the resource to add
	 * @param objectKey
	 *        the S3 object key
	 * @param digest
	 *        the digest of the resource contents
	 */
	public void addBackupResource(BackupResource resource, String objectKey, String digest) {
		S3BackupResourceMetadata meta = new S3BackupResourceMetadata();
		meta.setBackupPath(resource.getBackupPath());
		meta.setModificationDate(resource.getModificationDate());
		meta.setProviderKey(resource.getProviderKey());
		meta.setObjectKey(objectKey);
		meta.setDigest(digest);
		if ( resourceMetadata == null ) {
			resourceMetadata = new ArrayList<>(16);
		}
		resourceMetadata.add(meta);
	}

	public List<S3BackupResourceMetadata> getResourceMetadata() {
		return resourceMetadata;
	}

	public void setResourceMetadata(List<S3BackupResourceMetadata> resourceMetadata) {
		this.resourceMetadata = resourceMetadata;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

}
