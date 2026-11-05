/* ==================================================================
 * ControlGroupTests.java - 5 Nov 2026 7:05:14 pm
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
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_DEMAND_BALANCE;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createSetControlValueLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.entry;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.group.ControlGroup;
import net.solarnetwork.node.control.group.ControlGroupConsensusStrategy;
import net.solarnetwork.node.control.group.ControlGroupCoordinationStrategy;
import net.solarnetwork.node.control.group.InstructionExecution;
import net.solarnetwork.node.control.group.MemberControlConfig;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.GroupSettingSpecifier;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link ControlGroup} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class ControlGroupTests {

	private static final String GROUP_CONTROL_ID = "/test/site/limit";
	private static final String MEMBER_1_CONTROL_ID = "/test/inv/1/limit";
	private static final String MEMBER_2_CONTROL_ID = "/test/inv/2/limit";
	private static final String OTHER_CONTROL_ID = "/test/other";
	private static final String PROPERTY_NAME = "limit";

	@Mock
	private InstructionExecutionService instructionService;

	@Mock
	private EventAdmin eventAdmin;

	@Mock
	private NodeControlProvider control1;

	@Mock
	private NodeControlProvider control2;

	@Mock
	private ControlGroupCoordinationStrategy coordinationStrategy;

	@Mock
	private ControlGroupConsensusStrategy consensusStrategy;

	@Captor
	private ArgumentCaptor<Instruction> instructionCaptor;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Captor
	private ArgumentCaptor<NodeControlInfo> groupCaptor;

	@Captor
	private ArgumentCaptor<List<MemberControlConfig>> membersCaptor;

	@Captor
	private ArgumentCaptor<Map<String, InstructionExecution>> resultsCaptor;

	private InstantSource clock;
	private List<NodeControlProvider> controls;
	private ControlGroup service;

	@Before
	public void setup() {
		clock = InstantSource.fixed(Instant.parse("2026-11-05T06:00:00Z"));
		controls = new ArrayList<>();
		service = newControlGroup(null, null, instructionService);
	}

	private ControlGroup newControlGroup(@Nullable ControlGroupCoordinationStrategy coordinationStrategy,
			@Nullable ControlGroupConsensusStrategy consensusStrategy,
			@Nullable InstructionExecutionService instructionService) {
		ControlGroup result = new ControlGroup(clock, new StaticOptionalService<>(coordinationStrategy),
				new StaticOptionalService<>(consensusStrategy), controls,
				new StaticOptionalService<>(instructionService),
				new StaticOptionalService<>(eventAdmin));
		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasename(ControlGroup.class.getName());
		result.setMessageSource(msgSource);
		result.setControlId(GROUP_CONTROL_ID);
		result.setControlPropertyName(PROPERTY_NAME);
		return result;
	}

	private static MemberControlConfig[] members(String... controlIds) {
		MemberControlConfig[] result = new MemberControlConfig[controlIds.length];
		for ( int i = 0; i < controlIds.length; i++ ) {
			result[i] = new MemberControlConfig(controlIds[i]);
		}
		return result;
	}

	private static NodeControlInfo controlInfo(String controlId, NodeControlPropertyType type,
			String value) {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId(controlId)
				.withPropertyName(PROPERTY_NAME)
				.withType(type)
				.withValue(value)
				.build()
				;
		// @formatter:on
	}

	@Test
	public void handlesTopic_defaults() {
		// @formatter:off
		and.then(service.handlesTopic(TOPIC_SET_CONTROL_PARAMETER))
			.as("Set control parameter topic handled by default")
			.isTrue()
			;
		and.then(service.handlesTopic(TOPIC_DEMAND_BALANCE))
			.as("Demand balance topic handled by default")
			.isTrue()
			;
		and.then(service.handlesTopic("Other"))
			.as("Other topic not handled")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void handlesTopic_configured() {
		// GIVEN
		service.setInstructionTopicsValue("Foo, Bar");

		// THEN
		// @formatter:off
		and.then(service.getInstructionTopics())
			.as("Topics parsed from comma-delimited setting value")
			.containsExactly("Foo", "Bar")
			;
		and.then(service.handlesTopic("Bar"))
			.as("Configured topic handled")
			.isTrue()
			;
		and.then(service.handlesTopic(TOPIC_SET_CONTROL_PARAMETER))
			.as("Default topic not handled when not configured")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void instructionTopicsValue_null() {
		// GIVEN
		service.setInstructionTopicsValue("Foo");

		// WHEN
		service.setInstructionTopicsValue(null);

		// THEN
		// @formatter:off
		and.then(service.getInstructionTopics())
			.as("Default topics restored")
			.isEqualTo(ControlGroup.DEFAULT_INSTRUCTION_TOPICS)
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_otherTopic() {
		// GIVEN
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createLocalInstruction("Other", GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Instruction with unsupported topic not handled")
			.isNull()
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_noControlId() {
		// GIVEN
		service.setControlId(null);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(GROUP_CONTROL_ID, 50);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Instruction not handled when no group control ID configured")
			.isNull()
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_otherControl() {
		// GIVEN
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(MEMBER_1_CONTROL_ID, 50);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Instruction for another control (such as a member) not handled")
			.isNull()
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_readOnly() {
		// GIVEN
		service.setControlReadOnly(true);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(GROUP_CONTROL_ID, 50);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Status is for group instruction")
			.returns(instr.getId(), from(InstructionStatus::getInstructionId))
			.as("Read-only control update declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Read-only message provided")
			.containsEntry("message", "Control can not be updated as it is read-only.")
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_demandBalance_percent() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		given(instructionService.executeInstruction(any()))
				.willAnswer(invocation -> createStatus(invocation.getArgument(0), Completed));

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		then(instructionService).should(times(2)).executeInstruction(instructionCaptor.capture());
		and.then(instructionCaptor.getAllValues())
			.as("Member instructions executed in member order")
			.satisfiesExactly(
				i -> and.then(i)
					.as("Member 1 instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					.as("Member 1 given group percentage")
					.returns("50", from(in -> in.getParameterValue(MEMBER_1_CONTROL_ID)))
					,
				i -> and.then(i)
					.as("Member 2 instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					.as("Member 2 given group percentage")
					.returns("50", from(in -> in.getParameterValue(MEMBER_2_CONTROL_ID)))
			)
			;
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Status is for group instruction")
			.returns(instr.getId(), from(InstructionStatus::getInstructionId))
			.as("Group instruction completed")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Member updates provided in result")
			.containsEntry("updates", Map.of(MEMBER_1_CONTROL_ID, "50", MEMBER_2_CONTROL_ID, "50"))
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_setControlParameter_float() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Float);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		given(instructionService.executeInstruction(any()))
				.willAnswer(invocation -> createStatus(invocation.getArgument(0), Completed));

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(GROUP_CONTROL_ID, 2400);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		then(instructionService).should(times(2)).executeInstruction(instructionCaptor.capture());
		and.then(instructionCaptor.getAllValues())
			.as("Member instructions executed in member order")
			.satisfiesExactly(
				i -> and.then(i)
					.as("Member 1 given half of group total")
					.returns("1200", from(in -> in.getParameterValue(MEMBER_1_CONTROL_ID)))
					,
				i -> and.then(i)
					.as("Member 2 given half of group total")
					.returns("1200", from(in -> in.getParameterValue(MEMBER_2_CONTROL_ID)))
			)
			;
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction completed")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Member updates provided in result")
			.containsEntry("updates", Map.of(MEMBER_1_CONTROL_ID, "1200", MEMBER_2_CONTROL_ID, "1200"))
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_memberDeclined() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		given(instructionService.executeInstruction(any())).willAnswer(invocation -> {
			Instruction memberInstr = invocation.getArgument(0);
			return createStatus(memberInstr,
					memberInstr.getParameterValue(MEMBER_1_CONTROL_ID) != null ? Declined : Completed);
		});

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		then(instructionService).should(times(2)).executeInstruction(instructionCaptor.capture());
		and.then(instructionCaptor.getAllValues())
			.as("Remaining members still updated after a member declines")
			.extracting(i -> i.getParameterNames().iterator().next())
			.containsExactly(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID)
			;
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes declined member")
			.containsEntry("message", "Member control /test/inv/1/limit -> 50 was not completed (Declined).")
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_memberNotHandled() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// no handler available for member 2
		given(instructionService.executeInstruction(any())).willAnswer(invocation -> {
			Instruction memberInstr = invocation.getArgument(0);
			return (memberInstr.getParameterValue(MEMBER_2_CONTROL_ID) != null ? null
					: createStatus(memberInstr, Completed));
		});

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes unhandled member")
			.containsEntry("message", "Member control /test/inv/2/limit -> 50 was not handled.")
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_noInstructionService() {
		// GIVEN
		service = newControlGroup(null, null, null);
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes missing service")
			.containsEntry("message", "No InstructionExecutionService is available.")
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_memberException() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// member 1 handler throws an exception
		given(instructionService.executeInstruction(any())).willAnswer(invocation -> {
			Instruction memberInstr = invocation.getArgument(0);
			if ( memberInstr.getParameterValue(MEMBER_1_CONTROL_ID) != null ) {
				throw new RuntimeException("Boom");
			}
			return createStatus(memberInstr, Completed);
		});

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		then(instructionService).should(times(2)).executeInstruction(instructionCaptor.capture());
		and.then(instructionCaptor.getAllValues())
			.as("Remaining members still updated after a member throws an exception")
			.extracting(i -> i.getParameterNames().iterator().next())
			.containsExactly(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID)
			;
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes failed member")
			.containsEntry("message", "Member control /test/inv/1/limit -> 50 was not completed (Declined).")
			;
		// @formatter:on
	}

	@Test
	public void processInstruction_exception() {
		// GIVEN
		service = newControlGroup(coordinationStrategy, null, instructionService);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		given(coordinationStrategy.distribute(any(), any(), any(), any()))
				.willThrow(new RuntimeException("Boom"));

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message includes exception message")
			.containsEntry("message", "An exception occurred updating the member controls: Boom")
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_notDistributed() {
		// GIVEN
		service = newControlGroup(coordinationStrategy, null, instructionService);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		given(coordinationStrategy.distribute(any(), any(), any(), any())).willReturn(null);

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(GROUP_CONTROL_ID, 50);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes undistributed update")
			.containsEntry("message",
					"The control update was not handled by the configured coordination strategy.")
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_noMembers() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);

		// WHEN
		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined when no members configured")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes undistributed update")
			.containsEntry("message",
					"The control update was not handled by the configured coordination strategy.")
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_nonNumericValue() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Float);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		final Instruction instr = createSetControlValueLocalInstruction(GROUP_CONTROL_ID, "abc");
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Group instruction declined when value is not a number")
			.returns(Declined, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Message describes undistributed update")
			.containsEntry("message",
					"The control update was not handled by the configured coordination strategy.")
			;
		// @formatter:on
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void processInstruction_configuredStrategies() {
		// GIVEN
		service = newControlGroup(coordinationStrategy, consensusStrategy, instructionService);
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		final MemberControlConfig[] members = members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID);
		service.setMemberConfigs(members);

		final Instruction instr = createLocalInstruction(TOPIC_DEMAND_BALANCE, GROUP_CONTROL_ID, "50");
		final Instruction memberInstr = createSetControlValueLocalInstruction(MEMBER_1_CONTROL_ID, "40");
		given(coordinationStrategy.distribute(any(), any(), same(instr), same(controls)))
				.willReturn(Map.of(MEMBER_1_CONTROL_ID, memberInstr));

		final InstructionStatus memberStatus = createStatus(memberInstr, Completed);
		given(instructionService.executeInstruction(memberInstr)).willReturn(memberStatus);

		final InstructionStatus groupStatus = createStatus(instr, Completed);
		given(consensusStrategy.evaluate(any(), any(), same(instr), any())).willReturn(groupStatus);

		// WHEN
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		// @formatter:off
		then(coordinationStrategy).should().distribute(groupCaptor.capture(), membersCaptor.capture(),
				same(instr), same(controls));
		and.then(groupCaptor.getValue())
			.as("Group control ID provided to coordination strategy")
			.returns(GROUP_CONTROL_ID, from(NodeControlInfo::getControlId))
			.as("Group control type provided to coordination strategy")
			.returns(NodeControlPropertyType.Percent, from(NodeControlInfo::getType))
			.as("Group property name provided to coordination strategy")
			.returns(PROPERTY_NAME, from(NodeControlInfo::getPropertyName))
			.as("Group value not provided to coordination strategy")
			.returns(null, from(NodeControlInfo::getValue))
			;
		and.then(membersCaptor.getValue())
			.as("Member configurations provided to coordination strategy")
			.containsExactly(members)
			;
		then(consensusStrategy).should().evaluate(same(groupCaptor.getValue()),
				same(membersCaptor.getValue()), same(instr), resultsCaptor.capture());
		and.then(resultsCaptor.getValue())
			.as("Distributed instruction results provided to consensus strategy")
			.containsExactly(entry(MEMBER_1_CONTROL_ID, new InstructionExecution(memberInstr, memberStatus)))
			;
		and.then(result)
			.as("Consensus strategy result returned")
			.isSameAs(groupStatus)
			;
		// @formatter:on
	}

	@Test
	public void getAvailableControlIds() {
		// @formatter:off
		and.then(service.getAvailableControlIds())
			.as("Group control ID available")
			.containsExactly(GROUP_CONTROL_ID)
			;
		// @formatter:on
	}

	@Test
	public void getAvailableControlIds_noControlId() {
		// GIVEN
		service.setControlId(null);

		// THEN
		// @formatter:off
		and.then(service.getAvailableControlIds())
			.as("No control ID available when none configured")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void getCurrentControlInfo_otherControl() {
		// GIVEN
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(MEMBER_1_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No info for control other than group")
			.isNull()
			;
		// @formatter:on
		then(eventAdmin).shouldHaveNoInteractions();
	}

	@Test
	public void getCurrentControlInfo_percent() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));
		controls.add(control1);
		controls.add(control2);

		given(control1.getAvailableControlIds()).willReturn(List.of(MEMBER_1_CONTROL_ID));
		given(control1.getCurrentControlInfo(MEMBER_1_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_1_CONTROL_ID, NodeControlPropertyType.Percent, "50"));
		given(control2.getAvailableControlIds()).willReturn(List.of(MEMBER_2_CONTROL_ID));
		given(control2.getCurrentControlInfo(MEMBER_2_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_2_CONTROL_ID, NodeControlPropertyType.Percent, "50"));

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(GROUP_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is control datum")
			.asInstanceOf(type(SimpleNodeControlInfoDatum.class))
			.as("Control ID is group control ID")
			.returns(GROUP_CONTROL_ID, from(NodeControlInfo::getControlId))
			.as("Control type is group control type")
			.returns(NodeControlPropertyType.Percent, from(NodeControlInfo::getType))
			.as("Property name is group property name")
			.returns(PROPERTY_NAME, from(NodeControlInfo::getPropertyName))
			.as("Value combined from member values")
			.returns("50", from(NodeControlInfo::getValue))
			.as("Datum timestamp from clock")
			.returns(clock.instant(), from(SimpleNodeControlInfoDatum::getTimestamp))
			;
		then(eventAdmin).should().postEvent(eventCaptor.capture());
		and.then(eventCaptor.getValue())
			.as("Control info captured event posted")
			.returns(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, from(Event::getTopic))
			.as("Event datum is result")
			.returns(result, from(e -> e.getProperty(DatumEvents.DATUM_PROPERTY)))
			;
		// @formatter:on
	}

	@Test
	public void getCurrentControlInfo_float() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Float);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));
		controls.add(control1);
		controls.add(control2);

		given(control1.getAvailableControlIds()).willReturn(List.of(MEMBER_1_CONTROL_ID));
		given(control1.getCurrentControlInfo(MEMBER_1_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_1_CONTROL_ID, NodeControlPropertyType.Float, "1000.5"));
		given(control2.getAvailableControlIds()).willReturn(List.of(MEMBER_2_CONTROL_ID));
		given(control2.getCurrentControlInfo(MEMBER_2_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_2_CONTROL_ID, NodeControlPropertyType.Float, "1200"));

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(GROUP_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Value is sum of member values")
			.returns("2200.5", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void getCurrentControlInfo_onlyMembersRead() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// the group itself is also an available control, as in a deployed node
		controls.add(service);
		controls.add(control1);
		controls.add(control2);

		given(control1.getAvailableControlIds())
				.willReturn(List.of(OTHER_CONTROL_ID, MEMBER_1_CONTROL_ID));
		given(control1.getCurrentControlInfo(MEMBER_1_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_1_CONTROL_ID, NodeControlPropertyType.Percent, "80"));
		given(control2.getAvailableControlIds()).willReturn(List.of(MEMBER_2_CONTROL_ID));
		given(control2.getCurrentControlInfo(MEMBER_2_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_2_CONTROL_ID, NodeControlPropertyType.Percent, "80"));

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(GROUP_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Value combined from member values")
			.returns("80", from(NodeControlInfo::getValue))
			;
		// @formatter:on
		then(control1).should(never()).getCurrentControlInfo(OTHER_CONTROL_ID);
	}

	@Test
	public void getCurrentControlInfo_memberUnavailable() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// no control available for member 2
		controls.add(control1);

		given(control1.getAvailableControlIds()).willReturn(List.of(MEMBER_1_CONTROL_ID));
		given(control1.getCurrentControlInfo(MEMBER_1_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_1_CONTROL_ID, NodeControlPropertyType.Percent, "60"));

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(GROUP_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Value combined from available member values")
			.returns("60", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void getCurrentControlInfo_memberValueUnavailable() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));
		controls.add(control1);
		controls.add(control2);

		given(control1.getAvailableControlIds()).willReturn(List.of(MEMBER_1_CONTROL_ID));
		given(control1.getCurrentControlInfo(MEMBER_1_CONTROL_ID))
				.willReturn(controlInfo(MEMBER_1_CONTROL_ID, NodeControlPropertyType.Percent, "50"));

		// member 2 cannot be read, e.g. device offline
		given(control2.getAvailableControlIds()).willReturn(List.of(MEMBER_2_CONTROL_ID));
		given(control2.getCurrentControlInfo(MEMBER_2_CONTROL_ID)).willReturn(null);

		// WHEN
		NodeControlInfo result = service.getCurrentControlInfo(GROUP_CONTROL_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Value combined from readable member values")
			.returns("50", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void controlPropertyTypeKey() {
		// WHEN
		service.setControlPropertyTypeKey("p");

		// THEN
		// @formatter:off
		and.then(service)
			.as("Control type set from key")
			.returns(NodeControlPropertyType.Percent, from(ControlGroup::getControlPropertyType))
			.as("Control type key returned")
			.returns("p", from(ControlGroup::getControlPropertyTypeKey))
			;
		// @formatter:on
	}

	@Test
	public void controlPropertyTypeKey_unknown() {
		// GIVEN
		service.setControlPropertyType(NodeControlPropertyType.Percent);

		// WHEN
		service.setControlPropertyTypeKey("?");

		// THEN
		// @formatter:off
		and.then(service)
			.as("Unknown control type key sets default control type")
			.returns(ControlGroup.DEFAULT_CONTROL_PROPERTY_TYPE, from(ControlGroup::getControlPropertyType))
			;
		// @formatter:on
	}

	@Test
	public void strategyUids() {
		// WHEN
		service.setCoordinationStrategyUid("coord");
		service.setConsensusStrategyUid("consensus");

		// THEN
		// @formatter:off
		and.then(service)
			.as("Coordination strategy UID returned")
			.returns("coord", from(ControlGroup::getCoordinationStrategyUid))
			.as("Consensus strategy UID returned")
			.returns("consensus", from(ControlGroup::getConsensusStrategyUid))
			;
		and.then(service.getCoordinationStrategy().getPropertyFilters())
			.asInstanceOf(map(String.class, Object.class))
			.as("Coordination strategy UID applied as service filter")
			.containsEntry("uid", "coord")
			;
		and.then(service.getConsensusStrategy().getPropertyFilters())
			.asInstanceOf(map(String.class, Object.class))
			.as("Consensus strategy UID applied as service filter")
			.containsEntry("uid", "consensus")
			;
		// @formatter:on
	}

	@Test
	public void memberConfigsCount() {
		// WHEN
		service.setMemberConfigsCount(2);

		// THEN
		// @formatter:off
		and.then(service.getMemberConfigs())
			.as("Member configurations created")
			.hasSize(2)
			.doesNotContainNull()
			;
		and.then(service.getMemberConfigsCount())
			.as("Member configuration count returned")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	@Test
	public void settingSpecifiers_memberConfigs() {
		// GIVEN
		service.setMemberConfigs(members(MEMBER_1_CONTROL_ID, MEMBER_2_CONTROL_ID));

		// WHEN
		List<SettingSpecifier> result = service.getSettingSpecifiers();

		// THEN
		// @formatter:off
		and.then(result)
			.filteredOn(s -> s instanceof GroupSettingSpecifier g && "memberConfigs".equals(g.getKey()))
			.as("Member configurations setting provided")
			.singleElement(type(GroupSettingSpecifier.class))
			.as("Member configurations setting is a dynamic list")
			.returns(true, from(GroupSettingSpecifier::isDynamic))
			.extracting(GroupSettingSpecifier::getGroupSettings, list(SettingSpecifier.class))
			.as("One group setting per member")
			.hasSize(2)
			.flatExtracting(s -> ((GroupSettingSpecifier) s).getGroupSettings())
			.extracting(s -> ((KeyedSettingSpecifier<?>) s).getKey())
			.as("Member settings keyed by list index")
			.containsExactly(
				  "memberConfigs[0].controlId"
				, "memberConfigs[0].parametersValue"
				, "memberConfigs[1].controlId"
				, "memberConfigs[1].parametersValue"
			)
			;
		// @formatter:on
	}

}
