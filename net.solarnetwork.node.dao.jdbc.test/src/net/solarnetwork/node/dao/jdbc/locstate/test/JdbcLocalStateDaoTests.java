/* ==================================================================
 * JdbcLocalStateDaoTests.java - 14/04/2025 11:58:28 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.locstate.test;

import static java.time.temporal.ChronoUnit.MINUTES;
import static net.solarnetwork.dao.GenericDao.ENTITY_EVENT_ENTITY_ID_PROPERTY;
import static net.solarnetwork.dao.GenericDao.entityEventTopic;
import static net.solarnetwork.dao.GenericDao.EntityEventType.DELETED;
import static net.solarnetwork.dao.GenericDao.EntityEventType.STORED;
import static org.easymock.EasyMock.capture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.test.context.transaction.BeforeTransaction;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.locstate.JdbcLocalStateDao;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcLocalStateDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcLocalStateDaoTests extends AbstractNodeTransactionalTest {

	private EventAdmin eventAdmin;

	private JdbcLocalStateDao dao;

	@BeforeTransaction
	public void setup() throws IOException {
		dao = new JdbcLocalStateDao();

		eventAdmin = EasyMock.createMock(EventAdmin.class);

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin);
	}

	private void tick() {
		try {
			Thread.sleep(10);
		} catch ( InterruptedException e ) {
			// ignore and continue
		}
	}

	private void assertEntityEvent(Event event, GenericDao.EntityEventType expectedType,
			LocalState expectedEntity) {
		assertThat("Event generated", event, is(notNullValue()));
		assertThat("Event is STORED", event.getTopic(), is(
				equalTo(entityEventTopic(LocalState.class.getSimpleName(), expectedType.toString()))));
		assertThat("Event has ID property", event.getProperty(ENTITY_EVENT_ENTITY_ID_PROPERTY),
				is(equalTo(expectedEntity.getId())));
	}

	@Test
	public void insert() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Boolean, true);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		String pk = dao.save(entity);

		// THEN
		assertThat("PK returned", pk, is(equalTo(entity.getId())));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void getByPk() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Boolean, true);
		String pk = dao.save(entity);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("ID", result.getId(), equalTo(entity.getId()));
		assertThat("Created", result.getCreated(), is(equalTo(entity.getCreated())));
		assertThat("Modified (populated as created)", entity.getCreated(),
				is(equalTo(entity.getCreated())));
		assertThat("Sameness", result.isSameAs(entity), is(equalTo(true)));
		assertThat("Value", result.getValue(), is(equalTo(entity.getValue())));
	}

	@Test
	public void update() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Boolean, true);
		String pk = dao.save(entity);
		final LocalState orig = dao.get(pk);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		final LocalState update = orig.clone();
		update.setModified(Instant.now().truncatedTo(MINUTES).plus(5, MINUTES));
		update.setType(LocalStateType.String);
		update.setValue(UUID.randomUUID().toString());

		String result = dao.save(update);

		// THEN
		assertThat("PK unchanged", result, equalTo(pk));

		LocalState persisted = dao.get(pk);

		assertThat("Created unchanged", persisted.getCreated(), is(equalTo(orig.getCreated())));
		assertThat("Modified updated", persisted.getModified(), is(equalTo(update.getModified())));
		assertThat("Type and data updated", persisted.isSameAs(update), is(equalTo(true)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void findAll() {
		// GIVEN
		LocalState obj1 = new LocalState("a", LocalStateType.Boolean, true);
		LocalState obj2 = new LocalState("c", LocalStateType.String, UUID.randomUUID().toString());
		LocalState obj3 = new LocalState("b", LocalStateType.Int32, 123456);

		obj1 = dao.get(dao.save(obj1));
		obj2 = dao.get(dao.save(obj2));
		obj3 = dao.get(dao.save(obj3));

		// WHEN
		replayAll();
		Collection<LocalState> results = dao.getAll(null);

		// THEN
		assertThat("Results found in order", results, contains(obj1, obj3, obj2));

		List<LocalState> outputs = new ArrayList<>(results);
		List<LocalState> inputs = Arrays.asList(obj1, obj3, obj2);
		for ( int i = 0; i < inputs.size(); i++ ) {
			assertThat("Result %d has same values as input".formatted(i),
					outputs.get(i).isSameAs(inputs.get(i)), is(true));
		}
	}

	@Test
	public void saveIntString() {
		// GIVEN
		LocalState obj = new LocalState("a", LocalStateType.Int32, "123456");

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as integer", result.getValue(), is(equalTo(123456)));
	}

	@Test
	public void saveInt() {
		// GIVEN
		LocalState obj = new LocalState("a", LocalStateType.Int32, 123456);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as integer", result.getValue(), is(equalTo(123456)));
	}

	@Test
	public void saveLong() {
		// GIVEN
		LocalState obj = new LocalState("a", LocalStateType.Int64, 9876543210L);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(9876543210L)));
	}

	@Test
	public void saveInteger() {
		// GIVEN
		final BigInteger val = new BigInteger("987654321098765432109876543210");
		LocalState obj = new LocalState("a", LocalStateType.Integer, val);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(val)));
	}

	@Test
	public void saveDecimal() {
		// GIVEN
		final BigDecimal val = new BigDecimal("98765432109876543210.9876543210");
		LocalState obj = new LocalState("a", LocalStateType.Decimal, val);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(val)));
	}

	@Test
	public void saveFloat() {
		// GIVEN
		final Float val = 1234.5f;
		LocalState obj = new LocalState("a", LocalStateType.Float32, val);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(val)));
	}

	@Test
	public void saveDouble() {
		// GIVEN
		final Double val = 1234567898765.54321;
		LocalState obj = new LocalState("a", LocalStateType.Float64, val);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(val)));
	}

	@Test
	public void saveMap() {
		// GIVEN
		final Map<String, Object> val = Map.of("a", "one", "b", 2, "c", true);
		LocalState obj = new LocalState("a", LocalStateType.Mapping, val);

		String pk = dao.save(obj);

		// WHEN
		replayAll();
		LocalState result = dao.get(pk);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result ID returned", result.getId(), is(equalTo(obj.getId())));
		assertThat("Result type returned", result.getType(), is(equalTo(obj.getType())));
		assertThat("Result value returned as long", result.getValue(), is(equalTo(val)));
	}

	@Test
	public void delete() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Boolean, true);
		String pk = dao.save(entity);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		LocalState input = new LocalState(pk, null);
		dao.delete(input);

		// THEN
		Collection<LocalState> all = dao.getAll(null);
		assertThat("Token deleted from table", all, hasSize(0));

		assertEntityEvent(eventCaptor.getValue(), DELETED, entity);
	}

	@Test
	public void compareAndSave_insert() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32,
				originalValue);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		tick();
		LocalState result = dao.compareAndSave(entity, null);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given", result.isSameAs(entity), is(equalTo(true)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void compareAndSave_update_match() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		tick();
		// update to 3 ONLY IF currently 2
		entity.setValue(3);
		LocalState result = dao.compareAndSave(entity, originalValue);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given (updated to new)", result.isSameAs(entity), is(equalTo(true)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void compareAndSave_update_noMatch() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));

		// WHEN
		replayAll();
		tick();
		// update to 3 ONLY IF currently 2
		entity.setValue(3);
		LocalState result = dao.compareAndSave(entity, 2);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));

		assertThat("Result NOT same as given", result.isSameAs(entity), is(equalTo(false)));
		assertThat("Result value is previous (unchanged) value)", result.getValue(),
				is(equalTo(originalValue)));
	}

	@Test
	public void compareAndChange_insert() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32,
				originalValue);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		tick();
		LocalState result = dao.compareAndChange(entity);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given", result.isSameAs(entity), is(equalTo(true)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void compareAndChange_changed() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		tick();
		// update to 3
		LocalState update = entity.clone();
		update.setValue(3);
		LocalState result = dao.compareAndChange(update);

		// THEN
		assertThat("Result returned", result, is(equalTo(update)));
		assertThat("Result same as given (updated to new)", result.isSameAs(update), is(equalTo(true)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void compareAndChange_unchanged() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));

		// WHEN
		replayAll();
		tick();
		// update to 1 (same as previous)
		LocalState update = entity.clone();
		LocalState result = dao.compareAndChange(update);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result unchanged", result.isSameAs(entity), is(equalTo(true)));
		assertThat("Result value is previous (unchanged) value)", result.getValue(),
				is(equalTo(originalValue)));
	}

	@Test
	public void getAndSave_insert() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, 1);

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result NOT returned", result, is(nullValue()));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void getAndSave_update() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));
		Capture<Event> eventCaptor = EasyMock.newCapture();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		entity.setValue(2);
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result NOT same as given (returned old)", result.isSameAs(entity),
				is(equalTo(false)));
		assertThat("Result value is previous value", result.getValue(), is(equalTo(originalValue)));

		assertEntityEvent(eventCaptor.getValue(), STORED, entity);
	}

	@Test
	public void getAndSave_noChange() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		dao.setEventAdmin(new StaticOptionalService<>(eventAdmin));

		// WHEN
		replayAll();
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given (no change)", result.isSameAs(entity), is(equalTo(true)));
		assertThat("Result value is previous value", result.getValue(), is(equalTo(originalValue)));
	}

}
