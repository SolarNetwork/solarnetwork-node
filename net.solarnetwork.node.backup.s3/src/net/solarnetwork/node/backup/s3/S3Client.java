/* ==================================================================
 * S3Client.java - 3/10/2017 2:11:58 PM
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
import java.util.Set;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * API for accessing S3.
 * 
 * @author matt
 * @version 1.1
 */
public interface S3Client {

	/**
	 * Test if the client is fully configured.
	 * 
	 * @return {@literal true} if the client is configured
	 * @since 1.1
	 */
	boolean isConfigured();

	/**
	 * List all available objects matching a prefix.
	 * 
	 * @param prefix
	 *        the prefix to match
	 * @return the matching objects, never {@literal null}
	 */
	Set<S3ObjectReference> listObjects(String prefix);

	/**
	 * Get the contents of a S3 object as a string.
	 * 
	 * @param key
	 *        the key of the object to get
	 * @return the string, or {@literal null} if not found
	 */
	String getObjectAsString(String key);

	/**
	 * Get a S3 object.
	 * 
	 * @param key
	 *        the key of the object to get
	 * @return the object, or {@literal null} if not found
	 */
	S3Object getObject(String key);

	/**
	 * Put an object onto S3.
	 * 
	 * @param key
	 *        the key of the object to put
	 * @param in
	 *        the object contents
	 * @param objectMetadata
	 *        the object metadata
	 * @return a referenece to the put object
	 * @throws IOException
	 *         if an IO error occurs
	 */
	S3ObjectReference putObject(String key, InputStream in, ObjectMetadata objectMetadata)
			throws IOException;

	/**
	 * Delete a set of keys from S3.
	 * 
	 * @param keys
	 *        the keys to delete
	 */
	void deleteObjects(Set<String> keys) throws IOException;

}
