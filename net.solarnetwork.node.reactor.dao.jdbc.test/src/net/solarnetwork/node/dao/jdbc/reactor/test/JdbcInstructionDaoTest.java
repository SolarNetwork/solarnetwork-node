/* ==================================================================
 * JdbcInstructionDaoTest.java - Oct 1, 2011 11:55:54 AM
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.node.dao.jdbc.reactor.JdbcInstructionDao;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test case for the {@link JdbcInstructionDao} class.
 * 
 * @author matt
 * @version 1.2
 */
@ContextConfiguration
public class JdbcInstructionDaoTest extends AbstractNodeTransactionalTest {

	private static final String TEST_INSTRUCTOR = "Test Instructor";
	private static final String TEST_REMOTE_ID = "Test ID";
	private static final String TEST_TOPIC = "Test Topic";
	private static final String TEST_PARAM_KEY = "Test Param";
	private static final String TEST_PARAM_VALUE = "Test Value";

	@Autowired
	private JdbcInstructionDao dao;

	private Instruction lastDatum;

	@Before
	public void setUp() throws Exception {
		lastDatum = null;
	}

	@Test
	public void storeNew() {
		BasicInstruction datum = new BasicInstruction(TEST_TOPIC, new Date(), TEST_REMOTE_ID,
				TEST_INSTRUCTOR, null);

		for ( int i = 0; i < 2; i++ ) {
			datum.addParameter(String.format("%s %d", TEST_PARAM_KEY, i),
					String.format("%s %d %s", TEST_PARAM_KEY, i, TEST_PARAM_VALUE));
		}

		Long id = dao.storeInstruction(datum);
		assertNotNull(id);
		lastDatum = dao.getInstruction(id);
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		assertNotNull(lastDatum);
		assertEquals(TEST_TOPIC, lastDatum.getTopic());
		assertEquals(TEST_REMOTE_ID, lastDatum.getRemoteInstructionId());
		assertEquals(TEST_INSTRUCTOR, lastDatum.getInstructorId());
		assertNotNull(lastDatum.getInstructionDate());

		Set<String> expectedParameterNames = new HashSet<String>();
		for ( int i = 0; i < 2; i++ ) {
			expectedParameterNames.add(String.format("%s %d", TEST_PARAM_KEY, i));
		}
		assertNotNull(lastDatum.getParameterNames());
		for ( String paramName : lastDatum.getParameterNames() ) {
			assertTrue(expectedParameterNames.contains(paramName));
			String[] values = lastDatum.getAllParameterValues(paramName);
			assertNotNull(values);
			assertEquals(1, values.length);
			assertEquals(paramName + " " + TEST_PARAM_VALUE, values[0]);
		}

		InstructionStatus status = lastDatum.getStatus();
		assertNotNull(status);
		assertEquals(InstructionState.Received, status.getInstructionState());
		assertNull(status.getAcknowledgedInstructionState());
		assertNotNull(status.getStatusDate());
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
	public void updateState() {
		storeNew();
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Declined);
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId());
		InstructionStatus status = newDatum.getStatus();
		assertEquals(InstructionState.Declined, status.getInstructionState());
		assertNull(status.getAcknowledgedInstructionState());
		assertNull("Result parameters", status.getResultParameters());
	}

	@Test
	public void updateStateWithResultParameters() {
		storeNew();
		Map<String, Object> resultParameters = new LinkedHashMap<String, Object>();
		resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM, "ok");
		resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "505");
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Declined,
				resultParameters);
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId());
		InstructionStatus status = newDatum.getStatus();
		assertEquals(InstructionState.Declined, status.getInstructionState());
		assertNull(status.getAcknowledgedInstructionState());
		assertEquals("Result parameters", resultParameters, status.getResultParameters());
	}

	@Test
	public void updateAckState() {
		storeNew();
		InstructionStatus newStatus = lastDatum.getStatus()
				.newCopyWithAcknowledgedState(InstructionState.Received);
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		Instruction newDatum = dao.getInstruction(lastDatum.getId());
		InstructionStatus status = newDatum.getStatus();
		assertEquals(InstructionState.Received, status.getInstructionState());
		assertEquals(InstructionState.Received, status.getAcknowledgedInstructionState());
		assertNull("Result parameters", status.getResultParameters());
	}

	@Test
	public void updateAckStatePreservingResultParameters() {
		storeNew();
		Map<String, Object> resultParameters = Collections.singletonMap("foo", (Object) "bar");
		InstructionStatus newStatus = lastDatum.getStatus().newCopyWithState(InstructionState.Completed,
				resultParameters);
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		// now "ack"; should preserve existing result parameters
		Instruction newDatum = dao.getInstruction(lastDatum.getId());
		InstructionStatus status = newDatum.getStatus();
		newStatus = status.newCopyWithAcknowledgedState(InstructionState.Completed);
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		newDatum = dao.getInstruction(newDatum.getId());
		status = newDatum.getStatus();
		assertEquals(InstructionState.Completed, status.getInstructionState());
		assertEquals(InstructionState.Completed, status.getAcknowledgedInstructionState());
		assertEquals("Result parameters", resultParameters, status.getResultParameters());
	}

	@Test
	public void findForAck() {
		storeNew();
		List<Instruction> results = dao.findInstructionsForAcknowledgement();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(lastDatum.getId(), results.get(0).getId());

		// update ack now for second search
		InstructionStatus newStatus = lastDatum.getStatus()
				.newCopyWithAcknowledgedState(lastDatum.getStatus().getInstructionState());
		dao.storeInstructionStatus(lastDatum.getId(), newStatus);

		results = dao.findInstructionsForAcknowledgement();
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	@Test
	public void compareAndSetStatus() {
		storeNew();
		InstructionStatus execStatus = lastDatum.getStatus()
				.newCopyWithState(InstructionState.Executing);
		boolean updated = dao.compareAndStoreInstructionStatus(lastDatum.getId(),
				InstructionState.Received, execStatus);
		assertThat("Status updated", updated, equalTo(true));
		Instruction instr = dao.getInstruction(lastDatum.getId());
		assertThat("State", instr.getStatus().getInstructionState(),
				equalTo(InstructionState.Executing));
		assertThat("Acknoledged state", instr.getStatus().getAcknowledgedInstructionState(),
				nullValue());
	}

	@Test
	public void compareAndSetStatusNotChanged() {
		storeNew();
		InstructionStatus doneStatus = lastDatum.getStatus()
				.newCopyWithState(InstructionState.Completed);
		boolean updated = dao.compareAndStoreInstructionStatus(lastDatum.getId(),
				InstructionState.Executing, doneStatus);
		assertThat("Status updated", updated, equalTo(false));
		Instruction instr = dao.getInstruction(lastDatum.getId());
		assertThat("State", instr.getStatus().getInstructionState(), equalTo(InstructionState.Received));
		assertThat("Acknoledged state", instr.getStatus().getAcknowledgedInstructionState(),
				nullValue());
	}

}
