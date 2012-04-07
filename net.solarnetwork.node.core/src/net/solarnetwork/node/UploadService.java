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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node;

/**
 * API for posting local SolarNode data to a remote server.
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public interface UploadService {

	/**
	 * Get a unique key for this service.
	 * 
	 * <p>This key can be used as the {@code destination} value for
	 * {@link DatumUpload} objects. It need be unique across other
	 * UploadService implementations only.</p>
	 * 
	 * @return unique key
	 */
	String getKey();
	
	/**
	 * Upload Datum data.
	 * 
	 * <p>The returned primary key can be used as the {@code trackingId}
	 * value for {@link DatumUpload} objects.</p>
	 * 
	 * <p>If the supplied Datum object is not supported by an implementation
	 * this method will throw an {@link IllegalArgumentException}.</p>
	 * 
	 * @param data the data to upload
	 * @return the remote primary key
	 */
	Long uploadDatum(Datum data);
	
}
