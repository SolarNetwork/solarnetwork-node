/* ==================================================================
 * HttpRequesterJobTest.java - Jul 21, 2013 8:32:24 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.ping.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.node.control.ping.HttpRequesterJob;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.quartz.JobExecutionException;

/**
 * Unit tests for the {@link HttpRequesterJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class HttpRequesterJobTest extends AbstractNodeTest {

	private static final String TEST_CONTROL_ID = "/test/toggle";

	private InstructionHandler handler;
	private Server server;

	private class TestHandler extends AbstractHandler {

		private int count;

		private TestHandler() {
			super();
			count = 0;
		}

		@Override
		public void handle(String target, HttpServletRequest request, HttpServletResponse response,
				int dispatch) throws IOException, ServletException {
			log.debug("Request query: {}", request.getQueryString());
			assertEquals("HEAD", request.getMethod());
			response.setStatus(HttpServletResponse.SC_OK);
			((Request) request).setHandled(true);
			count++;
		}

		private int getCount() {
			return count;
		}
	}

	private HttpRequesterJob newJobInstance() {
		HttpRequesterJob job = new HttpRequesterJob();
		job.setControlId(TEST_CONTROL_ID);
		job.setUrl("http://localhost:8988/");
		job.setHandlers(Collections.singletonList(handler));
		job.setSleepSeconds(0);
		return job;
	}

	@Before
	public void setup() throws Exception {
		handler = EasyMock.createMock(InstructionHandler.class);
		server = new Server(8988);
		server.start();
	}

	@After
	public void teardown() throws Exception {
		if ( server != null && server.isRunning() ) {
			server.stop();
		}
	}

	@Test
	public void pingSuccessful() throws JobExecutionException {
		final TestHandler httpHandler = new TestHandler();
		server.setHandler(httpHandler);
		replay(handler);
		HttpRequesterJob job = newJobInstance();
		job.execute(null);
		verify(handler);
		assertEquals(1, httpHandler.getCount());
	}

	@Test
	public void pingFailure() throws Exception {
		final TestHandler httpHandler = new TestHandler();
		server.stop();
		Capture<BasicInstruction> instructions = new Capture<BasicInstruction>(CaptureType.ALL);
		expect(handler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)).andReturn(
				Boolean.TRUE);
		expect(handler.processInstruction(EasyMock.capture(instructions))).andReturn(
				InstructionState.Completed);
		expect(handler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)).andReturn(
				Boolean.TRUE);
		expect(handler.processInstruction(EasyMock.capture(instructions))).andReturn(
				InstructionState.Completed);
		replay(handler);
		HttpRequesterJob job = newJobInstance();
		job.execute(null);
		verify(handler);
		assertEquals(0, httpHandler.getCount());
		assertEquals(2, instructions.getValues().size());
		BasicInstruction instr1 = instructions.getValues().get(0);
		assertEquals(Boolean.FALSE.toString(), instr1.getParameterValue(TEST_CONTROL_ID));
		BasicInstruction instr2 = instructions.getValues().get(1);
		assertEquals(Boolean.TRUE.toString(), instr2.getParameterValue(TEST_CONTROL_ID));
	}
}
