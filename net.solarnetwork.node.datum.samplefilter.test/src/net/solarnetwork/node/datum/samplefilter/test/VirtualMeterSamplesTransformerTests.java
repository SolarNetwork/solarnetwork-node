/* ==================================================================
 * VirtualMeterSamplesTransformerTests.java - 16/02/2021 10:18:36 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.datum.samplefilter.SourceThrottlingSamplesTransformer;
import net.solarnetwork.node.datum.samplefilter.VirtualMeterConfig;
import net.solarnetwork.node.datum.samplefilter.VirtualMeterSamplesTransformer;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link VirtualMeterSamplesTransformer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class VirtualMeterSamplesTransformerTests {

	private static final String SOURCE_ID = "FILTER_ME";
	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATTS_SECONDS = "wattsSeconds";
	private static final String TEST_UID = "test";

	private DatumMetadataService datumMetadataService;
	private VirtualMeterSamplesTransformer xform;

	@Before
	public void setup() {
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
		SourceThrottlingSamplesTransformer.clearSettingCache();
		xform = new VirtualMeterSamplesTransformer(new StaticOptionalService<>(datumMetadataService));
		xform.setUid(TEST_UID);
		xform.setSourceId("^F");
	}

	@After
	public void teardown() {
		EasyMock.verify(datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumMetadataService);
	}

	private GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId(sourceId);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	private VirtualMeterConfig createTestVirtualMeterConfig(String propName) {
		final VirtualMeterConfig vmConfig = new VirtualMeterConfig();
		vmConfig.setPropertyKey(propName);
		vmConfig.setMaxAgeSeconds(60L);
		vmConfig.setTimeUnit(TimeUnit.SECONDS);
		return vmConfig;
	}

	private void assertOutputValue(String msg, GeneralDatumSamples result, BigDecimal expectedValue,
			BigDecimal expectedDerived) {
		assertOutputValue(msg, result, PROP_WATTS, PROP_WATTS_SECONDS, expectedValue, expectedDerived);
	}

	private void assertOutputValue(String msg, GeneralDatumSamples result, String propName,
			String readingPropName, BigDecimal expectedValue, BigDecimal expectedDerived) {
		assertThat("Prop value " + msg, result.getInstantaneousSampleDouble(propName),
				closeTo(expectedValue.doubleValue(), 0.1));
		if ( expectedDerived == null ) {
			assertThat("Meter value not available " + msg,
					result.getAccumulatingSampleDouble(readingPropName), nullValue());
		} else {
			assertThat("Meter value approx " + msg, result.getAccumulatingSampleDouble(readingPropName),
					closeTo(expectedDerived.doubleValue(), 1.0));
		}

	}

	private void assertVirtalMeterMetadata(String msg, GeneralDatumMetadata meta, long date,
			BigDecimal expectedValue, BigDecimal expectedReading) {
		assertVirtalMeterMetadata(msg, meta, PROP_WATTS_SECONDS, date, expectedValue, expectedReading);
	}

	private void assertVirtalMeterMetadata(String msg, GeneralDatumMetadata meta, String readingPropName,
			long date, BigDecimal expectedValue, BigDecimal expectedReading) {
		assertThat("Virtual meter date saved (close to) " + msg,
				meta.getInfoLong(readingPropName, VirtualMeterSamplesTransformer.VIRTUAL_METER_DATE_KEY)
						- date,
				lessThan(2000L));
		assertThat("Virtual meter value saved " + msg, meta.getInfoBigDecimal(readingPropName,
				VirtualMeterSamplesTransformer.VIRTUAL_METER_VALUE_KEY), equalTo(expectedValue));
		assertThat("Virtual meter reading saved " + msg,
				meta.getInfoBigDecimal(readingPropName,
						VirtualMeterSamplesTransformer.VIRTUAL_METER_READING_KEY),
				equalTo(expectedReading));
	}

	@Test
	public void filter_rollingAverage_firstSample() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));

		// WHEN
		replayAll();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples(), null);

		// THEN
		assertOutputValue("at first sample", result, new BigDecimal("23.4"), null);

		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertVirtalMeterMetadata("first", meta, System.currentTimeMillis(), new BigDecimal("23.4"),
				BigDecimal.ZERO);
	}

	@Test
	public void filter_rollingAverage_firstSample_customReadingPropertyName() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		vmConfig.setReadingPropertyName("foobar");
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));

		// WHEN
		replayAll();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples(), null);

		// THEN
		assertOutputValue("at first sample", result, PROP_WATTS, "foobar", new BigDecimal("23.4"), null);

		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertVirtalMeterMetadata("first", meta, "foobar", System.currentTimeMillis(),
				new BigDecimal("23.4"), BigDecimal.ZERO);
	}

	@Test
	public void filter_rollingAverage_multiSamples() throws InterruptedException {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		EasyMock.expectLastCall().times(3);

		// WHEN
		replayAll();
		List<GeneralDatumSamples> outputs = new ArrayList<>();
		List<Long> dates = new ArrayList<>();
		for ( int i = 0; i < 3; i++ ) {
			datum.putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(System.currentTimeMillis());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), null));
			Thread.sleep(999L);
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("7.5"),
				new BigDecimal("10") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("20") };

		for ( int i = 0; i < 3; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
		}
	}

	@Test
	public void filter_rollingAverage_multiSamples_rollover() throws InterruptedException {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		final int iterations = 6;
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		EasyMock.expectLastCall().times(iterations);

		// WHEN
		replayAll();
		List<GeneralDatumSamples> outputs = new ArrayList<>();
		List<Long> dates = new ArrayList<>();
		for ( int i = 0; i < iterations; i++ ) {
			datum.putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(System.currentTimeMillis());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), null));
			Thread.sleep(999L);
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("7.5"),
				new BigDecimal("10"), new BigDecimal("12.5"), new BigDecimal("17.5"),
				new BigDecimal("22.5") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("20"), new BigDecimal("37.5"), new BigDecimal("60"),
				new BigDecimal("87.5") };

		for ( int i = 0; i < iterations; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
		}
	}

}
