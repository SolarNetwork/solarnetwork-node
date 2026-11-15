/* ==================================================================
 * JdbcDatumHistorianUnitTests.java - 11 Nov 2026 12:41:58 am
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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.BasicAggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicReadingStreamDatum;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfo;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamMetadataRowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.DefaultNodeDatumFactory;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectAggregateDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreamMetadata;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectFilteredDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectHistorianFeatures;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreNodeTimeZone;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcDatumHistorian} class, without a database.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings({ "static-access", "unchecked" })
@RunWith(MockitoJUnitRunner.class)
public class JdbcDatumHistorianUnitTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;

	@Mock
	private JdbcOperations jdbcOps;

	@Mock
	private IdentityService identityService;

	@Mock
	private StreamDatumFilteredResultsProcessor processor;

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private ResultSet rs;

	@Mock
	private PlatformTransactionManager txManager;

	@Mock
	private TransactionStatus txStatus;

	@Captor
	private ArgumentCaptor<PreparedStatementCreator> sqlCaptor;

	@Captor
	private ArgumentCaptor<RowMapper<ObjectDatumStreamMetadata>> metaMapperCaptor;

	@Captor
	private ArgumentCaptor<Map<String, ?>> attrsCaptor;

	@Captor
	private ArgumentCaptor<StreamDatum> datumCaptor;

	private JdbcDatumHistorian historian;
	private ConcurrentMap<String, Integer> datumTypeCache;
	private ConcurrentMap<Long, DatumStreams> datumStreamsCache;

	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	@Before
	public void setup() {
		historian = new JdbcDatumHistorian(jdbcOps, new StaticOptionalService<>(identityService));
		datumTypeCache = new ConcurrentHashMap<>();
		datumStreamsCache = new ConcurrentHashMap<>();
	}

	private SimpleDatum datum(String sourceId) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 1);
		s.putAccumulatingSampleValue("wattHours", 2);
		s.putStatusSampleValue("mode", "x");
		s.addTag("t");
		return SimpleDatum.nodeDatum(sourceId, ts, s);
	}

	@Test
	public void construct_defaults() {
		// @formatter:off
		and.then(historian.getSchemaName())
			.as("Default schema")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME)
			;
		and.then(historian.getPathMatcher())
			.as("Default path matcher")
			.isInstanceOf(AntPathMatcher.class)
			.matches(m -> m.match("A/*", "a/b"), "is case-insensitive")
			;
		and.then(historian.getObjectMapper()).as("Default object mapper").isNotNull();
		and.then(historian.getDatumFactory()).as("Default datum factory").isNotNull();
		and.then(historian.getDatumTypeCache()).as("Default type cache").isNotNull().isEmpty();
		and.then(historian.getDatumStreamsCache()).as("Default streams cache").isNotNull().isEmpty();
		and.then(historian.getFetchSize())
			.as("Default fetch size")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_FETCH_SIZE)
			;
		// @formatter:on
	}

	@Test
	public void construct_invalid() {
		final var ident = new StaticOptionalService<>(identityService);
		thenIllegalArgumentException().isThrownBy(() -> new JdbcDatumHistorian(null, ident));
		thenIllegalArgumentException().isThrownBy(() -> new JdbcDatumHistorian(jdbcOps, null));
		thenIllegalArgumentException()
				.isThrownBy(() -> new JdbcDatumHistorian(jdbcOps, "solar.hist", ident));
		thenIllegalArgumentException().isThrownBy(() -> historian.setPathMatcher(null));
		thenIllegalArgumentException().isThrownBy(() -> historian.setObjectMapper(null));
		thenIllegalArgumentException().isThrownBy(() -> historian.setDatumFactory(null));
	}

	@Test
	public void setters() {
		// GIVEN
		AntPathMatcher pm = new AntPathMatcher();
		ObjectMapper om = new ObjectMapper();
		DefaultNodeDatumFactory f = new DefaultNodeDatumFactory();

		// WHEN
		historian.setPathMatcher(pm);
		historian.setObjectMapper(om);
		historian.setDatumFactory(f);
		historian.setDatumTypeCache(datumTypeCache);
		historian.setDatumStreamsCache(datumStreamsCache);
		historian.setFetchSize(5);

		// THEN
		// @formatter:off
		and.then(historian.getPathMatcher()).isSameAs(pm);
		and.then(historian.getObjectMapper()).isSameAs(om);
		and.then(historian.getDatumFactory()).isSameAs(f);
		and.then(historian.getDatumTypeCache()).isSameAs(datumTypeCache);
		and.then(historian.getDatumStreamsCache()).isSameAs(datumStreamsCache);
		and.then(historian.getFetchSize()).isEqualTo(5);
		// @formatter:on
	}

	@Test
	public void setters_cachesDisabled() {
		// WHEN
		historian.setDatumTypeCache(null);
		historian.setDatumStreamsCache(null);

		// THEN
		// @formatter:off
		and.then(historian.getDatumTypeCache()).as("Type cache disabled").isNull();
		and.then(historian.getDatumStreamsCache()).as("Streams cache disabled").isNull();
		// @formatter:on
	}

	@Test
	public void add_noNodeId() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.add(datum("a"));

		// THEN
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void add_noIdentityService() {
		// GIVEN
		historian = new JdbcDatumHistorian(jdbcOps, new StaticOptionalService<>(null));

		// WHEN
		historian.add(datum("a"));

		// THEN
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void add_storeSql() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(sqlCaptor.capture(), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));

		// WHEN
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getValue())
			.as("StoreDatum used")
			.asInstanceOf(type(StoreDatum.class))
			.as("Default schema used")
			.returns(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME, StoreDatum::getSchemaName)
			.as("Type registered with datum")
			.satisfies(s -> and.then(s.getSql()).contains("dh_store_datum_type"))
			;
		// @formatter:on
	}

	@Test
	public void add_error_ignored() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), same(StoreDatum.RESULT_MAPPER)))
				.willThrow(new QueryTimeoutException("Too slow"));

		// WHEN
		historian.add(datum("a"));

		// THEN
		// no exception thrown
	}

	@Test
	public void add_error_cachedTypeRemoved() {
		// GIVEN
		historian.setDatumTypeCache(datumTypeCache);
		datumTypeCache.put(SimpleDatum.class.getName(), 1);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(sqlCaptor.capture(), same(StoreDatum.RESULT_MAPPER)))
				.willThrow(new QueryTimeoutException("Too slow"));

		// WHEN
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		and.then(datumTypeCache)
			.as("Cached type ID removed after error")
			.isEmpty()
			;
		and.then(sqlCaptor.getValue())
			.asInstanceOf(type(StoreDatum.class))
			.as("Cached type ID used")
			.satisfies(s -> and.then(s.getSql()).doesNotContain("dh_store_datum_type"))
			;
		// @formatter:on
	}

	@Test
	public void add_noResult() {
		// GIVEN
		historian.setDatumTypeCache(datumTypeCache);
		historian.setDatumStreamsCache(datumStreamsCache);
		final DatumStreams streams = new DatumStreams(List.of(new DatumStreamInfo(1, "b", null)));
		datumStreamsCache.put(TEST_NODE_ID, streams);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of());

		// WHEN
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		and.then(datumTypeCache)
			.as("Type not cached without result")
			.isEmpty()
			;
		and.then(datumStreamsCache)
			.as("Cached streams kept without result")
			.containsEntry(TEST_NODE_ID, streams)
			;
		// @formatter:on
	}

	@Test
	public void add_typeCached_used() {
		// GIVEN
		historian.setDatumTypeCache(datumTypeCache);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(sqlCaptor.capture(), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));

		// WHEN
		historian.add(datum("a"));
		historian.add(datum("b"));

		// THEN
		final List<PreparedStatementCreator> sqls = sqlCaptor.getAllValues();
		// @formatter:off
		and.then(datumTypeCache)
			.as("Type ID cached from first result")
			.hasSize(1)
			.containsEntry(SimpleDatum.class.getName(), 1)
			;
		and.then(sqls)
			.as("Datum stored twice")
			.hasSize(2)
			;
		and.then(sqls.get(0))
			.asInstanceOf(type(StoreDatum.class))
			.as("Type registered with first datum")
			.satisfies(s -> and.then(s.getSql()).contains("dh_store_datum_type"))
			;
		and.then(sqls.get(1))
			.asInstanceOf(type(StoreDatum.class))
			.as("Cached type ID used for second datum")
			.satisfies(s -> and.then(s.getSql()).doesNotContain("dh_store_datum_type"))
			;
		// @formatter:on
	}

	@Test
	public void add_typeCacheDisabled() {
		// GIVEN
		historian.setDatumTypeCache(null);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(sqlCaptor.capture(), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));

		// WHEN
		historian.add(datum("a"));
		historian.add(datum("b"));

		// THEN
		// @formatter:off
		and.then(sqlCaptor.getAllValues())
			.as("Datum stored twice")
			.hasSize(2)
			.allSatisfy(c -> and.then(c)
				.asInstanceOf(type(StoreDatum.class))
				.as("Type registered with every datum without type cache")
				.satisfies(s -> and.then(s.getSql()).contains("dh_store_datum_type")))
			;
		// @formatter:on
	}

	@Test
	public void add_typeCached_streamEvicted() {
		// GIVEN
		historian.setDatumTypeCache(datumTypeCache);
		historian.setDatumStreamsCache(datumStreamsCache);
		datumStreamsCache.put(TEST_NODE_ID,
				new DatumStreams(List.of(new DatumStreamInfo(1, "b", null))));
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));

		// WHEN
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		and.then(datumTypeCache)
			.as("Type ID cached")
			.hasSize(1)
			.containsEntry(SimpleDatum.class.getName(), 1)
			;
		and.then(datumStreamsCache)
			.as("Cached streams removed when datum added to stream not in cache")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void add_streamCached_kept() {
		// GIVEN
		historian.setDatumStreamsCache(datumStreamsCache);
		final DatumStreams streams = new DatumStreams(List.of(new DatumStreamInfo(2, "a", null)));
		datumStreamsCache.put(TEST_NODE_ID, streams);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));

		// WHEN
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		and.then(datumStreamsCache)
			.as("Cached streams kept when datum added to stream in cache")
			.containsEntry(TEST_NODE_ID, streams)
			;
		// @formatter:on
	}

	@Test
	public void query_noNodeId() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		// @formatter:off
		and.then(historian.latest(emptySet(), NodeDatum.class)).isEmpty();
		and.then(historian.latest("a", NodeDatum.class)).isNull();
		and.then(historian.offset(singleton("a"), 1, NodeDatum.class)).isEmpty();
		and.then(historian.offset("a", 1, NodeDatum.class)).isNull();
		and.then(historian.offset(emptySet(), ts, 1, NodeDatum.class)).isEmpty();
		and.then(historian.offset("a", ts, 1, NodeDatum.class)).isNull();
		and.then(historian.slice("a", 0, 1, NodeDatum.class)).isEmpty();
		and.then(historian.slice("a", ts, 0, 1, NodeDatum.class)).isEmpty();
		// @formatter:on

		// THEN
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void query_invalidArguments() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);

		// WHEN
		// @formatter:off
		and.then(historian.offset(emptySet(), -1, NodeDatum.class)).isEmpty();
		and.then(historian.offset("a", -1, NodeDatum.class)).isNull();
		and.then(historian.slice("a", 0, 0, NodeDatum.class)).isEmpty();
		and.then(historian.slice("a", -1, 1, NodeDatum.class)).isEmpty();
		and.then(historian.latest((String) null, NodeDatum.class)).isNull();
		// @formatter:on

		// THEN
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void query_patternNoMatch() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatumStreams.class), any(RowMapper.class)))
				.willReturn(List.of(new DatumStreamInfo(1, "a", null)));

		// WHEN
		var result = historian.latest(singleton("b*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum when no stream matches")
			.isEmpty()
			;
		then(jdbcOps).should(never()).query(any(SelectDatum.class), any(RowMapper.class));
		// @formatter:on
	}

	@Test
	public void query_streamsCached() {
		// GIVEN
		historian.setDatumStreamsCache(datumStreamsCache);
		final DatumStreams streams = new DatumStreams(List.of(new DatumStreamInfo(1, "a", null)));
		datumStreamsCache.put(TEST_NODE_ID, streams);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class))).willReturn(List.of());

		// WHEN
		var result = historian.latest(singleton("a"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result).isEmpty();
		then(jdbcOps).should(never()).query(any(SelectDatumStreams.class), any(RowMapper.class));
		and.then(datumStreamsCache.get(TEST_NODE_ID))
			.as("Cached streams kept")
			.isSameAs(streams)
			;
		// @formatter:on
	}

	@Test
	public void query_streamsLoadedOnce() {
		// GIVEN
		historian.setDatumStreamsCache(datumStreamsCache);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatumStreams.class), any(RowMapper.class)))
				.willReturn(List.of(new DatumStreamInfo(1, "a", null)));
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class))).willReturn(List.of());

		// WHEN
		historian.latest(singleton("a"), NodeDatum.class);
		historian.latest(singleton("a*"), NodeDatum.class);

		// THEN
		// @formatter:off
		then(jdbcOps).should(times(1)).query(any(SelectDatumStreams.class), any(RowMapper.class));
		then(jdbcOps).should(times(2)).query(any(SelectDatum.class), any(RowMapper.class));
		and.then(datumStreamsCache)
			.as("Streams cached by node ID")
			.containsOnlyKeys(TEST_NODE_ID)
			;
		// @formatter:on
	}

	@Test
	public void query_streamsCacheDisabled() {
		// GIVEN
		historian.setDatumStreamsCache(null);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatumStreams.class), any(RowMapper.class)))
				.willReturn(List.of(new DatumStreamInfo(1, "a", null)));
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class))).willReturn(List.of());

		// WHEN
		historian.latest(singleton("a"), NodeDatum.class);
		historian.latest(singleton("a*"), NodeDatum.class);

		// THEN
		then(jdbcOps).should(times(2)).query(any(SelectDatumStreams.class), any(RowMapper.class));
	}

	@Test
	public void query_error_thrown() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class)))
				.willThrow(new DataRetrievalFailureException("Oops"));

		// THEN
		thenExceptionOfType(DataRetrievalFailureException.class)
				.isThrownBy(() -> historian.latest("a", NodeDatum.class));
	}

	@Test
	public void lifecycle() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.serviceDidStartup();
		historian.serviceDidShutdown();

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), same(SelectHistorianFeatures.RESULT_MAPPER));
		and.then(sqlCaptor.getValue())
			.as("Features of schema found at startup")
			.asInstanceOf(type(SelectHistorianFeatures.class))
			.as("Default schema used")
			.returns(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME, SelectHistorianFeatures::getSchemaName)
			;
		// No time zone stored without node ID
		then(jdbcOps).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Features and time zone
	 * ==================================================================
	 */

	private void givenFeatures(boolean aggregates) {
		givenFeatures(aggregates, aggregates);
	}

	private void givenFeatures(boolean aggregates, boolean minuteAggregates) {
		given(jdbcOps.query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER))).willReturn(
						List.of(new SelectHistorianFeatures.Result(aggregates, minuteAggregates)));
	}

	private void givenStoreDatum() {
		given(jdbcOps.query(any(StoreDatum.class), same(StoreDatum.RESULT_MAPPER)))
				.willReturn(List.of(new StoreDatum.Result(1, 2)));
	}

	@Test
	public void startup_aggregates_timeZoneStored() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.update(any(StoreNodeTimeZone.class))).willReturn(1);
		historian.setTimeZone(ZoneId.of("Pacific/Auckland"));

		// WHEN
		historian.serviceDidStartup();

		// THEN
		// @formatter:off
		final InOrder ordered = inOrder(jdbcOps);
		then(jdbcOps).should(ordered).query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).should(ordered).update(sqlCaptor.capture());
		then(jdbcOps).shouldHaveNoMoreInteractions();
		and.then(sqlCaptor.getValue())
			.as("Time zone stored")
			.asInstanceOf(type(StoreNodeTimeZone.class))
			.as("Default schema used")
			.returns(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME, StoreNodeTimeZone::getSchemaName)
			.as("Node ID of identity service")
			.returns(TEST_NODE_ID, StoreNodeTimeZone::getNodeId)
			.as("Configured time zone")
			.returns(ZoneId.of("Pacific/Auckland"), StoreNodeTimeZone::getTimeZone)
			;
		// @formatter:on
	}

	@Test
	public void startup_aggregates_systemTimeZone() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.update(any(StoreNodeTimeZone.class))).willReturn(0);

		// WHEN
		historian.serviceDidStartup();

		// THEN
		// @formatter:off
		then(jdbcOps).should().update(sqlCaptor.capture());
		and.then(historian.getTimeZone())
			.as("No time zone configured")
			.isNull()
			;
		and.then(sqlCaptor.getValue())
			.as("Time zone stored")
			.asInstanceOf(type(StoreNodeTimeZone.class))
			.as("System time zone used")
			.returns(ZoneId.systemDefault(), StoreNodeTimeZone::getTimeZone)
			;
		// @formatter:on
	}

	@Test
	public void startup_noAggregates_timeZoneStored() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(false);
		given(jdbcOps.update(any(StoreNodeTimeZone.class))).willReturn(1);
		givenStoreDatum();
		historian.setTimeZone(ZoneId.of("Pacific/Auckland"));

		// WHEN
		historian.serviceDidStartup();
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		final InOrder ordered = inOrder(jdbcOps);
		then(jdbcOps).should(ordered).query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).should(ordered).update(sqlCaptor.capture());
		then(jdbcOps).should(ordered).query(any(StoreDatum.class), same(StoreDatum.RESULT_MAPPER));
		// Time zone stored once without stored aggregates too
		then(jdbcOps).shouldHaveNoMoreInteractions();
		and.then(sqlCaptor.getValue())
			.as("Time zone stored")
			.asInstanceOf(type(StoreNodeTimeZone.class))
			.as("Node ID of identity service")
			.returns(TEST_NODE_ID, StoreNodeTimeZone::getNodeId)
			.as("Configured time zone")
			.returns(ZoneId.of("Pacific/Auckland"), StoreNodeTimeZone::getTimeZone)
			;
		// @formatter:on
	}

	@Test
	public void startup_noNodeId_timeZoneStoredOnceWhenAdding() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null, TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.update(any(StoreNodeTimeZone.class))).willReturn(1);
		givenStoreDatum();
		historian.setTimeZone(ZoneId.of("Pacific/Auckland"));

		// WHEN
		historian.serviceDidStartup();
		historian.add(datum("a"));
		historian.add(datum("b"));

		// THEN
		// @formatter:off
		final InOrder ordered = inOrder(jdbcOps);
		then(jdbcOps).should(ordered).query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).should(ordered).update(sqlCaptor.capture());
		then(jdbcOps).should(ordered, times(2)).query(any(StoreDatum.class),
				same(StoreDatum.RESULT_MAPPER));
		// Time zone stored once, with first datum once node ID known
		then(jdbcOps).shouldHaveNoMoreInteractions();
		and.then(sqlCaptor.getValue())
			.as("Time zone stored")
			.asInstanceOf(type(StoreNodeTimeZone.class))
			.as("Node ID of identity service when adding")
			.returns(TEST_NODE_ID, StoreNodeTimeZone::getNodeId)
			.as("Configured time zone")
			.returns(ZoneId.of("Pacific/Auckland"), StoreNodeTimeZone::getTimeZone)
			;
		// @formatter:on
	}

	@Test
	public void startup_nodeIdChanged_timeZoneStoredWhenAdding() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID, TEST_NODE_ID_2);
		givenFeatures(true);
		given(jdbcOps.update(any(StoreNodeTimeZone.class))).willReturn(1);
		givenStoreDatum();

		// WHEN
		historian.serviceDidStartup();
		historian.add(datum("a"));
		historian.add(datum("b"));

		// THEN
		// @formatter:off
		then(jdbcOps).should(times(2)).update(sqlCaptor.capture());
		and.then(sqlCaptor.getAllValues())
			.as("Time zone stored for node at startup, then for new node ID when adding")
			.extracting(s -> ((StoreNodeTimeZone) s).getNodeId())
			.containsExactly(TEST_NODE_ID, TEST_NODE_ID_2)
			;
		// @formatter:on
	}

	@Test
	public void add_notStarted_timeZoneStoredOnce() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenStoreDatum();

		// WHEN
		historian.add(datum("a"));
		historian.add(datum("b"));

		// THEN
		// @formatter:off
		final InOrder ordered = inOrder(jdbcOps);
		then(jdbcOps).should(ordered).update(sqlCaptor.capture());
		then(jdbcOps).should(ordered, times(2)).query(any(StoreDatum.class),
				same(StoreDatum.RESULT_MAPPER));
		// Time zone stored once, with first datum, without finding the features
		then(jdbcOps).shouldHaveNoMoreInteractions();
		and.then(sqlCaptor.getValue())
			.as("Time zone stored")
			.asInstanceOf(type(StoreNodeTimeZone.class))
			.as("Node ID of identity service")
			.returns(TEST_NODE_ID, StoreNodeTimeZone::getNodeId)
			.as("System time zone")
			.returns(ZoneId.systemDefault(), StoreNodeTimeZone::getTimeZone)
			;
		// @formatter:on
	}

	@Test
	public void startup_featuresError_ignored_foundWhenNeeded() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		given(jdbcOps.query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER)))
						.willThrow(new QueryTimeoutException("Too slow"))
						.willReturn(List.of(new SelectHistorianFeatures.Result(true, true)));
		final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setAggregation(Aggregation.Hour);

		// WHEN
		historian.serviceDidStartup();
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		// @formatter:off
		then(jdbcOps).should(times(2)).query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		// No time zone stored at startup without features, only with the first datum added
		then(jdbcOps).should(never()).update(any(PreparedStatementCreator.class));
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Stored aggregates queried once features found")
			.isInstanceOf(SelectAggregateDatum.class)
			;
		// @formatter:on
	}

	@Test
	public void startup_timeZoneError_ignored() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.update(any(StoreNodeTimeZone.class)))
				.willThrow(new QueryTimeoutException("Too slow"));
		givenStoreDatum();

		// WHEN
		historian.serviceDidStartup();
		historian.add(datum("a"));

		// THEN
		// @formatter:off
		// Error not thrown, and storing not tried again when adding
		then(jdbcOps).should(times(1)).update(any(PreparedStatementCreator.class));
		then(jdbcOps).should().query(any(StoreDatum.class), same(StoreDatum.RESULT_MAPPER));
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * findFiltered
	 * ==================================================================
	 */

	@Test
	public void findFiltered_noNodeId_noQuery() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(new BasicNodeDatumFilter());

		// THEN
		// @formatter:off
		and.then(result)
			.as("No results without node ID")
			.isEmpty()
			;
		verifyNoInteractions(jdbcOps);
		// @formatter:on
	}

	@Test
	public void findFiltered_withoutTotalResultsCount_noCountQuery() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMax(1);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		then(jdbcOps).should(never()).query(any(PreparedStatementCreator.class),
				any(ResultSetExtractor.class));
		and.then(sqlCaptor.getValue())
			.as("Filtered datum query executed")
			.isInstanceOf(SelectFilteredDatum.class)
			;
		and.then(result.getTotalResults())
			.as("No total results")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_totalResultsCount() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
				.willReturn(42L);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMax(1);
		filter.setOffset(2L);
		filter.setWithoutTotalResultsCount(false);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(result.getTotalResults())
			.as("Total results from count query")
			.isEqualTo(42L)
			;
		and.then(result.getStartingOffset())
			.as("Starting offset from filter")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

	/*
	 * ================================================================== Stream
	 * settings
	 * ==================================================================
	 */

	@Test
	public void streamSettings_defaults() {
		// @formatter:off
		and.then(historian.getStreamIdDomain())
			.as("Default stream ID domain")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_STREAM_ID_DOMAIN)
			;
		and.then(historian.getTransactionTemplate())
			.as("No transaction template by default")
			.isNull()
			;
		and.then(historian.getMaxMinuteAggregationHours())
			.as("Default minute aggregation limit is five weeks, as in SolarNet")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_MAX_MINUTE_AGGREGATION_HOURS)
			.isEqualTo(840)
			;
		// @formatter:on
	}

	@Test
	public void maxMinuteAggregationHours_setter() {
		// WHEN
		historian.setMaxMinuteAggregationHours(24);

		// THEN
		and.then(historian.getMaxMinuteAggregationHours()).isEqualTo(24);
	}

	@Test
	public void streamSettings_setters() {
		// GIVEN
		final TransactionTemplate tt = new TransactionTemplate(txManager);

		// WHEN
		historian.setStreamIdDomain(255);
		historian.setTransactionTemplate(tt);

		// THEN
		// @formatter:off
		and.then(historian.getStreamIdDomain()).isEqualTo(255);
		and.then(historian.getTransactionTemplate()).isSameAs(tt);
		// @formatter:on

		// WHEN
		historian.setStreamIdDomain(JdbcDatumHistorian.UNFILTERED_STREAM_ID_DOMAIN);
		historian.setTransactionTemplate(null);

		// THEN
		// @formatter:off
		and.then(historian.getStreamIdDomain()).isEqualTo(2);
		and.then(historian.getTransactionTemplate()).as("Transaction template cleared").isNull();
		// @formatter:on
	}

	@Test
	public void streamIdDomain_invalid() {
		thenIllegalArgumentException().isThrownBy(() -> historian.setStreamIdDomain(0));
		thenIllegalArgumentException().isThrownBy(() -> historian.setStreamIdDomain(256));
		// @formatter:off
		and.then(historian.getStreamIdDomain())
			.as("Stream ID domain not changed")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_STREAM_ID_DOMAIN)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * findDatumStreamMetadata
	 * ==================================================================
	 */

	private static ObjectDatumStreamMetadata nodeStreamMeta(int localStreamId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(
				encodeLocalStreamId(TEST_NODE_ID, JdbcDatumHistorian.DEFAULT_STREAM_ID_DOMAIN,
						localStreamId),
				"UTC", ObjectDatumKind.Node, TEST_NODE_ID, sourceId, new String[] { "i" }, null, null);
	}

	/**
	 * Verify the metadata query was executed.
	 *
	 * <p>
	 * The statement of the query is then created with the {@code con} mock.
	 * </p>
	 */
	private void thenMetadataQueried() throws SQLException {
		then(jdbcOps).should().query(sqlCaptor.capture(), metaMapperCaptor.capture());
		final PreparedStatementCreator sql = sqlCaptor.getValue();
		// @formatter:off
		and.then(sql)
			.as("Metadata query executed")
			.isInstanceOf(SelectDatumStreamMetadata.class)
			;
		and.then(metaMapperCaptor.getValue())
			.as("Metadata mapper used")
			.isInstanceOf(DatumStreamMetadataRowMapper.class)
			;
		// @formatter:on
		given(con.prepareStatement(anyString())).willReturn(stmt);
		sql.createPreparedStatement(con);
	}

	@Test
	public void findDatumStreamMetadata_nullFilter() {
		thenIllegalArgumentException().isThrownBy(() -> historian.findDatumStreamMetadata(null));
	}

	@Test
	public void findDatumStreamMetadata_noNodeId_noQuery() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian
				.findDatumStreamMetadata(new BasicNodeDatumFilter());

		// THEN
		// @formatter:off
		and.then(result)
			.as("No metadata without node ID")
			.isEmpty()
			;
		then(jdbcOps).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void findDatumStreamMetadata_streamIdsNotDecoded_noQuery() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { UUID.randomUUID(), encodeLocalStreamId(TEST_NODE_ID, 2, 3),
				encodeLocalStreamId(TEST_NODE_ID_2, 1, 3) });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No metadata for stream IDs not of this historian and node")
			.isEmpty()
			;
		then(jdbcOps).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void findDatumStreamMetadata() throws SQLException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		final ObjectDatumStreamMetadata meta = nodeStreamMeta(3, "a");
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of(meta));
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { meta.getStreamId(), UUID.randomUUID() });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Metadata from query returned")
			.containsExactly(meta)
			;
		thenMetadataQueried();
		then(stmt).should().setObject(1, TEST_NODE_ID, Types.BIGINT);
		then(stmt).should().setObject(2, 3);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findDatumStreamMetadata_nodeIds_streamIdsDecoded() throws SQLException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectId(TEST_NODE_ID_2);
		filter.setStreamIds(new UUID[] { encodeLocalStreamId(TEST_NODE_ID_2, 1, 5),
				encodeLocalStreamId(TEST_NODE_ID, 1, 6) });

		// WHEN
		historian.findDatumStreamMetadata(filter);

		// THEN
		thenMetadataQueried();
		// @formatter:off
		then(stmt).should().setObject(1, TEST_NODE_ID_2);
		// Only stream ID of filter node decoded
		then(stmt).should().setObject(2, 5);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findDatumStreamMetadata_locationIds_streamIdsOfRunningNodeDecoded() throws SQLException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setObjectId(TEST_LOC_ID);
		filter.setStreamIds(new UUID[] { encodeLocalStreamId(TEST_NODE_ID_2, 1, 5),
				encodeLocalStreamId(TEST_NODE_ID, 1, 6) });

		// WHEN
		historian.findDatumStreamMetadata(filter);

		// THEN
		thenMetadataQueried();
		// @formatter:off
		then(stmt).should().setObject(1, TEST_LOC_ID);
		// Only stream ID of running node decoded
		then(stmt).should().setObject(2, 6);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findDatumStreamMetadata_otherDomain_streamIdsDecoded() throws SQLException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		historian.setStreamIdDomain(2);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { encodeLocalStreamId(TEST_NODE_ID, 1, 5),
				encodeLocalStreamId(TEST_NODE_ID, 2, 6) });

		// WHEN
		historian.findDatumStreamMetadata(filter);

		// THEN
		thenMetadataQueried();
		// @formatter:off
		then(stmt).should().setObject(1, TEST_NODE_ID, Types.BIGINT);
		// Only stream ID of historian domain decoded
		then(stmt).should().setObject(2, 6);
		then(stmt).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * findFilteredStream
	 * ==================================================================
	 */

	/**
	 * Get the metadata provider given to the processor.
	 *
	 * @return the provider
	 */
	private ObjectDatumStreamMetadataProvider startedProvider() {
		final Map<String, ?> attrs = attrsCaptor.getValue();
		// @formatter:off
		and.then(attrs)
			.as("Only metadata provider attribute given")
			.containsOnlyKeys(METADATA_PROVIDER_ATTR)
			.extractingByKey(METADATA_PROVIDER_ATTR)
			.isInstanceOf(ObjectDatumStreamMetadataProvider.class)
			;
		// @formatter:on
		return (ObjectDatumStreamMetadataProvider) attrs.get(METADATA_PROVIDER_ATTR);
	}

	private Answer<Void> processRows(int count) {
		return invocation -> {
			final RowCallbackHandler handler = invocation.getArgument(1);
			for ( int i = 0; i < count; i++ ) {
				handler.processRow(rs);
			}
			return null;
		};
	}

	/**
	 * Set up the queries of one stream, with local stream ID 3, and datum rows.
	 *
	 * @param rowStreamId
	 *        the local stream ID of the first datum row
	 * @param otherRowStreamIds
	 *        the local stream IDs of any other datum rows
	 * @return the metadata of the stream
	 */
	private ObjectDatumStreamMetadata givenStreamQueries(Integer rowStreamId,
			Integer... otherRowStreamIds) throws SQLException {
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		final ObjectDatumStreamMetadata meta = nodeStreamMeta(3, "a");
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of(meta));
		given(rs.getInt(1)).willReturn(rowStreamId, otherRowStreamIds);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);
		given(rs.getTimestamp(3)).willReturn(Timestamp.from(ts));
		willAnswer(processRows(1 + otherRowStreamIds.length)).given(jdbcOps)
				.query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
		return meta;
	}

	@Test
	public void findFilteredStream_invalidArguments() {
		final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		thenIllegalArgumentException()
				.isThrownBy(() -> historian.findFilteredStream(null, processor, null, null, null));
		thenIllegalArgumentException()
				.isThrownBy(() -> historian.findFilteredStream(filter, null, null, null, null));
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void findFilteredStreamReadings_invalidArguments() {
		final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		thenIllegalArgumentException().isThrownBy(
				() -> historian.findFilteredStreamReadings(null, processor, null, null, null));
		thenIllegalArgumentException()
				.isThrownBy(() -> historian.findFilteredStreamReadings(filter, null, null, null, null));
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
	}

	@Test
	public void findFilteredStreamReadings_noReadingType() {
		// GIVEN
		final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();

		// WHEN
		thenIllegalArgumentException().as("Reading type required").isThrownBy(
				() -> historian.findFilteredStreamReadings(filter, processor, null, null, null));

		// THEN
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		then(identityService).shouldHaveNoInteractions();
	}

	@Test
	public void findFilteredStreamReadings_unsupportedReadingType() {
		for ( DatumReadingType type : DatumReadingType.values() ) {
			if ( type == DatumReadingType.Difference ) {
				continue;
			}
			// GIVEN
			final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
			filter.setReadingType(type);

			// WHEN
			// @formatter:off
			thenExceptionOfType(UnsupportedOperationException.class)
				.as("Reading type %s not supported", type)
				.isThrownBy(() -> historian.findFilteredStreamReadings(filter, processor, null, null, null))
				.withMessageContaining(type.name())
				;
			// @formatter:on
		}

		// THEN
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		then(identityService).shouldHaveNoInteractions();
	}

	@Test
	public void findFilteredStreamReadings_minuteAggregation() {
		for ( Aggregation agg : List.of(Aggregation.Minute, Aggregation.FiveMinute,
				Aggregation.TenMinute, Aggregation.FifteenMinute, Aggregation.ThirtyMinute) ) {
			// GIVEN
			final BasicNodeDatumFilter filter = minuteFilter(agg, 1);
			filter.setReadingType(DatumReadingType.Difference);

			// WHEN
			// @formatter:off
			thenIllegalArgumentException()
				.as("Aggregation %s refused for readings, as in SolarQuery", agg)
				.isThrownBy(() -> historian.findFilteredStreamReadings(filter, processor, null, null, null))
				.withMessage("Only Hour/Day/Month aggregation is supported for reading queries.")
				;
			// @formatter:on
		}

		// THEN
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		then(identityService).shouldHaveNoInteractions();
	}

	/**
	 * Get a filter with a minute aggregation over a number of hours.
	 */
	private static BasicNodeDatumFilter minuteFilter(Aggregation agg, int hours) {
		final BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setAggregation(agg);
		final Instant start = Instant.parse("2026-01-01T00:00:00Z");
		filter.setStartDate(start);
		filter.setEndDate(start.plus(hours, ChronoUnit.HOURS));
		return filter;
	}

	private void thenMinuteAggregationRefused(BasicNodeDatumFilter filter, String message) {
		// @formatter:off
		thenIllegalArgumentException()
			.as("Minute aggregation query refused before processor started")
			.isThrownBy(() -> historian.findFilteredStream(filter, processor, null, null, null))
			.withMessage(message)
			;
		then(processor).shouldHaveNoInteractions();
		then(jdbcOps).should().query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	private void thenStreamQueried() throws IOException {
		then(jdbcOps).should().query(any(SelectDatumStreamMetadata.class), any(RowMapper.class));
		then(processor).should().start(isNull(), isNull(), isNull(), attrsCaptor.capture());
	}

	@Test
	public void findFilteredStream_minuteAggregation_rangeLimit() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FiveMinute, 841);

		// THEN
		thenMinuteAggregationRefused(filter,
				"Minute level aggregation time span must be at most 840 hours.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_atRangeLimit() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.query(any(SelectDatumStreamMetadata.class), any(RowMapper.class)))
				.willReturn(List.of());
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.ThirtyMinute, 840);
		// part of an hour more is not counted, as in SolarNet
		filter.setEndDate(filter.getEndDate().plus(59, ChronoUnit.MINUTES));

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		thenStreamQueried();
	}

	@Test
	public void findFilteredStream_minuteAggregation_rangeLimit_configured() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		historian.setMaxMinuteAggregationHours(24);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.TenMinute, 25);

		// THEN
		thenMinuteAggregationRefused(filter,
				"Minute level aggregation time span must be at most 24 hours.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_noRangeLimit() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.query(any(SelectDatumStreamMetadata.class), any(RowMapper.class)))
				.willReturn(List.of());
		historian.setMaxMinuteAggregationHours(0);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FiveMinute, 24 * 365);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		thenStreamQueried();
	}

	@Test
	public void findFilteredStream_minuteAggregation_rangeLimit_localDates() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		// a short instant range, which the local date range is used in place of
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FifteenMinute, 1);
		filter.setLocalStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2026, 2, 5, 1, 0));

		// THEN
		thenMinuteAggregationRefused(filter,
				"Minute level aggregation time span must be at most 840 hours.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_localDatesWithinRangeLimit() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		given(jdbcOps.query(any(SelectDatumStreamMetadata.class), any(RowMapper.class)))
				.willReturn(List.of());
		// a long instant range, which the local date range is used in place of
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FifteenMinute, 24 * 365);
		filter.setLocalStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2026, 2, 5, 0, 0));

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		thenStreamQueried();
	}

	@Test
	public void findFilteredStream_minuteAggregation_minute() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.Minute, 1);

		// THEN
		thenMinuteAggregationRefused(filter,
				"The Minute aggregation is not supported; please query the data directly by "
						+ "omitting the aggregation criteria or using None.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_minute_rangeLimitFirst() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.Minute, 841);

		// THEN
		thenMinuteAggregationRefused(filter,
				"Minute level aggregation time span must be at most 840 hours.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_noDateRange() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FiveMinute, 1);
		filter.setEndDate(null);

		// THEN
		thenMinuteAggregationRefused(filter,
				"A date range must be specified for aggregation FiveMinute.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_mostRecent() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		givenFeatures(true);
		final BasicNodeDatumFilter filter = minuteFilter(Aggregation.FiveMinute, 1);
		filter.setMostRecent(true);

		// THEN
		thenMinuteAggregationRefused(filter,
				"The mostRecent flag cannot be used with aggregation FiveMinute.");
	}

	@Test
	public void findFilteredStream_minuteAggregation_notSupported_noAggregation() throws IOException {
		for ( boolean aggregates : new boolean[] { false, true } ) {
			// GIVEN
			Mockito.reset(jdbcOps, processor);
			historian = new JdbcDatumHistorian(jdbcOps, new StaticOptionalService<>(identityService));
			given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
			givenFeatures(aggregates, false);
			given(jdbcOps.query(any(SelectDatumStreamMetadata.class), any(RowMapper.class)))
					.willReturn(List.of());
			// beyond the range limit, and Minute, which would otherwise be refused
			final BasicNodeDatumFilter filter = minuteFilter(Aggregation.Minute, 841);

			// WHEN
			historian.findFilteredStream(filter, processor, null, null, null);

			// THEN
			thenStreamQueried();
		}
	}

	@Test
	public void findFilteredStream_noNodeId_noQuery() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		// @formatter:off
		then(processor).should().start(isNull(), isNull(), isNull(), attrsCaptor.capture());
		and.then(startedProvider().metadataStreamIds())
			.as("No streams without node ID")
			.isEmpty()
			;
		then(processor).shouldHaveNoMoreInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream_noNodeId_totalResultsCount() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, 5);

		// THEN
		// @formatter:off
		then(processor).should().start(eq(0L), eq(0), isNull(), attrsCaptor.capture());
		and.then(startedProvider().metadataStreamIds())
			.as("No streams without node ID")
			.isEmpty()
			;
		then(processor).shouldHaveNoMoreInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream_streamIdsNotDecoded_noQuery() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { UUID.randomUUID() });

		// WHEN
		historian.findFilteredStream(filter, processor, null, 2L, 5);

		// THEN
		// @formatter:off
		then(processor).should().start(isNull(), eq(2), isNull(), attrsCaptor.capture());
		and.then(startedProvider().metadataStreamIds())
			.as("No streams for stream IDs not of this historian")
			.isEmpty()
			;
		then(processor).shouldHaveNoMoreInteractions();
		then(jdbcOps).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream_noStreams_noDatumQuery() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, 5);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		and.then(sqlCaptor.getValue())
			.as("Metadata query executed")
			.isInstanceOf(SelectDatumStreamMetadata.class)
			;
		// No count or datum query without streams
		then(jdbcOps).shouldHaveNoMoreInteractions();
		then(processor).should().start(eq(0L), eq(0), isNull(), attrsCaptor.capture());
		and.then(startedProvider().metadataStreamIds())
			.as("No streams")
			.isEmpty()
			;
		then(processor).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream() throws IOException, SQLException {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStreamQueries(3, 3);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		final InOrder ordered = inOrder(jdbcOps, processor);
		// @formatter:off
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(RowMapper.class));
		then(processor).should(ordered).start(isNull(), isNull(), isNull(), attrsCaptor.capture());
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		then(processor).should(ordered, times(2)).handleResultItem(datumCaptor.capture());
		then(jdbcOps).shouldHaveNoMoreInteractions();
		// Processor not closed
		then(processor).shouldHaveNoMoreInteractions();

		and.then(sqlCaptor.getAllValues()).element(0)
			.as("Metadata query executed first")
			.isInstanceOf(SelectDatumStreamMetadata.class)
			;
		and.then(sqlCaptor.getAllValues()).element(1)
			.as("Datum stream query executed")
			.isInstanceOf(SelectFilteredDatum.class)
			.extracting(sql -> ((SqlProvider) sql).getSql())
			.asString()
			.as("Datum stream columns selected")
			.startsWith("SELECT m.stream_id, m.node_id, d.ts\n")
			;
		and.then(startedProvider().metadataStreamIds())
			.as("Metadata of streams given to processor")
			.containsExactly(meta.getStreamId())
			;
		and.then(datumCaptor.getAllValues())
			.as("Datum of each row given to processor")
			.hasSize(2)
			.allSatisfy(d -> and.then(d)
				.returns(meta.getStreamId(), from(StreamDatum::getStreamId))
				.returns(ts, from(StreamDatum::getTimestamp)))
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_totalResultsCount() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
				.willReturn(42L);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(filter, processor, null, 2L, 5);

		// THEN
		final InOrder ordered = inOrder(jdbcOps, processor);
		// @formatter:off
		then(jdbcOps).should(ordered).query(any(PreparedStatementCreator.class), any(RowMapper.class));
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(ResultSetExtractor.class));
		then(processor).should(ordered).start(eq(42L), eq(2), isNull(), any());
		then(jdbcOps).should(ordered).query(any(PreparedStatementCreator.class),
				any(RowCallbackHandler.class));
		then(processor).should(ordered).handleResultItem(any());
		and.then(sqlCaptor.getValue())
			.as("Count query executed")
			.isInstanceOf(SqlProvider.class)
			.extracting(sql -> ((SqlProvider) sql).getSql())
			.asString()
			.startsWith("SELECT COUNT(*)\n")
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_largeOffset_notGiven() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, Integer.MAX_VALUE + 1L,
				5);

		// THEN
		// @formatter:off
		// Offset larger than an int not given
		then(processor)
			.should().start(isNull(), isNull(), isNull(), any());
		// @formatter:on
	}

	@Test
	public void findFilteredStream_unknownStream_skipped() throws IOException, SQLException {
		// GIVEN
		// a row of a stream added after the metadata was loaded, then a known one
		final ObjectDatumStreamMetadata meta = givenStreamQueries(4, 3);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		// @formatter:off
		then(processor).should().handleResultItem(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Only datum of stream with metadata given to processor")
			.returns(meta.getStreamId(), from(StreamDatum::getStreamId))
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_handleError_thrown() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3, 3);
		final IOException err = new IOException("Oops");
		willThrow(err).given(processor).handleResultItem(any());

		// THEN
		// @formatter:off
		thenThrownBy(() -> historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null,
				null, null))
			.as("Processor exception thrown")
			.isSameAs(err)
			;
		// Processing stopped at error
		then(processor).should().handleResultItem(any());
		// @formatter:on
	}

	@Test
	public void findFilteredStream_startError_thrown() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of(nodeStreamMeta(3, "a")));
		final IOException err = new IOException("Oops");
		willThrow(err).given(processor).start(any(), any(), any(), any());

		// THEN
		// @formatter:off
		thenThrownBy(() -> historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null,
				null, null))
			.as("Processor exception thrown")
			.isSameAs(err)
			;
		// No datum query after start error
		then(jdbcOps).should(never())
				.query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
		// @formatter:on
	}

	@Test
	public void findFilteredStream_noNodeId_startError_thrown() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);
		final IOException err = new IOException("Oops");
		willThrow(err).given(processor).start(any(), any(), any(), any());

		// THEN
		// @formatter:off
		thenThrownBy(() -> historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null,
				null, null))
			.as("Processor exception thrown")
			.isSameAs(err)
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_transactionTemplate() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3, 3);
		given(txManager.getTransaction(any())).willReturn(txStatus);
		historian.setTransactionTemplate(new TransactionTemplate(txManager));

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		final InOrder ordered = inOrder(txManager, jdbcOps, processor);
		// @formatter:off
		then(txManager).should(ordered).getTransaction(any());
		then(jdbcOps).should(ordered).query(any(PreparedStatementCreator.class), any(RowMapper.class));
		then(processor).should(ordered).start(isNull(), isNull(), isNull(), any());
		then(jdbcOps).should(ordered).query(any(PreparedStatementCreator.class),
				any(RowCallbackHandler.class));
		then(processor).should(ordered, times(2)).handleResultItem(any());
		// Queries run in transaction
		then(txManager).should(ordered).commit(txStatus);
		then(txManager).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream_transactionTemplate_handleError_thrown()
			throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3, 3);
		given(txManager.getTransaction(any())).willReturn(txStatus);
		historian.setTransactionTemplate(new TransactionTemplate(txManager));
		final IOException err = new IOException("Oops");
		willThrow(err).given(processor).handleResultItem(any());

		// THEN
		// @formatter:off
		thenThrownBy(() -> historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null,
				null, null))
			.as("Processor exception thrown")
			.isSameAs(err)
			;
		then(txManager).should().getTransaction(any());
		// Transaction rolled back
		then(txManager).should().rollback(txStatus);
		then(txManager).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void findFilteredStream_transactionTemplate_noNodeId_notUsed() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);
		historian.setTransactionTemplate(new TransactionTemplate(txManager));

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		// @formatter:off
		then(processor).should().start(isNull(), isNull(), isNull(), any());
		// No transaction without queries
		then(txManager).shouldHaveNoInteractions();
		// @formatter:on
	}

	/*
	 * ================================================================== Stored
	 * aggregates and readings
	 * ==================================================================
	 */

	private static BasicNodeDatumFilter aggregateFilter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setAggregation(aggregation);
		return f;
	}

	private static BasicNodeDatumFilter readingFilter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = aggregateFilter(aggregation);
		f.setReadingType(DatumReadingType.Difference);
		return f;
	}

	@Test
	public void findFilteredStream_aggregates() throws IOException, SQLException {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStreamQueries(3, 3);
		givenFeatures(true);

		// WHEN
		historian.findFilteredStream(aggregateFilter(Aggregation.Hour), processor, null, null, null);

		// THEN
		final InOrder ordered = inOrder(jdbcOps, processor);
		// @formatter:off
		then(jdbcOps).should(ordered, times(2)).query(sqlCaptor.capture(), any(RowMapper.class));
		then(processor).should(ordered).start(isNull(), isNull(), isNull(), attrsCaptor.capture());
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		then(processor).should(ordered, times(2)).handleResultItem(datumCaptor.capture());
		then(jdbcOps).shouldHaveNoMoreInteractions();
		then(processor).shouldHaveNoMoreInteractions();

		and.then(sqlCaptor.getAllValues()).element(0)
			.as("Features of schema found first")
			.isInstanceOf(SelectHistorianFeatures.class)
			;
		and.then(sqlCaptor.getAllValues()).element(1)
			.as("Metadata query executed")
			.isInstanceOf(SelectDatumStreamMetadata.class)
			;
		and.then(sqlCaptor.getAllValues()).element(2)
			.as("Stored aggregate query executed")
			.asInstanceOf(type(SelectAggregateDatum.class))
			.as("Hourly aggregates queried")
			.returns("dh_agg_hourly", SelectAggregateDatum::getTableName)
			;
		and.then(startedProvider().metadataStreamIds())
			.as("Metadata of streams given to processor")
			.containsExactly(meta.getStreamId())
			;
		and.then(datumCaptor.getAllValues())
			.as("Aggregate datum of each row given to processor")
			.hasSize(2)
			.allSatisfy(d -> and.then(d)
				.isExactlyInstanceOf(BasicAggregateStreamDatum.class)
				.returns(meta.getStreamId(), from(StreamDatum::getStreamId))
				.returns(ts, from(StreamDatum::getTimestamp)))
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_aggregates_featuresFoundOnce() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		givenFeatures(true);

		// WHEN
		historian.findFilteredStream(aggregateFilter(Aggregation.Month), processor, null, null, null);
		historian.findFilteredStream(aggregateFilter(Aggregation.Day), processor, null, null, null);

		// THEN
		// @formatter:off
		then(jdbcOps).should(times(1)).query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).should(times(2)).query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getAllValues())
			.as("Aggregate table of each aggregation queried")
			.extracting(s -> ((SelectAggregateDatum) s).getTableName())
			.containsExactly("dh_agg_monthly", "dh_agg_daily")
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_aggregates_totalResultsCount() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		givenFeatures(true);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
				.willReturn(42L);
		final BasicNodeDatumFilter filter = aggregateFilter(Aggregation.Day);
		filter.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(filter, processor, null, 2L, 5);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), any(ResultSetExtractor.class));
		then(processor).should().start(eq(42L), eq(2), isNull(), any());
		and.then(sqlCaptor.getValue())
			.as("Count query executed")
			.isInstanceOf(SqlProvider.class)
			.extracting(sql -> ((SqlProvider) sql).getSql())
			.asString()
			.as("Stored aggregates counted")
			.startsWith("SELECT COUNT(*)\nFROM solarhist.dh_datum_meta m\nINNER JOIN solarhist.dh_agg_daily d ")
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_aggregationWithoutSetup_raw() throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		givenFeatures(false);

		// WHEN
		historian.findFilteredStream(aggregateFilter(Aggregation.Day), processor, null, null, null);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(any(SelectHistorianFeatures.class),
				same(SelectHistorianFeatures.RESULT_MAPPER));
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Aggregation treated as no aggregation without aggregate setup")
			.isInstanceOf(SelectFilteredDatum.class)
			;
		then(processor).should().handleResultItem(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Raw datum given to processor")
			.isExactlyInstanceOf(BasicStreamDatum.class)
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStream_unsupportedAggregation_raw_noFeatures()
			throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);

		// WHEN
		historian.findFilteredStream(aggregateFilter(Aggregation.Week), processor, null, null, null);

		// THEN
		// @formatter:off
		then(jdbcOps).should(never()).query(any(SelectHistorianFeatures.class), any(RowMapper.class));
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Aggregation without stored aggregates treated as no aggregation")
			.isInstanceOf(SelectFilteredDatum.class)
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStreamReadings_difference() throws IOException, SQLException {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStreamQueries(3);
		final Instant endTs = ts.plusSeconds(3600);
		given(rs.getTimestamp(10)).willReturn(Timestamp.from(endTs));
		final BasicNodeDatumFilter filter = readingFilter(null);
		filter.setStartDate(ts);
		filter.setEndDate(endTs);

		// WHEN
		historian.findFilteredStreamReadings(filter, processor, null, null, null);

		// THEN
		final InOrder ordered = inOrder(jdbcOps, processor);
		// @formatter:off
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(RowMapper.class));
		then(processor).should(ordered).start(isNull(), isNull(), isNull(), attrsCaptor.capture());
		then(jdbcOps).should(ordered).query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		then(processor).should(ordered).handleResultItem(datumCaptor.capture());
		// features of schema not needed without aggregation
		then(jdbcOps).shouldHaveNoMoreInteractions();
		then(processor).shouldHaveNoMoreInteractions();

		and.then(sqlCaptor.getAllValues()).element(0)
			.as("Metadata query executed")
			.isInstanceOf(SelectDatumStreamMetadata.class)
			;
		and.then(sqlCaptor.getAllValues()).element(1)
			.as("Reading difference query executed")
			.asInstanceOf(type(SelectReadingDifference.class))
			.as("Readings found at filter start date")
			.returns(ts, SelectReadingDifference::getStartDate)
			.as("Readings found at filter end date")
			.returns(endTs, SelectReadingDifference::getEndDate)
			;
		and.then(startedProvider().metadataStreamIds())
			.as("Metadata of streams given to processor")
			.containsExactly(meta.getStreamId())
			;
		and.then(datumCaptor.getValue())
			.as("Reading datum given to processor")
			.isExactlyInstanceOf(BasicReadingStreamDatum.class)
			.returns(meta.getStreamId(), from(StreamDatum::getStreamId))
			.as("Timestamp of starting reading")
			.returns(ts, from(StreamDatum::getTimestamp))
			;
		and.then(((AggregateStreamDatum) datumCaptor.getValue()).getEndTimestamp())
			.as("End timestamp of ending reading")
			.isEqualTo(endTs)
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStreamReadings_aggregates() throws IOException, SQLException {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStreamQueries(3, 3);
		givenFeatures(true);

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Day), processor, null, null,
				null);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Stored aggregates queried for readings")
			.asInstanceOf(type(SelectAggregateDatum.class))
			.as("Daily aggregates queried")
			.returns("dh_agg_daily", SelectAggregateDatum::getTableName)
			;
		then(processor).should(times(2)).handleResultItem(datumCaptor.capture());
		and.then(datumCaptor.getAllValues())
			.as("Reading datum of each row given to processor")
			.allSatisfy(d -> and.then(d)
				.isExactlyInstanceOf(BasicReadingStreamDatum.class)
				.returns(meta.getStreamId(), from(StreamDatum::getStreamId))
				.returns(ts, from(StreamDatum::getTimestamp)))
			.extracting(d -> ((AggregateStreamDatum) d).getEndTimestamp())
			.as("No end timestamp for stored aggregate readings")
			.containsOnlyNulls()
			;
		then(rs).should(never()).getTimestamp(10);
		// @formatter:on
	}

	@Test
	public void findFilteredStreamReadings_aggregationWithoutSetup_difference()
			throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		givenFeatures(false);
		final Instant endTs = ts.plusSeconds(60);
		given(rs.getTimestamp(10)).willReturn(Timestamp.from(endTs));

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Month), processor, null, null,
				null);

		// THEN
		// @formatter:off
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Aggregation treated as no aggregation without aggregate setup")
			.isInstanceOf(SelectReadingDifference.class)
			;
		then(processor).should().handleResultItem(datumCaptor.capture());
		and.then(((AggregateStreamDatum) datumCaptor.getValue()).getEndTimestamp())
			.as("End timestamp of ending reading")
			.isEqualTo(endTs)
			;
		// @formatter:on
	}

	@Test
	public void findFilteredStreamReadings_unsupportedAggregation_difference()
			throws IOException, SQLException {
		// GIVEN
		givenStreamQueries(3);
		given(rs.getTimestamp(10)).willReturn(Timestamp.from(ts));

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Week), processor, null, null,
				null);

		// THEN
		// @formatter:off
		then(jdbcOps).should(never()).query(any(SelectHistorianFeatures.class), any(RowMapper.class));
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		and.then(sqlCaptor.getValue())
			.as("Aggregation without stored aggregates treated as no aggregation")
			.isInstanceOf(SelectReadingDifference.class)
			;
		// @formatter:on
	}

	/*
	 * ================================================================== Fetch
	 * size ==================================================================
	 */

	@Test
	public void fetchSize_invalid() {
		thenIllegalArgumentException().isThrownBy(() -> historian.setFetchSize(-1));
		// @formatter:off
		and.then(historian.getFetchSize())
			.as("Fetch size not changed")
			.isEqualTo(JdbcDatumHistorian.DEFAULT_FETCH_SIZE)
			;
		// @formatter:on
	}

	/**
	 * Create the statement of a query with the {@code con} mock.
	 *
	 * @param sql
	 *        the query
	 */
	private void createStatement(PreparedStatementCreator sql) throws SQLException {
		given(con.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).willReturn(stmt);
		sql.createPreparedStatement(con);
	}

	@Test
	public void findFiltered_fetchSize() throws SQLException {
		// GIVEN
		historian.setFetchSize(5);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());

		// WHEN
		historian.findFiltered(new BasicNodeDatumFilter());

		// THEN
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		createStatement(sqlCaptor.getValue());
		then(stmt).should().setFetchSize(5);
	}

	@Test
	public void findFiltered_fetchSize_driverDefault() throws SQLException {
		// GIVEN
		historian.setFetchSize(0);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
				.willReturn(List.of());

		// WHEN
		historian.findFiltered(new BasicNodeDatumFilter());

		// THEN
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		createStatement(sqlCaptor.getValue());
		// no fetch size set, so the JDBC driver default is used
		then(stmt).should(never()).setFetchSize(anyInt());
	}

	@Test
	public void findFilteredStream_fetchSize() throws IOException, SQLException {
		// GIVEN
		historian.setFetchSize(5);
		givenStreamQueries(3);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowCallbackHandler.class));
		createStatement(sqlCaptor.getValue());
		then(stmt).should().setFetchSize(5);
	}

	@Test
	public void query_source_fetchSize() throws SQLException {
		// GIVEN
		historian.setFetchSize(5);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class))).willReturn(List.of());

		// WHEN
		historian.latest("a", NodeDatum.class);

		// THEN
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		createStatement(sqlCaptor.getValue());
		then(stmt).should().setFetchSize(5);
	}

	@Test
	public void query_streams_fetchSize() throws SQLException {
		// GIVEN
		historian.setFetchSize(5);
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(jdbcOps.query(any(SelectDatum.class), any(RowMapper.class))).willReturn(List.of());

		// WHEN
		historian.latest(emptySet(), NodeDatum.class);

		// THEN
		then(jdbcOps).should().query(sqlCaptor.capture(), any(RowMapper.class));
		createStatement(sqlCaptor.getValue());
		then(stmt).should().setFetchSize(5);
	}

}
