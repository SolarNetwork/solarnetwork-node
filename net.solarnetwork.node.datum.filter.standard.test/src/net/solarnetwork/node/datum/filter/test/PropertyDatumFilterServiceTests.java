/* ==================================================================
 * PropertyDatumFilterServiceTests.java - 31/10/2016 2:34:10 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.node.datum.filter.std.DatumFilterSupport.SETTING_KEY_TEMPLATE;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.datum.filter.std.PropertyDatumFilterService;
import net.solarnetwork.node.datum.filter.std.PropertyFilterConfig;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.OperationalModesService;

/**
 * Test cases for the {@link PropertyDatumFilterService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PropertyDatumFilterServiceTests {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final int TEST_FREQ = 1;
	private static final int TEST_SETTING_CACHE_SECS = 3;
	private static final String TEST_UID = "test";

	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATTHOURS = "wattHours";
	private static final String PROP_FREQUENCY = "frequency";
	private static final String PROP_PHASE = "phase";

	private SimpleDatum createTestGeneralNodeDatum(String sourceId) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		datum.getSamples().putInstantaneousSampleValue(PROP_FREQUENCY, 50.1);
		datum.getSamples().putInstantaneousSampleValue(PROP_WATTS, 23.4);
		datum.getSamples().putStatusSampleValue(PROP_PHASE, "Total");
		return datum;
	}

	@Before
	public void setup() {
		PropertyDatumFilterService.clearSettingCache();
	}

	@Test
	public void testExclude() {
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt" });
		xform.init();
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getSampleDouble(DatumSamplesType.Instantaneous, PROP_WATTS));
		assertEquals("Frequency",
				datum.getSamples().getSampleDouble(DatumSamplesType.Instantaneous, PROP_FREQUENCY),
				result.getSampleDouble(DatumSamplesType.Instantaneous, PROP_FREQUENCY));
		assertNull("Watt hours filtered",
				result.getSampleLong(DatumSamplesType.Accumulating, PROP_WATTHOURS));
		assertEquals("Phase", datum.getSamples().getSampleString(DatumSamplesType.Status, PROP_PHASE),
				result.getSampleString(DatumSamplesType.Status, PROP_PHASE));
	}

	@Test
	public void testExcludeMultiplePatterns() {
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt", "^phase$" });
		xform.init();
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getSampleDouble(DatumSamplesType.Instantaneous, PROP_WATTS));
		assertEquals("Frequency",
				datum.getSamples().getSampleDouble(DatumSamplesType.Instantaneous, PROP_FREQUENCY),
				result.getSampleDouble(DatumSamplesType.Instantaneous, PROP_FREQUENCY));
		assertNull("Watt hours filtered",
				result.getSampleLong(DatumSamplesType.Accumulating, PROP_WATTHOURS));
		assertNull("Phase filtered", result.getSampleString(DatumSamplesType.Status, PROP_PHASE));
	}

	@Test
	public void testNoMatchingExcludes() {
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^foo" });
		xform.init();
		DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
		assertSame("Sample sample instance", datum.getSamples(), result);
	}

	private String settingTypeValue(String sourceId, String propertyName) {
		return sourceId + ";" + propertyName;
	}

	@Test
	public void testIncludeLimitNoInitialSetting() {
		SettingDao settingDao = EasyMock.createMock(SettingDao.class);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.emptyList();
		expect(settingDao.getSettingValues(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = Capture.newInstance(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));
		EasyMock.expectLastCall().times(2);

		replay(settingDao);

		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), Instant.now()));
			DatumSamplesOperations result = xform.filter(d, d.getSamples(), emptyMap());
			if ( count == 0 ) {
				assertEquals("Not limited filter " + count, expectedSamples, result);
			} else {
				assertNull("Limited filter " + count, result);
			}
			count++;
		}
		assertTrue("More than 1 cycle examined", count > 1);

		assertNotNull("Seen date settings should be persisted", savedSettingCapture.getValues());
		assertEquals("Seen date settings count", 2, savedSettingCapture.getValues().size());

		for ( ListIterator<Setting> itr = savedSettingCapture.getValues().listIterator(); itr
				.hasNext(); ) {
			Setting savedSetting = itr.next();
			assertEquals("Setting key", settingKey, savedSetting.getKey());
			assertEquals("Setting flags",
					EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
					savedSetting.getFlags());
			assertTrue("Setting value",
					Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());

			String expectedPropName = null;
			switch (itr.previousIndex()) {
				case 0:
					expectedPropName = "watts";
					break;

				case 1:
					expectedPropName = "wattHours";
					break;

			}
			assertEquals("Setting type", settingTypeValue(datum.getSourceId(), expectedPropName),
					savedSetting.getType());
		}

		verify(settingDao);
	}

	@Test
	public void testIncludeLimitExpiredSetting() {
		SettingDao settingDao = EasyMock.createMock(SettingDao.class);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections
				.singletonList(new KeyValuePair(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS),
						Long.toString(System.currentTimeMillis() - TEST_FREQ * 10 * 1000L, 16)));
		expect(settingDao.getSettingValues(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = Capture.newInstance(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));
		EasyMock.expectLastCall().times(2);

		replay(settingDao);

		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), Instant.now()));
			DatumSamplesOperations result = xform.filter(d, d.getSamples(), emptyMap());
			if ( count == 0 ) {
				assertEquals("Not limited filter " + count, expectedSamples, result);
			} else {
				assertNull("Limited filter " + count, result);
			}
			count++;
		}
		assertTrue("More than 1 cycle examined", count > 1);

		assertNotNull("Seen date settings should be persisted", savedSettingCapture.getValues());
		assertEquals("Seen date settings count", 2, savedSettingCapture.getValues().size());

		for ( ListIterator<Setting> itr = savedSettingCapture.getValues().listIterator(); itr
				.hasNext(); ) {
			Setting savedSetting = itr.next();
			assertEquals("Setting key", settingKey, savedSetting.getKey());
			assertEquals("Setting flags",
					EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
					savedSetting.getFlags());
			assertTrue("Setting value",
					Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());

			String expectedPropName = null;
			switch (itr.previousIndex()) {
				case 0:
					expectedPropName = "watts";
					break;

				case 1:
					expectedPropName = "wattHours";
					break;

			}
			assertEquals("Setting type", settingTypeValue(datum.getSourceId(), expectedPropName),
					savedSetting.getType());
		}

		verify(settingDao);
	}

	@Test
	public void testIncludeLimitNonExpiredSetting() {
		SettingDao settingDao = EasyMock.createMock(SettingDao.class);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final long start = System.currentTimeMillis();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.singletonList(new KeyValuePair(
				settingTypeValue(TEST_SOURCE_ID, PROP_WATTS), Long.toString(start, 16)));
		expect(settingDao.getSettingValues(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = Capture.newInstance(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		DatumSamples firstExpectedSamples = new DatumSamples();
		firstExpectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);

		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
			if ( count == 0 ) {
				assertEquals("First limited filter " + count, firstExpectedSamples, result);
			} else {
				assertNull("Limited filter " + count, result);
			}
			count++;
		}
		assertTrue("More than 1 cycle examined", count > 1);

		assertNotNull("Seen date settings should be persisted", savedSettingCapture.getValues());
		assertEquals("Seen date settings count", 1, savedSettingCapture.getValues().size());

		for ( ListIterator<Setting> itr = savedSettingCapture.getValues().listIterator(); itr
				.hasNext(); ) {
			Setting savedSetting = itr.next();
			assertEquals("Setting key", settingKey, savedSetting.getKey());
			assertEquals("Setting flags",
					EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
					savedSetting.getFlags());
			assertTrue("Setting value",
					Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());
			assertEquals("Setting type", settingTypeValue(datum.getSourceId(), "wattHours"),
					savedSetting.getType());
		}

		verify(settingDao);
	}

	@Test
	public void testIncludeLimitDifferentExpiredSettings() {
		SettingDao settingDao = EasyMock.createMock(SettingDao.class);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watts$", 1),
				new PropertyFilterConfig("^wattHours$", 3) });
		xform.init();

		final long start = System.currentTimeMillis();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Arrays.asList(
				new KeyValuePair(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS),
						Long.toString(start - TEST_FREQ * 1000L - 500L, 16)),
				new KeyValuePair(settingTypeValue(TEST_SOURCE_ID, PROP_WATTHOURS),
						Long.toString(start - TEST_FREQ * 1000L - 500L, 16)));
		expect(settingDao.getSettingValues(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = Capture.newInstance(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
			if ( count == 0 ) {
				assertEquals("First limited filter " + count, expectedSamples, result);
			} else {
				assertNull("Limited filter " + count, result);
			}
			count++;
		}
		assertTrue("More than 1 cycle examined", count > 1);

		assertNotNull("Seen date settings should be persisted", savedSettingCapture.getValues());
		assertEquals("Seen date settings count", 1, savedSettingCapture.getValues().size());

		for ( ListIterator<Setting> itr = savedSettingCapture.getValues().listIterator(); itr
				.hasNext(); ) {
			Setting savedSetting = itr.next();
			assertEquals("Setting key", settingKey, savedSetting.getKey());
			assertEquals("Setting flags",
					EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
					savedSetting.getFlags());
			assertTrue("Setting value",
					Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());
			assertEquals("Setting type", settingTypeValue(datum.getSourceId(), "watts"),
					savedSetting.getType());
		}

		verify(settingDao);
	}

	@Test
	public void operationalMode_noMatch() {
		// GIVEN
		PropertyDatumFilterService xs = new PropertyDatumFilterService();
		xs.setSourceId("^test");
		xs.setExcludes(new String[] { "^watt" });
		xs.init();
		OperationalModesService opModesService = EasyMock.createMock(OperationalModesService.class);
		xs.setOpModesService(opModesService);
		xs.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		// WHEN
		replay(opModesService);
		SimpleDatum d = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("No change because required operational mode not active", out,
				is(sameInstance(d.getSamples())));
		verify(opModesService);
	}

	@Test
	public void operationalMode_match() {
		// GIVEN
		PropertyDatumFilterService xs = new PropertyDatumFilterService();
		xs.setSourceId("^test");
		xs.setExcludes(new String[] { "^watt" });
		xs.init();
		OperationalModesService opModesService = EasyMock.createMock(OperationalModesService.class);
		xs.setOpModesService(opModesService);
		xs.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);

		// WHEN
		replay(opModesService);
		SimpleDatum d = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		DatumSamplesOperations out = xs.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Change because required operational mode active", out,
				is(not(sameInstance(d.getSamples()))));
		assertThat("Watts filtered", out.getSampleDouble(Instantaneous, PROP_WATTS), is(nullValue()));
		assertThat("Frequency", d.getSampleBigDecimal(Instantaneous, PROP_FREQUENCY),
				is(equalTo(out.getSampleBigDecimal(Instantaneous, PROP_FREQUENCY))));
		assertThat("Watt hours filtered", out.getSampleLong(Accumulating, PROP_WATTHOURS),
				is(nullValue()));
		assertThat("Phase", d.getSampleString(Status, PROP_PHASE),
				is(equalTo(out.getSampleString(Status, PROP_PHASE))));
		verify(opModesService);
	}

}
