/* ==================================================================
 * ThrottlingDatumFilterServiceTests.java - 8/08/2017 4:32:44 PM
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

import static java.util.Collections.emptyMap;
import static net.solarnetwork.node.datum.filter.std.DatumFilterSupport.SETTING_KEY_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.dao.DefaultTransientSettingDao;
import net.solarnetwork.node.dao.TransientSettingDao;
import net.solarnetwork.node.datum.filter.std.ThrottlingDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Test cases for the {@link ThrottlingDatumFilterService} class.
 *
 * @author matt
 * @version 1.1
 */
public class ThrottlingDatumFilterServiceTests {

	private static final int TEST_FREQ = 1;
	private static final String PROP_WATTS = "watts";
	private static final String TEST_UID = "test";

	private TransientSettingDao transientSettingDao;
	private ThrottlingDatumFilterService xform;

	@Before
	public void setup() {
		transientSettingDao = new DefaultTransientSettingDao();
		xform = new ThrottlingDatumFilterService();
		xform.setFrequencySeconds(TEST_FREQ);
		xform.setSourceId("^F");
		xform.setTransientSettingDao(transientSettingDao);
		xform.setUid(TEST_UID);
		xform.init();
	}

	private SimpleDatum createTestGeneralNodeDatum(String sourceId) {
		SimpleDatum datum = new SimpleDatum(DatumId.nodeId(null, sourceId, Instant.now()),
				new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	@Test
	public void testSourceNoMatch() {
		SimpleDatum datum = createTestGeneralNodeDatum("a");
		long stop = System.currentTimeMillis() + TEST_FREQ * 2 * 1000L + 100;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
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

		SimpleDatum datum = createTestGeneralNodeDatum("FILTER_ME");
		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
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
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));
		assertThat("Last seen date persisted",
				(Instant) transientSettingDao.settings(settingKey).get(datum.getSourceId()),
				is(lessThanOrEqualTo(Instant.now())));
	}

	@Test
	public void testSourceMatchExpiredSetting() {
		final SimpleDatum datum = createTestGeneralNodeDatum("FILTER_ME");

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);

		final Instant start = Instant.now();
		final ConcurrentMap<String, Instant> settings = transientSettingDao.settings(settingKey);
		final Instant lastSeen = start.minusSeconds(TEST_FREQ * 10);
		settings.put(datum.getSourceId(), lastSeen);

		long stop = System.currentTimeMillis() + TEST_FREQ * 900L;
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			DatumSamplesOperations result = xform.filter(datum, datum.getSamples(), emptyMap());
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
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));
		assertThat("Last seen date persisted",
				(Instant) transientSettingDao.settings(settingKey).get(datum.getSourceId()),
				allOf(greaterThan(lastSeen), lessThanOrEqualTo(Instant.now())));
	}

	@Test
	public void testSourceMatchNonExpiredSetting() {
		final SimpleDatum datum = createTestGeneralNodeDatum("FILTER_ME");
		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		final Instant start = Instant.now();
		final ConcurrentMap<String, Instant> settings = transientSettingDao.settings(settingKey);
		final Instant lastSeen = start;
		settings.put(datum.getSourceId(), lastSeen);

		long stop = System.currentTimeMillis() + TEST_FREQ * 1000L + 900;
		int count = 0;
		int nonFilterCount = 0;
		while ( stop > System.currentTimeMillis() ) {
			Instant now = Instant.now();
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), now));
			DatumSamplesOperations result = xform.filter(d, d.getSamples(), emptyMap());
			if ( nonFilterCount > 0 || now.isBefore(start.plusSeconds(TEST_FREQ)) ) {
				assertNull("Filtered sample " + count, result);
			} else if ( nonFilterCount == 0 ) {
				assertSame("Not filtered sample " + count, d.getSamples(), result);
				nonFilterCount += 1;
			}
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));
		assertThat("Non filter count", nonFilterCount, is(1));
		assertThat("Last seen date persisted", settings.get(datum.getSourceId()),
				allOf(greaterThan(lastSeen), lessThanOrEqualTo(Instant.now())));
	}

	@Test
	public void testSourceMatch_discardAll() {
		// GIVEN
		xform.setFrequencySeconds(ThrottlingDatumFilterService.DISCARD_FREQUENCY_SECONDS);

		final SimpleDatum datum = createTestGeneralNodeDatum("FILTER_ME");

		long stop = System.currentTimeMillis() + TEST_FREQ * 1000L + 900;

		// WHEN
		int count = 0;
		while ( stop > System.currentTimeMillis() ) {
			Instant now = Instant.now();
			SimpleDatum d = datum.copyWithId(DatumId.nodeId(null, datum.getSourceId(), now));
			DatumSamplesOperations result = xform.filter(d, d.getSamples(), emptyMap());
			assertThat("Datum is filtered", result, is(nullValue()));
			count++;
			try {
				Thread.sleep(200);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));

		final ConcurrentMap<String, Instant> settings = transientSettingDao
				.settings(String.format(SETTING_KEY_TEMPLATE, TEST_UID));

		assertThat("No settings persisted", settings.keySet(), hasSize(0));
	}

}
