/* ==================================================================
 * TestDatum.java - 10 Nov 2026 11:58:03 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import java.time.Instant;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Datum classes with different constructors, for testing.
 *
 * @author matt
 * @version 1.0
 */
public final class TestDatum {

	private TestDatum() {
		// not available
	}

	/** A datum with only a node datum constructor. */
	public static class NodeOnly extends SimpleDatum {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 * @param samples
		 *        the samples
		 */
		public NodeOnly(String sourceId, Instant timestamp, DatumSamples samples) {
			super(DatumId.nodeId(null, sourceId, timestamp), samples);
		}

	}

	/** A datum with node and location datum constructors. */
	public static class NodeOrLocation extends SimpleDatum {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 * @param samples
		 *        the samples
		 */
		public NodeOrLocation(String sourceId, Instant timestamp, DatumSamples samples) {
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
		public NodeOrLocation(Long locationId, String sourceId, Instant timestamp,
				DatumSamples samples) {
			super(DatumId.locationId(locationId, sourceId, timestamp), samples);
		}

	}

	/** A datum without any supported constructor. */
	public static class NoConstructor extends SimpleDatum {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param sourceId
		 *        the source ID
		 */
		public NoConstructor(String sourceId) {
			super(DatumId.nodeId(null, sourceId, Instant.now()), new DatumSamples());
		}

	}

	/** A datum with a constructor that throws an exception. */
	public static class Broken extends SimpleDatum {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param id
		 *        the ID
		 * @param samples
		 *        the samples
		 */
		public Broken(DatumId id, DatumSamples samples) {
			super(id, samples);
			throw new UnsupportedOperationException("Broken");
		}

	}

}
