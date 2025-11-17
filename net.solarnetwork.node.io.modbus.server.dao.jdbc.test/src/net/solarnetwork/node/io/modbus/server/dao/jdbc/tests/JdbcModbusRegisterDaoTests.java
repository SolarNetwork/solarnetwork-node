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

import static net.solarnetwork.node.io.modbus.server.dao.BasicModbusRegisterFilter.forServerId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.test.context.transaction.BeforeTransaction;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;
import net.solarnetwork.node.io.modbus.server.dao.jdbc.JdbcModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.jdbc.ModbusServerDaoStat;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link JdbcModbusRegisterDao} class.
 *
 * @author matt
 * @version 1.2
 */
public class JdbcModbusRegisterDaoTests extends AbstractNodeTransactionalTest {

	private JdbcModbusRegisterDao dao;
	private ModbusRegisterEntity last;

	@BeforeTransaction
	public void setup() throws IOException {
		dao = new JdbcModbusRegisterDao();

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

	@Test
	public void batchExport() {
		// GIVEN
		ModbusRegisterEntity obj1 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		ModbusRegisterEntity obj2 = createTestModbusRegisterEntity("test2", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEE);
		ModbusRegisterEntity obj3 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 3, (short) 0xDD);

		obj1 = dao.get(dao.save(obj1));
		obj2 = dao.get(dao.save(obj2));
		obj3 = dao.get(dao.save(obj3));

		final List<ModbusRegisterEntity> allEntities = List.of(obj1, obj3, obj2);

		// WHEN
		List<ModbusRegisterEntity> results = new ArrayList<>();
		BasicBatchOptions opts = new BasicBatchOptions("export", 50, false, Map.of());
		dao.batchProcess(new BatchCallback<ModbusRegisterEntity>() {

			@Override
			public BatchCallbackResult handle(ModbusRegisterEntity entity) {
				results.add(entity);
				return BatchCallbackResult.CONTINUE;
			}
		}, opts);

		// THEN
		assertThat("Expected processed count", results, hasSize(allEntities.size()));
		assertThat("Expected results returned", results,
				contains(allEntities.toArray(ModbusRegisterEntity[]::new)));
	}

	@Test
	public void deleteAll() {
		// GIVEN
		ModbusRegisterEntity obj1 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		ModbusRegisterEntity obj2 = createTestModbusRegisterEntity("test2", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEE);
		ModbusRegisterEntity obj3 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 3, (short) 0xDD);

		obj1 = dao.get(dao.save(obj1));
		obj2 = dao.get(dao.save(obj2));
		obj3 = dao.get(dao.save(obj3));

		// WHEN
		int result = dao.deleteAll();

		// THEN
		assertThat("Result count is all entities", result, is(equalTo(3)));

		Collection<ModbusRegisterEntity> results = dao.getAll(null);
		assertThat("No entities available", results, hasSize(0));
	}

	@Test
	public void getMostRecentMoficationDate() throws Exception {
		// GIVEN
		ModbusRegisterEntity obj1 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 1, (short) 0xFF);
		ModbusRegisterEntity obj2 = createTestModbusRegisterEntity("test2", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEE);
		ModbusRegisterEntity obj3 = createTestModbusRegisterEntity("test1", 1,
				ModbusRegisterBlockType.Holding, 3, (short) 0xDD);

		obj1 = dao.get(dao.save(obj1));
		obj2 = dao.get(dao.save(obj2));
		obj3 = dao.get(dao.save(obj3));

		Thread.sleep(50);

		obj2 = dao.get(dao.save(createTestModbusRegisterEntity("test2", 1,
				ModbusRegisterBlockType.Holding, 2, (short) 0xEF)));

		// WHEN
		Instant result = dao.getMostRecentModificationDate();

		// THEN
		assertThat("Result is from most recently modified entity", result,
				is(equalTo(obj2.getModified())));
	}

	@Test
	public void getMostRecentMoficationDate_none() throws Exception {
		// GIVEN

		// WHEN
		Instant result = dao.getMostRecentModificationDate();

		// THEN
		assertThat("No modification date avaiable when no data", result, is(nullValue()));
	}

}
