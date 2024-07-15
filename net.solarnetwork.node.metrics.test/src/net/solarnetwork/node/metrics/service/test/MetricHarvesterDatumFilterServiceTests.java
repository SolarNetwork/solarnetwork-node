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

package net.solarnetwork.node.metrics.service.test;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.service.MetricHarvesterDatumFilterService;
import net.solarnetwork.node.metrics.service.MetricHarvesterPropertyConfig;

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
	private MetricHarvesterDatumFilterService service;

	@Before
	public void setup() {
		metricDao = EasyMock.createMock(MetricDao.class);
		service = new MetricHarvesterDatumFilterService(metricDao);
		service.setUid("Test Unchanged");
		service.setSourceId("^S");
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

}
