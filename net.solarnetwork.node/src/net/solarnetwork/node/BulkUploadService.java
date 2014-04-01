/* ==================================================================
 * BulkUploadService.java - Feb 23, 2011 11:41:22 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node;

import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.domain.Datum;

/**
 * API for posting local SolarNode data to a remote server in bulk.
 * 
 * @author matt
 * @version $Revision$
 */
public interface BulkUploadService {

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
	 * Upload Datum data in bulk.
	 * 
	 * <p>The returned list of results will be ordered in normal iterating
	 * order for the passed in {@code data} Collection, and will contain
	 * exactly the same number of objects (one for each Datum). The 
	 * {@link BulkUploadResult#getId()} value can be used to determine if
	 * the Datum was uploaded successfully (if it is non-null, it should be
	 * considered successfully uploaded). </p>
	 * 
	 * <p>If the supplied Datum object is not supported by an implementation
	 * this method will throw an {@link IllegalArgumentException}.</p>
	 * 
	 * @param data the data to upload
	 * @return list of BulkUploadResult objects
	 */
	List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data);

}
