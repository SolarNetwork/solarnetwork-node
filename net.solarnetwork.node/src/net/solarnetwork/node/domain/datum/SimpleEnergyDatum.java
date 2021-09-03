/* ==================================================================
 * SimpleEnergyDatum.java - Aug 26, 2014 10:32:27 AM
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

/**
 * GeneralNodeDatum that also implements {@link EnergyDatum}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleEnergyDatum extends SimpleDatum implements EnergyDatum {

	private static final long serialVersionUID = 420515359849018176L;

	private static final String[] DATUM_TYPES = new String[] {
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
	public SimpleEnergyDatum(String sourceId, Instant timestamp, DatumSamples samples) {
		super(DatumId.nodeId(null, sourceId, timestamp), samples);
	}

	@Override
	protected String[] datumTypes() {
		return DATUM_TYPES;
	}

}
