/* ==================================================================
 * SimpleControlGroupCoordinationStrategyTests.java - 5 Nov 2026 6:52:17 am
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

import static net.solarnetwork.domain.NodeControlPropertyType.Float;
import static net.solarnetwork.domain.NodeControlPropertyType.Integer;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_DEMAND_BALANCE;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createSetControlValueLocalInstruction;
import static net.solarnetwork.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.group.MemberControlConfig;
import net.solarnetwork.node.control.group.SimpleControlGroupCoordinationStrategy;
import net.solarnetwork.node.reactor.Instruction;

/**
 * Test cases for the {@link SimpleControlGroupCoordinationStrategy} class.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleControlGroupCoordinationStrategyTests {

	private NodeControlInfo group(String controlId, NodeControlPropertyType type, String propertyName) {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId(controlId)
				.withReadonly(Boolean.FALSE)
				.withType(type)
				.withPropertyName(propertyName)
				.build()
				;
		// @formatter:on
	}

	private List<MemberControlConfig> members(String @Nullable... controlIds) {
		if ( controlIds == null ) {
			return new ArrayList<>(0);
		}
		List<MemberControlConfig> result = new ArrayList<>(controlIds.length);
		for ( String controlId : controlIds ) {
			result.add(new MemberControlConfig(controlId));
		}
		return result;
	}

	@Test
	public void distribute_SetControlParameter_Percent() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = 50;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_DemandBalanceGeneration_Percent() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = "50";
		final var req = createLocalInstruction(TOPIC_DEMAND_BALANCE, group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Boolean() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Boolean, "state");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = "0";
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_String() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.String, "msg");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = randomString();
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Float() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "amount");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = 99;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target / 2.0), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Integer() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Integer, "amount");
		final var members = members(randomString(), randomString());

		// WHEN
		final var target = 99;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.hasSize(members.size())
			.as("Contains instructions for each member")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.allSatisfy((controlId, instr) -> {
				then(instr)
					.as("Member instruction is not null")
					.isNotNull()
					.as("Member instruction is SetControlParameter")
					.returns(TOPIC_SET_CONTROL_PARAMETER, from(Instruction::getTopic))
					;
			})
			.satisfies(m -> {
				then(m.get(members.get(0).controlId()))
					.as("Instruction 0 has single parameter for member control ID -> target/2 (rounded down)")
					.returns(String.valueOf(49), from(in -> in.getParameterValue(members.get(0).getControlId())))
					;
				then(m.get(members.get(1).controlId()))
					.as("Instruction 1 has single parameter for member control ID -> target/2 (remaining)")
					.returns(String.valueOf(50), from(in -> in.getParameterValue(members.get(1).getControlId())))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_otherTopic() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "amount");
		final var members = members(randomString(), randomString());

		// WHEN
		final var req = createLocalInstruction("Other", group.getControlId(), "99");
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instruction with unsupported topic not distributed")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Float_threeWay() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "amount");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var target = 100;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId()
					, members.get(2).controlId())
			.satisfies(m -> {
				then(m.get(members.get(0).controlId()))
					.as("Instruction 0 has single parameter for member control ID -> target/3 (rounded down)")
					.returns("33.333333333", from(in -> in.getParameterValue(members.get(0).controlId())))
					;
				then(m.get(members.get(1).controlId()))
					.as("Instruction 1 has single parameter for member control ID -> target/3 (rounded down)")
					.returns("33.333333333", from(in -> in.getParameterValue(members.get(1).controlId())))
					;
				then(m.get(members.get(2).controlId()))
					.as("Instruction 2 has single parameter for member control ID -> target/3 (remaining)")
					.returns("33.333333334", from(in -> in.getParameterValue(members.get(2).controlId())))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Integer_threeWay() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Integer, "amount");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var target = 100;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId()
					, members.get(2).controlId())
			.satisfies(m -> {
				then(m.get(members.get(0).controlId()))
					.as("Instruction 0 has single parameter for member control ID -> target/3 (rounded down)")
					.returns("33", from(in -> in.getParameterValue(members.get(0).controlId())))
					;
				then(m.get(members.get(1).controlId()))
					.as("Instruction 1 has single parameter for member control ID -> target/3 (rounded down)")
					.returns("33", from(in -> in.getParameterValue(members.get(1).controlId())))
					;
				then(m.get(members.get(2).controlId()))
					.as("Instruction 2 has single parameter for member control ID -> target/3 (remaining)")
					.returns("34", from(in -> in.getParameterValue(members.get(2).controlId())))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Integer_fractionalValue() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Integer, "amount");
		final var members = members(randomString(), randomString());

		// WHEN
		final var req = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, group.getControlId(),
				"99.5");
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated for controls based on member configurations.")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(1).controlId())
			.satisfies(m -> {
				then(m.get(members.get(0).controlId()))
					.as("Instruction 0 has single parameter for member control ID -> floor(target)/2 (rounded down)")
					.returns("49", from(in -> in.getParameterValue(members.get(0).controlId())))
					;
				then(m.get(members.get(1).controlId()))
					.as("Instruction 1 has single parameter for member control ID -> floor(target)/2 (remaining, whole number)")
					.returns("50", from(in -> in.getParameterValue(members.get(1).controlId())))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Percent_invalidMembersSkipped() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), null, " ", randomString());

		// WHEN
		final var target = 50;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated only for members with a control ID")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(3).controlId())
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target", e.getKey())
						.returns(String.valueOf(target), from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Float_invalidMembersSkipped() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "amount");
		final var members = members(randomString(), null, " ", randomString());

		// WHEN
		final var target = 100;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("Instructions generated only for members with a control ID")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(3).controlId())
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target/2", e.getKey())
						.returns("50", from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_SetControlParameter_Float_duplicateMembers() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "amount");
		final String duplicateControlId = randomString();
		final var members = members(duplicateControlId, duplicateControlId, randomString());

		// WHEN
		final var target = 90;
		final var req = createSetControlValueLocalInstruction(group.getControlId(), target);
		final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

		// THEN
		// @formatter:off
		then(result)
			.as("One instruction generated per unique member control ID")
			.containsOnlyKeys(
					  members.get(0).controlId()
					, members.get(2).controlId())
			.satisfies(m -> {
				for (Entry<String, Instruction> e : m.entrySet()) {
					then(e.getValue())
						.as("Instruction %s has single parameter for member control ID -> target/2", e.getKey())
						.returns("45", from(in -> in.getParameterValue(e.getKey())))
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void distribute_noValidMembers() {
		for ( NodeControlPropertyType type : NodeControlPropertyType.values() ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "amount");
			final var members = members(null, " ");

			// WHEN
			final var req = createSetControlValueLocalInstruction(group.getControlId(), 1);
			final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

			// THEN
			// @formatter:off
			then(result)
				.as("%s instruction not distributed when no valid members configured", type)
				.isNull()
				;
			// @formatter:on
		}
	}

	@Test
	public void distribute_noValue() {
		for ( NodeControlPropertyType type : NodeControlPropertyType.values() ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "amount");
			final var members = members(randomString(), randomString());

			// WHEN
			final var req = createSetControlValueLocalInstruction(randomString(), 1);
			final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

			// THEN
			// @formatter:off
			then(result)
				.as("%s instruction not distributed when no group value provided", type)
				.isNull()
				;
			// @formatter:on
		}
	}

	@Test
	public void distribute_SetControlParameter_nonNumeric() {
		for ( NodeControlPropertyType type : EnumSet.of(Integer, Float) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "amount");
			final var members = members(randomString(), randomString());

			// WHEN
			final var req = createSetControlValueLocalInstruction(group.getControlId(), "abc");
			final Map<String, Instruction> result = service.distribute(group, members, req, List.of());

			// THEN
			// @formatter:off
			then(result)
				.as("%s instruction not distributed when group value is not a number", type)
				.isNull()
				;
			// @formatter:on
		}
	}

	private List<NodeControlInfo> controlValues(NodeControlPropertyType type, String propertyName,
			List<MemberControlConfig> members, String @Nullable... values) {
		if ( values == null || members.size() != values.length ) {
			return new ArrayList<>(0);
		}
		var result = new ArrayList<NodeControlInfo>(members.size());
		for ( int i = 0; i < values.length; i++ ) {
			// @formatter:off
			var info = BasicNodeControlInfo.builder()
					.withControlId(members.get(i).getControlId())
					.withPropertyName(propertyName)
					.withType(type)
					.withValue(values[i])
					.build()
					;
			// @formatter:on
			result.add(info);
		}
		return result;
	}

	@Test
	public void combine_mode() {
		for ( NodeControlPropertyType type : EnumSet.of(NodeControlPropertyType.Boolean,
				NodeControlPropertyType.Percent, NodeControlPropertyType.String) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString(), randomString());

			// WHEN
			final var memberValues = controlValues(type, "max", members, "1", "22", "22");
			final NodeControlInfo result = service.combine(group, members, memberValues);

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("Combined control type is group type.")
				.returns(group.getType(), from(NodeControlInfo::getType))
				.as("Combined control property name is group property name.")
				.returns(group.getPropertyName(), from(NodeControlInfo::getPropertyName))
				.as("%s control values combined to mode (most frequent) value.", type)
				.returns("22", from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_mode_tie() {
		for ( NodeControlPropertyType type : EnumSet.of(NodeControlPropertyType.Boolean,
				NodeControlPropertyType.String) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString(), randomString(), randomString(),
					randomString());

			// WHEN
			final var memberValues = controlValues(type, "max", members, "1", "3", "3", "22", "22");
			final NodeControlInfo result = service.combine(group, members, memberValues);

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("Combined control type is group type.")
				.returns(group.getType(), from(NodeControlInfo::getType))
				.as("Combined control property name is group property name.")
				.returns(group.getPropertyName(), from(NodeControlInfo::getPropertyName))
				.as("%s control values combined to first mode (most frequent) value.", type)
				.returns("3", from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_mode_tie_highest() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), randomString(), randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(NodeControlPropertyType.Percent, "max", members, "1", "1",
				"2", "2");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("Percent control values combined to highest mode (most frequent) value.")
			.returns("2", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_mode_tie_highest_twoMembers() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(NodeControlPropertyType.Percent, "max", members, "50",
				"100");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Disagreeing Percent control values combined to highest value, compared as numbers.")
			.returns("100", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_mode_tie_highest_notNumber() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), NodeControlPropertyType.Percent, "max");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(NodeControlPropertyType.Percent, "max", members, null,
				"abc", "50");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Number value preferred over missing and non-number values in tie.")
			.returns("50", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_mode_moreThanTwo() {
		for ( NodeControlPropertyType type : EnumSet.of(NodeControlPropertyType.Boolean,
				NodeControlPropertyType.Percent, NodeControlPropertyType.String) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString(), randomString(), randomString(),
					randomString());

			// WHEN
			final var memberValues = controlValues(type, "max", members, "1", "1", "22", "22", "22");
			final NodeControlInfo result = service.combine(group, members, memberValues);

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("%s control values combined to mode (most frequent) value, counting every occurrence.", type)
				.returns("22", from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_sum() {
		for ( NodeControlPropertyType type : EnumSet.of(Integer, Float) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString(), randomString());

			// WHEN
			final var memberValues = controlValues(type, "max", members, "1", "2", "3");
			final NodeControlInfo result = service.combine(group, members, memberValues);

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("Combined control type is group type.")
				.returns(group.getType(), from(NodeControlInfo::getType))
				.as("Combined control property name is group property name.")
				.returns(group.getPropertyName(), from(NodeControlInfo::getPropertyName))
				.as("%s control values combined to sum value.", type)
				.returns("6", from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_sum_roundedInteger() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Integer, "max");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(Integer, "max", members, "1.1", "2.2", "3.3");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("Combined control type is group type.")
			.returns(group.getType(), from(NodeControlInfo::getType))
			.as("Combined control property name is group property name.")
			.returns(group.getPropertyName(), from(NodeControlInfo::getPropertyName))
			.as("%s control values combined to sum value (rounded up).", Integer)
			.returns("7", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_sum_float() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "max");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(Float, "max", members, "1.1", "2.2", "3.3");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("Combined control type is group type.")
			.returns(group.getType(), from(NodeControlInfo::getType))
			.as("Combined control property name is group property name.")
			.returns(group.getPropertyName(), from(NodeControlInfo::getPropertyName))
			.as("%s control values combined to sum value (floating point).", Float)
			.returns("6.6", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_sum_nonNumericIgnored() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "max");
		final var members = members(randomString(), randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(Float, "max", members, "1", "abc", "2");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("%s control values combined to sum value, ignoring non-numeric values.", Float)
			.returns("3", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_sum_noValues() {
		for ( NodeControlPropertyType type : EnumSet.of(Float, Integer) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString());

			// WHEN
			final NodeControlInfo result = service.combine(group, members, List.of());

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("%s control value not available without member values.", type)
				.returns(null, from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_sum_noNumericValues() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		final var group = group(randomString(), Float, "max");
		final var members = members(randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(Float, "max", members, "abc", null);
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("%s control value not available without numeric member values.", Float)
			.returns(null, from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void combine_mode_noValues() {
		for ( NodeControlPropertyType type : EnumSet.of(NodeControlPropertyType.Boolean,
				NodeControlPropertyType.Percent, NodeControlPropertyType.String) ) {
			// GIVEN
			final var service = new SimpleControlGroupCoordinationStrategy();
			final var group = group(randomString(), type, "max");
			final var members = members(randomString(), randomString());

			// WHEN
			final NodeControlInfo result = service.combine(group, members, List.of());

			// THEN
			// @formatter:off
			then(result)
				.as("Combined control ID is group ID")
				.returns(group.getControlId(), from(NodeControlInfo::getControlId))
				.as("%s control value not available without member values.", type)
				.returns(null, from(NodeControlInfo::getValue))
				;
			// @formatter:on
		}
	}

	@Test
	public void combine_groupInfo() {
		// GIVEN
		final var service = new SimpleControlGroupCoordinationStrategy();
		// @formatter:off
		final var group = BasicNodeControlInfo.builder()
				.withControlId(randomString())
				.withReadonly(Boolean.TRUE)
				.withType(Float)
				.withPropertyName("watts")
				.withUnit("W")
				.build()
				;
		// @formatter:on
		final var members = members(randomString(), randomString());

		// WHEN
		final var memberValues = controlValues(Float, "watts", members, "1", "2");
		final NodeControlInfo result = service.combine(group, members, memberValues);

		// THEN
		// @formatter:off
		then(result)
			.as("Combined control ID is group ID")
			.returns(group.getControlId(), from(NodeControlInfo::getControlId))
			.as("Combined control unit is group unit.")
			.returns("W", from(NodeControlInfo::getUnit))
			.as("Combined control read-only flag is group flag.")
			.returns(Boolean.TRUE, from(NodeControlInfo::getReadonly))
			.as("Combined control value is sum of member values.")
			.returns("3", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

}
