/* ==================================================================
 * VirtualMeterDatumFilterServiceTests.java - 16/02/2021 10:18:36 AM
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
import static net.solarnetwork.node.datum.filter.virt.VirtualMeterDatumFilterService.VIRTUAL_METER_DATE_KEY;
import static net.solarnetwork.node.datum.filter.virt.VirtualMeterDatumFilterService.VIRTUAL_METER_READING_KEY;
import static net.solarnetwork.node.datum.filter.virt.VirtualMeterDatumFilterService.VIRTUAL_METER_VALUE_KEY;
import static net.solarnetwork.util.NumberUtils.decimalArray;
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
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.datum.filter.std.ThrottlingDatumFilterService;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterConfig;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterDatumFilterService;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterExpressionConfig;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link VirtualMeterDatumFilterService} class.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class VirtualMeterDatumFilterServiceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String SOURCE_ID = "FILTER_ME";
	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATT_HOURS = "wattHours";
	private static final String PROP_WATT_HOURS_SECONDS = "wattHoursSeconds";
	private static final String PROP_WATT_HOURS_SECONDS_DIFF = "wattHoursSecondsDiff";
	private static final String PROP_COST = "cost";
	private static final String PROP_WATTS_SECONDS = "wattsSeconds";
	private static final String TEST_UID = "test";

	private DatumMetadataService datumMetadataService;
	private OperationalModesService opModesService;
	private VirtualMeterDatumFilterService xform;

	@Before
	public void setup() {
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
		ThrottlingDatumFilterService.clearSettingCache();
		xform = new VirtualMeterDatumFilterService(new StaticOptionalService<>(datumMetadataService));
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

	private static SimpleDatum createTestGeneralNodeDatum(String sourceId) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	private static VirtualMeterConfig createTestVirtualMeterConfig(String propName) {
		final VirtualMeterConfig vmConfig = new VirtualMeterConfig();
		vmConfig.setPropertyKey(propName);
		vmConfig.setMaxAgeSeconds(60L);
		vmConfig.setTimeUnit(TimeUnit.SECONDS);
		return vmConfig;
	}

	private static void assertOutputValue(String msg, DatumSamplesOperations result,
			BigDecimal expectedValue, BigDecimal expectedDerived) {
		assertOutputValue(msg, result, PROP_WATTS, PROP_WATTS_SECONDS, expectedValue, expectedDerived);
	}

	private static void assertOutputValue(String msg, DatumSamplesOperations result, String propName,
			String readingPropName, BigDecimal expectedValue, BigDecimal expectedDerived) {
		Number n = result.findSampleValue(propName); // use find to support both i & a styles
		assertThat("Prop " + propName + " value " + msg, n.doubleValue(),
				closeTo(expectedValue.doubleValue(), 0.1));
		if ( expectedDerived == null ) {
			assertThat("Meter value not available " + msg,
					result.getSampleDouble(DatumSamplesType.Accumulating, readingPropName), nullValue());
		} else {
			assertThat("Meter value approx " + msg,
					result.getSampleDouble(DatumSamplesType.Accumulating, readingPropName),
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
				meta.getInfoLong(readingPropName, VirtualMeterDatumFilterService.VIRTUAL_METER_DATE_KEY),
				equalTo(date));
		assertThat("Virtual meter value saved " + msg, meta.getInfoBigDecimal(readingPropName,
				VirtualMeterDatumFilterService.VIRTUAL_METER_VALUE_KEY), equalTo(expectedValue));
		assertThat("Virtual meter reading saved " + msg,
				meta.getInfoBigDecimal(readingPropName,
						VirtualMeterDatumFilterService.VIRTUAL_METER_READING_KEY),
				equalTo(expectedReading));
	}

	@Test
	public void filter_rollingAverage_firstSample() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
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
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), null);

		// THEN
		assertOutputValue("at first sample", result, new BigDecimal("23.4"), null);

		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertVirtualMeterMetadata("first", meta, datum.getTimestamp().toEpochMilli(),
				new BigDecimal("23.4"), BigDecimal.ZERO);
	}

	@Test
	public void filter_rollingAverage_firstSample_customReadingPropertyName() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
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
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), null);

		// THEN
		assertOutputValue("at first sample", result, PROP_WATTS, "foobar", new BigDecimal("23.4"), null);

		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertVirtualMeterMetadata("first", meta, "foobar", datum.getTimestamp().toEpochMilli(),
				new BigDecimal("23.4"), BigDecimal.ZERO);
	}

	@Test
	public void filter_rollingAverage_multiSamples() throws InterruptedException {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
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
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < 3; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), null));
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("7.5"),
				new BigDecimal("10") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("20") };

		for ( int i = 0; i < 3; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
		}
	}

	@Test
	public void filter_rollingAverage_multiSamples_rollover() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
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
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), null));
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
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
		}
	}

	@Test
	public void filter_accumulating() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(DatumSamplesType.Accumulating);
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
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, 5 * (i + 1));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("5"),
				new BigDecimal("10") };

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_WATT_HOURS_SECONDS,
					expectedValues[i], expectedReadings[i]);
		}
	}

	@Test
	public void filter_accumulating_expression() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(DatumSamplesType.Accumulating);
		vmConfig.setReadingPropertyName(PROP_COST);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig(PROP_COST,
				DatumSamplesType.Accumulating, "prevReading + (inputDiff * 3)", exprService.getUid());
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
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		List<Map<String, ?>> parameters = new ArrayList<>();
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, 5 * (i + 1));
			dates.add(ts);
			Map<String, Object> p = new LinkedHashMap<>();
			outputs.add(xform.filter(d, d.getSamples(), p));
			parameters.add(p);
		}

		// THEN
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("15"),
				new BigDecimal("30") };

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
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
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(DatumSamplesType.Accumulating);
		vmConfig.setReadingPropertyName(PROP_COST);
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig(PROP_COST,
				DatumSamplesType.Accumulating,
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
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected rolling average values
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15"), new BigDecimal("20") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("15"),
				new BigDecimal("15"), new BigDecimal("45") };

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_COST, expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved one less meter metdata because of track only changes", savedMetas, hasSize(3));

		assertVirtualMeterMetadata("first", savedMetas.get(0), PROP_COST, dates.get(0).toEpochMilli(),
				new BigDecimal("5"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("second", savedMetas.get(1), PROP_COST, dates.get(1).toEpochMilli(),
				new BigDecimal("10"), new BigDecimal("15"));
		assertVirtualMeterMetadata("third (after skip)", savedMetas.get(2), PROP_COST,
				dates.get(3).toEpochMilli(), new BigDecimal("20"), new BigDecimal("45"));
	}

	@Test
	public void operationalMode_noMatch() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setRollingAverageCount(4);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });
		xform.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		// WHEN
		replayAll();
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), null);

		// THEN
		assertThat("No change because required operational mode not active", result,
				is(sameInstance(datum.getSamples())));
	}

	@Test
	public void operationalMode_match() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
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
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), null);

		// THEN
		assertOutputValue("at first sample", result, new BigDecimal("23.4"), null);

		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertVirtualMeterMetadata("first", meta, datum.getTimestamp().toEpochMilli(),
				new BigDecimal("23.4"), BigDecimal.ZERO);
	}

	private static final long DELAY_MILLISECONDS = 200L;

	private static class DelayedDatum implements Delayed {

		private final SimpleDatum datum;

		private DelayedDatum(SimpleDatum datum) {
			super();
			this.datum = datum;
		}

		@Override
		public int compareTo(Delayed o) {
			return datum.getTimestamp().compareTo(((DelayedDatum) o).datum.getTimestamp());
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long ms = datum.getTimestamp().toEpochMilli() + DELAY_MILLISECONDS
					- System.currentTimeMillis();
			return unit.convert(ms, TimeUnit.MILLISECONDS);
		}
	}

	@Test
	public void filter_instDiff_defaultPropName() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setIncludeInstantaneousDiffProperty(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < 3; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), null));
		}

		// THEN
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("20") };
		BigDecimal[] expectedDiffs = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("12.5") };

		for ( int i = 0; i < 3; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
			assertThat("Instantaneous diff " + i,
					result.getSampleBigDecimal(DatumSamplesType.Instantaneous, "wattsSecondsDiff"),
					is(expectedDiffs[i]));
		}
	}

	@Test
	public void filter_instDiff_custPropName() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATTS);
		vmConfig.setIncludeInstantaneousDiffProperty(true);
		vmConfig.setInstantaneousDiffPropertyName("wattSecondsDelta");
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < 3; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putInstantaneousSampleValue(PROP_WATTS, 5 * (i + 1));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), null));
		}

		// THEN
		BigDecimal[] expectedValues = new BigDecimal[] { new BigDecimal("5"), new BigDecimal("10"),
				new BigDecimal("15") };
		BigDecimal[] expectedReadings = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("20") };
		BigDecimal[] expectedDiffs = new BigDecimal[] { null, new BigDecimal("7.5"),
				new BigDecimal("12.5") };

		for ( int i = 0; i < 3; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, expectedValues[i], expectedReadings[i]);
			assertThat("Instantaneous diff " + i,
					result.getSampleBigDecimal(DatumSamplesType.Instantaneous, "wattSecondsDelta"),
					is(expectedDiffs[i]));
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
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(DatumSamplesType.Accumulating);
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
						SimpleDatum d = datum.copyWithId(
								DatumId.nodeId(null, datum.getSourceId(), Instant.ofEpochMilli(date)));
						d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, (i + 1));
						inputs.offer(new DelayedDatum(d));
					}
				}
			});

		}

		// WHEN
		replayAll();
		final List<DatumSamplesOperations> outputs = Collections.synchronizedList(new ArrayList<>());

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
						log.debug("Processing datum {}", d.datum.getTimestamp().toEpochMilli());
						outputs.add(xform.filter(d.datum, d.datum.getSamples(), emptyMap()));
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
			DatumSamplesOperations result = outputs.get(i);
			BigDecimal expectedValue = new BigDecimal(i + 1);
			BigDecimal expectedReading = (i == 0 ? null : new BigDecimal(i - 1));
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_WATT_HOURS_SECONDS,
					expectedValue, expectedReading);
		}
	}

	@Test
	public void inputAccumulating_ignoreNegative_withDelta() {
		// GIVEN
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig(PROP_WATT_HOURS);
		vmConfig.setPropertyType(DatumSamplesType.Accumulating);
		vmConfig.setIncludeInstantaneousDiffProperty(true);
		vmConfig.setInstantaneousDiffPropertyName(PROP_WATT_HOURS_SECONDS_DIFF);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig(
				PROP_WATT_HOURS_SECONDS, DatumSamplesType.Accumulating,
				"inputDiff > 0 ? prevReading + inputDiff : prevReading", exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// start with given metadata
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta).anyTimes();

		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(5);

		// WHEN
		replayAll();
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.getSamples().putInstantaneousSampleValue(PROP_WATTS, null);
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < 3; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, i);
			outputs.add(xform.filter(d, d.getSamples(), null));
		}
		// now "reset" input accumulating data back to 0
		for ( int i = 0; i < 2; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i + 3));
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putAccumulatingSampleValue(PROP_WATT_HOURS, i * 5);
			outputs.add(xform.filter(d, d.getSamples(), null));
		}

		// THEN
		BigDecimal[] expectedValues = decimalArray("0", "1", "2", "0", "5");
		BigDecimal[] expectedReadings = decimalArray(null, "1", "2", "2", "7");
		Integer[] expectedDeltas = new Integer[] { null, 1, 1, 0, 5 };
		for ( int i = 0; i < 3; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, PROP_WATT_HOURS, PROP_WATT_HOURS_SECONDS,
					expectedValues[i], expectedReadings[i]);
			assertThat("Delta prop " + PROP_WATT_HOURS_SECONDS_DIFF + " " + i, result
					.getSampleInteger(DatumSamplesType.Instantaneous, PROP_WATT_HOURS_SECONDS_DIFF),
					is(expectedDeltas[i]));
		}
	}

	private static String pulseExpression() {
		/*-
		 The following expression is designed to deal with input readings of 0 or 1, where the desire
		 is to count the number of toggles from 1 -> 0 -> 1 that take more than 1s to complete. The
		 expression assumes the "track only when reading changes" setting is active. That means the
		 `prevInput` value is always the last CHANGED input value, not necessarily the previously 
		 SEEN input value!
		 
		 The expression follows these rules:
		
		 1. If the input is the same as the last CHANGE, do nothing.
		 2. If the input has changed to 0, add 0.1.
		 3. If the input has changed and the last CHANGE is a whole number, add 0.1.
		 4. If the time since the last CHANGE is less than 1s, round the reading DOWN to a whole number.
		 5. Otherwise round the reading UP.
		 
		 These rules have these general effects:
		
		 * The meter "partially" advances whenever the input changes to 0; need change back to 1 to 
		   "fully" advance.
		 * The meter "fully" advances when the input changes to 1, as long as duration of time at 
		   0 was 1s or more.
		
		 The expression can be translated into the following pseudo code:
		 
		 IF ( input has not changed ) THEN
		 	reading does not change
		 ELSE IF ( input is 0 OR reading is a whole number ) THEN
		 	add 0.1 to reading
		 ELSE IF ( time since last change is less than 1s ) THEN
		 	round reading down
		 ELSE
		 	round meter up
		 END IF		 
		 */

		// @formatter:off
		return "currInput == prevInput ? prevReading : "
				+ "currInput < 1 || prevReading.stripTrailingZeros().scale() <= 0 ? prevReading + 0.1 : "
				+ "prevReading.setScale(0, "
					+ "timeUnits < 1 ? T(java.math.RoundingMode).DOWN : T(java.math.RoundingMode).UP"
				+ ")";
		// @formatter:on
	}

	private static String pulseExpressionInverted() {
		// @formatter:off
		return "currInput == prevInput ? prevReading : "
				+ "currInput > 0 || prevReading.stripTrailingZeros().scale() <= 0 ? prevReading + 0.1 : "
				+ "prevReading.setScale(0, "
					+ "timeUnits < 1 ? T(java.math.RoundingMode).DOWN : T(java.math.RoundingMode).UP"
				+ ")";
		// @formatter:on
	}

	@Test
	public void filter_pulse_skipFromTimeConstraint() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.asMutableSampleOperations().setSampleData(DatumSamplesType.Instantaneous, null);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig("switch");
		vmConfig.setPropertyType(DatumSamplesType.Status);
		vmConfig.setReadingPropertyName("pulses");
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig("pulses",
				DatumSamplesType.Accumulating, pulseExpression(), exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		// @formatter:off
		final List<Integer> inputs = Arrays.asList(
				// The goal of this simulation is to count the number of input toggles but
				// ignore toggles that happen too quickly (faster than 1s). To accomplish
				// this "track only changes" is enabled and an expression is used that:
				//
				// 1. does nothing if the input has not changed
				// 2. adds 0.1 when the input changes from 1 to 0 (via `inputDiff < 1`)
				// 3. adds -0.1 if input changes from 1 to 0 in under 1s (via `timeUnits < 1`)
				// 4. adds 0.9 if when input changes from 1 to 0 in 1s or more
				//
				// The addition of 0.1 means the meter still rounds down to the previous reading
				// when cast to an integer, so a full toggle is only achieved after the input
				// reverts back to the starting value (1).
				
				// start high for 1s
				1, 1, 
				
				// jump low for 1.5s; meter advances to 0.1
				0, 0, 0,
				
				// back to high for 1s; meter advances to 1
				1, 1, 
				
				// jump low, but only 0.5s; meter advances to 1.1
				0,
				
				// back to high; meter reverts to 1
				1, 1,
				
				// jump low for 1s; meter advances to 1.1
				0, 0,
				
				// back to high; meter advances to 2
				1, 1);
		// @formatter:on
		final int iterations = inputs.size();
		final int changeCount = 7;
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(changeCount); // only for changes

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i) / 2);
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putStatusSampleValue("switch", inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected pulse count
		// @formatter:off
		BigDecimal[] expectedValues = decimalArray(
				 "1", "1",   "0",   "0",   "0", "1", "1",   "0", "1", "1",   "0",   "0", "1", "1");
		BigDecimal[] expectedReadings = decimalArray(
				null, "0", "0.1", "0.1", "0.1", "1", "1", "1.1", "1", "1", "1.1", "1.1", "2", "2");
		// @formatter:on

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, "switch", "pulses", expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved meter metdata only for changes", savedMetas, hasSize(changeCount));

		assertVirtualMeterMetadata("1st", savedMetas.get(0), "pulses", dates.get(0).toEpochMilli(),
				new BigDecimal("1"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("2nd", savedMetas.get(1), "pulses", dates.get(2).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("0.1"));
		assertVirtualMeterMetadata("3rd", savedMetas.get(2), "pulses", dates.get(5).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("1"));
		assertVirtualMeterMetadata("4th", savedMetas.get(3), "pulses", dates.get(7).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("1.1"));
		assertVirtualMeterMetadata("5th", savedMetas.get(4), "pulses", dates.get(8).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("1"));
		assertVirtualMeterMetadata("6th", savedMetas.get(5), "pulses", dates.get(10).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("1.1"));
		assertVirtualMeterMetadata("7th", savedMetas.get(6), "pulses", dates.get(12).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("2"));
	}

	@Test
	public void filter_pulse_multi() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.asMutableSampleOperations().setSampleData(DatumSamplesType.Instantaneous, null);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig("switch");
		vmConfig.setPropertyType(DatumSamplesType.Status);
		vmConfig.setReadingPropertyName("pulses");
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig("pulses",
				DatumSamplesType.Accumulating, pulseExpression(), exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		// @formatter:off
		final List<Integer> inputs = Arrays.asList(
				// start high for 1s
				1, 1, 
				
				// jump low for 1.5s; meter advances 0.1
				0, 0, 0,
				
				// back to high for 1s; meter advances to 1
				1, 1, 
				
				// jump low for 2s; meter advances 1.1
				0, 0, 0, 0,
				
				// back to high 1s; meter advances to 2
				1, 1);
		// @formatter:on
		final int iterations = inputs.size();
		final int changeCount = 5;
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(changeCount);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i) / 2);
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putStatusSampleValue("switch", inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected pulse count
		// @formatter:off
		BigDecimal[] expectedValues = decimalArray(
				 "1", "1",   "0",   "0",   "0", "1", "1",   "0",   "0",   "0",   "0", "1", "1");
		BigDecimal[] expectedReadings = decimalArray(
				null, "0", "0.1", "0.1", "0.1", "1", "1", "1.1", "1.1", "1.1", "1.1", "2", "2");
		// @formatter:on

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, "switch", "pulses", expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved meter metdata only for changes", savedMetas, hasSize(changeCount));

		assertVirtualMeterMetadata("1st", savedMetas.get(0), "pulses", dates.get(0).toEpochMilli(),
				new BigDecimal("1"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("2nd", savedMetas.get(1), "pulses", dates.get(2).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("0.1"));
		assertVirtualMeterMetadata("3rd", savedMetas.get(2), "pulses", dates.get(5).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("1"));
		assertVirtualMeterMetadata("4th", savedMetas.get(3), "pulses", dates.get(7).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("1.1"));
		assertVirtualMeterMetadata("5th", savedMetas.get(4), "pulses", dates.get(11).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("2"));
	}

	@Test
	public void filter_pulse_multi_inverted() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.asMutableSampleOperations().setSampleData(DatumSamplesType.Instantaneous, null);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig("switch");
		vmConfig.setPropertyType(DatumSamplesType.Status);
		vmConfig.setReadingPropertyName("pulses");
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig("pulses",
				DatumSamplesType.Accumulating, pulseExpressionInverted(), exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		// @formatter:off
		final List<Integer> inputs = Arrays.asList(
				// start low for 0s
				0, 0, 
				
				// jump high for 1.5s; meter advances 0.1
				1, 1, 1,
				
				// back to low for 1s; meter advances to 1
				0, 0, 
				
				// jump high for 1s; meter advances 1.1
				1, 1, 1, 1,
				
				// back to low 1s; meter advances to 2
				0, 0);
		// @formatter:on
		final int iterations = inputs.size();
		final int changeCount = 5;
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(changeCount);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i) / 2);
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putStatusSampleValue("switch", inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected pulse count
		// @formatter:off
		BigDecimal[] expectedValues = decimalArray(
				 "0", "0",   "1",   "1",   "1", "0", "0",   "1",   "1",   "1",   "1", "0", "0");
		BigDecimal[] expectedReadings = decimalArray(
				null, "0", "0.1", "0.1", "0.1", "1", "1", "1.1", "1.1", "1.1", "1.1", "2", "2");
		// @formatter:on

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, "switch", "pulses", expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved meter metdata only for changes", savedMetas, hasSize(changeCount));

		assertVirtualMeterMetadata("1st", savedMetas.get(0), "pulses", dates.get(0).toEpochMilli(),
				new BigDecimal("0"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("2nd", savedMetas.get(1), "pulses", dates.get(2).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("0.1"));
		assertVirtualMeterMetadata("3rd", savedMetas.get(2), "pulses", dates.get(5).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("1"));
		assertVirtualMeterMetadata("4th", savedMetas.get(3), "pulses", dates.get(7).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("1.1"));
		assertVirtualMeterMetadata("5th", savedMetas.get(4), "pulses", dates.get(11).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("2"));
	}

	@Test
	public void filter_pulse_startOff() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.asMutableSampleOperations().setSampleData(DatumSamplesType.Instantaneous, null);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig("switch");
		vmConfig.setPropertyType(DatumSamplesType.Status);
		vmConfig.setReadingPropertyName("pulses");
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig("pulses",
				DatumSamplesType.Accumulating, pulseExpression(), exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// no metadata available yet
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(null);

		// add metadata
		// @formatter:off
		final List<Integer> inputs = Arrays.asList(
				// start low for 2.5s
				0, 0, 0, 0, 0,
				
				// to high for 1s; meter advances to 0.1
				1, 1, 
				
				// jump low for 2s; meter advances 0.2
				0, 0, 0, 0,
				
				// back to high 1s; meter advances to 1
				1, 1);
		// @formatter:on
		final int iterations = inputs.size();
		final int changeCount = 4;
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(changeCount);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i) / 2);
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putStatusSampleValue("switch", inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN
		// expected pulse count
		// @formatter:off
		BigDecimal[] expectedValues = decimalArray(
				 "0", "0", "0", "0", "0",   "1",   "1",   "0",   "0",   "0",   "0", "1", "1");
		BigDecimal[] expectedReadings = decimalArray(
				null, "0", "0", "0", "0", "0.1", "0.1", "0.2", "0.2", "0.2", "0.2", "1", "1");
		// @formatter:on

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, "switch", "pulses", expectedValues[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved meter metdata only for changes", savedMetas, hasSize(changeCount));

		assertVirtualMeterMetadata("1st", savedMetas.get(0), "pulses", dates.get(0).toEpochMilli(),
				new BigDecimal("0"), BigDecimal.ZERO);
		assertVirtualMeterMetadata("2ns", savedMetas.get(1), "pulses", dates.get(5).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("0.1"));
		assertVirtualMeterMetadata("3rd", savedMetas.get(2), "pulses", dates.get(7).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("0.2"));
		assertVirtualMeterMetadata("4th", savedMetas.get(3), "pulses", dates.get(11).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("1"));
	}

	@Test
	public void filter_pulse_continueFromPartialAdvance() {
		// GIVEN
		final SimpleDatum datum = createTestGeneralNodeDatum(SOURCE_ID);
		datum.asMutableSampleOperations().setSampleData(DatumSamplesType.Instantaneous, null);
		final VirtualMeterConfig vmConfig = createTestVirtualMeterConfig("switch");
		vmConfig.setPropertyType(DatumSamplesType.Status);
		vmConfig.setReadingPropertyName("pulses");
		vmConfig.setTrackOnlyWhenReadingChanges(true);
		vmConfig.setMaxAgeSeconds(2);
		xform.setVirtualMeterConfigs(new VirtualMeterConfig[] { vmConfig });

		ExpressionService exprService = new SpelExpressionService();
		VirtualMeterExpressionConfig exprConfig = new VirtualMeterExpressionConfig("pulses",
				DatumSamplesType.Accumulating, pulseExpression(), exprService.getUid());
		xform.setExpressionConfigs(new VirtualMeterExpressionConfig[] { exprConfig });
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));

		// meter starting at 3.1, partially advanced but more than maxAgeSeconds ago, i.e. after reboot
		final Instant start = LocalDateTime.of(2021, 5, 14, 10, 0).toInstant(ZoneOffset.UTC);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("pulses", VIRTUAL_METER_DATE_KEY, start.minusSeconds(10).toEpochMilli());
		meta.putInfoValue("pulses", VIRTUAL_METER_VALUE_KEY, BigDecimal.ZERO);
		meta.putInfoValue("pulses", VIRTUAL_METER_READING_KEY, new BigDecimal("3.1"));
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta);

		// add metadata
		// @formatter:off
		final List<Integer> inputs = Arrays.asList(
				// start high for 1s
				1, 1,
				
				// jump low for 1s; meter advances 0.2
				0, 0,
				
				// back to high 1s; meter advances to 1
				1, 1);
		// @formatter:on
		final int iterations = inputs.size();
		final int changeCount = 3; // 2 changes + "reset" from maxAgeSeconds trip
		Capture<GeneralDatumMetadata> metaCaptor = new CloningCapture(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(SOURCE_ID), capture(metaCaptor));
		expectLastCall().times(changeCount);

		// WHEN
		replayAll();
		List<DatumSamplesOperations> outputs = new ArrayList<>();
		List<Instant> dates = new ArrayList<>();
		for ( int i = 0; i < iterations; i++ ) {
			Instant ts = start.plusMillis(TimeUnit.SECONDS.toMillis(i) / 2);
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), ts));
			d.getSamples().putStatusSampleValue("switch", inputs.get(i));
			dates.add(ts);
			outputs.add(xform.filter(d, d.getSamples(), emptyMap()));
		}

		// THEN

		// This test demonstrates what might happen if a node reboots in the middle of a pulse.
		// In this example, the node has started and the VM gets its first input 1, but the
		// VM has a value already of 3.1 which means it has stopped after a partial advance.
		// When the input changes to 0, the meter advances by 0.1 to reach 3.2 (another partial
		// advance). When the input changes back to 1, the meter rounds up to reach 4. Thus
		// the initial partial advance is discarded, as if the earlier pulse did not happen.

		// @formatter:off
		BigDecimal[] expectedInputs = decimalArray(   "1",   "1",   "0",   "0",  "1", "1");
		BigDecimal[] expectedReadings = decimalArray(null, "3.1", "3.2", "3.2",  "4", "4");
		// @formatter:on

		for ( int i = 0; i < iterations; i++ ) {
			DatumSamplesOperations result = outputs.get(i);
			assertOutputValue("at sample " + i, result, "switch", "pulses", expectedInputs[i],
					expectedReadings[i]);
		}

		List<GeneralDatumMetadata> savedMetas = metaCaptor.getValues();
		assertThat("Saved meter metdata only for changes", savedMetas, hasSize(changeCount));

		assertVirtualMeterMetadata("1st", savedMetas.get(0), "pulses", dates.get(0).toEpochMilli(),
				new BigDecimal("1"), null);
		assertVirtualMeterMetadata("2ns", savedMetas.get(1), "pulses", dates.get(2).toEpochMilli(),
				new BigDecimal("0"), new BigDecimal("3.2"));
		assertVirtualMeterMetadata("3rd", savedMetas.get(2), "pulses", dates.get(4).toEpochMilli(),
				new BigDecimal("1"), new BigDecimal("4"));
	}

}
