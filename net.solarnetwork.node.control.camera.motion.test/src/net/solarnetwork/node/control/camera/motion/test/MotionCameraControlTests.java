/* ==================================================================
 * MotionCameraControlTests.java - 21/10/2019 10:37:58 am
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

package net.solarnetwork.node.control.camera.motion.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.control.camera.motion.MotionCameraControl;
import net.solarnetwork.node.control.camera.motion.MotionSnapshotConfig;
import net.solarnetwork.node.control.camera.motion.MotionSnapshotJob;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link MotionCameraControl} class.
 *
 * @author matt
 * @version 1.0
 */
public class MotionCameraControlTests extends AbstractHttpServerTests {

	private TaskScheduler scheduler;

	private String controlId;
	private MotionCameraControl control;

	private List<Object> mocks;

	@Override
	@Before
	public void setup() {
		super.setup();

		scheduler = EasyMock.createMock(TaskScheduler.class);

		controlId = UUID.randomUUID().toString();
		control = new MotionCameraControl();
		control.setControlId(controlId);
		control.setMotionBaseUrl(getHttpServerBaseUrl());
		control.setScheduler(new StaticOptionalService<TaskScheduler>(scheduler));
	}

	@Override
	@After
	public void teardown() {
		super.teardown();
		if ( mocks != null ) {
			EasyMock.verify(mocks.toArray(new Object[mocks.size()]));
		}
	}

	private void replayAll() {
		if ( mocks != null ) {
			EasyMock.replay(mocks.toArray(new Object[mocks.size()]));
		}
	}

	private void addMock(Object o) {
		if ( mocks == null ) {
			mocks = new ArrayList<>(4);
		}
		mocks.add(o);
	}

	@Test
	public void signalInstruction_snapshotDefaultCamera() throws Exception {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getHttpURI().getPath(),
						equalTo("/1/action/snapshot"));
				respondWithText(request, response, "OK");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		Instruction signal = InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL,
				controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		InstructionStatus result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction completed", result.getInstructionState(),
				equalTo(InstructionState.Completed));
	}

	@Test
	public void signalInstruction_snapshotSpecificCamera() throws Exception {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getHttpURI().getPath(),
						equalTo("/2/action/snapshot"));
				respondWithText(request, response, "OK");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		Map<String, String> signalParams = new HashMap<>(2);
		signalParams.put(controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		signalParams.put(MotionCameraControl.CAMERA_ID_PARAM, "2");
		Instruction signal = InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL,
				signalParams);
		InstructionStatus result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction completed", result.getInstructionState(),
				equalTo(InstructionState.Completed));
	}

	@Test
	public void signalInstruction_snapshotCameraNotFound() throws Exception {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getHttpURI().getPath(),
						equalTo("/2/action/snapshot"));
				response.setStatus(404);
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		Map<String, String> signalParams = new HashMap<>(2);
		signalParams.put(controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		signalParams.put(MotionCameraControl.CAMERA_ID_PARAM, "2");
		Instruction signal = InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL,
				signalParams);
		InstructionStatus result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction for unknown camaera declined", result.getInstructionState(),
				equalTo(InstructionState.Declined));
	}

	private static class TestScheduledFuture extends CompletableFuture<Object>
			implements ScheduledFuture<Object> {

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return 0;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void scheduleSnapshotJob_interval() throws Exception {
		// GIVEN
		addMock(scheduler);
		MotionSnapshotConfig snapConfig = new MotionSnapshotConfig();
		snapConfig.setCameraId(1);
		snapConfig.setSchedule("123");
		control.setSnapshotConfigurations(new MotionSnapshotConfig[] { snapConfig });

		// schedule job
		Capture<Runnable> jobCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture taskFuture = new TestScheduledFuture();
		expect(scheduler.schedule(capture(jobCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) taskFuture);

		// WHEN
		replayAll();
		control.configurationChanged(null);

		// THEN
		Runnable job = jobCaptor.getValue();
		assertThat("Snapshot job available", job, notNullValue());
		assertThat("Snapshot job is expected class", job, instanceOf(MotionSnapshotJob.class));
		MotionSnapshotJob snapJob = (MotionSnapshotJob) job;
		assertThat("Snapshot job camera ID", snapJob.getCameraId(), equalTo(1));
		assertThat("Snapshot job service", snapJob.getService(), sameInstance(control));

		Trigger jobTrigger = trigCaptor.getValue();
		PeriodicTrigger expectedTrigger = new PeriodicTrigger(Duration.ofSeconds(123));
		expectedTrigger.setFixedRate(true);
		assertThat("Snapshot job trigger is simple", jobTrigger, equalTo(expectedTrigger));

		assertThat("Future not done", taskFuture.isDone(), equalTo(false));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void unscheduleSnapshotJob_interval() throws Exception {
		// GIVEN
		addMock(scheduler);
		MotionSnapshotConfig snapConfig = new MotionSnapshotConfig();
		snapConfig.setCameraId(1);
		snapConfig.setSchedule("123");
		control.setSnapshotConfigurations(new MotionSnapshotConfig[] { snapConfig });

		// schedule job
		Capture<Runnable> jobCaptor = Capture.newInstance();
		Capture<Trigger> trigCaptor = Capture.newInstance();
		TestScheduledFuture taskFuture = new TestScheduledFuture();
		expect(scheduler.schedule(capture(jobCaptor), capture(trigCaptor)))
				.andReturn((ScheduledFuture) taskFuture);

		// WHEN
		replayAll();
		control.configurationChanged(null);
		control.shutdown();

		// THEN
		assertThat("Future is cancelled", taskFuture.isCancelled(), equalTo(true));
	}

}
