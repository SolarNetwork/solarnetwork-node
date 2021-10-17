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

import java.security.SecureRandom;
import java.time.Instant;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.InstructionDao;
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
	public void store_withStatus() {
		// GIVEN
		Long instrId = new SecureRandom().nextLong();
		BasicInstructionStatus status = new BasicInstructionStatus(instrId, InstructionState.Executing,
				Instant.now());
		BasicInstruction instr = new BasicInstruction(instrId, TEST_TOPIC, Instant.now().minusSeconds(1),
				TEST_INSTRUCTOR_ID, status);

		instructionDao.storeInstructionStatus(instrId, TEST_INSTRUCTOR_ID, status);

		// WHEN
		replayAll();
		service.storeInstruction(instr);

		// THEN
	}

}
