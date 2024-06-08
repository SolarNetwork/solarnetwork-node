/* ==================================================================
 * DownsampleDatumFilterService.java - 24/08/2020 4:42:55 PM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.test;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.domain.datum.AggregateDatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.filter.std.DownsampleDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.OperationalModesService;

/**
 * Test cases for the {@link DownsampleDatumFilterService}.
 *
 * @author matt
 * @version 1.2
 */
public class DownsampleDatumFilterServiceTests {

	private static final String TEST_PROP = "foo";
	private static final String TEST_SOURCE = "test.source";

	@Test
	public void instantaneous_typical() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
			d.getSamples().putInstantaneousSampleValue(TEST_PROP, (i + 1));

			DatumSamplesOperations out = xs.filter(d, d.getSamples(),
					DownsampleDatumFilterService.SUB_SAMPLE_PROPS);
			assertThat("Sub-sample should be filtered out", out, nullValue());
		}

		// then our final non-sub-sample
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
		d.getSamples().putInstantaneousSampleValue(TEST_PROP, 5);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property average",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP),
				equalTo(new BigDecimal("3")));
		assertThat("Final output contains property min",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP + "_min"),
				equalTo(new BigDecimal("1")));
		assertThat("Final output contains property max",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP + "_max"),
				equalTo(new BigDecimal("5")));
	}

	@Test
	public void instantaneous_typical_sampleCount() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();
		xs.setSampleCount(5);

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
			d.getSamples().putInstantaneousSampleValue(TEST_PROP, (i + 1));

			DatumSamplesOperations out = xs.filter(d, d.getSamples(), Collections.emptyMap());
			assertThat(String.format("Sub-sample %d should be filtered out", i + 1), out, nullValue());
		}

		// then our final non-sub-sample
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
		d.getSamples().putInstantaneousSampleValue(TEST_PROP, 5);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property average",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP),
				equalTo(new BigDecimal("3")));
		assertThat("Final output contains property min",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP + "_min"),
				equalTo(new BigDecimal("1")));
		assertThat("Final output contains property max",
				out.getSampleBigDecimal(DatumSamplesType.Instantaneous, TEST_PROP + "_max"),
				equalTo(new BigDecimal("5")));
	}

	@Test
	public void accumulating_typical() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
			d.getSamples().putAccumulatingSampleValue(TEST_PROP, (i + 1));

			DatumSamplesOperations out = xs.filter(d, d.getSamples(),
					DownsampleDatumFilterService.SUB_SAMPLE_PROPS);
			assertThat("Sub-sample should be filtered out", out, nullValue());
		}

		// then our final non-sub-sample
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
		d.getSamples().putAccumulatingSampleValue(TEST_PROP, 5);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property last value",
				out.getSampleBigDecimal(DatumSamplesType.Accumulating, TEST_PROP),
				equalTo(new BigDecimal(5)));
	}

	@Test
	public void accumulating_typical_sampleCount() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();
		xs.setSampleCount(5);

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
			d.getSamples().putAccumulatingSampleValue(TEST_PROP, (i + 1));

			DatumSamplesOperations out = xs.filter(d, d.getSamples(), Collections.emptyMap());
			assertThat(String.format("Sub-sample %d should be filtered out", i), out, nullValue());
		}

		// then our final non-sub-sample
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
		d.getSamples().putAccumulatingSampleValue(TEST_PROP, 5);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property last value",
				out.getSampleBigDecimal(DatumSamplesType.Accumulating, TEST_PROP),
				equalTo(new BigDecimal(5)));
	}

	@Test
	public void operationalMode_noMatch() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();
		OperationalModesService opModesService = EasyMock.createMock(OperationalModesService.class);
		xs.setOpModesService(opModesService);
		xs.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		// WHEN
		replay(opModesService);
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
		d.getSamples().putAccumulatingSampleValue(TEST_PROP, 5);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("No change because required operational mode not active", out,
				sameInstance(d.getSamples()));
		verify(opModesService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void duration_invalid() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();

		// WHEN
		xs.setSampleDuration(Duration.ofNanos(333L));

	}

	@Test
	public void duration_1s() {
		// GIVEN
		DownsampleDatumFilterService xs = new DownsampleDatumFilterService();
		xs.setSampleDurationSecs(1);

		Clock tick = Clock.tick(Clock.systemUTC(), Duration.ofSeconds(1L));

		Map<Instant, List<DatumSamplesOperations>> input = new LinkedHashMap<>(8);
		List<DatumSamplesOperations> output = new ArrayList<>(8);

		// go for 3 samples
		Instant end = Instant.now().plusMillis(3500L);

		// WHEN
		int i = 0;
		while ( Instant.now().isBefore(end) ) {
			SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE);
			d.getSamples().putInstantaneousSampleValue(TEST_PROP, ++i);
			DatumSamplesOperations out = xs.filter(d, d.getSamples(), emptyMap());
			input.computeIfAbsent(tick.instant(), k -> new ArrayList<>(256)).add(d);
			if ( out != null ) {
				output.add(out);
			}
			try {
				Thread.sleep(100);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		assertThat("Output has 1 sample for each passed input time slot", output,
				hasSize(input.size() - 1));
		int outIdx = 0;
		for ( Entry<Instant, List<DatumSamplesOperations>> e : input.entrySet() ) {
			AggregateDatumSamples expectedAgg = new AggregateDatumSamples();
			for ( DatumSamplesOperations d : e.getValue() ) {
				expectedAgg.addSample(d);
			}
			DatumSamplesOperations expected = expectedAgg.average(xs.getDecimalScale(),
					xs.getMinPropertyFormat(), xs.getMaxPropertyFormat());
			DatumSamplesOperations out = output.get(outIdx++);
			assertThat("Averaged by duration " + (outIdx + 1), out, is(equalTo(expected)));
			if ( outIdx >= output.size() ) {
				break;
			}
		}
	}

}
