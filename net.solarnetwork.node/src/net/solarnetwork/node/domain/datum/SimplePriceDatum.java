/* ==================================================================
 * SimplePriceDatum.java - Oct 22, 2014 4:01:27 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.datum;

import java.time.Instant;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;

/**
 * Extension of {@link SimpleDatum} that implements {@link PriceDatum}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimplePriceDatum extends SimpleDatum implements PriceDatum {

	private static final long serialVersionUID = -1828470049598556387L;

	private static final String[] DATUM_TYPES = new String[] {
			net.solarnetwork.domain.datum.PriceDatum.class.getName(), };

	/**
	 * Constructor.
	 * 
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param samples
	 *        the samples
	 */
	public SimplePriceDatum(Long locationId, String sourceId, Instant timestamp, DatumSamples samples) {
		super(DatumId.locationId(locationId, sourceId, timestamp), samples);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param samples
	 *        the samples
	 */
	public SimplePriceDatum(DatumId id, DatumSamples samples) {
		super(id, samples);
	}

	@Override
	protected String[] datumTypes() {
		return DATUM_TYPES;
	}

	@Override
	public SimplePriceDatum copyWithSamples(DatumSamplesOperations samples) {
		DatumSamples newSamples = new DatumSamples();
		newSamples.copyFrom(samples);
		return new SimplePriceDatum(getId(), newSamples);
	}

	@Override
	public SimplePriceDatum copyWithId(DatumId id) {
		SimplePriceDatum d = new SimplePriceDatum(id, getSamples());
		d.setUploaded(this.getUploaded());
		return d;
	}

}
