/* ===================================================================
 * SimpleDatum.java
 * 
 * Created Dec 1, 2009 4:10:14 PM
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

package net.solarnetwork.node.domain.datum;

import java.time.Instant;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Abstract base class for {@link NodeDatum} implementations.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleDatum extends GeneralDatum implements MutableNodeDatum, Cloneable {

	private static final long serialVersionUID = 6865265065461341282L;

	private Instant uploaded = null;

	/**
	 * Create a node datum.
	 * 
	 * <p>
	 * The {@code nodeId} property will be set to {@literal null} and presumed
	 * to be equal to the ID of the running node. The {@code timestamp} will be
	 * set to the system time. A new {@code samples} instance will be created.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 * @return the new instance
	 */
	public static SimpleDatum nodeDatum(String sourceId) {
		return nodeDatum(sourceId, Instant.now(), new DatumSamples());
	}

	/**
	 * Create a node datum.
	 * 
	 * <p>
	 * The {@code nodeId} property will be set to {@literal null} and presumed
	 * to be equal to the ID of the running node.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param samples
	 *        the samples
	 * @return the new instance
	 */
	public static SimpleDatum nodeDatum(String sourceId, Instant timestamp, DatumSamples samples) {
		return new SimpleDatum(DatumId.nodeId(null, sourceId, timestamp), samples);
	}

	/**
	 * Create a location datum.
	 * 
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param samples
	 *        the samples
	 * @return the new instance
	 */
	public static SimpleDatum locationDatum(Long locationId, String sourceId, Instant timestamp,
			DatumSamples samples) {
		return new SimpleDatum(DatumId.locationId(locationId, sourceId, timestamp), samples);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param samples
	 *        the samples; if {@literal null} a new instance will be created
	 */
	public SimpleDatum(DatumId id, DatumSamples samples) {
		super(id, samples);
	}

	@Override
	public SimpleDatum clone() {
		return (SimpleDatum) super.clone();
	}

	@Override
	public NodeDatum copyWithSamples(DatumSamplesOperations samples) {
		return (NodeDatum) super.copyWithSamples(samples);
	}

	@Override
	public NodeDatum copyWithId(DatumId id) {
		SimpleDatum d = new SimpleDatum(id, getSamples());
		d.uploaded = this.uploaded;
		return d;
	}

	@Override
	public Instant getUploaded() {
		return uploaded;
	}

	public void setUploaded(Instant uploaded) {
		this.uploaded = uploaded;
	}

}
