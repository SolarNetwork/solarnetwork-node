/* ==================================================================
 * GeneralAtmosphericDatum.java - Oct 22, 2014 2:30:22 PM
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

package net.solarnetwork.node.domain;

import java.time.Instant;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * GeneralLocationDatum that also implements {@link AtmosphericDatum}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleAtmosphericDatum extends SimpleDatum implements AtmosphericDatum {

	private static final long serialVersionUID = -569470291206652748L;

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
	public SimpleAtmosphericDatum(String sourceId, Instant timestamp, DatumSamples samples) {
		super(DatumId.nodeId(null, sourceId, timestamp), samples);
	}

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
	public SimpleAtmosphericDatum(Long locationId, String sourceId, Instant timestamp,
			DatumSamples samples) {
		super(DatumId.locationId(locationId, sourceId, timestamp), samples);
	}

}
