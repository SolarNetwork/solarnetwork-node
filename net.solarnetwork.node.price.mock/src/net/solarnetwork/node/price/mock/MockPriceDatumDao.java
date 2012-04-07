/* ===================================================================
 * MockPriceDatumDao.java
 * 
 * Created Dec 3, 2009 3:51:21 PM
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

package net.solarnetwork.node.price.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.price.PriceDatum;
import net.solarnetwork.node.support.BasicDatumUpload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link DatumDao} for {@link PriceDatum} objects.
 * 
 * <p>This implementation does not persist anything, it is useful for 
 * testing and debugging only.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockPriceDatumDao 
implements DatumDao<PriceDatum> {

	private final Logger log = LoggerFactory.getLogger(MockPriceDatumDao.class);
	private final AtomicLong counter = new AtomicLong(0);
	
	public int deleteUploadedDataOlderThan(int hours) {
		return 0;
	}

	public List<PriceDatum> getDatumNotUploaded(String destination) {
		return Collections.emptyList();
	}

	public Class<? extends PriceDatum> getDatumType() {
		return PriceDatum.class;
	}

	public Long storeDatum(PriceDatum datum) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting PriceDatum: " +datum);
		}
		return counter.decrementAndGet();
	}

	public BasicDatumUpload storeDatumUpload(PriceDatum datum,
			String destination, Long trackingId) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting PriceDatum " +datum
					+" upload to [" +destination +"] with tracking ID ["
					+trackingId +']');
		}
		return new BasicDatumUpload(datum, counter.decrementAndGet(), 
				destination, trackingId);
	}

}
