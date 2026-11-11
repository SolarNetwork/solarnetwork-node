/* ==================================================================
 * JdbcDatumHistorian.java - 10 Nov 2026 3:49:36 pm
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

package net.solarnetwork.node.datumhistorian.jdbc;

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.executeCountQuery;
import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.dao.NodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectFilteredDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.AppendableDatumHistorian;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * JDBC implementation of {@link AppendableDatumHistorian} and
 * {@link FilterableDatumHistorian}.
 *
 * <p>
 * Datum are stored in the tables of a configurable schema, so more than one
 * historian can share a database. Each source has a history made up of all the
 * datum streams with that source ID (normally just one), ordered by timestamp.
 * A datum added with the same source ID and timestamp as an existing datum
 * replaces it.
 * </p>
 *
 * <p>
 * The {@code type} argument of the query methods is applied to the datum found
 * at the requested offset, the same way {@code DefaultDatumService} applies it
 * to its in-memory history: if the datum at that offset is not of the given
 * type, no datum is returned for that source.
 * </p>
 *
 * <p>
 * The database can be shared by more than one node. The {@code DatumHistorian}
 * query methods only return datum of the node given by the
 * {@link IdentityService}. The {@link #findFiltered(NodeDatumFilter, List, Long, Integer)}
 * method does the same when the filter has no object IDs, otherwise it returns
 * the datum of the nodes or locations given. Location datum stored by more than
 * one node are returned once, preferring the one stored by the
 * {@code IdentityService} node; see {@link SelectFilteredDatum} for how each
 * filter criteria is applied. The node datum it returns have their node ID
 * set.
 * </p>
 *
 * <p>
 * Errors adding a datum are logged and otherwise ignored, so a database problem
 * does not stop datum processing. Errors querying the history are thrown as
 * {@link DataAccessException}.
 * </p>
 *
 * <p>
 * Two in-memory caches are used by default, and either can be turned off by
 * setting it to {@code null}. The {@code datumTypeCache} saves looking up the
 * type ID of a datum class each time a datum is added. The
 * {@code datumStreamsCache} saves loading the list of streams each time a
 * source ID pattern query is run; the cached list is removed when a datum is
 * added to a stream that is not in it.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumHistorian extends BasicIdentifiable
		implements AppendableDatumHistorian, FilterableDatumHistorian, ServiceLifecycleObserver {

	/** The default schema name. */
	public static final String DEFAULT_SCHEMA_NAME = "solarhist";

	/** The schema name conventionally used for unfiltered datum. */
	public static final String UNFILTERED_SCHEMA_NAME = "solaruhist";

	private static final Logger log = LoggerFactory.getLogger(JdbcDatumHistorian.class);

	private final JdbcOperations jdbcOps;
	private final String schemaName;
	private final OptionalService<IdentityService> identityService;
	private PathMatcher pathMatcher = defaultPathMatcher();
	private ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper();
	private RowMapper<StoredDatum> storedDatumMapper = new StoredDatumRowMapper(objectMapper);
	private RowMapper<StoredDatum> filteredDatumMapper = new StoredDatumRowMapper(objectMapper, true);
	private NodeDatumFactory datumFactory = new DefaultNodeDatumFactory();
	private @Nullable ConcurrentMap<String, Integer> datumTypeCache = new ConcurrentHashMap<>(8, 0.9f,
			2);
	private @Nullable ConcurrentMap<Long, DatumStreams> datumStreamsCache = new ConcurrentHashMap<>(2,
			0.9f, 2);

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_SCHEMA_NAME} schema will be used.
	 * </p>
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param identityService
	 *        the identity service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcDatumHistorian(JdbcOperations jdbcOps, OptionalService<IdentityService> identityService) {
		this(jdbcOps, DEFAULT_SCHEMA_NAME, identityService);
	}

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param schemaName
	 *        the name of the schema holding the historian tables
	 * @param identityService
	 *        the identity service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code schemaName} is not a
	 *         valid schema name
	 */
	public JdbcDatumHistorian(JdbcOperations jdbcOps, String schemaName,
			OptionalService<IdentityService> identityService) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.schemaName = requireValidSchemaName(schemaName);
		this.identityService = requireNonNullArgument(identityService, "identityService");
		setUid("net.solarnetwork.node.datumhistorian.jdbc." + schemaName);
		setDisplayName("JDBC DatumHistorian [" + schemaName + "]");
	}

	private static PathMatcher defaultPathMatcher() {
		AntPathMatcher m = new AntPathMatcher();
		m.setCachePatterns(true);
		m.setCaseSensitive(false);
		return m;
	}

	@Override
	public void serviceDidStartup() {
		log.info("JDBC datum historian [{}] using DB schema [{}]", getUid(), schemaName);
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	private @Nullable Long nodeId() {
		final IdentityService ident = service(identityService);
		return (ident != null ? ident.getNodeId() : null);
	}

	@Override
	public void add(NodeDatum datum) {
		final Long nodeId = nodeId();
		if ( nodeId == null ) {
			// node not set up, cannot persist
			return;
		}
		if ( datum == null || datum.getSourceId() == null || datum.getTimestamp() == null ) {
			return;
		}
		final Class<?> datumClass = datum.getClass();
		final String className = datumClass.getName();
		final ConcurrentMap<String, Integer> typeCache = this.datumTypeCache;
		final Integer cachedTypeId = (typeCache != null ? typeCache.get(className) : null);
		try {
			final String jdata = samplesJson(datum);
			final StoreDatum sql = (cachedTypeId != null
					? new StoreDatum(schemaName, nodeId, datum, cachedTypeId, jdata)
					: new StoreDatum(schemaName, nodeId, datum, datumFactory.typeNames(datumClass),
							jdata));
			final List<StoreDatum.Result> results = jdbcOps.query(sql, StoreDatum.RESULT_MAPPER);
			if ( results.isEmpty() ) {
				return;
			}
			final StoreDatum.Result result = results.get(0);
			if ( typeCache != null && cachedTypeId == null ) {
				typeCache.put(className, result.typeId());
			}
			datumStored(nodeId, result.streamId());
		} catch ( DataAccessException | JsonProcessingException e ) {
			if ( typeCache != null && cachedTypeId != null ) {
				// in case the cached type ID is no longer valid
				typeCache.remove(className, cachedTypeId);
			}
			log.warn("Error adding datum {} to history [{}]: {}", datum.datumId(), schemaName,
					e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The pagination criteria of {@code filter} are normally used. Any
	 * {@code sorts}, {@code offset}, or {@code max} argument that is not
	 * {@code null} is used in place of the same {@code filter} criteria.
	 * </p>
	 *
	 * <p>
	 * The total result count is only counted when a maximum result count is
	 * given and {@code filter} does not have
	 * {@link NodeDatumFilter#isWithoutTotalResultsCount()} set. Without a
	 * maximum result count, the total is the number of results returned.
	 * </p>
	 *
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	@Override
	public FilterResults<NodeDatum, DatumId> findFiltered(NodeDatumFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		requireNonNullArgument(filter, "filter");
		NodeDatumFilter f = filter;
		if ( sorts != null || offset != null || max != null ) {
			final BasicNodeDatumFilter copy = BasicNodeDatumFilter.copyOf(filter);
			if ( sorts != null ) {
				copy.setSorts(sorts);
			}
			if ( offset != null ) {
				copy.setOffset(offset);
			}
			if ( max != null ) {
				copy.setMax(max);
			}
			f = copy;
		}
		final long startingOffset = (f.hasOffset() ? f.offset() : 0L);

		final Long[] objectIds = f.getObjectIds();
		final Long nodeId = nodeId();
		if ( nodeId == null && (objectIds == null || objectIds.length < 1) ) {
			// node not set up, so there is no node to find the datum of
			return new BasicFilterResults<>(List.of(), 0L, startingOffset, 0);
		}

		final SelectFilteredDatum sql = new SelectFilteredDatum(schemaName, f, nodeId);
		Long totalResults = null;
		if ( f.getMax() != null && !f.isWithoutTotalResultsCount() ) {
			totalResults = executeCountQuery(jdbcOps, sql.countPreparedStatementCreator());
		}
		final List<NodeDatum> results = datum(jdbcOps.query(sql, filteredDatumMapper), null);
		if ( f.getMax() == null ) {
			// all results from the offset were returned; the same as CommonJdbcUtils.executeFilterQuery()
			if ( startingOffset < 1 ) {
				totalResults = (long) results.size();
			} else if ( !results.isEmpty() ) {
				totalResults = startingOffset + results.size();
			}
		}
		return new BasicFilterResults<>(results, totalResults, startingOffset, results.size());
	}

	private String samplesJson(NodeDatum datum) throws JsonProcessingException {
		final DatumSamplesOperations ops = datum.asSampleOperations();
		final Map<String, Object> m = new LinkedHashMap<>(4);
		if ( ops != null ) {
			putSampleData(m, "i", ops.getSampleData(DatumSamplesType.Instantaneous));
			putSampleData(m, "a", ops.getSampleData(DatumSamplesType.Accumulating));
			putSampleData(m, "s", ops.getSampleData(DatumSamplesType.Status));
			final Set<String> tags = ops.getTags();
			if ( tags != null && !tags.isEmpty() ) {
				m.put("t", tags);
			}
		}
		return objectMapper.writeValueAsString(m);
	}

	private static void putSampleData(Map<String, Object> m, String key, @Nullable Map<String, ?> data) {
		if ( data != null && !data.isEmpty() ) {
			m.put(key, data);
		}
	}

	private void datumStored(long nodeId, int streamId) {
		final ConcurrentMap<Long, DatumStreams> cache = this.datumStreamsCache;
		if ( cache == null ) {
			return;
		}
		final DatumStreams streams = cache.get(nodeId);
		if ( streams != null && !streams.contains(streamId) ) {
			cache.remove(nodeId, streams);
		}
	}

	/**
	 * Get all the datum streams of a node.
	 *
	 * @param nodeId
	 *        the node ID
	 * @return the streams
	 */
	private DatumStreams datumStreams(long nodeId) {
		final ConcurrentMap<Long, DatumStreams> cache = this.datumStreamsCache;
		if ( cache != null ) {
			final DatumStreams cached = cache.get(nodeId);
			if ( cached != null ) {
				return cached;
			}
		}
		final DatumStreams result = new DatumStreams(jdbcOps
				.query(new SelectDatumStreams(schemaName, nodeId), DatumStreamInfoRowMapper.INSTANCE));
		if ( cache != null ) {
			cache.put(nodeId, result);
		}
		return result;
	}

	@Override
	public <T extends NodeDatum> Collection<T> latest(@Nullable Set<String> sourceIdFilter,
			@Nullable Class<T> type) {
		return offset(sourceIdFilter, 0, type);
	}

	@Override
	public <T extends NodeDatum> @Nullable T latest(String sourceId, @Nullable Class<T> type) {
		return offset(sourceId, 0, type);
	}

	@Override
	public <T extends NodeDatum> Collection<T> offset(@Nullable Set<String> sourceIdFilter, int offset,
			@Nullable Class<T> type) {
		return findOffset(sourceIdFilter, null, offset, type);
	}

	@Override
	public <T extends NodeDatum> @Nullable T offset(String sourceId, int offset,
			@Nullable Class<T> type) {
		return first(findSource(sourceId, null, offset, 1, type));
	}

	@Override
	public <T extends NodeDatum> Collection<T> offset(@Nullable Set<String> sourceIdFilter,
			Instant timestamp, int offset, @Nullable Class<T> type) {
		return findOffset(sourceIdFilter, timestamp, offset, type);
	}

	@Override
	public <T extends NodeDatum> @Nullable T offset(String sourceId, Instant timestamp, int offset,
			@Nullable Class<T> type) {
		return first(findSource(sourceId, timestamp, offset, 1, type));
	}

	@Override
	public <T extends NodeDatum> Collection<T> slice(String sourceId, int offset, int count,
			@Nullable Class<T> type) {
		return oldestFirst(findSource(sourceId, null, offset, count, type));
	}

	@Override
	public <T extends NodeDatum> Collection<T> slice(String sourceId, Instant timestamp, int offset,
			int count, @Nullable Class<T> type) {
		return oldestFirst(findSource(sourceId, timestamp, offset, count, type));
	}

	private static <T> @Nullable T first(List<T> list) {
		return (list.isEmpty() ? null : list.get(0));
	}

	private static <T> List<T> oldestFirst(List<T> list) {
		Collections.reverse(list);
		return list;
	}

	private static @Nullable String typeNameFilter(@Nullable Class<?> type) {
		// every stored type is a NodeDatum, so there is nothing to filter for that
		return (type != null && type != NodeDatum.class ? type.getName() : null);
	}

	/**
	 * Find datum of a single source, newest first.
	 */
	private <T extends NodeDatum> List<T> findSource(@Nullable String sourceId,
			@Nullable Instant timestamp, int offset, int count, @Nullable Class<T> type) {
		final Long nodeId = nodeId();
		if ( nodeId == null || sourceId == null || offset < 0 || count < 1 ) {
			return new ArrayList<>(0);
		}
		return datum(jdbcOps.query(SelectDatum.forSource(schemaName, nodeId, sourceId, timestamp, offset,
				count, typeNameFilter(type)), storedDatumMapper), type);
	}

	/**
	 * Find one datum for each source matching a source ID filter.
	 */
	private <T extends NodeDatum> List<T> findOffset(@Nullable Set<String> sourceIdFilter,
			@Nullable Instant timestamp, int offset, @Nullable Class<T> type) {
		final Long nodeId = nodeId();
		if ( nodeId == null || offset < 0 ) {
			return new ArrayList<>(0);
		}
		List<Integer> streamIds = null;
		if ( sourceIdFilter != null && !sourceIdFilter.isEmpty() ) {
			streamIds = matchingStreamIds(nodeId, sourceIdFilter);
			if ( streamIds.isEmpty() ) {
				return new ArrayList<>(0);
			}
		}
		return datum(jdbcOps.query(SelectDatum.forStreams(schemaName, nodeId, streamIds, timestamp,
				offset, typeNameFilter(type)), storedDatumMapper), type);
	}

	private List<Integer> matchingStreamIds(long nodeId, Set<String> sourceIdFilter) {
		final List<Integer> result = new ArrayList<>(8);
		for ( DatumStreamInfo stream : datumStreams(nodeId).streams() ) {
			for ( String pattern : sourceIdFilter ) {
				if ( pattern != null && pathMatcher.match(pattern, stream.sourceId()) ) {
					result.add(stream.streamId());
					break;
				}
			}
		}
		return result;
	}

	private <T extends NodeDatum> List<T> datum(List<StoredDatum> stored, @Nullable Class<T> type) {
		final List<T> result = new ArrayList<>(stored.size());
		for ( StoredDatum d : stored ) {
			if ( !d.isAssignableTo(type) ) {
				continue;
			}
			final T datum = datumFactory.createDatum(d, type);
			if ( datum != null ) {
				result.add(datum);
			}
		}
		return result;
	}

	/**
	 * Get the schema name.
	 *
	 * @return the schema name
	 */
	public final String getSchemaName() {
		return schemaName;
	}

	/**
	 * Get the path matcher.
	 *
	 * @return the path matcher
	 */
	public final PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Set the path matcher to match source ID patterns with.
	 *
	 * <p>
	 * The default is a case-insensitive {@link AntPathMatcher}, as used by the
	 * SolarNode datum service.
	 * </p>
	 *
	 * @param pathMatcher
	 *        the path matcher to set
	 * @throws IllegalArgumentException
	 *         if {@code pathMatcher} is {@code null}
	 */
	public final void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = requireNonNullArgument(pathMatcher, "pathMatcher");
	}

	/**
	 * Get the object mapper.
	 *
	 * @return the object mapper
	 */
	public final ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the object mapper to encode and decode datum samples with.
	 *
	 * @param objectMapper
	 *        the object mapper to set
	 * @throws IllegalArgumentException
	 *         if {@code objectMapper} is {@code null}
	 */
	public final void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.storedDatumMapper = new StoredDatumRowMapper(objectMapper);
		this.filteredDatumMapper = new StoredDatumRowMapper(objectMapper, true);
	}

	/**
	 * Get the datum factory.
	 *
	 * @return the datum factory
	 */
	public final NodeDatumFactory getDatumFactory() {
		return datumFactory;
	}

	/**
	 * Set the factory to create datum instances from stored datum with.
	 *
	 * @param datumFactory
	 *        the factory to set
	 * @throws IllegalArgumentException
	 *         if {@code datumFactory} is {@code null}
	 */
	public final void setDatumFactory(NodeDatumFactory datumFactory) {
		this.datumFactory = requireNonNullArgument(datumFactory, "datumFactory");
	}

	/**
	 * Get the datum type cache.
	 *
	 * @return the cache, or {@code null}
	 */
	public final @Nullable ConcurrentMap<String, Integer> getDatumTypeCache() {
		return datumTypeCache;
	}

	/**
	 * Set the datum type cache.
	 *
	 * <p>
	 * This cache maps datum class names to their type ID. Type IDs are specific
	 * to a schema, so historians using different schemas must not share the
	 * same cache. A new {@link ConcurrentHashMap} is used by default.
	 * </p>
	 *
	 * @param datumTypeCache
	 *        the cache to set, or {@code null} to not cache datum types
	 */
	public final void setDatumTypeCache(@Nullable ConcurrentMap<String, Integer> datumTypeCache) {
		this.datumTypeCache = datumTypeCache;
	}

	/**
	 * Get the datum streams cache.
	 *
	 * @return the cache, or {@code null}
	 */
	public final @Nullable ConcurrentMap<Long, DatumStreams> getDatumStreamsCache() {
		return datumStreamsCache;
	}

	/**
	 * Set the datum streams cache.
	 *
	 * <p>
	 * This cache maps node IDs to all the datum streams of that node. Streams
	 * are specific to a schema, so historians using different schemas must not
	 * share the same cache. A new {@link ConcurrentHashMap} is used by default.
	 * </p>
	 *
	 * @param datumStreamsCache
	 *        the cache to set, or {@code null} to not cache datum streams
	 */
	public final void setDatumStreamsCache(
			@Nullable ConcurrentMap<Long, DatumStreams> datumStreamsCache) {
		this.datumStreamsCache = datumStreamsCache;
	}

}
