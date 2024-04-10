/* ==================================================================
 * InstructionUtilsTests.java - 5/04/2023 6:14:40 am
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

package net.solarnetwork.node.reactor.test;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionUtils;

/**
 * Test cases for the {@link InstructionUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class InstructionUtilsTests {

	static {
		// force load of lazy-loaded local instruction ID to current date
	}

	private Long startingLocalInstructionId;

	@Before
	public void setup() {
		startingLocalInstructionId = InstructionUtils
				.createLocalInstruction("foo", Collections.emptyMap()).getId();
	}

	@Test
	public void createLocalInstruction() {
		// GIVEN
		final Map<String, String> params = new HashMap<>(4);
		params.put("foo", UUID.randomUUID().toString());
		params.put("bar", UUID.randomUUID().toString());

		final String topic = UUID.randomUUID().toString();
		final Instant start = Instant.now();

		// WHEN
		Instruction result = InstructionUtils.createLocalInstruction(topic, params);

		// THEN
		assertThat("BasicInstruction created", result, is(instanceOf(BasicInstruction.class)));
		assertThat("Instructor ID is LOCAL", result.getInstructorId(),
				is(equalTo(Instruction.LOCAL_INSTRUCTION_ID)));
		assertThat("Instruction ID generated", result.getId(),
				is(greaterThanOrEqualTo(startingLocalInstructionId)));
		assertThat("Creation date populated", result.getInstructionDate(), is(notNullValue()));
		assertThat("Craetion date set to now", result.getInstructionDate().isBefore(start), is(false));
		assertThat("Topic set", result.getTopic(), is(equalTo(topic)));
		assertThat("Params set", result.getParameterMap(), is(equalTo(params)));
	}

	@Test
	public void localInstructionIds() {
		// GIVEN
		final String topic = UUID.randomUUID().toString();

		// WHEN
		final int count = 10;
		final Instruction[] instructions = new Instruction[count];
		for ( int i = 0; i < count; i++ ) {
			instructions[i] = InstructionUtils.createLocalInstruction(topic, null);
		}

		// THEN
		Long prevId = null;
		for ( int i = 0; i < count; i++ ) {
			Instruction instr = instructions[i];
			if ( prevId == null ) {
				assertThat(format("Instruction %d ID generated with seed", i), instr.getId(),
						is(greaterThanOrEqualTo(startingLocalInstructionId)));
			} else {
				assertThat(format("Instruction %d ID generated sequentially", i), instr.getId(),
						is(equalTo(prevId + 1L)));
			}
			assertThat("Topic set", instr.getTopic(), is(equalTo(topic)));
			prevId = instr.getId();
		}
	}

	@Test
	public void createLocalInstruction_singleParam() {
		// GIVEN
		final String topic = UUID.randomUUID().toString();
		final String paramKey = UUID.randomUUID().toString();
		final String paramVal = UUID.randomUUID().toString();
		final Instant start = Instant.now();

		// WHEN
		Instruction result = InstructionUtils.createLocalInstruction(topic, paramKey, paramVal);

		// THEN
		assertThat("BasicInstruction created", result, is(instanceOf(BasicInstruction.class)));
		assertThat("Instructor ID is LOCAL", result.getInstructorId(),
				is(equalTo(Instruction.LOCAL_INSTRUCTION_ID)));
		assertThat("Instruction ID generated", result.getId(),
				is(greaterThanOrEqualTo(startingLocalInstructionId)));
		assertThat("Creation date populated", result.getInstructionDate(), is(notNullValue()));
		assertThat("Craetion date set to now", result.getInstructionDate().isBefore(start), is(false));
		assertThat("Topic set", result.getTopic(), is(equalTo(topic)));
		assertThat("Params set", result.getParameterMap(),
				is(equalTo(singletonMap(paramKey, paramVal))));
	}

	@Test
	public void createSetControlValueLocalInstruction() {
		// GIVEN
		final String controlId = UUID.randomUUID().toString();
		final String controlValue = UUID.randomUUID().toString();
		final Instant start = Instant.now();

		// WHEN
		Instruction result = InstructionUtils.createSetControlValueLocalInstruction(controlId,
				controlValue);

		// THEN
		assertThat("BasicInstruction created", result, is(instanceOf(BasicInstruction.class)));
		assertThat("Instructor ID is LOCAL", result.getInstructorId(),
				is(equalTo(Instruction.LOCAL_INSTRUCTION_ID)));
		assertThat("Instruction ID generated", result.getId(),
				is(greaterThanOrEqualTo(startingLocalInstructionId)));
		assertThat("Creation date populated", result.getInstructionDate(), is(notNullValue()));
		assertThat("Craetion date set to now", result.getInstructionDate().isBefore(start), is(false));
		assertThat("Topic set", result.getTopic(),
				is(equalTo(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)));
		assertThat("Params set", result.getParameterMap(),
				is(equalTo(singletonMap(controlId, controlValue))));
	}

}
