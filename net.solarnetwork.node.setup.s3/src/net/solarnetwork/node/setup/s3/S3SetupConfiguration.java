/* ==================================================================
 * S3SetupConfiguration.java - 13/10/2017 9:19:02 AM
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

package net.solarnetwork.node.setup.s3;

/**
 * Metadata about an S3 setup.
 * 
 * @author matt
 * @version 1.0
 */
public class S3SetupConfiguration {

	public static final String DEFAULT_CLEAN_PATHS = "{osgi.configuration.area}/config.ini";

	private Long version;
	private String objectKey;
	private String[] syncPaths;
	private String[] objects;
	private String[] cleanPaths;

	public String[] getSyncPaths() {
		return syncPaths;
	}

	/**
	 * Set a list of file path patterns
	 * 
	 * @param syncPaths
	 */
	public void setSyncPaths(String[] syncPaths) {
		this.syncPaths = syncPaths;
	}

	/**
	 * Get the list of S3 object keys to download.
	 * 
	 * @return the object keys
	 */
	public String[] getObjects() {
		return objects;
	}

	/**
	 * Set the list of S3 object keys to download.
	 * 
	 * @param objects
	 *        list of S3 object keys
	 */
	public void setObjects(String[] objects) {
		this.objects = objects;
	}

	/**
	 * Get the list of paths to delete after a setup operation completes.
	 * 
	 * @return the list of paths to delete
	 */
	public String[] getCleanPaths() {
		return cleanPaths;
	}

	/**
	 * Set a list of paths to delete after a setup operation completes.
	 * 
	 * @param cleanPaths
	 *        list of paths to delete
	 */
	public void setCleanPaths(String[] cleanPaths) {
		this.cleanPaths = cleanPaths;
	}

	/**
	 * Get the configuration version.
	 * 
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * Set the configuration version.
	 * 
	 * <p>
	 * Version numbers should be globally unique and increase only over time.
	 * </p>
	 * 
	 * @param version
	 *        the version to set
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * Get the S3 object key for this metadata object.
	 * 
	 * @return the S3 object key for this data
	 */
	public String getObjectKey() {
		return objectKey;
	}

	/**
	 * Set the S3 object key for this metadata object.
	 * 
	 * @param objectKey
	 *        the S3 object key to use
	 */
	public void setObjectKey(String objectKey) {
		this.objectKey = objectKey;
	}

}
