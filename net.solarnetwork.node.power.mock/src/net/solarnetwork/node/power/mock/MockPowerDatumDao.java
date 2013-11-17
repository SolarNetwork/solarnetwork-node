/* ===================================================================
 * MockPowerDatumDao.java
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
 */

package net.solarnetwork.node.power.mock;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.power.PowerDatum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link DatumDao} for {@link PowerDatum} objects.
 * 
 * <p>
 * This implementation does not persist anything, it is useful for testing and
 * debugging only.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class MockPowerDatumDao implements DatumDao<PowerDatum> {

	private final Logger log = LoggerFactory.getLogger(MockPowerDatumDao.class);

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		return 0;
	}

	@Override
	public List<PowerDatum> getDatumNotUploaded(String destination) {
		return Collections.emptyList();
	}

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public void storeDatum(PowerDatum datum) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting PowerDatum: " + datum);
		}
	}

	@Override
	public void setDatumUploaded(PowerDatum datum, Date date, String destination, Long trackingId) {
		if ( log.isDebugEnabled() ) {
			log.debug("MOCK: persisting PowerDatum " + datum + " upload to [" + destination
					+ "] with tracking ID [" + trackingId + ']');
		}
	}

}
