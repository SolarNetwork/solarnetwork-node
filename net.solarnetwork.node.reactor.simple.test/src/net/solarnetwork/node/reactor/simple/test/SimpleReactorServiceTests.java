/* ==================================================================
 * SimpleReactorServiceTests.java - 12/10/2021 4:27:24 PM
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

package net.solarnetwork.node.reactor.simple.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.simple.SimpleReactorService;

/**
 * Test cases for the {@link SimpleReactorService}.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleReactorServiceTests {

	private static final String TEST_INSTRUCTOR_ID = "test.instructor";
	private static final String TEST_TOPIC = "test.topic";

	private InstructionDao instructionDao;
	private SimpleReactorService service;

	@Before
	public void setup() {
		instructionDao = EasyMock.createMock(InstructionDao.class);
		service = new SimpleReactorService(instructionDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionDao);
	}

	private void replayAll() {
		EasyMock.replay(instructionDao);
	}

	@Test
	public void store_withoutStatus() {
		// GIVEN
		Long instrId = new SecureRandom().nextLong();
		BasicInstruction instr = new BasicInstruction(instrId, TEST_TOPIC, Instant.now(),
				TEST_INSTRUCTOR_ID, null);

		instructionDao.storeInstruction(instr);

		// WHEN
		replayAll();
		service.storeInstruction(instr);

		// THEN
	}

	@Test
	public void store_withStatus_insert() {
		// GIVEN
		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId, TEST_TOPIC, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);

		expect(instructionDao.getInstruction(instrId, TEST_INSTRUCTOR_ID)).andReturn(null);

		instructionDao.storeInstruction(instr);

		// WHEN
		replayAll();
		service.storeInstruction(instr);

		// THEN
	}

	@Test
	public void store_withStatus_update() {
		// GIVEN
		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId, TEST_TOPIC, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);

		BasicInstruction existing = new BasicInstruction(instr, null);

		expect(instructionDao.getInstruction(instrId, TEST_INSTRUCTOR_ID)).andReturn(existing);

		instructionDao.storeInstructionStatus(instrId, TEST_INSTRUCTOR_ID, status);

		// WHEN
		replayAll();
		service.storeInstruction(instr);

		// THEN
	}

	@Test
	public void cancel_notFound() {
		// GIVEN
		Long oldInstrId = new SecureRandom().nextLong();

		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId,
				InstructionHandler.TOPIC_CANCEL_INSTRUCTION, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);
		instr.addParameter(InstructionHandler.PARAM_ID, oldInstrId.toString());

		// get instruction to cancel based on this instruction's "id" parameter
		expect(instructionDao.getInstruction(oldInstrId, TEST_INSTRUCTOR_ID)).andReturn(null);

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("State is Declined", result.getInstructionState(),
				is(equalTo(InstructionState.Declined)));
	}

	@Test
	public void cancel() {
		// GIVEN
		Long oldInstrId = new SecureRandom().nextLong();
		BasicInstructionStatus oldInstrStatus = new BasicInstructionStatus(oldInstrId,
				InstructionState.Received, Instant.now());
		BasicInstruction oldInstr = new BasicInstruction(oldInstrId,
				InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, oldInstrStatus);

		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId,
				InstructionHandler.TOPIC_CANCEL_INSTRUCTION, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);
		instr.addParameter(InstructionHandler.PARAM_ID, oldInstrId.toString());

		// get instruction to cancel based on this instruction's "id" parameter
		expect(instructionDao.getInstruction(oldInstrId, TEST_INSTRUCTOR_ID)).andReturn(oldInstr);

		// update that instruction's state to Declined
		Capture<InstructionStatus> declineInstructionStatusCaptor = Capture.newInstance();
		expect(instructionDao.compareAndStoreInstructionStatus(eq(oldInstrId), eq(TEST_INSTRUCTOR_ID),
				eq(oldInstrStatus.getInstructionState()), capture(declineInstructionStatusCaptor)))
						.andReturn(true);

		// search for child instructions to cancel
		expect(instructionDao.findInstructionsForStateAndParent(InstructionState.Received,
				TEST_INSTRUCTOR_ID, oldInstrId)).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		InstructionStatus declineInstructionStatus = declineInstructionStatusCaptor.getValue();
		assertThat("Old instruction state set to Declined",
				declineInstructionStatus.getInstructionState(), is(equalTo(InstructionState.Declined)));
		assertThat("Old instruction message provided", declineInstructionStatus.getResultParameters(),
				hasEntry(equalTo(InstructionHandler.PARAM_MESSAGE), notNullValue()));
	}

	@Test
	public void cancel_withChildren() {
		// GIVEN
		Long oldInstrId = new SecureRandom().nextLong();
		BasicInstructionStatus oldInstrStatus = new BasicInstructionStatus(oldInstrId,
				InstructionState.Received, Instant.now());
		BasicInstruction oldInstr = new BasicInstruction(oldInstrId,
				InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, oldInstrStatus);

		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId,
				InstructionHandler.TOPIC_CANCEL_INSTRUCTION, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);
		instr.addParameter(InstructionHandler.PARAM_ID, oldInstrId.toString());

		// get instruction to cancel based on this instruction's "id" parameter
		expect(instructionDao.getInstruction(oldInstrId, TEST_INSTRUCTOR_ID)).andReturn(oldInstr);

		// update that instruction's state to Declined
		Capture<InstructionStatus> declineInstructionStatusCaptor = Capture.newInstance();
		expect(instructionDao.compareAndStoreInstructionStatus(eq(oldInstrId), eq(TEST_INSTRUCTOR_ID),
				eq(oldInstrStatus.getInstructionState()), capture(declineInstructionStatusCaptor)))
						.andReturn(true);

		// search for child instructions to cancel (find 2)
		List<Instruction> children = new ArrayList<>(2);
		children.add(InstructionUtils.createSetControlValueLocalInstruction("foo", "bar"));
		children.add(InstructionUtils.createSetControlValueLocalInstruction("bim", "bam"));
		expect(instructionDao.findInstructionsForStateAndParent(InstructionState.Received,
				TEST_INSTRUCTOR_ID, oldInstrId)).andReturn(children);

		// cancel the 2 children
		Capture<InstructionStatus> declineChildInstructionStatusCaptor = Capture
				.newInstance(CaptureType.ALL);
		expect(instructionDao.compareAndStoreInstructionStatus(eq(children.get(0).getId()),
				eq(Instruction.LOCAL_INSTRUCTION_ID), eq(InstructionState.Received),
				capture(declineChildInstructionStatusCaptor))).andReturn(true);
		expect(instructionDao.compareAndStoreInstructionStatus(eq(children.get(1).getId()),
				eq(Instruction.LOCAL_INSTRUCTION_ID), eq(InstructionState.Received),
				capture(declineChildInstructionStatusCaptor))).andReturn(true);

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		InstructionStatus declineInstructionStatus = declineInstructionStatusCaptor.getValue();
		assertThat("Old instruction state set to Declined",
				declineInstructionStatus.getInstructionState(), is(equalTo(InstructionState.Declined)));
		assertThat("Old instruction message provided", declineInstructionStatus.getResultParameters(),
				hasEntry(equalTo(InstructionHandler.PARAM_MESSAGE), notNullValue()));

		List<InstructionStatus> declineChildInstructionStatuses = declineChildInstructionStatusCaptor
				.getValues();
		for ( InstructionStatus declined : declineChildInstructionStatuses ) {
			assertThat("Child instruction state set to Declined", declined.getInstructionState(),
					is(equalTo(InstructionState.Declined)));
			assertThat("Child instruction message provided", declined.getResultParameters(),
					hasEntry(equalTo(InstructionHandler.PARAM_MESSAGE), notNullValue()));
		}
	}

}
