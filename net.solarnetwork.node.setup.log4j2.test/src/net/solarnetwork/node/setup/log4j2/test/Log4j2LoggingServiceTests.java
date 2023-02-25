/* ==================================================================
 * Log4j2LoggingServiceTests.java - 24/02/2023 2:28:04 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.log4j2.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.LoggingService;
import net.solarnetwork.node.setup.log4j2.Log4j2LoggingService;

/**
 * Test cases for the {@link Log4j2LoggingService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class Log4j2LoggingServiceTests {

	private Log4j2LoggingService service;

	@Before
	public void setup() {
		service = new Log4j2LoggingService();
	}

	@Test
	public void changeLoggerInstruction_wrongTopic() {
		// GIVEN
		Instruction instr = InstructionUtils.createLocalInstruction("foo", null);

		// WHEN
		boolean canHandle = service.handlesTopic(instr.getTopic());
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Invalid topic will not be handled", canHandle, is(false));
		assertThat("Invalid topic not handled", result, is(nullValue()));
	}

	@Test
	public void changeLoggerInstruction_missingLoggerNames() {
		// GIVEN
		Instruction instr = InstructionUtils.createLocalInstruction(LoggingService.TOPIC_UPDATE_LOGGER,
				null);

		// WHEN
		boolean canHandle = service.handlesTopic(instr.getTopic());
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Topic will be handled", canHandle, is(true));
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Instruction declined from missing logger names", result.getInstructionState(),
				is(InstructionState.Declined));
		assertThat("Error message provided", result.getResultParameters(),
				hasEntry(InstructionHandler.PARAM_MESSAGE, "No logger provided."));
	}

	@Test
	public void changeLoggerInstruction_missingLoggerLevel() {
		// GIVEN
		Instruction instr = InstructionUtils.createLocalInstruction(LoggingService.TOPIC_UPDATE_LOGGER,
				LoggingService.PARAM_LOGGER_NAME, "foo");

		// WHEN
		boolean canHandle = service.handlesTopic(instr.getTopic());
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Topic will be handled", canHandle, is(true));
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Instruction declined from missing logger level", result.getInstructionState(),
				is(InstructionState.Declined));
		assertThat("Error message provided", result.getResultParameters(),
				hasEntry(InstructionHandler.PARAM_MESSAGE, "Invalid logger level [null]."));
	}

	@Test
	public void changeLoggerInstruction_invalidLoggerLevel() {
		// GIVEN
		Map<String, String> params = new HashMap<>(4);
		params.put(LoggingService.PARAM_LOGGER_NAME, "foo");
		params.put(LoggingService.PARAM_LOGGER_LEVEL, "bar");
		Instruction instr = InstructionUtils.createLocalInstruction(LoggingService.TOPIC_UPDATE_LOGGER,
				params);

		// WHEN
		boolean canHandle = service.handlesTopic(instr.getTopic());
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Topic will be handled", canHandle, is(true));
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Instruction declined from missing logger level", result.getInstructionState(),
				is(InstructionState.Declined));
		assertThat("Error message provided", result.getResultParameters(),
				hasEntry(InstructionHandler.PARAM_MESSAGE, "Invalid logger level [bar]."));
	}

}
