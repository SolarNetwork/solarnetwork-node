/* ==================================================================
 * BasicInstructionDeserializerTests.java - 11/08/2021 10:12:05 AM
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

package net.solarnetwork.node.reactor.support.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionDeserializer;

/**
 * Test cases for the {@link BasicInstructionDeserializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicInstructionDeserializerTests {

	private ObjectMapper createObjectMapper(BasicInstructionDeserializer deserializer) {
		ObjectMapper m = new ObjectMapper();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		m.setDateFormat(sdf);
		SimpleModule mod = new SimpleModule("Test");
		mod.addDeserializer(Instruction.class, deserializer);
		m.registerModule(mod);
		return m;
	}

	private void assertInstructionEquals(String msg, Instruction instr, Instruction expected) {
		assertThat(msg + " not null", instr, is(notNullValue()));
		assertThat(msg + " ID matches", instr.getId(), is(equalTo(expected.getId())));
		assertThat(msg + " remote ID matches", instr.getRemoteInstructionId(),
				is(equalTo(expected.getRemoteInstructionId())));
		assertThat(msg + " instructor ID matches", instr.getInstructorId(),
				is(equalTo(expected.getInstructorId())));
		assertThat(msg + " topic matches", instr.getTopic(), is(equalTo(expected.getTopic())));
		assertThat(msg + " date matches", instr.getInstructionDate(),
				is(equalTo(expected.getInstructionDate())));
		assertThat(msg + " status matches", instr.getStatus(), is(equalTo(expected.getStatus())));
		assertThat(msg + " parameters match", instr.getParameterMultiMap(),
				is(equalTo(expected.getParameterMultiMap())));
	}

	@Test
	public void deserialize_local_remoteId_noParams() throws IOException {
		// GIVEN
		ObjectMapper mapper = createObjectMapper(BasicInstructionDeserializer.LOCAL_INSTANCE);

		// @formatter:off
		String json = "{\n"
				+ "	\"topic\" : \"Mock/Topic\",\n"
				+ "	\"id\" : \"1\",\n"
				+ "	\"instructionDate\" : \"2014-01-01 12:00:00.000Z\"\n"
				+ "}";
		// @formatter:on

		// WHEN
		Instruction result = mapper.readValue(json, Instruction.class);

		// THEN
		Date date = Date.from(LocalDateTime.of(2014, 1, 1, 12, 0).atOffset(ZoneOffset.UTC).toInstant());
		BasicInstruction expected = new BasicInstruction("Mock/Topic", date, "1",
				Instruction.LOCAL_INSTRUCTION_ID, null);
		assertInstructionEquals("Local instruction without params", result, expected);
	}

	@Test
	public void deserialize_local_remoteId_withParams() throws IOException {
		// GIVEN
		ObjectMapper mapper = createObjectMapper(BasicInstructionDeserializer.LOCAL_INSTANCE);

		// @formatter:off
		String json = "{\n"
				+ "	\"topic\" : \"Mock/Topic\",\n"
				+ "	\"id\" : \"1\",\n"
				+ "	\"instructionDate\" : \"2014-01-01 12:00:00.000Z\",\n"
				+ "	\"parameters\" : [\n"
				+ "	  { \"name\" : \"foo\", \"value\" : \"bar\" }\n"
				+ "	]\n"
				+ "}";
		// @formatter:on

		// WHEN
		Instruction result = mapper.readValue(json, Instruction.class);

		// THEN
		Date date = Date.from(LocalDateTime.of(2014, 1, 1, 12, 0).atOffset(ZoneOffset.UTC).toInstant());
		BasicInstruction expected = new BasicInstruction("Mock/Topic", date, "1",
				Instruction.LOCAL_INSTRUCTION_ID, null);
		expected.addParameter("foo", "bar");
		assertInstructionEquals("Local instruction with params", result, expected);
	}

	@Test
	public void deserialize_local_localId_noParams() throws IOException {
		// GIVEN
		ObjectMapper mapper = createObjectMapper(BasicInstructionDeserializer.LOCAL_INSTANCE);

		// @formatter:off
		String json = "{\n"
				+ "	\"topic\" : \"Mock/Topic\",\n"
				+ "	\"instructionDate\" : \"2014-01-01 12:00:00.000Z\"\n"
				+ "}";
		// @formatter:on

		// WHEN
		Instruction result = mapper.readValue(json, Instruction.class);

		// THEN
		Date date = Date.from(LocalDateTime.of(2014, 1, 1, 12, 0).atOffset(ZoneOffset.UTC).toInstant());
		BasicInstruction expected = new BasicInstruction("Mock/Topic", date,
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		assertInstructionEquals("Local instruction without params", result, expected);
	}

}
