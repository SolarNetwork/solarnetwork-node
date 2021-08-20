/* ==================================================================
 * VirtualMeterTransformServiceTests.java - 16/02/2021 10:18:36 AM
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

package net.solarnetwork.node.datum.filter.test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.datum.filter.std.SourceThrottlingSamplesTransformer;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterConfig;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterExpressionConfig;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterTransformService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link VirtualMeterTransformService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class VirtualMeterTransformServiceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String SOURCE_ID = "FILTER_ME";
	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATT_HOURS = "wattHours";
	private static final String PROP_WATT_HOURS_SECONDS = "wattHoursSeconds";
	private static final String PROP_COST = "cost";
	private static final String PROP_WATTS_SECONDS = "wattsSeconds";
	private static final String TEST_UID = "test";

	private DatumMetadataService datumMetadataService;
	private OperationalModesService opModesService;
	private VirtualMeterTransformService xform;

	@Before
	public void setup() {
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
		SourceThrottlingSamplesTransformer.clearSettingCache();
		xform = new VirtualMeterTransformService(new StaticOptionalService<>(datumMetadataService));
		xform.setUid(TEST_UID);
		xform.setSourceId("^F");
		xform.setOpModesService(opModesService);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumMetadataService, opModesService);
	}

	private void replayAll() {
		EasyMock.replay(datumMetadataService, opModesService);
	}

	private static GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(sourceId);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	private static VirtualMeterConfig createTestVirtualMeterConfig(String propName) {
		final VirtualMeterConfig vmConfig = new VirtualMeterConfig();
		vmConfig.setPropertyKey(propName);
		vmConfig.setMaxAgeSeconds(60L);
		vmConfig.setTimeUnit(TimeUnit.SECONDS);
		return vmConfig;
	}

	private static void assertOutputValue(String msg, GeneralDatumSamples result,
			BigDecimal expectedValue, BigDecimal expectedDerived) {
		assertOutputValue(msg, result, PROP_WATTS, PROP_WATTS_SECONDS, expectedValue, expectedDerived);
	}

	private static void assertOutputValue(String msg, GeneralDatumSamples result, String propName,
			String readingPropName, BigDecimal expectedValue, BigDecimal expectedDerived) {
		Number n = result.findSampleValue(propName); // use find to support both i & a styles
		assertThat("Prop " + propName + " value " + msg, n.doubleValue(),
				closeTo(expectedValue.doubleValue(), 0.1));
		if ( expectedDerived == null ) {
			assertThat("Meter value not available " + msg,
					result.getAccumulatingSampleDouble(readingPropName), nullValue());
		} else {
			assertThat("Meter value approx " + msg, result.getAccumulatingSampleDouble(readingPropName),
					closeTo(expectedDerived.doubleValue(), 1.0));
		}

	}

	private static void assertVirtualMeterMetadata(String msg, GeneralDatumMetadata meta, long date,
			BigDecimal expectedValue, BigDecimal expectedReading) {
		assertVirtualMeterMetadata(msg, meta, PROP_WATTS_SECONDS, date, expectedValue, expectedReading);
	}

	private static void assertVirtualMeterMetadata(String msg, GeneralDatumMetadata meta,
			String readingPropName, long date, BigDecimal expectedValue, BigDecimal expectedReading) {
		assertThat("Virtual meter date saved " + msg,
				meta.getInfoLong(readingPropName, VirtualMeterTransformService.VIRTUAL_METER_DATE_KEY),
				equalTo(date));
		assertThat("Virtual meter value saved " + msg, meta.getInfoBigDecimal(readingPropName,
				VirtualMeterTransformService.VIRTUAL_METER_VALUE_KEY), equalTo(expectedValue));
		assertThat("Virtual meter reading saved " + msg,
				meta.getInfoBigDecimal(readingPropName,
						VirtualMeterTransformService.VIRTUAL_METER_READING_KEY),
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
		assertVirtualMeterMetadata("first", meta, datum.getCreated().getTime(), new BigDecimal("23.4"),
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
		assertVirtualMeterMetadata("first", meta, "foobar", datum.getCreated().getTime(),
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
		List<Date> dates = new ArrayList<>();
		final Date start = new Date(
				LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
		for ( int i = 0; i < 3; i++ ) {
			datum.setCreated(new Date(start.getTime() + TimeUnit.SECONDS.toMillis(i)));
			datum.putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(datum.getCreated());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), null));
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
	public void filter_rollingAverage_multiSamples_rollover() {
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
		List<Date> dates = new ArrayList<>();
		final Date start = new Date(
				LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
		for ( int i = 0; i < iterations; i++ ) {
			datum.setCreated(new Date(start.getTime() + TimeUnit.SECONDS.toMillis(i)));
			datum.putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(datum.getCreated());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), null));
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

	@Test
	public void filter_accumulating() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(GeneralDatumSamplesType.Accumulating);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		final int iterations = 3;
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(iterations);

		// WHEN
		replayAll();
		List<GeneralDatumSamples> outputs = new ArrayList<>();
		List<Date> dates = new ArrayList<>();
		final Date start = new Date(
				LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
		for ( int i = 0; i < iterations; i++ ) {
			datum.setCreated(new Date(start.getTime() + TimeUnit.SECONDS.toMillis(i)));
			datum.putAccumulatingSampleValue(PROP_WATT_HOURS, 5 * (i + 1));
			dates.add(datum.getCreated());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), emptyMap()));
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("5"),
				new BigDecimal("10") };

		for ( int i = 0; i < iterations; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_WATT_HOURS_SECONDS,
					expectedValues[i], expectedReadings[i]);
		}
	}

	@Test
	public void filter_accumulating_expression() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(GeneralDatumSamplesType.Accumulating);
		vmConfig.setReadingPropertyName(PROP_COST);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig(PROP_COST,
				GeneralDatumSamplesType.Accumulating, "prevReading + (inputDiff * 3)",
				exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		final int iterations = 3;
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(iterations);

		// WHEN
		replayAll();
		List<GeneralDatumSamples> outputs = new ArrayList<>();
		List<Date> dates = new ArrayList<>();
		List<Map<String, ?>> parameters = new ArrayList<>();
		final Date start = new Date(
				LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
		for ( int i = 0; i < iterations; i++ ) {
			datum.setCreated(new Date(start.getTime() + TimeUnit.SECONDS.toMillis(i)));
			datum.putAccumulatingSampleValue(PROP_WATT_HOURS, 5 * (i + 1));
			dates.add(datum.getCreated());
			Map<String, Object> p = new LinkedHashMap<>();
			outputs.add(xform.transformSamples(datum, datum.getSamples(), p));
			parameters.add(p);
		}

		// THEN
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("15"),
				new BigDecimal("30") };

		for ( int i = 0; i < iterations; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_COST, expectedValues[i],
					expectedReadings[i]);
			Map<String, ?> p = parameters.get(i);
			if ( i == 0 ) {
				assertThat("Input diff parameter not created " + i, p.keySet(), hasSize(0));
			} else {
				assertThat("Xform parameters created", p.keySet(),
						contains("wattHours_diff", "cost_diff"));
				assertThat("Input diff parameter created " + i, p,
						hasEntry("wattHours_diff", new BigDecimal("5")));
				assertThat("Output diff parameter created " + i, p,
						hasEntry("cost_diff", new BigDecimal("15")));
			}
		}
	}

	private static class CloningCapture extends Capture<GeneralDatumMetadata> {

		private static final long serialVersionUID = -7208625218989724725L;

		public CloningCapture(CaptureType type) {
			super(type);
		}

		@Override
		public void setValue(GeneralDatumMetadata value) {
			// so we can capture a snapshot in time
			super.setValue(new GeneralDatumMetadata(value));
		}

	}

	@Test
	public void filter_accumulating_expression_unchangedReading() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(GeneralDatumSamplesType.Accumulating);
		vmConfig.setReadingPropertyName(PROP_COST);
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig(PROP_COST,
				GeneralDatumSamplesType.Accumulating,
				"currInput == 15 ? prevReading : prevReading + (inputDiff * 3)", exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		final List<Integer> inputs = Arrays.asList(5, 10, 15, 20);
		final int iterations = inputs.size();
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(iterations - 1); // one less metadata save

		// WHEN
		replayAll();
		List<GeneralDatumSamples> outputs = new ArrayList<>();
		List<Date> dates = new ArrayList<>();
		final Date start = new Date(
				LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
		for ( int i = 0; i < iterations; i++ ) {
			datum.setCreated(new Date(start.getTime() + TimeUnit.SECONDS.toMillis(i)));
			datum.putAccumulatingSampleValue(PROP_WATT_HOURS, inputs.get(i));
			dates.add(datum.getCreated());
			outputs.add(xform.transformSamples(datum, datum.getSamples(), emptyMap()));
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15"), new BigDecimal("20") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("15"),
				new BigDecimal("15"), new BigDecimal("45") };

		for ( int i = 0; i < iterations; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_COST, expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved one less meter metdata because of track only changes", savedMetas, hasSize(3));

		assertVirtualMeterMetadata("first", savedMetas.get(0), PROP_COST, dates.get(0).getTime(),
				new BigDecimal("5"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("second", savedMetas.get(1), PROP_COST, dates.get(1).getTime(),
				new BigDecimal("10"), new BigDecimal("15"));
		assertVirtualMeterMetadata("third (after skip)", savedMetas.get(2), PROP_COST,
				dates.get(3).getTime(), new BigDecimal("20"), new BigDecimal("45"));
	}

	@Test
	public void operationalMode_noMatch() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });
		xform.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		// WHEN
		replayAll();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples(), null);

		// THEN
		assertThat("No change because required operational mode not active", result,
				is(sameInstance(datum.getSamples())));
	}

	@Test
	public void operationalMode_match() {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });
		xform.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);

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
		assertVirtualMeterMetadata("first", meta, datum.getCreated().getTime(), new BigDecimal("23.4"),
				BigDecimal.ZERO);
	}

	private static final long DELAY_MILLISECONDS = 200L;

	private static class DelayedDatum implements Delayed {

		private final GeneralNodeDatum datum;

		private DelayedDatum(GeneralNodeDatum datum) {
			super();
			this.datum = datum;
		}

		@Override
		public int compareTo(Delayed o) {
			return datum.getCreated().compareTo(((DelayedDatum) o).datum.getCreated());
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long ms = datum.getCreated().getTime() + DELAY_MILLISECONDS - System.currentTimeMillis();
			return unit.convert(ms, TimeUnit.MILLISECONDS);
		}

	}

	/**
	 * This test explores using a DelayQueue to order processing of concurrent
	 * datum by time.
	 * 
	 * @throws InterruptedException
	 *         if an interruption occurs
	 */
	@Test
	public void filter_concurrently() throws InterruptedException {
		// GIVEN
		final GeneralNodeDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(GeneralDatumSamplesType.Accumulating);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// start with given metadata
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta).anyTimes();

		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().anyTimes();

		// add metadata
		final int iterations = 10;
		final long start = Instant.now().plusSeconds(2).truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
		final AtomicInteger counter = new AtomicInteger();
		final AtomicBoolean latch = new AtomicBoolean(true);
		final BlockingQueue<DelayedDatum> inputs = new DelayQueue<>();

		// start producing
		final int numProducers = 4;
		final ExecutorService producerPool = Executors.newFixedThreadPool(numProducers);
		for ( int i = 0; i < numProducers; i++ ) {
			producerPool.submit(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						int i = counter.getAndAccumulate(1, (curr, inc) -> {
							return (curr < iterations ? curr + 1 : curr);
						});
						if ( i >= iterations ) {
							return;
						}
						long date = start + TimeUnit.MILLISECONDS.toMillis(i * 10);
						log.debug("Generating datum {} @ {}", i, date);
						GeneralNodeDatum d = datum.clone();
						d.setCreated(new Date(date));
						d.putAccumulatingSampleValue(PROP_WATT_HOURS, (i + 1));
						inputs.offer(new DelayedDatum(d));
					}
				}
			});

		}

		// WHEN
		replayAll();
		final List<GeneralDatumSamples> outputs = Collections.synchronizedList(new ArrayList<>());

		// start consuming
		Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {
				DelayedDatum d = null;
				do {
					try {
						d = inputs.poll(5, TimeUnit.SECONDS);
					} catch ( InterruptedException e ) {
						continue;
					}
					if ( d != null ) {
						log.debug("Processing datum {}", d.datum.getCreated().getTime());
						outputs.add(xform.transformSamples(d.datum, d.datum.getSamples(), emptyMap()));
					}
				} while ( latch.get() || d != null );
			}
		});
		consumer.start();

		producerPool.shutdown();
		producerPool.awaitTermination(1, TimeUnit.MINUTES);
		latch.set(false);
		consumer.join(2000 + iterations * 1000L);

		// THEN
		assertThat("All inputs processed", outputs, hasSize(iterations));
		for ( int i = 0; i < iterations; i++ ) {
			GeneralDatumSamples result = outputs.get(i);
			BigDecimal expectedValue = new BigDecimal(i + 1);
			BigDecimal expectedReading = (i == 0 ? null : new BigDecimal(i - 1));
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_WATT_HOURS_SECONDS,
					expectedValue, expectedReading);
		}
	}

}
