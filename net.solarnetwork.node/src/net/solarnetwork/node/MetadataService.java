/* ==================================================================
 * MetadataService.java - 7/05/2021 10:05:17 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * API for accessing dynamic metadata.
 * 
 * @author matt
 * @version 1.0
 * @since 1.83
 */
public interface MetadataService extends Identifiable {

	/**
	 * Get all metadata available.
	 * 
	 * @return the metadata, or {@literal null} if none available
	 */
	GeneralDatumMetadata getAllMetadata();

	/**
	 * Get a metadata value at a given path.
	 * 
	 * @param path
	 *        the path of the metadata object to get
	 * @return the value, or {@literal null}
	 * @see GeneralDatumMetadata#metadataAtPath(String)
	 */
	default Object metadataAtPath(String path) {
		GeneralDatumMetadata meta = getAllMetadata();
		return (meta != null ? meta.metadataAtPath(path) : null);
	}

	/**
	 * Get a metadata value of a given type at a given path.
	 * 
	 * @param <T>
	 *        the expected value type
	 * @param path
	 *        the path of the metadata object to get
	 * @param class
	 *        the expected value type class
	 * @return the value, or {@literal null}
	 * @see GeneralDatumMetadata#metadataAtPath(String, Class)
	 */
	default <T> T metadataAtPath(String path, Class<T> clazz) {
		GeneralDatumMetadata meta = getAllMetadata();
		return (meta != null ? meta.metadataAtPath(path, clazz) : null);
	}

}
