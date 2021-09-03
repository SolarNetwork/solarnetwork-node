/* ==================================================================
 * FilteringDatumDao.java - 15/03/2019 11:12:18 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import static net.solarnetwork.service.OptionalService.service;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;

/**
 * Delegating DAO for {@link NodeDatum} that applies filters before persisting.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class FilteringDatumDao implements DatumDao {

	private final OptionalService<DatumDao> delegate;
	private final OptionalService<DatumFilterService> filterService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the DAO to delegate to
	 * @param filterService
	 *        the transformer service
	 */
	public FilteringDatumDao(OptionalService<DatumDao> delegate,
			OptionalService<DatumFilterService> filterService) {
		super();
		this.delegate = delegate;
		this.filterService = filterService;
	}

	@Override
	public void storeDatum(NodeDatum datum) {
		final DatumFilterService filter = service(filterService);
		final DatumDao dao = service(delegate);
		if ( datum != null && filter != null && datum.asSampleOperations() != null ) {
			DatumSamplesOperations samples = filter.filter(datum, datum.asSampleOperations(), null);
			if ( samples == null || samples.isEmpty() ) {
				log.debug("Datum filter service filtered out datum {} @ {}; will not persist",
						datum.getSourceId(), datum.getTimestamp());
				return;
			} else if ( samples.differsFrom(datum.asSampleOperations()) ) {
				log.debug("Samples transform service modified datum {} @ {} properties to {}",
						datum.getSourceId(), datum.getTimestamp(), samples);
				NodeDatum copy = datum.copyWithSamples(samples);
				dao.storeDatum(copy);
				return;
			}
		}
		dao.storeDatum(datum);
	}

	@Override
	public List<NodeDatum> getDatumNotUploaded(String destination) {
		final DatumDao dao = service(delegate);
		return (dao != null ? dao.getDatumNotUploaded(destination) : Collections.emptyList());
	}

	@Override
	public void setDatumUploaded(NodeDatum datum, Instant date, String destination, String trackingId) {
		final DatumDao dao = service(delegate);
		if ( dao != null ) {
			dao.setDatumUploaded(datum, date, destination, trackingId);
		}
	}

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		final DatumDao dao = service(delegate);
		return (dao != null ? dao.deleteUploadedDataOlderThan(hours) : 0);
	}

}
