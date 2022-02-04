/* ==================================================================
 * DatumStreamReactorTests.java - 3/02/2022 2:01:04 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.datumreactor.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SIGNAL;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.control.datumreactor.DatumStreamReactor;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link DatumStreamReactor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumStreamReactorTests {

	private static final String TEST_SOURCE_ID = "/load/1";

	private static final String TEST_CONTROL_ID = "/throttle/1";

	private static final String TEST_DATUM_PROP = "load";

	private ExpressionService expressionService;
	private InstructionExecutionService instructionExecutionService;
	private DatumService datumService;
	private DatumStreamReactor service;

	@Before
	public void setup() {
		expressionService = new SpelExpressionService();

		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		datumService = EasyMock.createMock(DatumService.class);

		service = new DatumStreamReactor();
		service.setSourceIdRegexValue(TEST_SOURCE_ID);
		service.getConfig().setControlId(TEST_CONTROL_ID);
		service.getConfig().setExpressionServiceId(expressionService.getUid());

		service.setExpressionServices(
				new StaticOptionalServiceCollection<>(singleton(expressionService)));
		service.setInstructionExecutionService(new StaticOptionalService<>(instructionExecutionService));
		service.setDatumService(new StaticOptionalService<>(datumService));
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionExecutionService, datumService);
	}

	private void replayAll() {
		EasyMock.replay(instructionExecutionService, datumService);
	}

	private static SimpleDatum createTestGeneralNodeDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	private static Event datumCapturedEvent(NodeDatum datum) {
		return DatumEvents.datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
	}

	@Test
	public void simple() {
		// GIVEN
		final Integer inputVal = 124;
		final SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID, TEST_DATUM_PROP, inputVal);
		service.getConfig().setExpression(String.format("%s / 2", TEST_DATUM_PROP));

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionExecutionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic", instr.getTopic(),
								is(TOPIC_SET_CONTROL_PARAMETER));
						assertThat("Control output value is result of expression",
								instr.getParameterValue(TEST_CONTROL_ID),
								is(String.valueOf(inputVal / 2)));
						return InstructionUtils.createStatus(instr, InstructionState.Completed);
					}
				});

		// WHEN
		replayAll();
		service.handleEvent(datumCapturedEvent(datum));

		// THEN
	}

	// @formatter:off
	private static final String OFFSET_EXPR = 
			"has('load') and hasOffset(1) and offset(1).has('load') ? ("
			+ "offset(1).load == 0 and load == 1 "
			+ "? 'PlugConnected' "
			+ ": (offset(1).load == 1 and load == 0 "
			   + "? 'PlugDisconnected' "
			   + ": null)"
			+ ") : null";
	// @formatter:on

	@Test
	public void expressionWithHistoryOffset_01() {
		// GIVEN
		service.setInstructionTopic(TOPIC_SIGNAL);
		final Integer inputVal = 1;
		final SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID, TEST_DATUM_PROP, inputVal);

		final Integer prevInputVal = 0;
		final DatumSamples prevDatumSamples = new DatumSamples(datum.getSamples());
		prevDatumSamples.putInstantaneousSampleValue(TEST_DATUM_PROP, prevInputVal);
		final SimpleDatum prevDatum = new SimpleDayDatum(TEST_SOURCE_ID,
				datum.getTimestamp().minusSeconds(60), prevDatumSamples);

		// execute hasOffset(1) in expression to get prev datum in stream
		expect(datumService.offset(TEST_SOURCE_ID, datum.getTimestamp(), 1, NodeDatum.class))
				.andReturn(prevDatum).anyTimes();

		service.getConfig().setExpression(OFFSET_EXPR);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionExecutionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic", instr.getTopic(), is(TOPIC_SIGNAL));
						assertThat("Control signal is result of expression",
								instr.getParameterValue(TEST_CONTROL_ID), is("PlugConnected"));
						return InstructionUtils.createStatus(instr, InstructionState.Completed);
					}
				});

		// WHEN
		replayAll();
		service.handleEvent(datumCapturedEvent(datum));

		// THEN
	}

	@Test
	public void expressionWithHistoryOffset_02() {
		// GIVEN
		service.setInstructionTopic(TOPIC_SIGNAL);
		final Integer inputVal = 0;
		final SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID, TEST_DATUM_PROP, inputVal);

		final Integer prevInputVal = 1;
		final DatumSamples prevDatumSamples = new DatumSamples(datum.getSamples());
		prevDatumSamples.putInstantaneousSampleValue(TEST_DATUM_PROP, prevInputVal);
		final SimpleDatum prevDatum = new SimpleDayDatum(TEST_SOURCE_ID,
				datum.getTimestamp().minusSeconds(60), prevDatumSamples);

		// execute hasOffset(1) in expression to get prev datum in stream
		expect(datumService.offset(TEST_SOURCE_ID, datum.getTimestamp(), 1, NodeDatum.class))
				.andReturn(prevDatum).anyTimes();

		service.getConfig().setExpression(OFFSET_EXPR);

		Capture<Instruction> instrCaptor = new Capture<>();
		expect(instructionExecutionService.executeInstruction(capture(instrCaptor)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						Instruction instr = instrCaptor.getValue();
						assertThat("Instruction topic", instr.getTopic(), is(TOPIC_SIGNAL));
						assertThat("Control signal is result of expression",
								instr.getParameterValue(TEST_CONTROL_ID), is("PlugDisconnected"));
						return InstructionUtils.createStatus(instr, InstructionState.Completed);
					}
				});

		// WHEN
		replayAll();
		service.handleEvent(datumCapturedEvent(datum));

		// THEN
	}

	@Test
	public void expressionWithHistoryOffset_03() {
		// GIVEN
		service.setInstructionTopic(TOPIC_SIGNAL);
		final Integer inputVal = 0;
		final SimpleDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID, TEST_DATUM_PROP, inputVal);

		// execute hasOffset(1) in expression to get prev datum in stream
		expect(datumService.offset(TEST_SOURCE_ID, datum.getTimestamp(), 1, NodeDatum.class))
				.andReturn(null).anyTimes();

		service.getConfig().setExpression(OFFSET_EXPR);

		// WHEN
		replayAll();
		service.handleEvent(datumCapturedEvent(datum));

		// THEN
	}

}
