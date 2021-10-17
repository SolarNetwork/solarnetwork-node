/* ==================================================================
 * GeneralNodejava - Aug 26, 2014 10:29:21 AM
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
 * GeneralNodeDatum that also implements {@link AcEnergyDatum}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleAcEnergyDatum extends SimpleEnergyDatum implements AcEnergyDatum {

	private static final long serialVersionUID = -4888646280053353487L;

	private static final String[] DATUM_TYPES = new String[] {
			net.solarnetwork.domain.datum.AcEnergyDatum.class.getName(),
			net.solarnetwork.domain.datum.EnergyDatum.class.getName(), };

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This constructs a node datum.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param samples
	 *        the samples
	 */
	public SimpleAcEnergyDatum(String sourceId, Instant timestamp, DatumSamples samples) {
		super(sourceId, timestamp, samples);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param samples
	 *        the samples
	 */
	public SimpleAcEnergyDatum(DatumId id, DatumSamples samples) {
		super(id, samples);
	}

	@Override
	protected String[] datumTypes() {
		return DATUM_TYPES;
	}

	@Override
	public SimpleAcEnergyDatum copyWithSamples(DatumSamplesOperations samples) {
		DatumSamples newSamples = new DatumSamples();
		newSamples.copyFrom(samples);
		return new SimpleAcEnergyDatum(getId(), newSamples);
	}

	@Override
	public SimpleAcEnergyDatum copyWithId(DatumId id) {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum(id, getSamples());
		d.setUploaded(this.getUploaded());
		return d;
	}

}
