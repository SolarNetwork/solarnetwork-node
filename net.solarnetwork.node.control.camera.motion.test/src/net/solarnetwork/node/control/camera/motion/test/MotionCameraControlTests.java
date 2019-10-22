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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.control.camera.motion.MotionCameraControl;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;

/**
 * Test cases for the {@link MotionCameraControl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MotionCameraControlTests extends AbstractHttpClientTests {

	private String controlId;
	private MotionCameraControl control;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		controlId = UUID.randomUUID().toString();
		control = new MotionCameraControl();
		control.setControlId(controlId);
		control.setMotionBaseUrl(getHttpServerBaseUrl());
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

}
