/* ===================================================================
 * UploadService.java
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
 * ===================================================================
 */

package net.solarnetwork.node.service;

import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for posting local SolarNode data to a remote server.
 * 
 * @author matt.magoffin
 * @version 1.2
 */
public interface UploadService {

	/**
	 * An event topic for when a {@link NodeDatum} has been uploaded.
	 * 
	 * <p>
	 * The properties of the event shall be {@link NodeDatum#asSimpleMap()}.
	 * </p>
	 * 
	 * @since 1.2
	 */
	public static final String EVENT_TOPIC_DATUM_UPLOADED = "net/solarnetwork/node/UploadService/DATUM_UPLOADED";

	/**
	 * Get a unique key for this service.
	 * 
	 * <p>
	 * This key can be used as the {@code destination} value for {@link NodeDatum}
	 * objects. It need be unique across other UploadService implementations
	 * only.
	 * </p>
	 * 
	 * @return unique key
	 */
	String getKey();

	/**
	 * Upload Datum data.
	 * 
	 * <p>
	 * The returned primary key can be used as the {@code trackingId} value for
	 * {@link NodeDatum} objects.
	 * </p>
	 * 
	 * <p>
	 * If the supplied Datum object is not supported by an implementation this
	 * method will throw an {@link IllegalArgumentException}.
	 * </p>
	 * 
	 * @param data
	 *        the data to upload
	 * @return the remote primary key
	 */
	String uploadDatum(NodeDatum data);

}
