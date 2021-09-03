/* ==================================================================
 * QueueGeneralDatumDao.java - 22/08/2021 8:13:53 AM
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

package net.solarnetwork.node.dao;

import java.time.Instant;
import java.util.List;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumQueue;

/**
 * {@link DatumDao} that offers datum to a {@link DatumQueue}.
 * 
 * @author matt
 * @version 2.0
 * @since 1.89
 */
public class QueueGeneralDatumDao implements DatumDao {

	private final DatumQueue datumQueue;

	/**
	 * Constructor.
	 * 
	 * @param datumQueue
	 *        the queue
	 */
	public QueueGeneralDatumDao(DatumQueue datumQueue) {
		super();
		this.datumQueue = datumQueue;
	}

	@Override
	public void storeDatum(NodeDatum datum) {
		datumQueue.offer(datum);
	}

	@Override
	public List<NodeDatum> getDatumNotUploaded(String destination) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDatumUploaded(NodeDatum datum, Instant date, String destination, String trackingId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		throw new UnsupportedOperationException();
	}

}
