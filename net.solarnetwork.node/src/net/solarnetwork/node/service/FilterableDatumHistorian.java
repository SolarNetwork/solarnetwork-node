/* ==================================================================
 * FilterableDatumHistorian.java - 11 Nov 2026 8:46:23 am
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

package net.solarnetwork.node.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.dao.NodeDatumFilter;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Extension of {@link DatumHistorian} with DAO filtering support.
 *
 * <p>
 * The {@code SORT_BY_*} constants are the supported sort keys. Results are
 * sorted by {@link #DEFAULT_SORTS} when no sorts are given.
 * </p>
 *
 * <p>
 * The {@link #findDatumStreamMetadata(NodeDatumFilter)} and
 * {@link #findFilteredStream(NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)}
 * and
 * {@link #findFilteredStreamReadings(NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)}
 * methods provide the history as datum streams, each with a UUID stream ID. A
 * historian that numbers its streams with integers can create these IDs with
 * {@link net.solarnetwork.node.service.support.DatumStreamIdUtils}.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public interface FilterableDatumHistorian
		extends DatumHistorian, FilterableDao<NodeDatum, DatumId, NodeDatumFilter> {

	/** A sort key for the node ID. */
	String SORT_BY_NODE = "node";

	/** A sort key for the location ID. */
	String SORT_BY_LOCATION = "loc";

	/** A sort key for the source ID. */
	String SORT_BY_SOURCE = "source";

	/** A sort key for the datum timestamp. */
	String SORT_BY_TIME = "time";

	/**
	 * The default sorts: by node, location, source, and then time.
	 *
	 * <p>
	 * This keeps the datum of each stream together, oldest first.
	 * </p>
	 */
	List<SortDescriptor> DEFAULT_SORTS = List.of(new SimpleSortDescriptor(SORT_BY_NODE),
			new SimpleSortDescriptor(SORT_BY_LOCATION), new SimpleSortDescriptor(SORT_BY_SOURCE),
			new SimpleSortDescriptor(SORT_BY_TIME));

	/**
	 * Find the metadata of the datum streams matching a filter.
	 *
	 * <p>
	 * The stream IDs of the results can be used as the stream criteria of later
	 * queries on this historian.
	 * </p>
	 *
	 * @param filter
	 *        the search filter
	 * @return the matching metadata, never {@code null}
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 * @since 1.1
	 */
	Collection<ObjectDatumStreamMetadata> findDatumStreamMetadata(NodeDatumFilter filter);

	/**
	 * Find datum matching a filter and pass them to a processor as stream
	 * datum.
	 *
	 * <p>
	 * The processor is started once, before any results, with the metadata of
	 * the matching datum streams as the
	 * {@link StreamDatumFilteredResultsProcessor#METADATA_PROVIDER_ATTR}
	 * attribute. It is started even when there are no results. Each result is
	 * then passed to the processor in turn. The processor is not closed.
	 * </p>
	 *
	 * <p>
	 * The {@code sorts}, {@code offset} and {@code max} arguments are applied
	 * the same way as by {@code findFiltered()}.
	 * </p>
	 *
	 * @param filter
	 *        the search filter
	 * @param processor
	 *        the processor to pass the results to
	 * @param sorts
	 *        the optional sort descriptors
	 * @param offset
	 *        the optional starting offset
	 * @param max
	 *        the optional maximum result count
	 * @throws IllegalArgumentException
	 *         if {@code filter} or {@code processor} is {@code null}
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	void findFilteredStream(NodeDatumFilter filter, StreamDatumFilteredResultsProcessor processor,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max)
			throws IOException;

	/**
	 * Find "reading" results matching a filter and pass them to a processor as
	 * stream datum.
	 *
	 * <p>
	 * This is the SolarQuery reading query: the {@link NodeDatumFilter} reading
	 * type is required, and selects the kind of reading calculated for each
	 * datum stream. Each result is a
	 * {@link net.solarnetwork.domain.datum.ReadingStreamDatum}, such as a
	 * {@link net.solarnetwork.domain.datum.BasicReadingStreamDatum}, whose
	 * accumulating statistics hold the reading difference, starting reading,
	 * and ending reading of each accumulating property. The results are meant
	 * to match those of SolarQuery for the same criteria.
	 * </p>
	 *
	 * <p>
	 * Only the {@link DatumReadingType#Difference} reading type is supported.
	 * With it, the filter aggregation selects the results:
	 * </p>
	 *
	 * <dl>
	 * <dt>no aggregation, or {@link Aggregation#None}</dt>
	 * <dd>One result for each matching stream that has readings for the date
	 * range: the reading difference over the range, found from raw datum the
	 * way SolarQuery finds it for this reading type. The result timestamp and
	 * end timestamp are those of the starting and ending readings used, and the
	 * result has only accumulating properties. A missing start or end date is
	 * taken to be the current time, as SolarQuery does. A historian that no
	 * longer has the raw datum at an end of the range may use readings from its
	 * stored aggregate results instead.</dd>
	 * <dt>{@link Aggregation#Hour}, {@link Aggregation#Day}, or
	 * {@link Aggregation#Month}</dt>
	 * <dd>One result for each matching stream and aggregate period within the
	 * date range, whose timestamp is the start of the period and whose end
	 * timestamp is {@code null}, as the period end is implicit. The
	 * instantaneous, status, and tag properties are those of the aggregate
	 * results of
	 * {@link #findFilteredStream(NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)}
	 * with the same aggregation, and the accumulating statistics are the
	 * readings at the start and end of the period and their difference.</dd>
	 * <dt>any other aggregation</dt>
	 * <dd>Not supported: treated as no aggregation. A historian that does not
	 * support an aggregation, for example because it does not store aggregate
	 * results, also treats it as no aggregation.</dd>
	 * </dl>
	 *
	 * <p>
	 * The other criteria, the metadata the processor is started with, and the
	 * {@code sorts}, {@code offset} and {@code max} arguments are applied the
	 * same way as by
	 * {@link #findFilteredStream(NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)},
	 * except that the partial aggregation criteria are ignored. Location datum
	 * stored by more than one node are given as one result for each stream, not
	 * merged. The processor is started even when there are no results, and is
	 * not closed.
	 * </p>
	 *
	 * @param filter
	 *        the search filter, with a reading type
	 * @param processor
	 *        the processor to pass the results to
	 * @param sorts
	 *        the optional sort descriptors
	 * @param offset
	 *        the optional starting offset
	 * @param max
	 *        the optional maximum result count
	 * @throws IllegalArgumentException
	 *         if {@code filter} or {@code processor} is {@code null}, or
	 *         {@code filter} has no reading type
	 * @throws UnsupportedOperationException
	 *         if the reading type of {@code filter} is not supported, before
	 *         the processor is started
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	void findFilteredStreamReadings(NodeDatumFilter filter,
			StreamDatumFilteredResultsProcessor processor, @Nullable List<SortDescriptor> sorts,
			@Nullable Long offset, @Nullable Integer max) throws IOException;

}
