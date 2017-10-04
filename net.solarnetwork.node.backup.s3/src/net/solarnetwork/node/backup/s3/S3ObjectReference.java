/* ==================================================================
 * S3ObjectReference.java - 3/10/2017 2:54:27 PM
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

import java.util.Date;

/**
 * A reference to an S3 object.
 * 
 * @author matt
 * @version 1.0
 */
public class S3ObjectReference {

	private final String key;
	private final long size;
	private final Date modified;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 */
	public S3ObjectReference(String key) {
		this(key, -1, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param size
	 *        the size
	 * @param modified
	 *        the modification date
	 */
	public S3ObjectReference(String key, long size, Date modified) {
		super();
		this.key = key;
		this.size = size;
		this.modified = modified;
	}

	/**
	 * Get the object key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the object size.
	 * 
	 * @return the size, in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modified date
	 */
	public Date getModified() {
		return modified;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof S3ObjectReference) ) {
			return false;
		}
		S3ObjectReference other = (S3ObjectReference) obj;
		if ( key == null ) {
			if ( other.key != null ) {
				return false;
			}
		} else if ( !key.equals(other.key) ) {
			return false;
		}
		return true;
	}

}
