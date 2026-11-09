/* ==================================================================
 * DefaultPlatformServiceTests.java - 9/11/2026 2:41:18 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.runtime.DefaultPlatformService;
import net.solarnetwork.node.service.PlatformService.PlatformState;
import net.solarnetwork.node.service.PlatformService.PlatformTask;
import net.solarnetwork.node.service.PlatformService.PlatformTaskStatusHandler;

/**
 * Test cases for the {@link DefaultPlatformService} class.
 *
 * @author matt
 * @version 1.0
 */
public class DefaultPlatformServiceTests {

	private ExecutorService executor;
	private DefaultPlatformService service;

	@Before
	public void setup() {
		// more than one thread, so a second task can be rejected while the first is active
		executor = Executors.newFixedThreadPool(2);
		service = new DefaultPlatformService();
		service.setSingletonExecutorService(executor);
	}

	@After
	public void teardown() {
		executor.shutdownNow();
	}

	private static final class TestTask implements PlatformTask<String> {

		private final String id;
		private final CountDownLatch started = new CountDownLatch(1);
		private final CountDownLatch release = new CountDownLatch(1);
		private volatile boolean complete;

		private TestTask(String id) {
			super();
			this.id = id;
		}

		@Override
		public String call() throws Exception {
			started.countDown();
			release.await(5, TimeUnit.SECONDS);
			complete = true;
			return id;
		}

		@Override
		public String getTaskId() {
			return id;
		}

		@Override
		public String getTitle(Locale locale) {
			return id;
		}

		@Override
		public String getMessage(Locale locale) {
			return id;
		}

		@Override
		public double getPercentComplete() {
			return (complete ? 1.0 : 0.0);
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public boolean isRestartRequired() {
			return false;
		}

		@Override
		public void registerStatusHandler(PlatformTaskStatusHandler handler) {
			// not used
		}

	}

	@Test
	public void rejectedTask_activeTaskStateUnchanged() throws Exception {
		// GIVEN
		final TestTask taskA = new TestTask("a");
		final TestTask taskB = new TestTask("b");
		taskB.release.countDown();

		Future<String> futureA = service.performTaskWithState(PlatformState.UserBlockingSystemTask,
				taskA);
		assertThat("Task A started", taskA.started.await(5, TimeUnit.SECONDS), is(true));

		// WHEN
		Future<String> futureB = service.performTaskWithState(PlatformState.UserBlockingSystemTask,
				taskB);
		try {
			futureB.get(5, TimeUnit.SECONDS);
			fail("Task B should be rejected while task A is active");
		} catch ( ExecutionException e ) {
			assertThat("Task B rejected", e.getCause(), is(instanceOf(IllegalStateException.class)));
		}

		// THEN
		assertThat("Platform state still that of active task", service.activePlatformState(),
				is(equalTo(PlatformState.UserBlockingSystemTask)));
		assertThat("Active task still task A", service.activePlatformTaskStatus(),
				is(sameInstance(taskA)));

		taskA.release.countDown();
		assertThat("Task A result", futureA.get(5, TimeUnit.SECONDS), is(equalTo("a")));
		assertThat("Platform state restored after active task completes", service.activePlatformState(),
				is(equalTo(PlatformState.Normal)));
	}

}
