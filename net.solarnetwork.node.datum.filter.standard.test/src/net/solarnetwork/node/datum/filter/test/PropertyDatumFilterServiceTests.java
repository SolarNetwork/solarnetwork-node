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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.dao.DefaultTransientSettingDao;
import net.solarnetwork.node.dao.TransientSettingDao;
import net.solarnetwork.node.datum.filter.std.PropertyDatumFilterService;
import net.solarnetwork.node.datum.filter.std.PropertyFilterConfig;
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

	@Test
	public void testExclude() {
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
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
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
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
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
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
	public void testIncludeLimitNoInitialSetting() throws Exception {
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();

		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final Instant start = Instant.now();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);

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
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));

		final ConcurrentMap<String, Instant> settings = transientSettingDao.settings(settingKey);

		assertThat("Seen date for watts persisted",
				settings.get(settingTypeValue(datum.getSourceId(), "watts")),
				allOf(greaterThanOrEqualTo(start), lessThan(Instant.now())));
		assertThat("Seen date for wattHours persisted",
				settings.get(settingTypeValue(datum.getSourceId(), "wattHours")),
				allOf(greaterThanOrEqualTo(start), lessThan(Instant.now())));
	}

	@Test
	public void testIncludeLimitExpiredSetting() {
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);

		final Instant start = Instant.now();
		final ConcurrentMap<String, Instant> settings = transientSettingDao.settings(settingKey);
		final Instant lastSeen = start.minusSeconds(TEST_FREQ * 10);
		settings.put(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS), lastSeen);

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
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));

		assertThat("Seen date for watts persisted",
				settings.get(settingTypeValue(datum.getSourceId(), "watts")),
				allOf(greaterThanOrEqualTo(start), lessThan(Instant.now())));
		assertThat("Seen date for wattHours persisted",
				settings.get(settingTypeValue(datum.getSourceId(), "wattHours")),
				allOf(greaterThanOrEqualTo(start), lessThan(Instant.now())));
	}

	@Test
	public void testIncludeLimitNonExpiredSetting() {
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watt", 1) });
		xform.init();

		final Instant start = Instant.now();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		final ConcurrentMap<String, Instant> settings = transientSettingDao.settings(settingKey);
		final Instant lastSeen = start;
		settings.put(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS), lastSeen);

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
		assertThat("More than 1 cycle examined", count, is(greaterThan(1)));

		assertThat("Seen date for wattHours persisted",
				settings.get(settingTypeValue(datum.getSourceId(), "wattHours")),
				allOf(greaterThanOrEqualTo(start), lessThan(Instant.now())));
	}

	@Test
	public void testIncludeLimitDifferentExpiredSettings() {
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();

		final PropertyDatumFilterService xform = new PropertyDatumFilterService();
		xform.setTransientSettingDao(transientSettingDao);
		xform.setUid(TEST_UID);
		xform.setSourceId("^test");
		xform.setPropIncludes(new PropertyFilterConfig[] { new PropertyFilterConfig("^watts$", 1),
				new PropertyFilterConfig("^wattHours$", 3) });
		xform.init();

		final long start = System.currentTimeMillis();

		final String settingKey = String.format(SETTING_KEY_TEMPLATE, TEST_UID);
		final ConcurrentMap<String, Object> settings = transientSettingDao.settings(settingKey);
		final Instant lastSeen = Instant.ofEpochMilli(start - TEST_FREQ * 1000L - 500L);
		settings.put(settingTypeValue(TEST_SOURCE_ID, PROP_WATTS), lastSeen);
		settings.put(settingTypeValue(TEST_SOURCE_ID, PROP_WATTHOURS), lastSeen);

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
		assertThat("More than 1 cycle examined", count, greaterThan(1));

		assertThat("Last seen date value updated for watts property",
				(Instant) settings.get(settingTypeValue(datum.getSourceId(), "watts")),
				allOf(greaterThan(lastSeen), lessThan(Instant.now())));

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
		final TransientSettingDao transientSettingDao = new DefaultTransientSettingDao();
		PropertyDatumFilterService xs = new PropertyDatumFilterService();
		xs.setTransientSettingDao(transientSettingDao);
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
