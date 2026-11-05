/* ==================================================================
 * AllOrNothingControlGroupConsensusStrategyTests.java - 5 Nov 2026 7:18:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.group.test;

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Executing;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_DEMAND_BALANCE;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createSetControlValueLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static org.assertj.core.api.BDDAssertions.entry;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.group.AllOrNothingControlGroupConsensusStrategy;
import net.solarnetwork.node.control.group.InstructionExecution;
import net.solarnetwork.node.control.group.MemberControlConfig;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * Test cases for the {@link AllOrNothingControlGroupConsensusStrategy} class.
 *
 * @author matt
 * @version 1.0
 */
public class AllOrNothingControlGroupConsensusStrategyTests {

	private static final String GROUP_CONTROL_ID = "/test/site/limit";
	private static final String MEMBER_1_CONTROL_ID = "/test/inv/1/limit";
	private static final String MEMBER_2_CONTROL_ID = "/test/inv/2/limit";

	private final AllOrNothingControlGroupConsensusStrategy service = new AllOrNothingControlGroupConsensusStrategy();

	private NodeControlInfo group() {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId(GROUP_CONTROL_ID)
				.withReadonly(Boolean.FALSE)
				.withType(NodeControlPropertyType.Percent)
				.build()
				;
		// @formatter:on
	}

	private List<MemberControlConfig> members() {
		return List.of(new MemberControlConfig(MEMBER_1_CONTROL_ID),
				new MemberControlConfig(MEMBER_2_CONTROL_ID));
	}

	private Instruction request() {
		return createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
	}

	private static InstructionExecution execution(Instruction instr, @Nullable InstructionState state) {
		return new InstructionExecution(instr, (state != null ? createStatus(instr, state) : null));
	}

	private static InstructionExecution execution(String controlId, String value,
			@Nullable InstructionState state) {
		return execution(createSetControlValueLocalInstruction(controlId, value), state);
	}

	@Test
	public void evaluate_allCompleted() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);
		results.put(MEMBER_1_CONTROL_ID, execution(MEMBER_1_CONTROL_ID, "50", Completed));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "50", Completed));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Status is for group instruction")
			.returns(req.getId(), from(InstructionStatus::getInstructionId))
			.as("Completed when all members completed")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Member updates provided in result")
			.containsOnlyKeys("updates")
			.extractingByKey("updates", map(String.class, String.class))
			.as("Member update values provided in member order")
			.containsExactly(
				  entry(MEMBER_1_CONTROL_ID, "50")
				, entry(MEMBER_2_CONTROL_ID, "50")
			)
			;
		// @formatter:on
	}

	@Test
	public void evaluate_memberNotHandled() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);
		results.put(MEMBER_1_CONTROL_ID, execution(MEMBER_1_CONTROL_ID, "50", Completed));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "50", null));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Status is for group instruction")
			.returns(req.getId(), from(InstructionStatus::getInstructionId))
			.as("Declined when member result not available")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes unhandled member")
			.containsOnly(entry("message", "Member control /test/inv/2/limit -> 50 was not handled."))
			;
		// @formatter:on
	}

	@Test
	public void evaluate_memberNotCompleted() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);
		results.put(MEMBER_1_CONTROL_ID, execution(MEMBER_1_CONTROL_ID, "50", Completed));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "50", Declined));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Declined when member declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes declined member and its state")
			.containsOnly(entry("message", "Member control /test/inv/2/limit -> 50 was not completed (Declined)."))
			;
		// @formatter:on
	}

	@Test
	public void evaluate_memberExecuting() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);
		results.put(MEMBER_1_CONTROL_ID, execution(MEMBER_1_CONTROL_ID, "50", Executing));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "50", Completed));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Declined when member has not completed yet")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes incomplete member and its state")
			.containsOnly(entry("message", "Member control /test/inv/1/limit -> 50 was not completed (Executing)."))
			;
		// @formatter:on
	}

	@Test
	public void evaluate_firstFailureReported() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);
		results.put(MEMBER_1_CONTROL_ID, execution(MEMBER_1_CONTROL_ID, "40", Declined));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "60", null));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Declined when members fail")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes first failed member")
			.containsOnly(entry("message", "Member control /test/inv/1/limit -> 40 was not completed (Declined)."))
			;
		// @formatter:on
	}

	@Test
	public void evaluate_memberValueMissing_notCompleted() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(1);

		// member instruction does not have a parameter for the member control ID
		final Instruction memberInstr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, "foo",
				"bar");
		results.put(MEMBER_1_CONTROL_ID, execution(memberInstr, Declined));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Declined when member declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message uses placeholder for missing member value")
			.containsOnly(entry("message", "Member control /test/inv/1/limit -> ? was not completed (Declined)."))
			;
		// @formatter:on
	}

	@Test
	public void evaluate_memberValueMissing_completed() {
		// GIVEN
		final Instruction req = request();
		final Map<String, InstructionExecution> results = new LinkedHashMap<>(2);

		// member 1 instruction does not have a parameter for the member control ID
		final Instruction memberInstr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, "foo",
				"bar");
		results.put(MEMBER_1_CONTROL_ID, execution(memberInstr, Completed));
		results.put(MEMBER_2_CONTROL_ID, execution(MEMBER_2_CONTROL_ID, "50", Completed));

		// WHEN
		InstructionStatus result = service.evaluate(group(), members(), req, results);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Completed when all members completed")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.extractingByKey("updates", map(String.class, String.class))
			.as("Member update without value omitted from result")
			.containsOnly(entry(MEMBER_2_CONTROL_ID, "50"))
			;
		// @formatter:on
	}

}
