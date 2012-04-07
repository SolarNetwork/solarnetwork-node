/* ===================================================================
 * MockDayDatumDao.java
 * 
 * Created Dec 2, 2009 11:03:55 AM
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

package net.solarnetwork.node.weather.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.weather.DayDatum;
import net.solarnetwork.node.support.BasicDatumUpload;

/**
 * Mock implementation of {@link DatumDao} for {@link DayDatum} objects.
 * 
 * <p>This implementation does not persist anything, it is useful for 
 * testing and debugging only.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockDayDatumDao 
implements DatumDao<DayDatum> {

	private final Logger log = LoggerFactory.getLogger(MockDayDatumDao.class);
	private final AtomicLong counter = new AtomicLong(0);
	
	public int deleteUploadedDataOlderThan(int hours) {
		return 0;
	}

	public List<DayDatum> getDatumNotUploaded(String destination) {
		return Collections.emptyList();
	}

	public Class<? extends DayDatum> getDatumType() {
		return DayDatum.class;
	}

	public Long storeDatum(DayDatum datum) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting DayDatum: " +datum);
		}
		return counter.decrementAndGet();
	}

	public BasicDatumUpload storeDatumUpload(DayDatum datum,
			String destination, Long trackingId) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting DayDatum " +datum
					+" upload to [" +destination +"] with tracking ID ["
					+trackingId +']');
		}
		return new BasicDatumUpload(datum, counter.decrementAndGet(), 
				destination, trackingId);
	}

}
