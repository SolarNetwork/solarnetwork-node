/* ===================================================================
 * MockUploadService.java
 * 
 * Created Dec 3, 2009 2:13:38 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.Datum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link UploadService}.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockUploadService implements UploadService, BulkUploadService {

	private final AtomicLong TRACKER_ID = new AtomicLong(0);

	private final Logger log = LoggerFactory.getLogger(MockUploadService.class);
	
	@Override
	public String getKey() {
		return "MockUploadService";
	}

	@Override
	public Long uploadDatum(Datum data) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: uploading datum [" +data + ']');
		}
		return TRACKER_ID.incrementAndGet();
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data) {
		List<BulkUploadResult> results = new ArrayList<BulkUploadResult>(data.size());
		for ( Datum d : data ) {
			results.add(new BulkUploadResult(d, TRACKER_ID.incrementAndGet()));
		}
		return results;
	}

}
