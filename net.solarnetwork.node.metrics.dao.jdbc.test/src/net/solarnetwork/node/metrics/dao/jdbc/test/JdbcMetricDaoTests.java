/* ==================================================================
 * JdbcMetricDaoTests.java - 14/07/2024 12:04:42 pm
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

import static java.lang.Math.random;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.node.metrics.domain.Metric.metricValue;
import static net.solarnetwork.node.test.TestDbUtils.allTableData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.dao.jdbc.JdbcMetricDao;
import net.solarnetwork.node.metrics.dao.jdbc.MetricDaoStat;
import net.solarnetwork.node.metrics.domain.BasicMetricAggregate;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.node.metrics.domain.MetricKey;
import net.solarnetwork.node.metrics.domain.ParameterizedMetricAggregate;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.TestEmbeddedDatabase;

/**
 * Test cases for the {@link
 *
 * @author matt
 * @version 1.1
 */
public class JdbcMetricDaoTests extends AbstractNodeTest {

	private TestEmbeddedDatabase dataSource;

	private JdbcOperations jdbcOps;

	private JdbcMetricDao dao;
	private Metric last;

	@Before
	public void setup() throws IOException {
		dao = new JdbcMetricDao();

		TestEmbeddedDatabase db = createEmbeddedDatabase("data.db.type");
		if ( db.getDatabaseType() != EmbeddedDatabaseType.H2 ) {
			String dbType = db.getDatabaseType().toString().toLowerCase();
			dao.setInitSqlResource(
					new ClassPathResource(format("%s-metric-init.sql", dbType), JdbcMetricDao.class));
			dao.setSqlResourcePrefix(format("%s-metric", dbType));
		}
		dataSource = db;

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();

		jdbcOps = new JdbcTemplate(dataSource);
	}

	@Test
	public void insert() {
		// GIVEN
		Metric m = Metric.metricValue(Instant.now(), "test", "test", 123.0);

		// WHEN
		MetricKey pk = dao.save(m);

		// THEN
		assertThat("PK generated", pk, notNullValue());
		assertThat("PK same as provided", pk, is(equalTo(m.getId())));
		last = m;
	}

	@Test
	public void getByPK() {
		// GIVEN
		insert();

		// WHEN
		Metric entity = dao.get(last.getId());

		// THEN
		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Value", entity.getValue(), is(equalTo(last.getValue())));
	}

	@Test(expected = DuplicateKeyException.class)
	public void update() {
		// GIVEN
		insert();
		Metric orig = dao.get(last.getId());

		// WHEN
		Metric update = new Metric(orig.getId(), 234.0);
		dao.save(update);

		// THEN
		// throws DuplicateKeyException because updates not allowed
	}

	@Test
	public void findFiltered_dateRange() {
		// GIVEN
		final int rowCount = 10;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(rowCount);
		for ( int i = 0; i < rowCount; i++ ) {
			Metric m = Metric.metricValue(start.plusSeconds(i), UUID.randomUUID().toString(),
					UUID.randomUUID().toString(), random());
			dao.save(m);
			allMetrics.add(m);
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start.plusSeconds(2));
		filter.setEndDate(start.plusSeconds(6));
		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("Expected result count returned", resultList, hasSize(6 - 2));
		assertThat("Expected results returned", resultList,
				contains(allMetrics.subList(2, 6).toArray(new Metric[6 - 2])));
	}

	@Test
	public void findFiltered_types() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(rowCount);
		final String[] types = new String[] { "a", "b", "c" };
		for ( int i = 0; i < rowCount; i++ ) {
			Metric m = metricValue(start.plusSeconds(i), types[i % types.length], "m", random());
			dao.save(m);
			allMetrics.add(m);
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setTypes(new String[] { "a", "c" });
		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("Expected result count returned", resultList, hasSize(8));
		assertThat("Expected results returned", resultList, contains(allMetrics.stream().filter(m -> {
			for ( String t : filter.getTypes() ) {
				if ( t.equals(m.getType()) ) {
					return true;
				}
			}
			return false;
		}).toArray(Metric[]::new)));
	}

	@Test
	public void findFiltered_names() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(rowCount);
		final String[] names = new String[] { "a", "b", "c" };
		for ( int i = 0; i < rowCount; i++ ) {
			Metric m = metricValue(start.plusSeconds(i), "t", names[i % names.length], random());
			dao.save(m);
			allMetrics.add(m);
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setNames(new String[] { "a", "c" });
		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("Expected result count returned", resultList, hasSize(8));
		assertThat("Expected results returned", resultList, contains(allMetrics.stream().filter(m -> {
			for ( String t : filter.getNames() ) {
				if ( t.equals(m.getName()) ) {
					return true;
				}
			}
			return false;
		}).toArray(Metric[]::new)));
	}

	@Test
	public void findFiltered() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(rowCount);
		final String[] types = new String[] { "t1", "t2", "t3" };
		final String[] names = new String[] { "a", "b", "c" };
		for ( int i = 0; i < rowCount; i++ ) {
			for ( int j = 0; j < types.length; j++ ) {
				Metric m = metricValue(start.plusSeconds(i), types[j % types.length],
						names[i % names.length], random());
				dao.save(m);
				allMetrics.add(m);
			}
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start.plusSeconds(3));
		filter.setEndDate(start.plusSeconds(6));
		filter.setType("t1");
		filter.setName("b");
		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("Expected result count returned", resultList, hasSize(1));
		assertThat("Expected results returned", resultList, contains(allMetrics.stream().filter(m -> {
			return "t1".equals(m.getType()) && "b".equals(m.getName())
					&& m.getTimestamp().compareTo(filter.getStartDate()) >= 0
					&& m.getTimestamp().compareTo(filter.getEndDate()) < 0;
		}).toArray(Metric[]::new)));
	}

	@Test
	public void findFiltered_agg() {
		// GIVEN
		final int metricCount = 2;
		final int sampleCount = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(metricCount * sampleCount);
		for ( int i = 0; i < sampleCount; i++ ) {
			for ( int j = 0; j < metricCount; j++ ) {
				Metric m = metricValue(start.plusSeconds(i), Metric.METRIC_TYPE_SAMPLE, "metric." + j,
						(i * 10) + j);
				dao.save(m);
				allMetrics.add(m);
			}
		}

		allTableData(log, jdbcOps, "SOLARNODE.MTR_METRIC", "ts, mtype, mname");

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start);
		filter.setEndDate(start.plusSeconds(sampleCount));
		filter.setType(Metric.METRIC_TYPE_SAMPLE);
		// @formatter:off
		filter.setAggregates(new MetricAggregate[] {
			BasicMetricAggregate.Minimum,
			BasicMetricAggregate.Maximum,
			BasicMetricAggregate.Average,
			BasicMetricAggregate.Count,
			BasicMetricAggregate.Sum,
			ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_25,
			ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_75
		});
		// @formatter:on

		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("One result per metric per aggregate", resultList,
				hasSize(metricCount * filter.getAggregates().length));

		// @formatter:off
		final double[] expectedValues = new double[] {
			10, // avg
			3,  // cnt
			20, // max
			0,  // min
			5,  // q:25
			15, // q:75
			30, // sum
			11,
			3,
			21,
			1,
			6,
			16,
			33,
		};
		// @formatter:on

		MetricAggregate[] sortedAggs = new MetricAggregate[filter.getAggregates().length];
		System.arraycopy(filter.getAggregates(), 0, sortedAggs, 0, sortedAggs.length);
		Arrays.sort(sortedAggs, Comparator.comparing(MetricAggregate::key));

		Iterator<Metric> itr = results.iterator();
		for ( int i = 0; i < metricCount; i++ ) {
			for ( int j = 0; j < filter.getAggregates().length; j++ ) {
				final String expectedType = sortedAggs[j].key();
				final String expectedName = "metric." + i;
				final int mIdx = (i * filter.getAggregates().length) + j;
				final double expectedVal = expectedValues[mIdx];
				Metric m = itr.next();
				assertThat(format("Result metric %d name", mIdx), m.getName(),
						is(equalTo(expectedName)));
				assertThat(format("Result metric %d type", mIdx), m.getType(),
						is(equalTo(expectedType)));
				assertThat(format("Result metric %d value", mIdx), m.getValue(),
						is(equalTo(expectedVal)));
			}
		}
	}

	@Test
	public void deleteFiltered() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final String[] types = new String[] { "t1", "t2" };
		final String[] names = new String[] { "a", "b" };
		final List<Metric> allMetrics = new ArrayList<>(rowCount * types.length);
		for ( int i = 0; i < rowCount; i++ ) {
			for ( int j = 0; j < types.length; j++ ) {
				Metric m = metricValue(start.plusSeconds(i), types[j % types.length],
						names[i % names.length], random());
				dao.save(m);
				allMetrics.add(m);
			}
		}

		final List<Map<String, Object>> rowsBefore = allTableData(log, jdbcOps, "SOLARNODE.MTR_METRIC",
				"ts, mtype, mname");

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setEndDate(start.plusSeconds(6));
		filter.setType("t1");
		int result = dao.deleteFiltered(filter);

		// THEN
		final int expectedDeleteCount = 6;
		assertThat("Expected row count before delete", rowsBefore, hasSize(rowCount * types.length));
		assertThat("Result matches expected delete count", result, is(equalTo(expectedDeleteCount)));

		final List<Map<String, Object>> rowsAfter = allTableData(log, jdbcOps, "SOLARNODE.MTR_METRIC",
				"ts, mtype, mname");
		assertThat("Expected row count after delete", rowsAfter, hasSize(rowCount * types.length - 6));
		assertThat("Expected rows deleted", rowsAfter.stream().noneMatch((row) -> {
			Instant ts = ((OffsetDateTime) row.get("ts")).toInstant();
			return ("t1".equals(row.get("type")) && ts.isBefore(filter.getEndDate()));
		}), is(equalTo(true)));

		assertThat(dao.getStats().get(MetricDaoStat.MetricsDeleted),
				is(equalTo((long) expectedDeleteCount)));
	}

	@Test
	public void findFiltered_pagination() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final String[] types = new String[] { "t1", "t2", "t3" };
		final String[] names = new String[] { "a", "b", "c" };
		final List<Metric> allMetrics = new ArrayList<>(rowCount * types.length);
		for ( int i = 0; i < rowCount; i++ ) {
			for ( int j = 0; j < types.length; j++ ) {
				Metric m = metricValue(start.plusSeconds(i), types[j % types.length],
						names[i % names.length], random());
				dao.save(m);
				allMetrics.add(m);
			}
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setType("t1");
		filter.setMax(2);
		FilterResults<Metric, MetricKey> results1 = dao.findFiltered(filter);

		filter.setOffset(2);
		FilterResults<Metric, MetricKey> results2 = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results1, is(notNullValue()));
		assertThat("Result returned", results2, is(notNullValue()));

		final Metric[] t1 = allMetrics.stream().filter(m -> {
			return "t1".equals(m.getType());
		}).toArray(Metric[]::new);

		final List<Metric> resultList1 = stream(results1.spliterator(), false).collect(toList());
		assertThat("Expected page 1 count returned", resultList1, hasSize(2));
		assertThat("Expected page 1 returned", resultList1, contains(t1[0], t1[1]));

		final List<Metric> resultList2 = stream(results2.spliterator(), false).collect(toList());
		assertThat("Expected page 1 count returned", resultList2, hasSize(2));
		assertThat("Expected page 1 returned", resultList2, contains(t1[2], t1[3]));
	}

	@Test
	public void findFiltered_mostRecent() {
		// GIVEN
		final int rowCount = 12;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final String[] types = new String[] { "t1", "t2", "t3" };
		final String[] names = new String[] { "a", "b", "c" };
		final List<Metric> allMetrics = new ArrayList<>(rowCount * types.length);
		for ( int i = 0; i < rowCount; i++ ) {
			for ( int j = 0; j < types.length; j++ ) {
				Metric m = metricValue(start.plusSeconds(i), types[j % types.length],
						names[i % names.length], random());
				dao.save(m);
				allMetrics.add(m);
			}
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setType("t1");
		filter.setNames(new String[] { "a", "b" });
		filter.setMostRecent(true);
		filter.setSorts(MetricDao.SORT_BY_TYPE_NAME);
		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		// THEN
		assertThat("Result returned", results, is(notNullValue()));

		// pull out expected most recent a/b values
		Metric expected_a = null;
		Metric expected_b = null;
		for ( ListIterator<Metric> itr = allMetrics.listIterator(allMetrics.size()); itr
				.hasPrevious(); ) {
			Metric m = itr.previous();
			if ( "t1".equals(m.getType()) ) {
				if ( expected_a == null && "a".equals(m.getName()) ) {
					expected_a = m;
				} else if ( expected_b == null && "b".equals(m.getName()) ) {
					expected_b = m;
				}
			}
			if ( expected_a != null && expected_b != null ) {
				break;
			}
		}

		final List<Metric> resultList = stream(results.spliterator(), false).collect(toList());
		assertThat("1 result per name returned", resultList, hasSize(2));
		assertThat("Results ordered by name", resultList, contains(expected_a, expected_b));
	}

	@Test
	public void batchExport() {
		// GIVEN
		final int rowCount = 10;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final List<Metric> allMetrics = new ArrayList<>(rowCount);
		for ( int i = 0; i < rowCount; i++ ) {
			Metric m = Metric.metricValue(start.plusSeconds(i), UUID.randomUUID().toString(),
					UUID.randomUUID().toString(), random());
			dao.save(m);
			allMetrics.add(m);
		}

		// WHEN
		BasicMetricFilter filter = new BasicMetricFilter();
		filter.setStartDate(start.plusSeconds(2));
		filter.setEndDate(start.plusSeconds(6));

		List<Metric> results = new ArrayList<>();
		Map<String, Object> params = new HashMap<>(4);
		params.put(MetricDao.BATCH_PARAM_FILTER, filter);
		BasicBatchOptions opts = new BasicBatchOptions("export", 50, false, params);
		dao.batchProcess(new BatchCallback<Metric>() {

			@Override
			public BatchCallbackResult handle(Metric metric) {
				results.add(metric);
				return BatchCallbackResult.CONTINUE;
			}
		}, opts);

		// THEN
		assertThat("Expected processed count", results, hasSize(6 - 2));
		assertThat("Expected results returned", results,
				contains(allMetrics.subList(2, 6).toArray(new Metric[6 - 2])));
	}

}
