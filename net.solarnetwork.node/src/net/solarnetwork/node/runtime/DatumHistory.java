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
 * @version 1.0
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
	 * Get an iterable over the latest available raw datum.
	 * 
	 * @return the iterable, never {@literal null}
	 */
	public Iterable<NodeDatum> latest() {
		// 
		return new Iterable<NodeDatum>() {

			@Override
			public Iterator<NodeDatum> iterator() {
				final List<NodeDatum> datum = new ArrayList<>(raw.size());
				for ( Queue<NodeDatum> q : raw.values() ) {
					NodeDatum d;
					synchronized ( q ) {
						int end = q.size() - 1;
						if ( end >= 0 ) {
							d = ((CircularFifoQueue<NodeDatum>) q).get(end);
						} else {
							d = null;
						}
					}
					if ( d != null ) {
						datum.add(d);
					}
				}
				return datum.iterator();
			}
		};
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
