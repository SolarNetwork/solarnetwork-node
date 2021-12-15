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

import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.control.ping.HttpRequesterJob;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.SimpleInstructionExecutionService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Unit tests for the {@link HttpRequesterJob} class.
 * 
 * @author matt
 * @version 2.1
 */
public class HttpRequesterJobTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
			assertThat("HTTP method", request.getMethod(), is("HEAD"));
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
		job.setInstructionExecutionService(new StaticOptionalService<>(
				new SimpleInstructionExecutionService(singletonList(handler))));
		job.setSleepSeconds(0);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(HttpRequesterJob.class.getName());
		job.setMessageSource(msgSource);

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
	public void pingSuccessful() throws Exception {
		final TestHandler httpHandler = new TestHandler();
		server.setHandler(httpHandler);
		replay(handler);
		HttpRequesterJob job = newJobInstance();
		job.executeJobService();
		verify(handler);
		assertThat("Request count", httpHandler.getCount(), is(1));
	}

	@Test
	public void pingFailure() throws Exception {
		final TestHandler httpHandler = new TestHandler();
		server.stop();
		Capture<BasicInstruction> instructions = new Capture<>(CaptureType.ALL);
		expect(handler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER))
				.andReturn(Boolean.TRUE);
		expect(handler.processInstruction(capture(instructions)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						int size = instructions.getValues().size();
						return InstructionUtils.createStatus(instructions.getValues().get(size - 1),
								InstructionState.Completed);
					}
				});
		expect(handler.handlesTopic(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER))
				.andReturn(Boolean.TRUE);
		expect(handler.processInstruction(capture(instructions)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						int size = instructions.getValues().size();
						return InstructionUtils.createStatus(instructions.getValues().get(size - 1),
								InstructionState.Completed);
					}
				});
		replay(handler);
		HttpRequesterJob job = newJobInstance();
		job.executeJobService();
		verify(handler);
		assertThat("No requests", httpHandler.getCount(), is(0));
		assertThat("Instructions executed", instructions.getValues(), hasSize(2));
		Instruction instr1 = instructions.getValues().get(0);
		assertThat("Control set ON", instr1.getParameterValue(TEST_CONTROL_ID),
				is(Boolean.TRUE.toString()));
		Instruction instr2 = instructions.getValues().get(1);
		assertThat("Control set OFF", instr2.getParameterValue(TEST_CONTROL_ID),
				is(Boolean.FALSE.toString()));
	}

	@Test
	public void systemConfigure_success() throws Exception {
		// GIVEN
		final TestHandler httpHandler = new TestHandler();
		server.setHandler(httpHandler);
		HttpRequesterJob job = newJobInstance();

		// WHEN
		replay(handler);
		Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(InstructionHandler.PARAM_SERVICE, HttpRequesterJob.PING_SERVICE_NAME);
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, instrParams);
		InstructionStatus result = job.processInstruction(instr);

		// THEN
		verify(handler);
		assertThat("Request count", httpHandler.getCount(), is(1));
		assertThat("Instruction returned result", result, is(notNullValue()));
		assertThat("Completed ping", result.getInstructionState(), is(InstructionState.Completed));
		assertThat("Result parameters provided", result.getResultParameters(), is(notNullValue()));
		log.debug("Got ping result message: {}",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE));
		assertThat("Result parameter message provided",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE),
				is(instanceOf(String.class)));
	}

	@Test
	public void systemConfigure_statusError() throws Exception {
		// GIVEN
		final AbstractHandler httpHandler = new AbstractHandler() {

			@Override
			public void handle(String target, HttpServletRequest request, HttpServletResponse response,
					int dispatch) throws IOException, ServletException {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				((Request) request).setHandled(true);
			}
		};
		server.setHandler(httpHandler);
		HttpRequesterJob job = newJobInstance();

		// WHEN
		replay(handler);
		Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(InstructionHandler.PARAM_SERVICE, HttpRequesterJob.PING_SERVICE_NAME);
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, instrParams);
		InstructionStatus result = job.processInstruction(instr);

		// THEN
		verify(handler);
		assertThat("Instruction returned result", result, is(notNullValue()));
		assertThat("Completed ping", result.getInstructionState(), is(InstructionState.Completed));
		assertThat("Result parameters provided", result.getResultParameters(), is(notNullValue()));
		log.debug("Got ping result message: {}",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE));
		assertThat("Result parameter message provided",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE),
				is(instanceOf(String.class)));
		assertThat("Result parameter result is status code",
				result.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT),
				is(equalTo(500)));
	}

	@Test
	public void systemConfigure_error() throws Exception {
		// GIVEN
		server.stop();
		HttpRequesterJob job = newJobInstance();

		// WHEN
		replay(handler);
		Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(InstructionHandler.PARAM_SERVICE, HttpRequesterJob.PING_SERVICE_NAME);
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, instrParams);
		InstructionStatus result = job.processInstruction(instr);

		// THEN
		verify(handler);
		assertThat("Instruction returned result", result, is(notNullValue()));
		assertThat("Completed ping", result.getInstructionState(), is(InstructionState.Completed));
		assertThat("Result parameters provided", result.getResultParameters(), is(notNullValue()));
		log.debug("Got ping result message: {}",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE));
		assertThat("Result parameter message provided",
				result.getResultParameters().get(InstructionHandler.PARAM_MESSAGE),
				is(instanceOf(String.class)));
		assertThat("Result parameter result is error code",
				result.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT),
				is(equalTo(-1)));
	}

}
