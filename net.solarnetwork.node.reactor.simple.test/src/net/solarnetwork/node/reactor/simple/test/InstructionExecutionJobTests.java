/* ==================================================================
 * InstructionExecutionJobTests.java - 24/02/2022 10:05:46 AM
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

package net.solarnetwork.node.reactor.simple.test;

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Executing;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Received;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.simple.InstructionExecutionJob;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link InstructionExecutionJob}.
 * 
 * @author matt
 * @version 1.0
 */
public class InstructionExecutionJobTests {

	private static final String TEST_INSTRUCTOR_ID = "test.instructor";

	private InstructionDao instructionDao;
	private InstructionExecutionService instructionExecutionService;

	private InstructionExecutionJob job;

	@Before
	public void setup() {
		instructionDao = EasyMock.createMock(InstructionDao.class);
		instructionExecutionService = EasyMock.createMock(InstructionExecutionService.class);
		job = new InstructionExecutionJob(instructionDao, instructionExecutionService);
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionDao, instructionExecutionService);
	}

	private void replayAll() {
		EasyMock.replay(instructionDao, instructionExecutionService);
	}

	@Test
	public void noInstructions() throws Exception {
		// GIVEN
		expect(instructionDao.findInstructionsForState(Received)).andReturn(Collections.emptyList());
		expect(instructionDao.findInstructionsForState(Executing)).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		job.executeJobService();
	}

	@Test
	public void noHandler() throws Exception {
		// GIVEN
		BasicInstruction instr = new BasicInstruction(1L, "foo", Instant.now(), TEST_INSTRUCTOR_ID,
				new BasicInstructionStatus(1L, Received, Instant.now()));
		expect(instructionDao.findInstructionsForState(Received))
				.andReturn(Collections.singletonList(instr));

		// update to Executing
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Received),
				assertWith(new Assertion<InstructionStatus>() {

					@Override
					public void check(InstructionStatus status) throws Throwable {
						assertThat("Status provided", status, is(notNullValue()));
						assertThat("State is Executing", status.getInstructionState(), is(Executing));
					}
				}))).andReturn(true);

		expect(instructionExecutionService.executeInstruction(instr)).andReturn(null);

		// roll back to Received
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Executing), same(instr.getStatus()))).andReturn(true);

		expect(instructionDao.findInstructionsForState(Executing)).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		job.executeJobService();
	}

	@Test
	public void noHandler_expired() throws Exception {
		// GIVEN
		// instruction is older than maximumIncompleteHours
		Instant ts = Instant.now().truncatedTo(ChronoUnit.HOURS)
				.minus(job.getMaximumIncompleteHours() + 1, ChronoUnit.HOURS);
		BasicInstruction instr = new BasicInstruction(1L, "foo", ts, TEST_INSTRUCTOR_ID,
				new BasicInstructionStatus(1L, Received, ts));
		expect(instructionDao.findInstructionsForState(Received))
				.andReturn(Collections.singletonList(instr));

		// update to Executing
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Received),
				assertWith(new Assertion<InstructionStatus>() {

					@Override
					public void check(InstructionStatus status) throws Throwable {
						assertThat("Status provided", status, is(notNullValue()));
						assertThat("State is Executing", status.getInstructionState(), is(Executing));
					}
				}))).andReturn(true);

		expect(instructionExecutionService.executeInstruction(instr)).andReturn(null);

		// jump to Declined
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Executing),
				assertWith(new Assertion<InstructionStatus>() {

					@Override
					public void check(InstructionStatus status) throws Throwable {
						assertThat("Status provided", status, is(notNullValue()));
						assertThat("State is Declined", status.getInstructionState(), is(Declined));
						assertThat("Error code defined", status.getResultParameters(),
								hasEntry(InstructionStatus.ERROR_CODE_RESULT_PARAM,
										InstructionExecutionJob.ERROR_CODE_INSTRUCTION_EXPIRED));
						assertThat("Error message provided", status.getResultParameters(), hasEntry(
								is(InstructionStatus.MESSAGE_RESULT_PARAM), is(notNullValue())));
					}
				}))).andReturn(true);

		expect(instructionDao.findInstructionsForState(Executing)).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		job.executeJobService();
	}

	@Test
	public void handled_completed() throws Exception {
		// GIVEN
		BasicInstruction instr = new BasicInstruction(1L, "foo", Instant.now(), TEST_INSTRUCTOR_ID,
				new BasicInstructionStatus(1L, Received, Instant.now()));
		expect(instructionDao.findInstructionsForState(Received))
				.andReturn(Collections.singletonList(instr));

		// update to Executing
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Received),
				assertWith(new Assertion<InstructionStatus>() {

					@Override
					public void check(InstructionStatus status) throws Throwable {
						assertThat("Status provided", status, is(notNullValue()));
						assertThat("State is Executing", status.getInstructionState(), is(Executing));
					}
				}))).andReturn(true);

		InstructionStatus handledStatus = InstructionUtils.createStatus(instr, Completed);
		expect(instructionExecutionService.executeInstruction(instr)).andReturn(handledStatus);

		// update to handled status
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Executing), same(handledStatus))).andReturn(true);

		expect(instructionDao.findInstructionsForState(Executing)).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		job.executeJobService();
	}

	@Test
	public void executing_expired() throws Exception {
		// GIVEN
		expect(instructionDao.findInstructionsForState(Received)).andReturn(Collections.emptyList());

		// instruction is older than maximumIncompleteHours
		Instant ts = Instant.now().truncatedTo(ChronoUnit.HOURS)
				.minus(job.getMaximumIncompleteHours() + 1, ChronoUnit.HOURS);
		BasicInstruction instr = new BasicInstruction(1L, "foo", ts, TEST_INSTRUCTOR_ID,
				new BasicInstructionStatus(1L, Executing, ts));
		expect(instructionDao.findInstructionsForState(Executing))
				.andReturn(Collections.singletonList(instr));

		// jump to Declined
		expect(instructionDao.compareAndStoreInstructionStatus(eq(instr.getId()),
				eq(instr.getInstructorId()), eq(Executing),
				assertWith(new Assertion<InstructionStatus>() {

					@Override
					public void check(InstructionStatus status) throws Throwable {
						assertThat("Status provided", status, is(notNullValue()));
						assertThat("State is Declined", status.getInstructionState(), is(Declined));
						assertThat("Error code defined", status.getResultParameters(),
								hasEntry(InstructionStatus.ERROR_CODE_RESULT_PARAM,
										InstructionExecutionJob.ERROR_CODE_INSTRUCTION_EXPIRED));
						assertThat("Error message provided", status.getResultParameters(), hasEntry(
								is(InstructionStatus.MESSAGE_RESULT_PARAM), is(notNullValue())));
					}
				}))).andReturn(true);

		// WHEN
		replayAll();
		job.executeJobService();

	}

}
