/* ==================================================================
 * JdbcMqttMessageDaoTests.java - 11/06/2021 6:51:21 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.mqtt.jdbc.test;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.test.context.transaction.BeforeTransaction;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.dao.BasicMqttMessageEntity;
import net.solarnetwork.common.mqtt.dao.MqttMessageDao;
import net.solarnetwork.common.mqtt.dao.MqttMessageEntity;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.mqtt.jdbc.JdbcMqttMessageDao;
import net.solarnetwork.node.dao.mqtt.jdbc.MqttMessageDaoStat;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link JdbcMqttMessageDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcMqttMessageDaoTests extends AbstractNodeTransactionalTest {

	private JdbcMqttMessageDao dao;
	private BasicMqttMessageEntity last;

	@BeforeTransaction
	public void setup() throws IOException {
		dao = new JdbcMqttMessageDao();

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao.setDataSource(dataSource);
		dao.init();
	}

	private BasicMqttMessageEntity createTestMqttMessageEntity(String dest, String topic,
			boolean retained, MqttQos qos, byte[] payload) {
		return new BasicMqttMessageEntity(null, Instant.now().truncatedTo(ChronoUnit.MILLIS), dest,
				topic, retained, qos, payload);
	}

	@Test
	public void insert() {
		BasicMqttMessageEntity msg = createTestMqttMessageEntity("test", "test/topic", true,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final long saveCount = dao.getStats().get(MqttMessageDaoStat.MessagesStored);
		Long pk = dao.save(msg);
		assertThat("PK generated", pk, notNullValue());
		assertThat("Save count incremented", dao.getStats().get(MqttMessageDaoStat.MessagesStored),
				is(equalTo(saveCount + 1)));
		last = msg.withId(pk);
	}

	@Test
	public void getByPK() {
		insert();
		MqttMessageEntity entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), is(equalTo(last.getCreated())));
		assertThat("Destination", entity.getDestination(), is(equalTo(last.getDestination())));
		assertThat("Topic", entity.getTopic(), is(equalTo(last.getTopic())));
		assertThat("Retained", entity.isRetained(), is(equalTo(last.isRetained())));
		assertThat("QoS", entity.getQosLevel(), is(equalTo(last.getQosLevel())));
		assertThat("Payload", Arrays.equals(entity.getPayload(), last.getPayload()), is(equalTo(true)));
	}

	@Test
	public void update() {
		insert();
		MqttMessageEntity orig = dao.get(last.getId());
		BasicMqttMessageEntity update = new BasicMqttMessageEntity(orig.getId(), orig.getCreated(),
				"newdest", "new/topic", false, MqttQos.ExactlyOnce, "Goodbye, world".getBytes());
		Long pk = dao.save(update);
		assertThat("PK unchanged", pk, equalTo(orig.getId()));

		MqttMessageEntity entity = dao.get(pk);
		assertThat("Destination updated", entity.getDestination(), is(equalTo(update.getDestination())));
		assertThat("Topic updated", entity.getTopic(), is(equalTo(update.getTopic())));
		assertThat("Retained updated", entity.isRetained(), is(equalTo(update.isRetained())));
		assertThat("QoS updated", entity.getQosLevel(), is(equalTo(update.getQosLevel())));
		assertThat("Payload updated", Arrays.equals(entity.getPayload(), update.getPayload()),
				is(equalTo(true)));
	}

	@Test
	public void findAll() {
		MqttMessageEntity obj1 = createTestMqttMessageEntity("dest", "test", false, MqttQos.AtLeastOnce,
				"Hello, world.".getBytes());
		obj1 = dao.get(dao.save(obj1));
		MqttMessageEntity obj2 = new BasicMqttMessageEntity(null, obj1.getCreated().plusSeconds(60),
				obj1.getDestination(), obj1);
		obj2 = dao.get(dao.save(obj2));
		MqttMessageEntity obj3 = new BasicMqttMessageEntity(null, obj2.getCreated().plusSeconds(60),
				obj1.getDestination(), obj1);
		obj3 = dao.get(dao.save(obj3));

		Collection<MqttMessageEntity> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj1, obj2, obj3));
	}

	@Test
	public void batchRead() {
		final BasicMqttMessageEntity template = createTestMqttMessageEntity("dest", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final int count = 5;
		final List<Long> ids = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids.add(dao.save(template));
		}
		dao.batchProcess(new BatchableDao.BatchCallback<MqttMessageEntity>() {

			@Override
			public BatchCallbackResult handle(MqttMessageEntity entity) {
				assertThat("Batch entity is not null", entity, is(notNullValue()));
				assertThat("Batch entity returned in order of ID", entity.getId(),
						is(equalTo(ids.get(0))));
				ids.remove(0);
				return BatchableDao.BatchCallbackResult.CONTINUE;
			}

		}, new BasicBatchOptions("Test"));
		assertThat("All entities processed in batch", ids, hasSize(0));
	}

	@Test
	public void batchRead_destination() {
		final BasicMqttMessageEntity template = createTestMqttMessageEntity("dest1", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final int count = 5;
		final List<Long> ids = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids.add(dao.save(template));
		}
		final BasicMqttMessageEntity template2 = createTestMqttMessageEntity("dest2", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final List<Long> ids2 = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids2.add(dao.save(template2));
		}
		dao.batchProcess(new BatchableDao.BatchCallback<MqttMessageEntity>() {

			@Override
			public BatchCallbackResult handle(MqttMessageEntity entity) {
				assertThat("Batch entity is not null", entity, is(notNullValue()));
				assertThat("Batch entity returned in order of ID", entity.getId(),
						is(equalTo(ids.get(0))));
				ids.remove(0);
				return BatchableDao.BatchCallbackResult.CONTINUE;
			}

		}, new BasicBatchOptions("Test", BasicBatchOptions.DEFAULT_BATCH_SIZE, false,
				singletonMap(MqttMessageDao.BATCH_OPTION_DESTINATION, "dest1")));
		assertThat("All dest1 entities processed in batch", ids, hasSize(0));
		assertThat("No dest2 entities processed in batch", ids2, hasSize(5));
	}

	@Test
	public void batchDelete() {
		final BasicMqttMessageEntity template = createTestMqttMessageEntity("dest", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final long deleteCount = dao.getStats().get(MqttMessageDaoStat.MessagesDeleted);
		final int count = 5;
		final List<Long> ids = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids.add(dao.save(template));
		}
		dao.batchProcess(new BatchableDao.BatchCallback<MqttMessageEntity>() {

			private int count = 0;

			@Override
			public BatchCallbackResult handle(MqttMessageEntity entity) {
				assertThat("Batch entity is not null", entity, is(notNullValue()));
				BatchCallbackResult action;
				switch (count++) {
					case 0:
					case 1:
						action = BatchCallbackResult.DELETE;
						ids.remove(0);
						break;

					default:
						action = BatchCallbackResult.CONTINUE;
						break;

				}
				return action;
			}

		}, new BasicBatchOptions("Test", BasicBatchOptions.DEFAULT_BATCH_SIZE, true, null));
		assertThat("2 entities deleted in batch", ids, hasSize(3));

		List<Long> remainingIds = dao.getAll(null).stream().map(MqttMessageEntity::getId)
				.collect(Collectors.toList());
		assertThat("3 entities remain", remainingIds, is(equalTo(ids)));
		assertThat("Delete count incremented", dao.getStats().get(MqttMessageDaoStat.MessagesDeleted),
				is(equalTo(deleteCount + 2)));
	}

	@Test
	public void batchDelete_destination() {
		final BasicMqttMessageEntity template = createTestMqttMessageEntity("dest1", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final long deleteCount = dao.getStats().get(MqttMessageDaoStat.MessagesDeleted);
		final int count = 5;
		final List<Long> ids = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids.add(dao.save(template));
		}
		final BasicMqttMessageEntity template2 = createTestMqttMessageEntity("dest2", "test", false,
				MqttQos.AtLeastOnce, "Hello, world.".getBytes());
		final List<Long> ids2 = new ArrayList<>(5);
		for ( int i = 0; i < count; i += 1 ) {
			ids2.add(dao.save(template2));
		}
		dao.batchProcess(new BatchableDao.BatchCallback<MqttMessageEntity>() {

			private int count = 0;

			@Override
			public BatchCallbackResult handle(MqttMessageEntity entity) {
				assertThat("Batch entity is not null", entity, is(notNullValue()));
				BatchCallbackResult action;
				switch (count++) {
					case 0:
					case 1:
						action = BatchCallbackResult.DELETE;
						ids.remove(0);
						break;

					default:
						action = BatchCallbackResult.CONTINUE;
						break;

				}
				return action;
			}

		}, new BasicBatchOptions("Test", BasicBatchOptions.DEFAULT_BATCH_SIZE, true,
				singletonMap(MqttMessageDao.BATCH_OPTION_DESTINATION, "dest1")));
		assertThat("2 entities deleted in batch", ids, hasSize(3));

		List<Long> expectedRemainingIds = new ArrayList<>(ids);
		expectedRemainingIds.addAll(ids2);
		List<Long> remainingIds = dao.getAll(null).stream().map(MqttMessageEntity::getId)
				.collect(Collectors.toList());
		assertThat("3 dest1 + 5 dest2 entities remain", remainingIds, is(equalTo(expectedRemainingIds)));
		assertThat("Delete count incremented", dao.getStats().get(MqttMessageDaoStat.MessagesDeleted),
				is(equalTo(deleteCount + 2)));
	}
}
