/* ==================================================================
 * WatchdogTests.java - 3/09/2019 9:09:43 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.stabiliti30c.test;

import static net.solarnetwork.node.io.modbus.ModbusWriteFunction.WriteHoldingRegister;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.control.stabiliti30c.Watchdog;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link Watchdog} class.
 *
 * @author matt
 * @version 2.1
 */
public class WatchdogTests {

	private static final int TEST_UNIT_ID = 2;
	private static final int TEST_TIMEOUT_SECS = 44;
	private static final long TEST_UPDATE_FREQ = 10;
	private static final int TEST_STARTUP_DELAY = 0;

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private TaskScheduler taskScheduler;
	private ScheduledFuture<?> scheduledFuture;
	private Watchdog service;

	@Before
	public void setup() {
		taskScheduler = EasyMock.createMock(TaskScheduler.class);
		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		scheduledFuture = EasyMock.createMock(ScheduledFuture.class);
		service = new Watchdog();
		service.setTaskScheduler(taskScheduler);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
		service.setUnitId(TEST_UNIT_ID);
		service.setUpdateFrequency(TEST_UPDATE_FREQ);
		service.setTimeoutSeconds(TEST_TIMEOUT_SECS);
		service.setStartupDelaySeconds(TEST_STARTUP_DELAY);
	}

	private class TestModbusNetwork extends AbstractModbusNetwork {

		@Override
		public <T> T performAction(int unitId, ModbusConnectionAction<T> action) throws IOException {
			return action.doWithConnection(conn);
		}

	}

	@After
	public void teardown() {
		EasyMock.verify(taskScheduler, modbus, conn, scheduledFuture);
	}

	private void replayAll() {
		EasyMock.replay(taskScheduler, modbus, conn, scheduledFuture);
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> ScheduledFuture<T> scheduledFuture() {
		return (ScheduledFuture) scheduledFuture;
	}

	@Test
	public void startupSchedulesTask() {
		// GIVEN
		Capture<Runnable> taskCaptor = Capture.newInstance();
		Capture<Instant> dateCaptor = Capture.newInstance();
		Capture<Duration> delayCaptor = Capture.newInstance();
		expect(taskScheduler.scheduleWithFixedDelay(capture(taskCaptor), capture(dateCaptor),
				capture(delayCaptor))).andReturn(scheduledFuture());

		// WHEN
		replayAll();
		final long now = System.currentTimeMillis();
		service.startup();

		// THEN
		assertThat("Task scheduled", taskCaptor, notNullValue());
		assertThat("Task start date", dateCaptor.getValue().toEpochMilli(),
				greaterThanOrEqualTo(now + TEST_STARTUP_DELAY * 1000L));
		assertThat("Task delay", delayCaptor.getValue(), equalTo(Duration.ofSeconds(TEST_UPDATE_FREQ)));
	}

	@Test
	public void runTaskWritesTimeoutSecondsToWatchdogRegister() throws IOException {
		// GIVEN
		Capture<Runnable> taskCaptor = Capture.newInstance();
		expect(taskScheduler.scheduleWithFixedDelay(capture(taskCaptor), anyObject(Instant.class),
				anyObject(Duration.class))).andReturn(scheduledFuture());

		expect(modbus.performAction(eq(TEST_UNIT_ID), anyAction(Void.class)))
				.andDelegateTo(new TestModbusNetwork());

		conn.writeWords(eq(WriteHoldingRegister), eq(40), aryEq(new int[] { 44 }));

		// WHEN
		replayAll();
		service.startup();
		assertThat("Task scheduled", taskCaptor, notNullValue());
		taskCaptor.getValue().run();

		// THEN
	}

}
