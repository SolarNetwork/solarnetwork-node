/* ==================================================================
 * SourceThrottlingSamplesTransformerTests.java - 8/08/2017 4:32:44 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.datum.filter.std.SourceThrottlingSamplesTransformer;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.support.KeyValuePair;

/**
 * Test cases for the {@link SourceThrottlingSamplesTransformer} class.
 * 
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("deprecation")
public class SourceThrottlingSamplesTransformerTests {

	private static final int TEST_FREQ = 1;
	private static final int TEST_SETTING_CACHE_SECS = 3;
	private static final String PROP_WATTS = "watts";
	private static final String TEST_UID = "test";

	private SettingDao settingDao;
	private SourceThrottlingSamplesTransformer xform;

	@Before
	public void setup() {
		SourceThrottlingSamplesTransformer.clearSettingCache();
		settingDao = EasyMock.createMock(SettingDao.class);
		xform = new SourceThrottlingSamplesTransformer();
		xform.setFrequencySeconds(TEST_FREQ);
		xform.setSettingKey(format(SourceThrottlingSamplesTransformer.SETTING_KEY_TEMPLATE, TEST_UID));
		xform.setSourceId("^F");
		xform.setSettingDao(settingDao);
		xform.setSettingCacheSecs(TEST_SETTING_CACHE_SECS);
	}

	private GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId(sourceId);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	@Test
	public void testSourceNoMatch() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum("a");
		long stop = System.currentTimeMillis() + TEST_FREQ * 2 * 1000L + 100;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
			assertSame("Not filtered sample " + count, datum.getSamples(), result);
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		assertTrue("More than 1 cycle examined", count > 1);
	}

	@Test
	public void testSourceMatchNoInitialSetting() {
		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.emptyList();
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>();
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum("FILTER_ME");
		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
			if ( count == 0 ) {
				assertSame("Not filtered sample " + count, datum.getSamples(), result);
			} else {
				assertNull("Filtered sample " + count, result);
			}
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		assertTrue("More than 1 cycle examined", count > 1);

		Setting savedSetting = savedSettingCapture.getValue();
		assertNotNull("Procssed date setting should be persisted", savedSetting);
		assertEquals("Setting key", settingKey, savedSetting.getKey());
		assertEquals("Setting type", datum.getSourceId(), savedSetting.getType());
		assertEquals("Setting flags",
				EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
				savedSetting.getFlags());
		assertTrue("Setting value",
				Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());

		verify(settingDao);
	}

	@Test
	public void testSourceMatchExpiredSetting() {
		final GeneralNodeDatum datum = createTestGeneralNodeDatum("FILTER_ME");

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections
				.singletonList(new KeyValuePair(datum.getSourceId(),
						Long.toString(System.currentTimeMillis() - TEST_FREQ * 10 * 1000L, 16)));
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>();
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
			if ( count == 0 ) {
				assertSame("Not filtered sample " + count, datum.getSamples(), result);
			} else {
				assertNull("Filtered sample " + count, result);
			}
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		assertTrue("More than 1 cycle examined", count > 1);

		Setting savedSetting = savedSettingCapture.getValue();
		assertNotNull("Procssed date setting should be persisted", savedSetting);
		assertEquals("Setting key", settingKey, savedSetting.getKey());
		assertEquals("Setting type", datum.getSourceId(), savedSetting.getType());
		assertEquals("Setting flags",
				EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
				savedSetting.getFlags());
		assertTrue("Setting value",
				Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());

		verify(settingDao);
	}

	@Test
	public void testSourceMatchNonExpiredSetting() {
		final GeneralNodeDatum datum = createTestGeneralNodeDatum("FILTER_ME");
		final long start = System.currentTimeMillis();
		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections
				.singletonList(new KeyValuePair(datum.getSourceId(), Long.toString(start, 16)));
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		Capture<Setting> savedSettingCapture = new Capture<Setting>();
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		long stop = System.currentTimeMillis() + TEST_FREQ * 1000L + 900;
		int count = 0;
		int nonFilterCount = 0;
		while ( stop > System.currentTimeMillis() ) {
			long now = System.currentTimeMillis();
			GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
			if ( nonFilterCount > 0 || now < start + TEST_FREQ * 1000L ) {
				assertNull("Filtered sample " + count, result);
			} else if ( nonFilterCount == 0 ) {
				assertSame("Not filtered sample " + count, datum.getSamples(), result);
				nonFilterCount += 1;
			}
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		assertTrue("More than 1 cycle examined", count > 1);
		assertEquals("Non filter count", 1, nonFilterCount);

		Setting savedSetting = savedSettingCapture.getValue();
		assertNotNull("Procssed date setting should be persisted", savedSetting);
		assertEquals("Setting key", settingKey, savedSetting.getKey());
		assertEquals("Setting type", datum.getSourceId(), savedSetting.getType());
		assertEquals("Setting flags",
				EnumSet.of(Setting.SettingFlag.Volatile, Setting.SettingFlag.IgnoreModificationDate),
				savedSetting.getFlags());
		assertTrue("Setting value",
				Long.valueOf(savedSetting.getValue(), 16) < System.currentTimeMillis());

		verify(settingDao);
	}

	@Test
	public void settingCacheExpiresNoInitialSetting() throws InterruptedException {
		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections.emptyList();
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		final Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		// reload cached settings from DAO after expires
		expect(settingDao.getSettings(settingKey)).andAnswer(new IAnswer<List<KeyValuePair>>() {

			@Override
			public List<KeyValuePair> answer() throws Throwable {
				List<Setting> capturedSettings = savedSettingCapture.getValues();
				List<KeyValuePair> pairs = new ArrayList<KeyValuePair>();
				if ( capturedSettings != null ) {
					for ( Setting setting : capturedSettings ) {
						pairs.add(new KeyValuePair(setting.getType(), setting.getValue()));
					}
				}
				return pairs;
			}
		});

		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		GeneralNodeDatum datum = createTestGeneralNodeDatum("FILTER_ME");

		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertThat("Non-filtered 1st result", result, notNullValue());

		result = xform.transformSamples(datum, datum.getSamples());
		assertThat("Filtered 2nd result", result, nullValue());

		Thread.sleep(TEST_SETTING_CACHE_SECS * 1000L + 200);

		result = xform.transformSamples(datum, datum.getSamples());
		assertThat("Non-filtered 3rd result", result, notNullValue());

		Thread.sleep(100);

		List<Setting> savedSettings = savedSettingCapture.getValues();
		assertThat("Processed date setting should be persisted twice", savedSettings, hasSize(2));
		for ( Setting savedSetting : savedSettings ) {
			assertThat("Setting key", savedSetting.getKey(), equalTo(settingKey));
			assertThat("Setting type", savedSetting.getType(), equalTo(datum.getSourceId()));
			assertThat("Setting flags", savedSetting.getFlags(),
					equalTo((Set<Setting.SettingFlag>) EnumSet.of(Setting.SettingFlag.Volatile,
							Setting.SettingFlag.IgnoreModificationDate)));
			assertThat("Setting value", Long.valueOf(savedSetting.getValue(), 16),
					lessThan(System.currentTimeMillis()));
		}

		verify(settingDao);
	}

	@Test
	public void settingCacheExpiresInitiallyNotExpired() throws InterruptedException {
		final GeneralNodeDatum datum = createTestGeneralNodeDatum("FILTER_ME");
		final long start = System.currentTimeMillis();
		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		List<KeyValuePair> initialSettings = Collections
				.singletonList(new KeyValuePair(datum.getSourceId(), Long.toString(start, 16)));
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		// reload cached settings from DAO after expires (no changes)
		expect(settingDao.getSettings(settingKey)).andReturn(initialSettings);

		final Capture<Setting> savedSettingCapture = new Capture<Setting>(CaptureType.ALL);
		settingDao.storeSetting(capture(savedSettingCapture));

		replay(settingDao);

		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertThat("Filtered 1st result", result, nullValue());

		Thread.sleep(TEST_SETTING_CACHE_SECS * 1000L + 200);

		result = xform.transformSamples(datum, datum.getSamples());
		assertThat("Non-filtered 2rd result", result, notNullValue());

		Thread.sleep(100);

		List<Setting> savedSettings = savedSettingCapture.getValues();
		assertThat("Processed date setting should be persisted once", savedSettings, hasSize(1));
		for ( Setting savedSetting : savedSettings ) {
			assertThat("Setting key", savedSetting.getKey(), equalTo(settingKey));
			assertThat("Setting type", savedSetting.getType(), equalTo(datum.getSourceId()));
			assertThat("Setting flags", savedSetting.getFlags(),
					equalTo((Set<Setting.SettingFlag>) EnumSet.of(Setting.SettingFlag.Volatile,
							Setting.SettingFlag.IgnoreModificationDate)));
			assertThat("Setting value", Long.valueOf(savedSetting.getValue(), 16),
					lessThan(System.currentTimeMillis()));
		}

		verify(settingDao);
	}

}
