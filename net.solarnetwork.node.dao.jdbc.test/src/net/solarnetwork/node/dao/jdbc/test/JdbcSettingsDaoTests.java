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

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.test.context.transaction.BeforeTransaction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcSettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.Setting.SettingFlag;
import net.solarnetwork.node.domain.SettingNote;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcSettingDao}.
 *
 * @author matt
 * @version 1.2
 */
public class JdbcSettingsDaoTests extends AbstractNodeTransactionalTest {

	private JdbcSettingDao dao;
	private SettingDao settingDao; // to work with just public API
	private EventAdmin eventAdminMock;

	@BeforeTransaction
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		dao = new JdbcSettingDao();
		dao.setDataSource(dataSource);
		dao.setTransactionTemplate(txTemplate);

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
	public void insertWithNote() {
		// GIVEN
		Capture<Event> eventCapture = Capture.newInstance();
		eventAdminMock.postEvent(capture(eventCapture));

		// WHEN
		replay(eventAdminMock);

		Setting s = new Setting(TEST_KEY, TEST_TYPE, TEST_VALUE, "Test note here.", null);
		settingDao.storeSetting(s);

		// THEN
		Event event = eventCapture.getValue();
		assertThat("Event generated", event, is(notNullValue()));
		assertThat("Event topic", event.getTopic(), is(equalTo(SettingDao.EVENT_TOPIC_SETTING_CHANGED)));
		assertThat("Event key property", event.getProperty(SettingDao.SETTING_KEY),
				is(equalTo(TEST_KEY)));
		assertThat("Event type property", event.getProperty(SettingDao.SETTING_TYPE),
				is(equalTo(TEST_TYPE)));
		assertThat("Event value property", event.getProperty(SettingDao.SETTING_VALUE),
				is(equalTo(TEST_VALUE)));
	}

	@Test
	public void findNotesForKey() {
		// GIVEN
		final int keyCount = 2;
		final int typeCount = 4;
		final Map<String, List<Setting>> settings = new HashMap<>(2);
		for ( int i = 0; i < keyCount; i++ ) {
			for ( int j = 0; j < typeCount; j++ ) {
				// note added only for even-numbered types
				Setting s = new Setting(String.format("key.%d", i), String.format("type.%d", j),
						String.format("value.%d.%d", i, j),
						j % 2 == 0 ? String.format("note.%d.%d", i, j) : null, null);
				settingDao.storeSetting(s);
				settings.computeIfAbsent(s.getKey(), k -> new ArrayList<>()).add(s);
			}
		}

		// WHEN
		final String keyToFind = "key.0";
		List<SettingNote> result = settingDao.notesForKey(keyToFind);

		// THEN
		assertThat("Results available for key", result, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			SettingNote note = result.get(i);
			assertThat("Notes returned for requested key", note.getKey(), is(equalTo(keyToFind)));
			assertThat("Notes returned in type order", note.getType(),
					is(equalTo(String.format("type.%d", i * 2))));
			assertThat("Note returned", note.getNote(), is(equalTo(String.format("note.0.%d", i * 2))));
		}
	}

	@Test
	public void insertNotes() {
		final int keyCount = 2;
		final int typeCount = 4;
		final Map<String, List<SettingNote>> settings = new HashMap<>(2);
		for ( int i = 0; i < keyCount; i++ ) {
			for ( int j = 0; j < typeCount; j++ ) {
				SettingNote s = Setting.note(String.format("key.%d", i), String.format("type.%d", j),
						String.format("note.%d.%d", i, j));
				settings.computeIfAbsent(s.getKey(), k -> new ArrayList<>()).add(s);
			}
		}

		// WHEN
		final List<SettingNote> notes = settings.values().stream().flatMap(l -> l.stream())
				.collect(Collectors.toList());
		settingDao.storeNotes(notes);

		// THEN
		for ( Entry<String, List<SettingNote>> e : settings.entrySet() ) {
			List<SettingNote> persisted = settingDao.notesForKey(e.getKey());
			assertThat("Persisted all notes", persisted, hasSize(e.getValue().size()));
			for ( int i = 0; i < typeCount; i++ ) {
				SettingNote persistedNote = persisted.get(i);
				assertThat("Key persisted", persistedNote.getKey(), is(equalTo(e.getKey())));
				assertThat("Type persisted", persistedNote.getType(),
						is(equalTo(e.getValue().get(i).getType())));
				assertThat("Note persisted", persistedNote.getNote(),
						is(equalTo(e.getValue().get(i).getNote())));
			}
		}
	}

	@Test
	public void updateNotes() {
		final int keyCount = 2;
		final int typeCount = 4;
		final Map<String, List<Setting>> settings = new HashMap<>(2);
		for ( int i = 0; i < keyCount; i++ ) {
			for ( int j = 0; j < typeCount; j++ ) {
				// note added only for even-numbered types
				Setting s = new Setting(String.format("key.%d", i), String.format("type.%d", j),
						String.format("value.%d.%d", i, j),
						j % 2 == 0 ? String.format("note.%d.%d", i, j) : null, null);
				settingDao.storeSetting(s);
				settings.computeIfAbsent(s.getKey(), k -> new ArrayList<>()).add(s);
			}
		}

		// WHEN
		final String updateKey = "key.0";
		final List<Setting> notes = settings.get(updateKey);
		for ( Setting s : notes ) {
			s.setNote((s.getNote() != null ? s.getNote() : "") + " UPDATED");
		}
		settingDao.storeNotes(notes);

		// THEN
		List<SettingNote> persisted = settingDao.notesForKey(updateKey);
		assertThat("Persisted all notes", persisted, hasSize(notes.size()));
		for ( int i = 0; i < typeCount; i++ ) {
			SettingNote persistedNote = persisted.get(i);
			assertThat("Key preserved", persistedNote.getKey(), is(equalTo(updateKey)));
			assertThat("Type preserved", persistedNote.getType(), is(equalTo(notes.get(i).getType())));
			assertThat("Note updated", persistedNote.getNote(), is(equalTo(notes.get(i).getNote())));
		}
	}

	@Test
	public void emptyNotePersistedAsNull() {
		// GIVEN
		Setting s = new Setting("k", "t", "v", "", null);
		settingDao.storeSetting(s);

		// WHEN
		Setting result = settingDao.readSetting(s.getKey(), s.getType());

		// THEN
		assertThat("Result available", result, is(not(nullValue())));
		assertThat("Empty note persisted as NULL", result.getNote(), is(nullValue()));
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
				} else if ( (TEST_KEY + "4").equals(domainObject.getKey()) ) {
					domainObject.setValue(TEST_VALUE + ".UPDATED");
					domainObject.setNote("This is my note, UPDATED.");
					action = BatchCallbackResult.UPDATE;
				} else {
					action = BatchCallbackResult.CONTINUE;
				}
				processed.incrementAndGet();
				return action;
			}
		}, new BasicBatchOptions("Test", BasicBatchOptions.DEFAULT_BATCH_SIZE, true, null));
		assertThat("Processed all rows", processed.intValue(), is(equalTo(count)));
		assertThat("Batch delete executed", settingDao.getSetting(TEST_KEY + "0", TEST_TYPE),
				is(nullValue()));
		assertThat("Updated row 1", settingDao.getSetting(TEST_KEY + 1, TEST_TYPE),
				is(equalTo(TEST_VALUE + ".UPDATED")));
		assertThat("Updated row 3", settingDao.getSetting(TEST_KEY + 3, TEST_TYPE),
				is(equalTo(TEST_VALUE + ".UPDATED")));

		Setting s = settingDao.readSetting(TEST_KEY + "4", TEST_TYPE);
		assertThat("Setting value updated", s.getValue(), is(equalTo(TEST_VALUE + ".UPDATED")));
		assertThat("Setting note updated", s.getNote(), is(equalTo("This is my note, UPDATED.")));
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
	public void readSingle_withNote() {
		// GIVEN
		Setting in = new Setting(TEST_KEY, TEST_TYPE, TEST_VALUE, "This is my test note.",
				EnumSet.of(Setting.SettingFlag.IgnoreModificationDate));
		settingDao.storeSetting(in);

		// WHEN
		Setting s = settingDao.readSetting(TEST_KEY, TEST_TYPE);

		// THEN
		assertThat("Setting returned", s, is(notNullValue()));
		assertThat("Different instance returneed", s, is(not(sameInstance(in))));
		assertThat("Key matches input", s.getKey(), is(equalTo(in.getKey())));
		assertThat("Type matches input", s.getType(), is(equalTo(in.getType())));
		assertThat("Value matches input", s.getValue(), is(equalTo(in.getValue())));
		assertThat("Note matches input", s.getNote(), is(equalTo(in.getNote())));
		assertThat("Flags preserved", s.getFlags(), is(equalTo(in.getFlags())));
		assertThat("Modification date populated", s.getModified(), is(notNullValue()));
	}

	@Test
	public void mostRecentDate() throws Exception {
		// GIVEN
		final int count = 5;
		Instant before = null;
		Instant after = null;
		for ( int i = 0; i < count; i += 1 ) {
			before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
			after = Instant.now().truncatedTo(ChronoUnit.MILLIS);
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
			before = Instant.now().truncatedTo(MILLIS);
			settingDao.storeSetting(TEST_KEY + i, TEST_TYPE, TEST_VALUE);
			after = Instant.now().truncatedTo(MILLIS);
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
		Instant before = Instant.now().truncatedTo(MILLIS);
		settingDao.storeSetting(TEST_KEY + 2, TEST_TYPE, TEST_VALUE + " updated");
		Instant after = Instant.now().truncatedTo(MILLIS);

		// WHEN
		Date result = settingDao.getMostRecentModificationDate();

		// THEN
		assertThat("Most recent date returned", result.toInstant(),
				allOf(greaterThanOrEqualTo(before), lessThanOrEqualTo(after)));
	}

}
