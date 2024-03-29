/* ==================================================================
 * JdbcSettingsDaoTests.java - 7/06/2016 8:32:09 pm
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

package net.solarnetwork.node.dao.jdbc.test;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.dao.BasicBatchOptions;
import net.solarnetwork.node.dao.BatchableDao;
import net.solarnetwork.node.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcSettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.Setting.SettingFlag;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcSettingDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSettingsDaoTests extends AbstractNodeTransactionalTest {

	private JdbcSettingDao dao;
	private SettingDao settingDao; // to work with just public API
	private EventAdmin eventAdminMock;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		dao = new JdbcSettingDao();
		dao.setDataSource(dataSource);
		dao.setTransactionTemplate(txTemplate);
		dao.setSqlResourcePrefix(
				String.format("%s-settings", dataSource.getDatabaseType().toString().toLowerCase()));

		eventAdminMock = EasyMock.createMock(EventAdmin.class);
		dao.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdminMock));
		settingDao = dao;
	}

	private static final String TEST_KEY = "_test_key_";
	private static final String TEST_TYPE = "_test_type_";
	private static final String TEST_VALUE = "_test_value_";

	@Test
	public void insertWithChangeEvent() {
		Capture<Event> eventCapture = Capture.newInstance();
		eventAdminMock.postEvent(EasyMock.capture(eventCapture));

		replay(eventAdminMock);

		settingDao.storeSetting(TEST_KEY, TEST_TYPE, TEST_VALUE);

		verify(eventAdminMock);

		Event event = eventCapture.getValue();
		Assert.assertNotNull(event);
		Assert.assertEquals(SettingDao.EVENT_TOPIC_SETTING_CHANGED, event.getTopic());
		Assert.assertEquals(TEST_KEY, event.getProperty(SettingDao.SETTING_KEY));
		Assert.assertEquals(TEST_TYPE, event.getProperty(SettingDao.SETTING_TYPE));
		Assert.assertEquals(TEST_VALUE, event.getProperty(SettingDao.SETTING_VALUE));
	}

	@Test
	public void insertVolatile() {
		replay(eventAdminMock);

		Setting s = new Setting();
		s.setKey(TEST_KEY);
		s.setType(TEST_TYPE);
		s.setValue(TEST_VALUE);
		s.setFlags(EnumSet.of(SettingFlag.Volatile));

		settingDao.storeSetting(s);

		verify(eventAdminMock);
	}

	@Test
	public void updateWithChangeEvent() {
		insertWithChangeEvent();
		EasyMock.reset(eventAdminMock);

		Capture<Event> eventCapture = Capture.newInstance();
		eventAdminMock.postEvent(EasyMock.capture(eventCapture));

		replay(eventAdminMock);

		settingDao.storeSetting(TEST_KEY, TEST_TYPE, "foo");

		verify(eventAdminMock);

		Event event = eventCapture.getValue();
		Assert.assertNotNull(event);
		Assert.assertEquals(SettingDao.EVENT_TOPIC_SETTING_CHANGED, event.getTopic());
		Assert.assertEquals(TEST_KEY, event.getProperty(SettingDao.SETTING_KEY));
		Assert.assertEquals(TEST_TYPE, event.getProperty(SettingDao.SETTING_TYPE));
		Assert.assertEquals("foo", event.getProperty(SettingDao.SETTING_VALUE));
	}

	@Test
	public void updateVolatile() {
		insertVolatile();
		EasyMock.reset(eventAdminMock);

		replay(eventAdminMock);

		Setting s = new Setting();
		s.setKey(TEST_KEY);
		s.setType(TEST_TYPE);
		s.setValue("foo");
		s.setFlags(EnumSet.of(SettingFlag.Volatile));

		settingDao.storeSetting(s);

		verify(eventAdminMock);
	}

	@Test
	public void deleteWithChangeEvent() {
		insertWithChangeEvent();
		EasyMock.reset(eventAdminMock);

		Capture<Event> eventCapture = Capture.newInstance();
		eventAdminMock.postEvent(EasyMock.capture(eventCapture));

		replay(eventAdminMock);

		boolean result = settingDao.deleteSetting(TEST_KEY, TEST_TYPE);

		verify(eventAdminMock);

		Assert.assertTrue(result);

		Event event = eventCapture.getValue();
		Assert.assertNotNull(event);
		Assert.assertEquals(SettingDao.EVENT_TOPIC_SETTING_CHANGED, event.getTopic());
		Assert.assertEquals(TEST_KEY, event.getProperty(SettingDao.SETTING_KEY));
		Assert.assertEquals(TEST_TYPE, event.getProperty(SettingDao.SETTING_TYPE));
		Assert.assertEquals(TEST_VALUE, event.getProperty(SettingDao.SETTING_VALUE));
	}

	@Test
	public void deleteVolatile() {
		insertVolatile();
		EasyMock.reset(eventAdminMock);

		replay(eventAdminMock);

		boolean result = settingDao.deleteSetting(TEST_KEY, TEST_TYPE);

		verify(eventAdminMock);

		Assert.assertTrue(result);
	}

	@Test
	public void deleteWithoutType() {
		insertVolatile();
		EasyMock.reset(eventAdminMock);

		replay(eventAdminMock);

		boolean result = settingDao.deleteSetting(TEST_KEY);

		verify(eventAdminMock);

		Assert.assertTrue(result);
	}

	@Test
	public void batchRead() {
		final int count = 5;
		for ( int i = 0; i < count; i += 1 ) {
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
		}
		final AtomicInteger processed = new AtomicInteger(0);
		settingDao.batchProcess(new BatchableDao.BatchCallback<Setting>() {

			@Override
			public BatchableDao.BatchCallbackResult handle(Setting domainObject) {
				Assert.assertNotNull(domainObject);
				if ( TEST_TYPE.equals(domainObject.getType()) ) { // skip other stuff
					assertEquals(TEST_KEY + processed.intValue(), domainObject.getKey());
					assertEquals(TEST_VALUE, domainObject.getValue());
					processed.incrementAndGet();
				}
				return BatchableDao.BatchCallbackResult.CONTINUE;
			}
		}, new BasicBatchOptions("Test"));
		assertEquals(count, processed.intValue());
	}

	@Test
	public void batchUpdate() {
		final int count = 5;
		for ( int i = 0; i < count; i += 1 ) {
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
		}
		final AtomicInteger processed = new AtomicInteger(0);
		settingDao.batchProcess(new BatchableDao.BatchCallback<Setting>() {

			@Override
			public BatchCallbackResult handle(Setting domainObject) {
				Assert.assertNotNull(domainObject);
				if ( !TEST_TYPE.equals(domainObject.getType()) ) { // skip other stuff
					return BatchCallbackResult.CONTINUE;
				}
				BatchCallbackResult action;
				if ( (TEST_KEY + "0").equals(domainObject.getKey()) ) {
					action = BatchCallbackResult.DELETE;
				} else if ( (TEST_KEY + "1").equals(domainObject.getKey()) ) {
					domainObject.setValue(TEST_VALUE + ".UPDATED");
					action = BatchCallbackResult.UPDATE;
				} else if ( (TEST_KEY + "3").equals(domainObject.getKey()) ) {
					domainObject.setValue(TEST_VALUE + ".UPDATED");
					action = BatchCallbackResult.UPDATE;
				} else {
					action = BatchCallbackResult.CONTINUE;
				}
				processed.incrementAndGet();
				return action;
			}
		}, new BasicBatchOptions("Test", BasicBatchOptions.DEFAULT_BATCH_SIZE, true, null));
		assertEquals(count, processed.intValue());

		Assert.assertNull(settingDao.getSetting(TEST_KEY + "0", TEST_TYPE));
		assertEquals(TEST_VALUE + ".UPDATED", settingDao.getSetting(TEST_KEY + 1, TEST_TYPE));
		assertEquals(TEST_VALUE + ".UPDATED", settingDao.getSetting(TEST_KEY + 3, TEST_TYPE));
	}

	@Test
	public void readSingle() {
		Setting in = new Setting(TEST_KEY, TEST_TYPE, TEST_VALUE,
				EnumSet.of(Setting.SettingFlag.IgnoreModificationDate));
		settingDao.storeSetting(in);
		Setting s = settingDao.readSetting(TEST_KEY, TEST_TYPE);
		Assert.assertNotNull(s);
		Assert.assertNotSame(in, s);
		assertEquals(TEST_KEY, s.getKey());
		assertEquals(TEST_TYPE, s.getType());
		assertEquals(TEST_VALUE, s.getValue());
		Assert.assertNotNull(s.getModified());
		assertEquals(in.getFlags(), s.getFlags());
	}

	@Test
	public void mostRecentDate() throws Exception {
		// GIVEN
		final int count = 5;
		Instant before = null;
		Instant after = null;
		for ( int i = 0; i < count; i += 1 ) {
			before = Instant.now();
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
			after = Instant.now();
			Thread.sleep(200);
		}

		// WHEN
		Date result = settingDao.getMostRecentModificationDate();

		// THEN
		assertThat("Most recent date returned", result.toInstant(),
				allOf(greaterThanOrEqualTo(before), lessThanOrEqualTo(after)));
	}

	@Test
	public void mostRecentDate_unchangedValue() throws Exception {
		// GIVEN
		final int count = 5;
		Instant before = null;
		Instant after = null;
		for ( int i = 0; i < count; i += 1 ) {
			before = Instant.now();
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
			after = Instant.now();
			Thread.sleep(200);
		}

		// we're not changing the value, so the latest date is still the old value
		settingDao.storeSetting(TEST_KEY + 2, TEST_TYPE, TEST_VALUE);

		// WHEN
		Date result = settingDao.getMostRecentModificationDate();

		// THEN
		assertThat("Most recent date returned", result.toInstant(),
				allOf(greaterThanOrEqualTo(before), lessThanOrEqualTo(after)));
	}

	@Test
	public void mostRecentDate_changedValue() throws Exception {
		// GIVEN
		final int count = 5;
		for ( int i = 0; i < count; i += 1 ) {
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
		}
		Thread.sleep(200);

		// the modification date is set by the DAO, so have to box the update by time to verify date
		Instant before = Instant.now();
		settingDao.storeSetting(TEST_KEY + 2, TEST_TYPE, TEST_VALUE + " updated");
		Instant after = Instant.now();

		// WHEN
		Date result = settingDao.getMostRecentModificationDate();

		// THEN
		assertThat("Most recent date returned", result.toInstant(),
				allOf(greaterThanOrEqualTo(before), lessThanOrEqualTo(after)));
	}

}
