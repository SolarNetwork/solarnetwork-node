/* ==================================================================
 * SampleTransformingGeneralNodeDatumDao.java - 15/03/2019 11:12:18 am
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

import static net.solarnetwork.util.OptionalService.service;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.util.OptionalService;

/**
 * Proxy DAO for {@link GeneralNodeDatum} that applies sample transforms before
 * persisting.
 * 
 * @author matt
 * @version 1.1
 * @since 1.66
 */
public class SampleTransformingGeneralNodeDatumDao implements DatumDao<GeneralNodeDatum> {

	private final OptionalService<GeneralDatumSamplesTransformService> samplesTransformService;
	private final OptionalService<DatumDao<GeneralNodeDatum>> delegate;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the DAO to delegate to
	 * @param samplesTransformService
	 *        the transformer service
	 */
	public SampleTransformingGeneralNodeDatumDao(OptionalService<DatumDao<GeneralNodeDatum>> delegate,
			OptionalService<GeneralDatumSamplesTransformService> samplesTransformService) {
		super();
		this.samplesTransformService = samplesTransformService;
		this.delegate = delegate;
	}

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		final DatumDao<GeneralNodeDatum> dao = service(delegate);
		return (dao != null ? dao.getDatumType() : GeneralNodeDatum.class);
	}

	@Override
	public void storeDatum(GeneralNodeDatum datum) {
		final GeneralDatumSamplesTransformService xformService = service(samplesTransformService);
		final DatumDao<GeneralNodeDatum> dao = service(delegate);
		if ( datum != null && xformService != null && datum.getSamples() != null ) {
			GeneralDatumSamples samples = xformService.transformSamples(datum, datum.getSamples(), null);
			if ( samples == null || samples.isEmpty() ) {
				log.debug("Samples transform service filtered out datum {} @ {}; will not persist",
						datum.getSourceId(), datum.getCreated());
				return;
			} else if ( !samples.equals(datum.getSamples()) ) {
				log.debug("Samples transform service modified datum {} @ {} properties to {}",
						datum.getSourceId(), datum.getCreated(), samples.getSampleData());
				GeneralNodeDatum copy = datum.clone();
				copy.setSamples(samples);
				dao.storeDatum(copy);
				return;
			}
		}
		dao.storeDatum(datum);
	}

	@Override
	public List<GeneralNodeDatum> getDatumNotUploaded(String destination) {
		final DatumDao<GeneralNodeDatum> dao = service(delegate);
		return (dao != null ? dao.getDatumNotUploaded(destination) : Collections.emptyList());
	}

	@Override
	public void setDatumUploaded(GeneralNodeDatum datum, Date date, String destination,
			String trackingId) {
		final DatumDao<GeneralNodeDatum> dao = service(delegate);
		if ( dao != null ) {
			dao.setDatumUploaded(datum, date, destination, trackingId);
		}
	}

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		final DatumDao<GeneralNodeDatum> dao = service(delegate);
		return (dao != null ? dao.deleteUploadedDataOlderThan(hours) : 0);
	}

}
