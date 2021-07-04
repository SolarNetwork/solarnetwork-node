/* ==================================================================
 * PropertyFilterSampleTransformerTests.java - 31/10/2016 2:34:10 PM
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

import static java.lang.String.format;
import static net.solarnetwork.node.datum.filter.std.SamplesTransformerSupport.SETTING_KEY_TEMPLATE;
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
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.datum.filter.std.PropertyFilterConfig;
import net.solarnetwork.node.datum.filter.std.PropertyFilterSamplesTransformer;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.support.KeyValuePair;

/**
 * Test cases for the {@link PropertyFilterSamplesTransformer} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class PropertyFilterSampleTransformerTests {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final int TEST_FREQ = 1;
	private static final int TEST_SETTING_CACHE_SECS = 3;
	private static final String TEST_UID = "test";

	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATTHOURS = "wattHours";
	private static final String PROP_FREQUENCY = "frequency";
	private static final String PROP_PHASE = "phase";

	private GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId(sourceId);
		datum.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		datum.putInstantaneousSampleValue(PROP_FREQUENCY, 50.1);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		datum.putStatusSampleValue(PROP_PHASE, "Total");
		return datum;
	}

	@Before
	public void setup() {
		PropertyFilterSamplesTransformer.clearSettingCache();
	}

	@Test
	public void testInclude() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertEquals("Watts", datum.getSamples().getInstantaneousSampleDouble(PROP_WATTS),
				result.getInstantaneousSampleDouble(PROP_WATTS));
		assertNull("Frequency filtered", result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertEquals("Watt hours", datum.getSamples().getAccumulatingSampleLong(PROP_WATTHOURS),
				result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertNull("Phase filtered", result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testIncludeMultiplePatterns() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt", "^phase$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertEquals("Watts", datum.getSamples().getInstantaneousSampleDouble(PROP_WATTS),
				result.getInstantaneousSampleDouble(PROP_WATTS));
		assertNull("Frequency filtered", result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertEquals("Watt hours", datum.getSamples().getAccumulatingSampleLong(PROP_WATTHOURS),
				result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertEquals("Phase", datum.getSamples().getStatusSampleString(PROP_PHASE),
				result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testExclude() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getInstantaneousSampleDouble(PROP_WATTS));
		assertEquals("Frequency", datum.getSamples().getInstantaneousSampleDouble(PROP_FREQUENCY),
				result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertNull("Watt hours filtered", result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertEquals("Phase", datum.getSamples().getStatusSampleString(PROP_PHASE),
				result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testExcludeMultiplePatterns() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt", "^phase$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getInstantaneousSampleDouble(PROP_WATTS));
		assertEquals("Frequency", datum.getSamples().getInstantaneousSampleDouble(PROP_FREQUENCY),
				result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertNull("Watt hours filtered", result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertNull("Phase filtered", result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testIncludeAndExclude() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt" });
		xform.setExcludes(new String[] { "^watts$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getInstantaneousSampleDouble(PROP_WATTS));
		assertNull("Frequency filtered", result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertEquals("Watt hours", datum.getSamples().getAccumulatingSampleLong(PROP_WATTHOURS),
				result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertNull("Phase filtered", result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testNonMatchingSourceId() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum("other.source");
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt" });
		xform.setExcludes(new String[] { "^watts$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertSame("Sample sample instance", datum.getSamples(), result);
	}

	@Test
	public void testNoMatchingIncludes() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^foo" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNull("Entire sample filtered", result);
	}

	@Test
	public void testNoMatchingExcludes() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^foo" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertSame("Sample sample instance", datum.getSamples(), result);
	}

	private String settingTypeValue(String sourceId, String propertyName) {
		return sourceId + ";" + propertyName;
	}

	@Test
	public void testIncludeLimitNoInitialSetting() {
		SettingDao settingDao = EasyMock.createMock(SettingDao.class);
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setSettingKey(format(PropertyFilterSamplesTransformer.SETTING_KEY_TEMPLATE, TEST_UID));
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.emptyList();
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));
		EasyMock.expectLastCall().times(2);

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		GeneralDatumSamples expectedSamples = new GeneralDatumSamples();
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
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
					expectedPropName = "wattHours";
					break;

				case 1:
					expectedPropName = "watts";
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
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setSettingKey(format(PropertyFilterSamplesTransformer.SETTING_KEY_TEMPLATE, TEST_UID));
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections
				.singletonList(new KeyValuePair(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS),
						Long.toString(System.currentTimeMillis() - TEST_FREQ * 10 * 1000L, 16)));
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));
		EasyMock.expectLastCall().times(2);

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		GeneralDatumSamples expectedSamples = new GeneralDatumSamples();
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
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
					expectedPropName = "wattHours";
					break;

				case 1:
					expectedPropName = "watts";
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
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setSettingKey(format(PropertyFilterSamplesTransformer.SETTING_KEY_TEMPLATE, TEST_UID));
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final long start = System.currentTimeMillis();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.singletonList(new KeyValuePair(
				settingTypeValue(TEST_SOURCE_ID, PROP_WATTS), Long.toString(start, 16)));
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		GeneralDatumSamples firstExpectedSamples = new GeneralDatumSamples();
		firstExpectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);

		GeneralDatumSamples expectedSamples = new GeneralDatumSamples();
		expectedSamples.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
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
		PropertyFilterSamplesTransformer xform = new PropertyFilterSamplesTransformer();
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
		xform.setSettingKey(format(PropertyFilterSamplesTransformer.SETTING_KEY_TEMPLATE, TEST_UID));
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
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		GeneralDatumSamples expectedSamples = new GeneralDatumSamples();
		expectedSamples.putInstantaneousSampleValue(PROP_WATTS, 23.4);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
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
		PropertyFilterSamplesTransformer xs = new PropertyFilterSamplesTransformer();
		xs.setSourceId("^test");
		xs.setExcludes(new String[] { "^watt" });
		xs.init();
		OperationalModesService opModesService = EasyMock.createMock(OperationalModesService.class);
		xs.setOpModesService(opModesService);
		xs.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		// WHEN
		replay(opModesService);
		GeneralNodeDatum d = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("No change because required operational mode not active", out,
				is(sameInstance(d.getSamples())));
		verify(opModesService);
	}

	@Test
	public void operationalMode_match() {
		// GIVEN
		PropertyFilterSamplesTransformer xs = new PropertyFilterSamplesTransformer();
		xs.setSourceId("^test");
		xs.setExcludes(new String[] { "^watt" });
		xs.init();
		OperationalModesService opModesService = EasyMock.createMock(OperationalModesService.class);
		xs.setOpModesService(opModesService);
		xs.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);

		// WHEN
		replay(opModesService);
		GeneralNodeDatum d = createTestGeneralNodeDatum(TEST_SOURCE_ID);

		GeneralDatumSamples out = xs.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Change because required operational mode active", out,
				is(not(sameInstance(d.getSamples()))));
		assertThat("Watts filtered", out.getInstantaneousSampleDouble(PROP_WATTS), is(nullValue()));
		assertThat("Frequency", d.getInstantaneousSampleBigDecimal(PROP_FREQUENCY),
				is(equalTo(out.getInstantaneousSampleBigDecimal(PROP_FREQUENCY))));
		assertThat("Watt hours filtered", out.getAccumulatingSampleLong(PROP_WATTHOURS),
				is(nullValue()));
		assertThat("Phase", d.getStatusSampleString(PROP_PHASE),
				is(equalTo(out.getStatusSampleString(PROP_PHASE))));
		verify(opModesService);
	}

}
