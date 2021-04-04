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

import static net.solarnetwork.node.control.camera.motion.MotionCameraControl.SNAPSHOT_JOB_KEY;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import net.solarnetwork.node.control.camera.motion.MotionCameraControl;
import net.solarnetwork.node.control.camera.motion.MotionSnapshotConfig;
import net.solarnetwork.node.control.camera.motion.MotionSnapshotJob;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link MotionCameraControl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MotionCameraControlTests extends AbstractHttpClientTests {

	private Scheduler scheduler;

	private String controlId;
	private MotionCameraControl control;

	private List<Object> mocks;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		scheduler = EasyMock.createMock(Scheduler.class);

		controlId = UUID.randomUUID().toString();
		control = new MotionCameraControl();
		control.setControlId(controlId);
		control.setMotionBaseUrl(getHttpServerBaseUrl());
		control.setScheduler(new StaticOptionalService<Scheduler>(scheduler));
	}

	@Override
	@After
	public void teardown() throws Exception {
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
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getPathInfo(),
						equalTo("/1/action/snapshot"));
				respondWithText(response, "OK");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		BasicInstruction signal = new BasicInstruction(InstructionHandler.TOPIC_SIGNAL, new Date(),
				UUID.randomUUID().toString(), null, null);
		signal.addParameter(controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		InstructionState result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction completed", result, equalTo(InstructionState.Completed));
	}

	@Test
	public void signalInstruction_snapshotSpecificCamera() throws Exception {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getPathInfo(),
						equalTo("/2/action/snapshot"));
				respondWithText(response, "OK");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		BasicInstruction signal = new BasicInstruction(InstructionHandler.TOPIC_SIGNAL, new Date(),
				UUID.randomUUID().toString(), null, null);
		signal.addParameter(controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		signal.addParameter(MotionCameraControl.CAMERA_ID_PARAM, "2");
		InstructionState result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction completed", result, equalTo(InstructionState.Completed));
	}

	@Test
	public void signalInstruction_snapshotCameraNotFound() throws Exception {
		// GIVEN
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Snapshot request method", request.getMethod(), equalTo("GET"));
				assertThat("Snapshot Request path", request.getPathInfo(),
						equalTo("/2/action/snapshot"));
				response.setStatus(404);
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		BasicInstruction signal = new BasicInstruction(InstructionHandler.TOPIC_SIGNAL, new Date(),
				UUID.randomUUID().toString(), null, null);
		signal.addParameter(controlId, MotionCameraControl.SIGNAL_SNAPSHOT);
		signal.addParameter(MotionCameraControl.CAMERA_ID_PARAM, "2");
		InstructionState result = control.processInstruction(signal);

		// THEN
		assertThat("HTTP method called", handler.isHandled(), equalTo(true));
		assertThat("Snapshot instruction for unknown camaera declined", result,
				equalTo(InstructionState.Declined));
	}

	@Test
	public void scheduleSnapshotJob_interval() throws Exception {
		// GIVEN
		addMock(scheduler);
		MotionSnapshotConfig snapConfig = new MotionSnapshotConfig();
		snapConfig.setCameraId(1);
		snapConfig.setSchedule("123");
		control.setSnapshotConfigurations(new MotionSnapshotConfig[] { snapConfig });

		TriggerKey tk = TriggerKey.triggerKey(String.format("%s-%d", controlId, 1),
				MotionCameraControl.SNAPSHOT_JOB_GROUP);

		// look for existing trigger; not found
		expect(scheduler.getTrigger(tk)).andReturn(null);

		// look for existing job; not found
		expect(scheduler.getJobDetail(SNAPSHOT_JOB_KEY)).andReturn(null);

		// create job
		Capture<JobDetail> jobCaptor = new Capture<>();
		scheduler.addJob(capture(jobCaptor), eq(true));

		// schedule job
		Capture<Trigger> trigCaptor = new Capture<>();
		expect(scheduler.scheduleJob(capture(trigCaptor))).andReturn(new Date());

		// WHEN
		replayAll();
		control.configurationChanged(null);

		// THEN
		JobDetail jobDetail = jobCaptor.getValue();
		assertThat("Snapshot job detail available", jobDetail, notNullValue());
		assertThat("Snapshot job is expected class", jobDetail.getJobClass(),
				equalTo(MotionSnapshotJob.class));
		assertThat("Snapshot job data has no props", jobDetail.getJobDataMap().keySet(), hasSize(0));

		Trigger jobTrigger = trigCaptor.getValue();
		assertThat("Snapshot job trigger is simple", jobTrigger,
				Matchers.instanceOf(SimpleTrigger.class));
		assertThat("Snapshot job trigger interval milliseconds",
				((SimpleTrigger) jobTrigger).getRepeatInterval(), equalTo(123000L));
		assertThat("Snapshot job trigger has 2 props", jobTrigger.getJobDataMap().keySet(), hasSize(2));
		assertThat("Snapshot job trigger data camera ID", jobTrigger.getJobDataMap(),
				hasEntry("cameraId", 1));
		assertThat("Snapshot job trigger data service", jobTrigger.getJobDataMap(),
				hasEntry("service", control));
	}

	@Test
	public void unscheduleSnapshotJob_interval() throws Exception {
		// GIVEN
		addMock(scheduler);
		MotionSnapshotConfig snapConfig = new MotionSnapshotConfig();
		snapConfig.setCameraId(1);
		snapConfig.setSchedule("123");
		control.setSnapshotConfigurations(new MotionSnapshotConfig[] { snapConfig });

		TriggerKey tk = TriggerKey.triggerKey(String.format("%s-%d", controlId, 1),
				MotionCameraControl.SNAPSHOT_JOB_GROUP);

		// look for existing trigger; not found
		expect(scheduler.getTrigger(tk)).andReturn(null);

		// look for existing job; not found
		expect(scheduler.getJobDetail(SNAPSHOT_JOB_KEY)).andReturn(null);

		// create job
		Capture<JobDetail> jobCaptor = new Capture<>();
		scheduler.addJob(capture(jobCaptor), eq(true));

		// schedule job
		Capture<Trigger> trigCaptor = new Capture<>();
		expect(scheduler.scheduleJob(capture(trigCaptor))).andReturn(new Date());

		// unschedule job
		expect(scheduler.unscheduleJob(tk)).andReturn(true);

		// WHEN
		replayAll();
		control.configurationChanged(null);
		control.shutdown();

		// THEN
		JobDetail jobDetail = jobCaptor.getValue();
		assertThat("Snapshot job detail available", jobDetail, notNullValue());
		assertThat("Snapshot job is expected class", jobDetail.getJobClass(),
				equalTo(MotionSnapshotJob.class));
		assertThat("Snapshot job data has no props", jobDetail.getJobDataMap().keySet(), hasSize(0));

		Trigger jobTrigger = trigCaptor.getValue();
		assertThat("Snapshot job trigger is simple", jobTrigger,
				Matchers.instanceOf(SimpleTrigger.class));
		assertThat("Snapshot job trigger interval milliseconds",
				((SimpleTrigger) jobTrigger).getRepeatInterval(), equalTo(123000L));
		assertThat("Snapshot job trigger has 2 props", jobTrigger.getJobDataMap().keySet(), hasSize(2));
		assertThat("Snapshot job trigger data camera ID", jobTrigger.getJobDataMap(),
				hasEntry("cameraId", 1));
		assertThat("Snapshot job trigger data service", jobTrigger.getJobDataMap(),
				hasEntry("service", control));
	}

}
