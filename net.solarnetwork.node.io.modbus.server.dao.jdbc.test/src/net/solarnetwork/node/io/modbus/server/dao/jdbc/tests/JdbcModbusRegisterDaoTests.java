/* ==================================================================
 * JdbcModbusRegisterDaoTests.java - 4/11/2024 10:14:24 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.dao.jdbc.tests;

import static java.lang.String.format;
import static net.solarnetwork.node.io.modbus.server.dao.BasicModbusRegisterFilter.forServerId;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;
import net.solarnetwork.node.io.modbus.server.dao.jdbc.JdbcModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.jdbc.ModbusServerDaoStat;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.TestEmbeddedDatabase;

/**
 * Test cases for the {@link JdbcModbusRegisterDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcModbusRegisterDaoTests extends AbstractNodeTest {

	private TestEmbeddedDatabase dataSource;

	private JdbcModbusRegisterDao dao;
	private ModbusRegisterEntity last;

	@Before
	public void setup() throws IOException {
		dao = new JdbcModbusRegisterDao();

		TestEmbeddedDatabase db = createEmbeddedDatabase("data.db.type");
		if ( db.getDatabaseType() != EmbeddedDatabaseType.DERBY ) {
			String dbType = db.getDatabaseType().toString().toLowerCase();
			dao.setInitSqlResource(new ClassPathResource(format("%s-modbus-server-init.sql", dbType),
					JdbcModbusRegisterDao.class));
			dao.setSqlResourcePrefix(format("%s-register", dbType));
		}
		dataSource = db;

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();
	}

	private ModbusRegisterEntity createTestModbusRegisterEntity(String serverId, int unitId,
			ModbusRegisterBlockType blockType, int address, short value) {
		ModbusRegisterEntity result = new ModbusRegisterEntity(
				new ModbusRegisterKey(serverId, unitId, blockType, address),
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		result.setModified(result.getCreated());
		result.setValue(value);
		return result;
	}

	@Test
	public void insert() {
		ModbusRegisterEntity msg = createTestModbusRegisterEntity("test", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		final long saveCount = dao.getStats().get(ModbusServerDaoStat.EntitiesUpdated);
		ModbusRegisterKey pk = dao.save(msg);
		assertThat("Assigned PK returned", pk, is(equalTo(msg.getId())));
		assertThat("Save count incremented", dao.getStats().get(ModbusServerDaoStat.EntitiesUpdated),
				is(equalTo(saveCount + 1)));
		last = msg;
	}

	@Test
	public void getByPK() {
		insert();
		ModbusRegisterEntity entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Modified", entity.getModified(), is(equalTo(last.getModified())));
		assertThat("Value", entity.getValue(), is(equalTo(last.getValue())));
	}

	@Test
	public void update() {
		insert();
		ModbusRegisterEntity orig = dao.get(last.getId());
		ModbusRegisterEntity update = orig.clone();
		update.setValue((short) 0xEE);
		update.setModified(orig.getModified().plusSeconds(1));
		ModbusRegisterKey pk = dao.save(update);
		assertThat("PK unchanged", pk, equalTo(orig.getId()));

		ModbusRegisterEntity entity = dao.get(pk);
		assertThat("Modified updated", entity.getModified(), is(equalTo(update.getModified())));
		assertThat("Value updated", entity.getValue(), is(equalTo(update.getValue())));
	}

	@Test
	public void findAll() {
		ModbusRegisterEntity obj1 = createTestModbusRegisterEntity("test", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		obj1 = dao.get(dao.save(obj1));
		ModbusRegisterEntity obj2 = createTestModbusRegisterEntity("test", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEE);
		obj2 = dao.get(dao.save(obj2));
		ModbusRegisterEntity obj3 = createTestModbusRegisterEntity("test", 1,
				ModbusRegisterBlockType.Holding, 3, (short) 0xDD);
		obj3 = dao.get(dao.save(obj3));

		Collection<ModbusRegisterEntity> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj1, obj2, obj3));
	}

	@Test
	public void findFiltered_serverId() {
		ModbusRegisterEntity obj1 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		obj1 = dao.get(dao.save(obj1));
		ModbusRegisterEntity obj2 = createTestModbusRegisterEntity("test2", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEE);
		obj2 = dao.get(dao.save(obj2));
		ModbusRegisterEntity obj3 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 3, (short) 0xDD);
		obj3 = dao.get(dao.save(obj3));

		FilterResults<ModbusRegisterEntity, ModbusRegisterKey> results1 = dao
				.findFiltered(forServerId("test1"));
		assertThat("Results found in order", results1, contains(obj1, obj3));

		FilterResults<ModbusRegisterEntity, ModbusRegisterKey> results2 = dao
				.findFiltered(forServerId("test2"));
		assertThat("Results found in order", results2, contains(obj2));

		FilterResults<ModbusRegisterEntity, ModbusRegisterKey> results3 = dao
				.findFiltered(forServerId("test.nope"));
		assertThat("Results found in order", results3, emptyIterable());
	}

}
