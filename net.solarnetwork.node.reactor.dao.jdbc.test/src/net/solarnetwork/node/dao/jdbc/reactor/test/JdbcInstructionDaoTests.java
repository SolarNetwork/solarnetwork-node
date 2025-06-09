/* ==================================================================
 * JdbcInstructionDaoTests.java - Oct 1, 2011 11:55:54 AM
 *
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.reactor.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.reactor.JdbcInstructionDao;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test case for the {@link JdbcInstructionDao} class.
 *
 * @author matt
 * @version 2.2
 */
public class JdbcInstructionDaoTests extends AbstractNodeTransactionalTest {

	private static final Long TEST_ID = Math.abs(UUID.randomUUID().getMostSignificantBits());
	private static final String TEST_INSTRUCTOR = "Test Instructor";
	private static final String TEST_TOPIC = "Test Topic";
	private static final String TEST_PARAM_KEY = "Test Param";
	private static final String TEST_PARAM_VALUE = "Test Value";

	private JdbcInstructionDao dao;

	private Instruction lastDatum;

	@BeforeTransaction
	public void setUp() throws Exception {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		dao = new JdbcInstructionDao();
		dao.setDataSource(dataSource);
		dao.init();

		lastDatum = null;
	}

	private Instruction storeNew(Instant date) {
		return storeNew(TEST_ID, TEST_INSTRUCTOR, date);
	}

	private Instruction storeNew(Long id, String instructorId, Instant date) {
		BasicInstruction instr = new BasicInstruction(id, TEST_TOPIC, date, instructorId, null);

		for ( int i = 0; i < 2; i++ ) {
			instr.addParameter(String.format("%s %d", TEST_PARAM_KEY, i),
					String.format("%s %d %s", TEST_PARAM_KEY, i, TEST_PARAM_VALUE));
		}

		dao.storeInstruction(instr);
		return dao.getInstruction(instr.getId(), instr.getInstructorId());
	}

	@Test
	public void storeNew() {
		lastDatum = storeNew(Instant.now());
	}

	private List<Map<String, Object>> listInstructions() {
		return new JdbcTemplate(dataSource).queryForList("select * from solarnode.sn_instruction")
				.stream().map(m -> {
					Map<String, Object> lcm = new LinkedHashMap<>(m.size());
					for ( Entry<String, Object> e : m.entrySet() ) {
						lcm.put(e.getKey().toLowerCase(), e.getValue());
					}
					return lcm;
				}).collect(Collectors.toList());
	}

	private List<Map<String, Object>> listInstructionParams() {
		return new JdbcTemplate(dataSource).queryForList("select * from solarnode.sn_instruction_param")
				.stream().map(m -> {
					Map<String, Object> lcm = new LinkedHashMap<>(m.size());
					for ( Entry<String, Object> e : m.entrySet() ) {
						lcm.put(e.getKey().toLowerCase(), e.getValue());
					}
					return lcm;
				}).collect(Collectors.toList());
	}

	@Test
	public void storeNew_withExecuteDate() {
		final Instant executeDate = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1,
				ChronoUnit.HOURS);
		final String executeDateStr = DateTimeFormatter.ISO_INSTANT.format(executeDate);
		BasicInstruction instr = new BasicInstruction(
				Math.abs(UUID.randomUUID().getMostSignificantBits()), TEST_TOPIC, Instant.now(),
				TEST_INSTRUCTOR, null);
		instr.addParameter(Instruction.PARAM_EXECUTION_DATE, executeDateStr);

		for ( int i = 0; i < 2; i++ ) {
			instr.addParameter(String.format("%s %d", TEST_PARAM_KEY, i),
					String.format("%s %d %s", TEST_PARAM_KEY, i, TEST_PARAM_VALUE));
		}

		dao.storeInstruction(instr);
		lastDatum = dao.getInstruction(instr.getId(), instr.getInstructorId());

		assertThat("Execution date returned", lastDatum.getExecutionDate(), equalTo(executeDate));

		// verify date stored
		List<Map<String, Object>> rows = listInstructions();
		assertThat("Row stored", rows, hasSize(1));
		assertThat("Execution date stored", rows.get(0), hasKey("execute_at"));
	}

	private String stringResource(String resource) {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(getClass().getResourceAsStream(resource), "UTF-8"));
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void storeNew_fromJson() throws IOException {
		// GIVEN
		ObjectMapper mapper = JsonUtils.newDatumObjectMapper();
		String json = stringResource("instr-01.json");
		net.solarnetwork.domain.Instruction externalInstr = mapper.readValue(json,
				net.solarnetwork.domain.Instruction.class);
		BasicInstruction instr = BasicInstruction.from(externalInstr, TEST_INSTRUCTOR);

		// WHEN
		dao.storeInstruction(instr);
		Instruction result = dao.getInstruction(instr.getId(), instr.getInstructorId());

		// THEN
		assertThat("Instruction persisted", result, is(notNullValue()));
	}

	@Test(expected = DuplicateKeyException.class)
	public void storeDuplicate() {
		BasicInstruction instr = new BasicInstruction(TEST_ID, TEST_TOPIC, Instant.now(),
				TEST_INSTRUCTOR, null);

		for ( int i = 0; i < 2; i++ ) {
			instr.addParameter(String.format("%s %d", TEST_PARAM_KEY, i),
					String.format("%s %d %s", TEST_PARAM_KEY, i, TEST_PARAM_VALUE));
		}

		dao.storeInstruction(instr);
		dao.storeInstruction(instr);
	}

	@Test
	public void storeLocal() {
		// GIVEN
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction("/test/control", "1");

		// WHEN
		dao.storeInstruction(instr);
		Instruction result = dao.getInstruction(instr.getId(), instr.getInstructorId());

		// THEN
		assertThat("Instruction persisted", result, is(notNullValue()));
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		assertThat("Retrieved by primary key", lastDatum, is(notNullValue()));
		Set<String> expectedParameterNames = new LinkedHashSet<>();
		for ( int i = 0; i < 2; i++ ) {
			expectedParameterNames.add(String.format("%s %d", TEST_PARAM_KEY, i));
		}
		assertThat("Retrieved param names", lastDatum.getParameterNames(), is(expectedParameterNames));
		for ( String paramName : lastDatum.getParameterNames() ) {
			assertThat("Param " + paramName + " value", lastDatum.getAllParameterValues(paramName),
					arrayContaining(paramName + " Test Value"));
		}

		InstructionStatus status = lastDatum.getStatus();
		assertThat("Status retrieved", status, is(notNullValue()));
		assertThat("State", status.getInstructionState(), is(InstructionState.Received));
		assertThat("Status date populated", status.getStatusDate(), is(notNullValue()));
		assertThat("Ack state", status.getAcknowledgedInstructionState(), is(nullValue()));
	}

	@Test
	public void findByState() {
		storeNew();
		List<Instruction> results = dao.findInstructionsForState(InstructionState.Completed);
		assertNotNull(results);
		assertEquals(0, results.size());

		results = dao.findInstructionsForState(InstructionState.Received);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(lastDatum.getId(), results.get(0).getId());
	}

	@Test
	public void findByState_withoutDeferredInstructions() {
		// GIVEN
		storeNew();
		Instruction instr = lastDatum;

		final Instant executeDate = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1,
				ChronoUnit.HOURS);
		final String executeDateStr = DateTimeFormatter.ISO_INSTANT.format(executeDate);
		BasicInstruction instr2 = new BasicInstruction(
				Math.abs(UUID.randomUUID().getMostSignificantBits()), TEST_TOPIC, Instant.now(),
				TEST_INSTRUCTOR, null);
		instr2.addParameter(Instruction.PARAM_EXECUTION_DATE, executeDateStr);
		dao.storeInstruction(instr2);

		// WHEN
		List<Instruction> results = dao.findInstructionsForState(InstructionState.Received);

		// THEN
		List<Map<String, Object>> rows = listInstructions();
		assertThat("Two instruction rows exist", rows, hasSize(2));

		assertThat("One instruction returned becauase deferred instruction omitted", results,
				hasSize(1));
		assertThat("Immediate instruction returned", results.get(0).getId(), is(equalTo(instr.getId())));
	}

	@Test
	public void findByStateAndParent() {
		// GIVEN

		// store a parent instruction
		storeNew();

		// store several children
		final int count = 3;
		final List<Instruction> instructions = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			BasicInstruction instr = new BasicInstruction(UUID.randomUUID().getMostSignificantBits(),
					TEST_TOPIC, Instant.now(), TEST_INSTRUCTOR, null);
			instr.addParameter(Instruction.PARAM_PARENT_INSTRUCTOR_ID, lastDatum.getInstructorId());
			instr.addParameter(Instruction.PARAM_PARENT_INSTRUCTION_ID, lastDatum.getId().toString());
			for ( int j = 0; j < 2; j++ ) {
				instr.addParameter(String.format("%s %d", TEST_PARAM_KEY, j),
						String.format("%s %d %s", TEST_PARAM_KEY, j, TEST_PARAM_VALUE));
			}
			dao.storeInstruction(instr);
			instructions.add(dao.getInstruction(instr.getId(), instr.getInstructorId()));
		}

		log.debug("Instruction params: [{}]", listInstructionParams().stream().map(Object::toString)
				.collect(Collectors.joining("\n  , ", "\n    ", "\n")));

		// WHEN
		List<Instruction> results = dao.findInstructionsForStateAndParent(InstructionState.Received,
				lastDatum.getInstructorId(), lastDatum.getId());

		// THEN
		assertThat("All children returned", results, hasSize(3));

		Long[] expectedChildIds = instructions.stream().map(Instruction::getId).toArray(Long[]::new);
		Set<Long> childIds = results.stream().map(Instruction::getId).collect(Collectors.toSet());
		assertThat("All children accounted for", childIds,
				containsInAnyOrder((Object[]) expectedChildIds));
	}

	@Test
	public void updateState() {
		storeNew();
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Declined);
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		InstructionStatus status = newDatum.getStatus();
		assertThat("State updated", status.getInstructionState(), is(InstructionState.Declined));
		assertThat("Status date populated", status.getStatusDate(), is(notNullValue()));
		assertThat("Ack state", status.getAcknowledgedInstructionState(), is(nullValue()));
		assertThat("Result pararms", status.getResultParameters(), is(nullValue()));
	}

	@Test
	public void updateStateWithResultParameters() {
		storeNew();
		Map<String, Object> resultParameters = new LinkedHashMap<String, Object>();
		resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM, "ok");
		resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "505");
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Declined,
				resultParameters);
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		InstructionStatus status = newDatum.getStatus();
		assertThat("State updated", status.getInstructionState(), is(InstructionState.Declined));
		assertThat("Status date populated", status.getStatusDate(), is(notNullValue()));
		assertThat("Ack state", status.getAcknowledgedInstructionState(), is(nullValue()));
		assertThat("Result pararms saved", status.getResultParameters(), is(resultParameters));
	}

	@Test
	public void updateAckState() {
		storeNew();
		InstructionStatus newStatus = lastDatum.getStatus()
				.newCopyWithAcknowledgedState(InstructionState.Received);
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		InstructionStatus status = newDatum.getStatus();
		assertThat("State updated", status.getInstructionState(), is(InstructionState.Received));
		assertThat("Status date populated", status.getStatusDate(), is(notNullValue()));
		assertThat("Ack state updated", status.getAcknowledgedInstructionState(),
				is(InstructionState.Received));
		assertThat("Result pararms", status.getResultParameters(), is(nullValue()));
	}

	@Test
	public void updateAckStatePreservingResultParameters() {
		storeNew();
		Map<String, Object> resultParameters = Collections.singletonMap("foo", (Object) "bar");
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Completed,
				resultParameters);
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		// now "ack"; should preserve existing result parameters
		Instruction newDatum = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		InstructionStatus status = newDatum.getStatus();
		newStatus = status.newCopyWithAcknowledgedState(InstructionState.Completed);
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		newDatum = dao.getInstruction(newDatum.getId(), newDatum.getInstructorId());
		status = newDatum.getStatus();
		assertThat("State updated", status.getInstructionState(), is(InstructionState.Completed));
		assertThat("Status date populated", status.getStatusDate(), is(notNullValue()));
		assertThat("Ack state updated", status.getAcknowledgedInstructionState(),
				is(InstructionState.Completed));
		assertThat("Result pararms", status.getResultParameters(), is(resultParameters));
	}

	@Test
	public void findForAck() {
		storeNew();
		List<Instruction> results = dao.findInstructionsForAcknowledgement();
		assertThat("Result provided", results, hasSize(1));
		assertEquals(lastDatum.getId(), results.get(0).getId());

		// update ack now for second search
		InstructionStatus newStatus = lastDatum.getStatus()
				.newCopyWithAcknowledgedState(lastDatum.getStatus().getInstructionState());
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		results = dao.findInstructionsForAcknowledgement();
		assertThat("Result not found", results, hasSize(0));
	}

	@Test
	public void findForAck_localOnly() {
		// GIVEN
		final String controlId = UUID.randomUUID().toString();
		final String controlVal = UUID.randomUUID().toString();
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(controlId,
				controlVal);
		dao.storeInstruction(instr);
		instr = dao.getInstruction(instr.getId(), instr.getInstructorId());

		// WHEN
		List<Instruction> results = dao.findInstructionsForAcknowledgement();
		assertThat("Results empty because local instruction ignored", results, hasSize(0));

		// update ack now for second search
		InstructionStatus newStatus = instr.getStatus()
				.newCopyWithAcknowledgedState(instr.getStatus().getInstructionState());
		dao.storeInstructionStatus(instr.getId(), instr.getInstructorId(), newStatus);

		results = dao.findInstructionsForAcknowledgement();
		assertThat("Result still not found", results, hasSize(0));
	}

	@Test
	public void findForAck_localIgnored() {
		// GIVEN
		final String controlId = UUID.randomUUID().toString();
		final String controlVal = UUID.randomUUID().toString();
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(controlId,
				controlVal);
		dao.storeInstruction(instr);
		instr = dao.getInstruction(instr.getId(), instr.getInstructorId());

		// now store non-local instruction
		storeNew();

		// WHEN
		List<Instruction> results = dao.findInstructionsForAcknowledgement();
		assertThat("Result provided (local ignored)", results, hasSize(1));
		assertEquals(lastDatum.getId(), results.get(0).getId());

		// update ack now for second search
		InstructionStatus newStatus = lastDatum.getStatus()
				.newCopyWithAcknowledgedState(lastDatum.getStatus().getInstructionState());
		dao.storeInstructionStatus(lastDatum.getId(), lastDatum.getInstructorId(), newStatus);

		results = dao.findInstructionsForAcknowledgement();
		assertThat("Result no longer found because acknowledged", results, hasSize(0));
	}

	@Test
	public void compareAndSetStatus() {
		storeNew();
		InstructionStatus execStatus = lastDatum.getStatus()
				.newCopyWithState(InstructionState.Executing);
		boolean updated = dao.compareAndStoreInstructionStatus(lastDatum.getId(),
				lastDatum.getInstructorId(), InstructionState.Received, execStatus);
		assertThat("Status updated", updated, equalTo(true));
		Instruction instr = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		assertThat("State", instr.getStatus().getInstructionState(),
				equalTo(InstructionState.Executing));
		assertThat("Acknoledged state", instr.getStatus().getAcknowledgedInstructionState(),
				is(nullValue()));
	}

	@Test
	public void compareAndSetStatusNotChanged() {
		storeNew();
		InstructionStatus doneStatus = lastDatum.getStatus()
				.newCopyWithState(InstructionState.Completed);
		boolean updated = dao.compareAndStoreInstructionStatus(lastDatum.getId(),
				lastDatum.getInstructorId(), InstructionState.Executing, doneStatus);
		assertThat("Status updated", updated, equalTo(false));
		Instruction instr = dao.getInstruction(lastDatum.getId(), lastDatum.getInstructorId());
		assertThat("State", instr.getStatus().getInstructionState(), equalTo(InstructionState.Received));
		assertThat("Acknoledged state", instr.getStatus().getAcknowledgedInstructionState(),
				is(nullValue()));
	}

	@Test
	public void deleteHandledOlderThan_ackComplete() {
		// GIVEN
		Instruction instr = storeNew(Instant.now().minus(2, ChronoUnit.HOURS));

		// update to Complete + acknowledged Complete
		InstructionStatus doneStatus = instr.getStatus().newCopyWithState(InstructionState.Completed)
				.newCopyWithAcknowledgedState(InstructionState.Completed);
		dao.storeInstructionStatus(instr.getId(), instr.getInstructorId(), doneStatus);

		// WHEN
		int result = dao.deleteHandledInstructionsOlderThan(1);

		// THEN
		assertThat("One instruction deleted because older than 1 hour and acknowledged Complete", result,
				is(equalTo(1)));

		List<Map<String, Object>> rows = listInstructions();
		assertThat("No rows exist", rows, hasSize(0));
	}

	@Test
	public void deleteHandledOlderThan_ackDeclined() {
		// GIVEN
		Instruction instr = storeNew(Instant.now().minus(2, ChronoUnit.HOURS));

		// update to Complete + acknowledged Complete
		InstructionStatus doneStatus = instr.getStatus().newCopyWithState(InstructionState.Declined)
				.newCopyWithAcknowledgedState(InstructionState.Declined);
		dao.storeInstructionStatus(instr.getId(), instr.getInstructorId(), doneStatus);

		// WHEN
		int result = dao.deleteHandledInstructionsOlderThan(1);

		// THEN
		assertThat("One instruction deleted because older than 1 hour and acknowledged Declined", result,
				is(equalTo(1)));

		List<Map<String, Object>> rows = listInstructions();
		assertThat("No rows exist", rows, hasSize(0));
	}

	@Test
	public void deleteHandledOlderThan_complete() {
		// GIVEN
		Instruction instr = storeNew(Instant.now().minus(2, ChronoUnit.HOURS));

		// update to Complete + acknowledged Received
		InstructionStatus doneStatus = instr.getStatus().newCopyWithState(InstructionState.Completed)
				.newCopyWithAcknowledgedState(InstructionState.Received);
		dao.storeInstructionStatus(instr.getId(), instr.getInstructorId(), doneStatus);

		// WHEN
		int result = dao.deleteHandledInstructionsOlderThan(1);

		// THEN
		assertThat("No instruction deleted because acknowledged state != state", result, is(equalTo(0)));

		List<Map<String, Object>> rows = listInstructions();
		assertThat("Rows still exists", rows, hasSize(1));
	}

	@Test
	public void deleteHandledOlderThan_local_complete() {
		// GIVEN
		Instruction instr = InstructionUtils.createSetControlValueLocalInstruction("foo", "bar");
		instr = storeNew(instr.getId(), instr.getInstructorId(),
				Instant.now().minus(2, ChronoUnit.HOURS));

		// update to Complete without any ack
		InstructionStatus doneStatus = instr.getStatus().newCopyWithState(InstructionState.Completed);
		dao.storeInstructionStatus(instr.getId(), instr.getInstructorId(), doneStatus);

		// WHEN
		int result = dao.deleteHandledInstructionsOlderThan(1);

		// THEN
		assertThat("One instruction deleted because older than 1 hour and Complete and local", result,
				is(equalTo(1)));

		List<Map<String, Object>> rows = listInstructions();
		assertThat("No rows exist", rows, hasSize(0));
	}

}
