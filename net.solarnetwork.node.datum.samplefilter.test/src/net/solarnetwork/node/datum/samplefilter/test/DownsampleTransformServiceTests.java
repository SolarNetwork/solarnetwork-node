/* ==================================================================
 * DownsampleTransformService.java - 24/08/2020 4:42:55 PM
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

package net.solarnetwork.node.datum.samplefilter.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.datum.samplefilter.DownsampleTransformService;
import net.solarnetwork.node.domain.GeneralNodeDatum;

/**
 * Test cases for the {@link DownsampleTransformService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DownsampleTransformServiceTests {

	private static final String TEST_PROP = "foo";
	private static final String TEST_SOURCE = "test.source";

	@Test
	public void instantaneous_typical() {
		// GIVEN
		DownsampleTransformService xs = new DownsampleTransformService();

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setSourceId(TEST_SOURCE);
			d.putInstantaneousSampleValue(TEST_PROP, (i + 1));

			GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(),
					DownsampleTransformService.SUB_SAMPLE_PROPS);
			assertThat("Sub-sample should be filtered out", out, nullValue());
		}

		// then our final non-sub-sample
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSourceId(TEST_SOURCE);
		d.putInstantaneousSampleValue(TEST_PROP, 5);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property average",
				out.getInstantaneousSampleBigDecimal(TEST_PROP), equalTo(new BigDecimal("3")));
		assertThat("Final output contains property min",
				out.getInstantaneousSampleBigDecimal(TEST_PROP + "_min"), equalTo(new BigDecimal("1")));
		assertThat("Final output contains property max",
				out.getInstantaneousSampleBigDecimal(TEST_PROP + "_max"), equalTo(new BigDecimal("5")));
	}

	@Test
	public void instantaneous_typical_sampleCount() {
		// GIVEN
		DownsampleTransformService xs = new DownsampleTransformService();
		xs.setSampleCount(5);

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setSourceId(TEST_SOURCE);
			d.putInstantaneousSampleValue(TEST_PROP, (i + 1));

			GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), Collections.emptyMap());
			assertThat(String.format("Sub-sample %d should be filtered out", i + 1), out, nullValue());
		}

		// then our final non-sub-sample
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSourceId(TEST_SOURCE);
		d.putInstantaneousSampleValue(TEST_PROP, 5);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property average",
				out.getInstantaneousSampleBigDecimal(TEST_PROP), equalTo(new BigDecimal("3")));
		assertThat("Final output contains property min",
				out.getInstantaneousSampleBigDecimal(TEST_PROP + "_min"), equalTo(new BigDecimal("1")));
		assertThat("Final output contains property max",
				out.getInstantaneousSampleBigDecimal(TEST_PROP + "_max"), equalTo(new BigDecimal("5")));
	}

	@Test
	public void accumulating_typical() {
		// GIVEN
		DownsampleTransformService xs = new DownsampleTransformService();

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setSourceId(TEST_SOURCE);
			d.putAccumulatingSampleValue(TEST_PROP, (i + 1));

			GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(),
					DownsampleTransformService.SUB_SAMPLE_PROPS);
			assertThat("Sub-sample should be filtered out", out, nullValue());
		}

		// then our final non-sub-sample
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSourceId(TEST_SOURCE);
		d.putAccumulatingSampleValue(TEST_PROP, 5);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property last value",
				out.getAccumulatingSampleBigDecimal(TEST_PROP), equalTo(new BigDecimal(5)));
	}

	@Test
	public void accumulating_typical_sampleCount() {
		// GIVEN
		DownsampleTransformService xs = new DownsampleTransformService();
		xs.setSampleCount(5);

		// WHEN

		// first some sub-samples
		for ( int i = 0; i < 4; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setSourceId(TEST_SOURCE);
			d.putAccumulatingSampleValue(TEST_PROP, (i + 1));

			GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), Collections.emptyMap());
			assertThat(String.format("Sub-sample %d should be filtered out", i), out, nullValue());
		}

		// then our final non-sub-sample
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSourceId(TEST_SOURCE);
		d.putAccumulatingSampleValue(TEST_PROP, 5);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Final output should not be null", out, notNullValue());
		assertThat("Final output contains property last value",
				out.getAccumulatingSampleBigDecimal(TEST_PROP), equalTo(new BigDecimal(5)));
	}
}
