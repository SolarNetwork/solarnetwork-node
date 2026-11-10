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
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfo;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.datumhistorian.jdbc.DefaultNodeDatumFactory;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreDatum;
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

	@Mock
	private JdbcOperations jdbcOps;

	@Mock
	private IdentityService identityService;

	@Captor
	private ArgumentCaptor<PreparedStatementCreator> sqlCaptor;

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

		// THEN
		// @formatter:off
		and.then(historian.getPathMatcher()).isSameAs(pm);
		and.then(historian.getObjectMapper()).isSameAs(om);
		and.then(historian.getDatumFactory()).isSameAs(f);
		and.then(historian.getDatumTypeCache()).isSameAs(datumTypeCache);
		and.then(historian.getDatumStreamsCache()).isSameAs(datumStreamsCache);
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
		// WHEN
		historian.serviceDidStartup();
		historian.serviceDidShutdown();

		// THEN
		then(jdbcOps).shouldHaveNoInteractions();
	}

}
