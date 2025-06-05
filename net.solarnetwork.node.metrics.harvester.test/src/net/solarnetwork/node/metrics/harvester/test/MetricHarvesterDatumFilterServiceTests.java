/* ==================================================================
 * MetricHarvesterDatumFilterServiceTests.java - 15/07/2024 2:42:14 pm
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

package net.solarnetwork.node.metrics.harvester.test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.harvester.MetricHarvesterDatumFilterService;
import net.solarnetwork.node.metrics.harvester.MetricHarvesterPropertyConfig;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link MetricHarvesterDatumFilterService} class.
 *
 * @author matt
 * @version 1.0
 */
public class MetricHarvesterDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "S_1";
	private static final String PROP_1 = "watts";

	private MetricDao metricDao;
	private ExpressionService exprService;
	private MetricHarvesterDatumFilterService service;

	@Before
	public void setup() {
		metricDao = EasyMock.createMock(MetricDao.class);

		exprService = new SpelExpressionService();

		service = new MetricHarvesterDatumFilterService(metricDao);
		service.setUid("Test Metric Harvester");
		service.setSourceId("^S");
		service.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));
	}

	private void replayAll() {
		EasyMock.replay(metricDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(metricDao);
	}

	private SimpleDatum createTestDatum(Instant ts, String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, ts);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void harvest() {
		// GIVEN
		MetricHarvesterPropertyConfig pConfig = new MetricHarvesterPropertyConfig("watts",
				DatumSamplesType.Instantaneous, "metric.1");
		service.setPropertyConfigs(new MetricHarvesterPropertyConfig[] { pConfig });

		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);

		Capture<Metric> metricCaptor = Capture.newInstance();
		expect(metricDao.save(capture(metricCaptor))).andAnswer(() -> {
			return metricCaptor.getValue().getId();
		});

		// WHEN
		replayAll();
		DatumSamplesOperations result = service.filter(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Input samples returned", result, is(sameInstance(d.getSamples())));

		Metric m = metricCaptor.getValue();
		assertThat("Metric persisted", m, is(notNullValue()));
		assertThat("Metric timestamp is datum timestamp", m.getTimestamp(),
				is(equalTo(d.getTimestamp())));
		assertThat("Metric type is 'sample'", m.getType(), is(equalTo(Metric.METRIC_TYPE_SAMPLE)));
		assertThat("Metric name is from prop config", m.getName(), is(equalTo(pConfig.getMetricName())));
		assertThat("Metric value is from datum", m.getValue(), is(
				equalTo(d.getSampleDouble(DatumSamplesType.Instantaneous, pConfig.getPropertyKey()))));
	}

	@Test
	public void harvest_withUnitSlope() {
		// GIVEN
		MetricHarvesterPropertyConfig pConfig = new MetricHarvesterPropertyConfig("watts",
				DatumSamplesType.Instantaneous, "metric.1");
		pConfig.setUnitSlope(new BigDecimal("0.001"));
		service.setPropertyConfigs(new MetricHarvesterPropertyConfig[] { pConfig });

		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);

		Capture<Metric> metricCaptor = Capture.newInstance();
		expect(metricDao.save(capture(metricCaptor))).andAnswer(() -> {
			return metricCaptor.getValue().getId();
		});

		// WHEN
		replayAll();
		DatumSamplesOperations result = service.filter(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Input samples returned", result, is(sameInstance(d.getSamples())));

		Metric m = metricCaptor.getValue();
		assertThat("Metric persisted", m, is(notNullValue()));
		assertThat("Metric timestamp is datum timestamp", m.getTimestamp(),
				is(equalTo(d.getTimestamp())));
		assertThat("Metric type is 'sample'", m.getType(), is(equalTo(Metric.METRIC_TYPE_SAMPLE)));
		assertThat("Metric name is from prop config", m.getName(), is(equalTo(pConfig.getMetricName())));
		assertThat("Metric value is from datum", m.getValue(),
				is(equalTo(
						d.getSampleBigDecimal(DatumSamplesType.Instantaneous, pConfig.getPropertyKey())
								.multiply(pConfig.getUnitSlope()).doubleValue())));
	}

	@Test
	public void expressions() throws Exception {
		// GIVEN
		ExpressionConfig config1 = new ExpressionConfig();
		config1.setName("m1");
		config1.setExpressionServiceId(exprService.getUid());
		config1.setExpression("voltage * 2");

		ExpressionConfig config2 = new ExpressionConfig();
		config2.setName("m2");
		config2.setExpressionServiceId(exprService.getUid());
		config2.setExpression("voltage * amps");
		service.setExpressionConfigs(new ExpressionConfig[] { config1, config2 });

		Capture<Metric> metricCaptor = Capture.newInstance(CaptureType.ALL);
		expect(metricDao.save(capture(metricCaptor))).andAnswer(() -> {
			List<Metric> metrics = metricCaptor.getValues();
			return metrics.get(metrics.size() - 1).getId();
		}).times(2);

		// WHEN
		replayAll();
		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		SimpleDatum d = createTestDatum(start, SOURCE_ID_1, PROP_1, 1);
		d.putSampleValue(DatumSamplesType.Instantaneous, "amps", 12);
		d.putSampleValue(DatumSamplesType.Instantaneous, "voltage", 345);
		DatumSamplesOperations result = service.filter(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Input samples returned", result, is(sameInstance(d.getSamples())));

		List<Metric> metrics = metricCaptor.getValues();
		assertThat("Two metrics persisted", metrics, hasSize(2));

		for ( int i = 0; i < metrics.size(); i++ ) {
			Metric m = metrics.get(i);

			assertThat("Metric timestamp is datum timestamp", m.getTimestamp(),
					is(equalTo(d.getTimestamp())));
			assertThat("Metric type is 'sample'", m.getType(), is(equalTo(Metric.METRIC_TYPE_SAMPLE)));
			assertThat("Metric name is from prop config", m.getName(),
					is(equalTo(service.getExpressionConfigs()[i].getPropertyKey())));
			assertThat("Metric value is from expression result", m.getValue(),
					is(equalTo(i == 0 ? 690.0 : 4140.0)));
		}
	}
}
