/* ==================================================================
 * DatumHistory.java - 18/08/2021 7:44:31 AM
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

package net.solarnetwork.node.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.util.CircularFifoQueue;

/**
 * Class to help track the history of datum capture, by source ID.
 * 
 * <p>
 * This class maintains a fixed-size history of {@link NodeDatum} at various
 * time levels:
 * </p>
 * 
 * <dl>
 * <dt>raw</dt>
 * <dd>Individual datum stored as-is.</dd>
 * </dl>
 * 
 * <p>
 * <b>Note</b> that no time-based ordering of datum is maintained by this class.
 * They are maintained simply by insertion time, i.e. the time when
 * {@code #add(Datum)} is invoked. Concurrent invocation of {@code #add(Datum)}
 * is allowed, but the order of the added elements is undefined in that case.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.89
 */
public class DatumHistory {

	/** A default configuration instance. */
	public static final Configuration DEFAULT_CONFIG = new Configuration(5);

	private final Configuration config;
	private final ConcurrentMap<String, Queue<NodeDatum>> raw;

	/**
	 * History configuration.
	 */
	public static class Configuration {

		private final int rawCount;

		/**
		 * Constructor.
		 * 
		 * @param rawCount
		 *        the number of raw elements to maintain
		 * @throws IllegalArgumentException
		 *         if any count is less
		 */
		public Configuration(int rawCount) {
			super();
			if ( rawCount < 1 ) {
				throw new IllegalArgumentException("The rawCount must be greater than 0.");
			}
			this.rawCount = rawCount;
		}

		/**
		 * Get the raw count.
		 * 
		 * @return the count
		 */
		public int getRawCount() {
			return rawCount;
		}

	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A default load factory and concurrency level will be used.
	 * </p>
	 * 
	 * @param config
	 *        the configuration to use
	 */
	public DatumHistory(Configuration config) {
		this(config, 0.75f, 4);
	}

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration to use
	 * @param loadFactor
	 *        a load factor to use for sizing the internal data structures
	 * @param concurrencyLevel
	 *        a concurrency level to use for sizing the internal data structures
	 * @throws IllegalArgumentException
	 *         if {@code config} is {@literal null}
	 */
	public DatumHistory(Configuration config, float loadFactor, int concurrencyLevel) {
		this(config, new ConcurrentHashMap<>(8, loadFactor, concurrencyLevel));
	}

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration to use
	 * @param raw
	 *        a map to use for raw datum
	 * @throws IllegalArgumentException
	 *         if {@code config} is {@literal null}
	 */
	public DatumHistory(Configuration config, ConcurrentMap<String, Queue<NodeDatum>> raw) {
		super();
		if ( config == null ) {
			throw new IllegalArgumentException("The config argument must not be null.");
		}
		this.config = config;
		this.raw = raw;
	}

	/**
	 * Add a datum.
	 * 
	 * <p>
	 * If {@code datum} is {@literal null} or does not have a source ID or
	 * creation date, nothing will be added.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to add
	 */
	public void add(NodeDatum datum) {
		if ( datum == null || datum.getSourceId() == null || datum.getTimestamp() == null ) {
			return;
		}
		Queue<NodeDatum> q = raw.computeIfAbsent(datum.getSourceId(),
				k -> new CircularFifoQueue<>(config.rawCount));
		synchronized ( q ) {
			q.add(datum);
		}
	}

	/**
	 * Get an {@code Iterable} over the latest available raw datum.
	 * 
	 * <p>
	 * This is equivalent to calling {@code offset(0)}.
	 * </p>
	 * 
	 * @return the {@code Iterable}, never {@literal null}
	 * @see #offset(int)
	 */
	public Iterable<NodeDatum> latest() {
		return offset(0);
	}

	/**
	 * Get the latest datum available with a given source ID.
	 * 
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, 0)}.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID to find
	 * @return the datum, or {@literal null}
	 * @see #offset(String,int)
	 */
	public NodeDatum latest(String sourceId) {
		return offset(sourceId, 0);
	}

	/**
	 * Get an {@code Iterable} over an offset from the latest available raw
	 * datum.
	 * 
	 * <p>
	 * An offset of {@literal 0} means the latest datum, and {@literal 1} means
	 * the one before the latest datum, and so on.
	 * </p>
	 * 
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the {@code Iterable}, never {@literal null}
	 * @since 1.1
	 */
	public Iterable<NodeDatum> offset(int offset) {
		return new Iterable<NodeDatum>() {

			@Override
			public Iterator<NodeDatum> iterator() {
				final List<NodeDatum> datum = new ArrayList<>(raw.size());
				for ( String sourceId : raw.keySet() ) {
					NodeDatum d = offset(sourceId, offset);
					if ( d != null ) {
						datum.add(d);
					}
				}
				return datum.iterator();
			}
		};
	}

	/**
	 * Get the datum offset from the latest available raw datum for a given
	 * source ID.
	 * 
	 * <p>
	 * An offset of {@literal 0} means the latest datum, and {@literal 1} means
	 * the one before the latest datum, and so on.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID to find
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the {@code Iterable}, never {@literal null}
	 * @since 1.1
	 */
	public NodeDatum offset(String sourceId, int offset) {
		final Queue<NodeDatum> q = raw.get(sourceId);
		if ( q == null ) {
			return null;
		}
		NodeDatum result;
		synchronized ( q ) {
			int idx = q.size() - 1 - offset;
			if ( idx >= 0 ) {
				result = ((CircularFifoQueue<NodeDatum>) q).get(idx);
			} else {
				result = null;
			}
		}
		return result;
	}

	/**
	 * Get an {@code Iterable} over an offset from a datum offset from a given
	 * timestamp.
	 * 
	 * <p>
	 * An offset of {@literal 0} means the datum closest before or equal to the
	 * given timestamp, and {@literal 1} means the next one before that, and so
	 * on. Note that no sorting of datum is performed by this method: it assumes
	 * elements have been added in ascending timestamp order already.
	 * </p>
	 * 
	 * @param timestamp
	 *        the timestamp to offset from
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @return the {@code Iterable}, never {@literal null}
	 * @since 1.1
	 */
	public Iterable<NodeDatum> offset(Instant timestamp, int offset) {
		return new Iterable<NodeDatum>() {

			@Override
			public Iterator<NodeDatum> iterator() {
				final List<NodeDatum> datum = new ArrayList<>(raw.size());
				for ( String sourceId : raw.keySet() ) {
					NodeDatum d = offset(sourceId, timestamp, offset);
					if ( d != null ) {
						datum.add(d);
					}
				}
				return datum.iterator();
			}
		};
	}

	/**
	 * Get the datum offset from a given timestamp for a given source ID.
	 * 
	 * <p>
	 * An offset of {@literal 0} means the datum closest before or equal to the
	 * given timestamp, and {@literal 1} means the next one before that, and so
	 * on. Note that no sorting of datum is performed by this method: it assumes
	 * elements have been added in ascending timestamp order already.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID to find
	 * @param timestamp
	 *        the timestamp to offset from
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the datum, or {@literal null} if no such datum is available
	 * @since 1.1
	 */
	public NodeDatum offset(String sourceId, Instant timestamp, int offset) {
		final Queue<NodeDatum> q = raw.get(sourceId);
		if ( q == null ) {
			return null;
		}
		NodeDatum result = null;
		synchronized ( q ) {
			int idx = q.size();
			while ( --idx >= 0 ) {
				NodeDatum d = ((CircularFifoQueue<NodeDatum>) q).get(idx);
				if ( d.getTimestamp().compareTo(timestamp) <= 0 ) {
					// found reference, so return offset from here
					int offsetIdx = idx - offset;
					if ( offsetIdx > 0 ) {
						result = (offsetIdx == idx ? d
								: ((CircularFifoQueue<NodeDatum>) q).get(offsetIdx));
					}
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Get the configuration.
	 * 
	 * @return the configuration, never {@literal null}
	 */
	public Configuration getConfig() {
		return config;
	}

}
