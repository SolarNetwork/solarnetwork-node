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

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.locstate.JdbcLocalStateDao;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.TestEmbeddedDatabase;

/**
 * Test cases for the {@link JdbcLocalStateDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcLocalStateDaoTests extends AbstractNodeTest {

	private TestEmbeddedDatabase dataSource;

	private JdbcLocalStateDao dao;
	private LocalState last;

	@Before
	public void setup() throws IOException {
		dao = new JdbcLocalStateDao();

		TestEmbeddedDatabase db = createEmbeddedDatabase("data.db.type");
		if ( db.getDatabaseType() != EmbeddedDatabaseType.DERBY ) {
			String dbType = db.getDatabaseType().toString().toLowerCase();
			dao.setInitSqlResource(new ClassPathResource(format("%s-locstate-init.sql", dbType),
					JdbcLocalStateDao.class));
			dao.setSqlResourcePrefix(format("%s-locstate", dbType));
		}
		dataSource = db;

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();
	}

	@Test
	public void insert() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Boolean, true);

		// WHEN
		String pk = dao.save(entity);

		// THEN
		assertThat("PK returned", pk, is(equalTo(entity.getId())));
		last = entity;
	}

	@Test
	public void getByPk() {
		// GIVEN
		insert();

		// WHEN
		LocalState entity = dao.get(last.getId());

		// THEN
		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Modified (populated as created)", entity.getCreated(),
				is(equalTo(last.getCreated())));
		assertThat("Sameness", entity.isSameAs(last), is(equalTo(true)));
		assertThat("Value", entity.getValue(), is(equalTo(last.getValue())));
	}

	@Test
	public void update() {
		// GIVEN
		insert();
		final LocalState orig = dao.get(last.getId());

		// WHEN

		final LocalState update = orig.clone();
		update.setModified(Instant.now().truncatedTo(MINUTES).plus(5, MINUTES));
		update.setType(LocalStateType.String);
		update.setValue(UUID.randomUUID().toString());

		String pk = dao.save(update);

		// THEN
		assertThat("PK unchanged", pk, equalTo(orig.getId()));

		LocalState entity = dao.get(pk);

		assertThat("Created unchanged", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Modified updated", entity.getModified(), is(equalTo(update.getModified())));
		assertThat("Type and data updated", entity.isSameAs(update), is(equalTo(true)));
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
		Collection<LocalState> results = dao.getAll(null);

		// THEN
		assertThat("Results found in order", results, contains(obj1, obj3, obj2));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		LocalState input = new LocalState(last.getId(), null);
		dao.delete(input);

		// THEN
		Collection<LocalState> all = dao.getAll(null);
		assertThat("Token deleted from table", all, hasSize(0));
	}

	@Test
	public void compareAndSave_insert() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		// WHEN
		LocalState result = dao.compareAndSave(entity, null);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given", result.isSameAs(entity), is(equalTo(true)));
		last = result;
	}

	@Test
	public void compareAndSave_update_match() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		// WHEN
		// update to 3 ONLY IF currently 2
		entity.setValue(3);
		LocalState result = dao.compareAndSave(entity, originalValue);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given (updated to new)", result.isSameAs(entity), is(equalTo(true)));
		last = result;
	}

	@Test
	public void compareAndSave_update_noMatch() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		// WHEN
		// update to 3 ONLY IF currently 2
		entity.setValue(3);
		LocalState result = dao.compareAndSave(entity, 2);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result NOT same as given", result.isSameAs(entity), is(equalTo(false)));
		assertThat("Result value is previous (unchanged) value)", result.getValue(),
				is(equalTo(originalValue)));
		last = result;
	}

	@Test
	public void getAndSave_insert() {
		// GIVEN
		LocalState entity = new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, 1);

		// WHEN
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result NOT returned", result, is(nullValue()));
	}

	@Test
	public void getAndSave_update() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		// WHEN
		entity.setValue(2);
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result NOT same as given (returned old)", result.isSameAs(entity),
				is(equalTo(false)));
		assertThat("Result value is previous value", result.getValue(), is(equalTo(originalValue)));
		last = result;
	}

	@Test
	public void getAndSave_noChange() {
		// GIVEN
		final int originalValue = 1;
		LocalState entity = dao.get(dao.save(
				new LocalState(UUID.randomUUID().toString(), LocalStateType.Int32, originalValue)));

		// WHEN
		LocalState result = dao.getAndSave(entity);

		// THEN
		assertThat("Result returned", result, is(equalTo(entity)));
		assertThat("Result same as given (no change)", result.isSameAs(entity), is(equalTo(true)));
		assertThat("Result value is previous value", result.getValue(), is(equalTo(originalValue)));
		last = result;
	}

}
