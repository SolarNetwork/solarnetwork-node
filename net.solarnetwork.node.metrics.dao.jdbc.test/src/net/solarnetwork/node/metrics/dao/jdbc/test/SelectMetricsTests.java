/* ==================================================================
 * SelectMetricsTests.java - 15/07/2024 7:01:58 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.dao.jdbc.test;

import static net.solarnetwork.node.test.MatcherUtils.equalToTextResource;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.dao.jdbc.SelectMetrics;
import net.solarnetwork.node.metrics.domain.BasicMetricAggregate;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.node.metrics.domain.ParameterizedMetricAggregate;

/**
 * Test cases for the {@link SelectMetrics} class.
 *
 * @author matt
 * @version 1.0
 */
public class SelectMetricsTests {

	private Connection conn;
	private PreparedStatement ps;
	private Array textArray;

	@Before
	public void setup() {
		conn = EasyMock.createMock(Connection.class);
		ps = EasyMock.createMock(PreparedStatement.class);
		textArray = EasyMock.createMock(Array.class);
	}

	private void replayAll() {
		EasyMock.replay(conn, ps);
	}

	@After
	public void teardown() {
		EasyMock.verify(conn, ps);
	}

	@Test
	public void rawSamples() throws SQLException {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(1));

		Capture<String> sqlCaptor = Capture.newInstance();
		expect(conn.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(ps);
		ps.setFetchSize(SelectMetrics.DEFAULT_FETCH_SIZE);
		ps.setObject(1, filter.getStartDate());
		ps.setObject(2, filter.getEndDate());

		// WHEN
		replayAll();
		SelectMetrics select = new SelectMetrics(filter);
		PreparedStatement ps = select.createPreparedStatement(conn);

		// THEN
		assertThat("PreparedStatement returned", ps, is(notNullValue()));
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-metrics-01.sql", getClass(), null));
	}

	@Test
	public void aggs() throws SQLException {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setType(Metric.METRIC_TYPE_SAMPLE);
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(1));
		// @formatter:off
		filter.setAggregates(new MetricAggregate[] {
			BasicMetricAggregate.Minimum,
			BasicMetricAggregate.Maximum,
			BasicMetricAggregate.Average,
			ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_25,
			ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_75
		});
		// @formatter:on

		Capture<String> sqlCaptor = Capture.newInstance();
		expect(conn.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(ps);
		ps.setFetchSize(SelectMetrics.DEFAULT_FETCH_SIZE);

		ps.setObject(1, ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_25.numberParameter(0));
		ps.setObject(2, ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_75.numberParameter(0));

		ps.setObject(3, filter.getStartDate());
		ps.setObject(4, filter.getEndDate());

		expect(conn.createArrayOf(eq("VARCHAR"), aryEq(new String[] { filter.getType() })))
				.andReturn(textArray);
		ps.setArray(5, textArray);
		textArray.free();

		// WHEN
		replayAll();
		SelectMetrics select = new SelectMetrics(filter);
		PreparedStatement ps = select.createPreparedStatement(conn);

		// THEN
		assertThat("PreparedStatement returned", ps, is(notNullValue()));
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-metrics-agg-01.sql", getClass(), null));
	}

	@Test
	public void pagination() throws SQLException {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(1));
		filter.setOffset(1);
		filter.setMax(2);

		Capture<String> sqlCaptor = Capture.newInstance();
		expect(conn.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(ps);
		ps.setFetchSize(SelectMetrics.DEFAULT_FETCH_SIZE);
		ps.setObject(1, filter.getStartDate());
		ps.setObject(2, filter.getEndDate());
		ps.setInt(3, filter.getOffset());
		ps.setInt(4, filter.getMax());

		// WHEN
		replayAll();
		SelectMetrics select = new SelectMetrics(filter);
		PreparedStatement ps = select.createPreparedStatement(conn);

		// THEN
		assertThat("PreparedStatement returned", ps, is(notNullValue()));
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-metrics-02.sql", getClass(), null));
	}

	@Test
	public void sorted() throws SQLException {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(1));
		filter.setSorts(Arrays.asList(new SimpleSortDescriptor(MetricDao.SORT_BY_DATE, true),
				new SimpleSortDescriptor(MetricDao.SORT_BY_NAME),
				new SimpleSortDescriptor(MetricDao.SORT_BY_VALUE, true)));

		Capture<String> sqlCaptor = Capture.newInstance();
		expect(conn.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(ps);
		ps.setFetchSize(SelectMetrics.DEFAULT_FETCH_SIZE);
		ps.setObject(1, filter.getStartDate());
		ps.setObject(2, filter.getEndDate());

		// WHEN
		replayAll();
		SelectMetrics select = new SelectMetrics(filter);
		PreparedStatement ps = select.createPreparedStatement(conn);

		// THEN
		assertThat("PreparedStatement returned", ps, is(notNullValue()));
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-metrics-03.sql", getClass(), null));
	}

	@Test
	public void sortedPagination() throws SQLException {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(1));
		filter.setSorts(Arrays.asList(new SimpleSortDescriptor(MetricDao.SORT_BY_DATE, true),
				new SimpleSortDescriptor(MetricDao.SORT_BY_NAME),
				new SimpleSortDescriptor(MetricDao.SORT_BY_VALUE, true)));
		filter.setOffset(1);
		filter.setMax(2);

		Capture<String> sqlCaptor = Capture.newInstance();
		expect(conn.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(ps);
		ps.setFetchSize(SelectMetrics.DEFAULT_FETCH_SIZE);
		ps.setObject(1, filter.getStartDate());
		ps.setObject(2, filter.getEndDate());
		ps.setInt(3, filter.getOffset());
		ps.setInt(4, filter.getMax());

		// WHEN
		replayAll();
		SelectMetrics select = new SelectMetrics(filter);
		PreparedStatement ps = select.createPreparedStatement(conn);

		// THEN
		assertThat("PreparedStatement returned", ps, is(notNullValue()));
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-metrics-04.sql", getClass(), null));
	}

}
