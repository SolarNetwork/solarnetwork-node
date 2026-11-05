/* ==================================================================
 * SimpleControlGroupCoordinationStrategy.java - 4 Nov 2026 6:03:19 pm
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

package net.solarnetwork.node.control.group;

import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_DEMAND_BALANCE;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.StringUtils.numberValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.util.NumberUtils;

/**
 * A basic implementation of {@link ControlGroupCoordinationStrategy} that
 * handles {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} and
 * {@link InstructionHandler#TOPIC_DEMAND_BALANCE} instructions.
 *
 * <p>
 * This strategy implements the following logic: for <b>Float</b> and
 * <b>Integer</b> control types the group control value is treated as a sum
 * total and each member receives an equal portion of that total. Some small
 * variation in the portion size can occur due to rounding. For <b>Integer</b>
 * the total is first rounded down to a whole number, so that every portion is a
 * whole number. For all other types the group value is passed through to the
 * members as-is.
 * </p>
 *
 * <p>
 * When combining member values into a group value, <b>Float</b> and
 * <b>Integer</b> member values are summed. For all other types the most common
 * member value is used. If more than one value is the most common, the highest
 * of those values is used for <b>Percent</b>, and the first one otherwise.
 * </p>
 *
 * <p>
 * A request is not handled, so {@code distribute()} returns {@code null}, if it
 * does not provide a group value, the value is not a number for the
 * <b>Float</b> and <b>Integer</b> types, or no valid members are configured.
 * </p>
 *
 *
 * @author matt
 * @version 1.0
 */
public class SimpleControlGroupCoordinationStrategy extends BaseIdentifiable
		implements ControlGroupCoordinationStrategy {

	/** A default instance. */
	public static final ControlGroupCoordinationStrategy INSTANCE = new SimpleControlGroupCoordinationStrategy();

	/**
	 * Constructor.
	 */
	public SimpleControlGroupCoordinationStrategy() {
		super();
	}

	@Override
	public NodeControlInfo combine(NodeControlInfo group, List<MemberControlConfig> memberConfigs,
			Collection<NodeControlInfo> values) {
		final var type = nonnull(group.getType(), "Control type");
		return switch (type) {
			case Boolean -> combineMode(type, group, memberConfigs, values);
			case Float, Integer -> combineSum(type, group, memberConfigs, values);
			case Percent -> combineMode(type, group, memberConfigs, values);
			case String -> combineMode(type, group, memberConfigs, values);
		};
	}

	private NodeControlInfo combineMode(NodeControlPropertyType type, NodeControlInfo group,
			List<MemberControlConfig> memberConfigs, Collection<NodeControlInfo> values) {
		// find the mode (most popular value); in the case of a tie the highest of those values is
		// selected for number types, otherwise the first one of those values is selected
		final boolean number = (type == NodeControlPropertyType.Float
				|| type == NodeControlPropertyType.Integer || type == NodeControlPropertyType.Percent);
		Map<String, Integer> countsByValue = new LinkedHashMap<>(values.size());
		for ( NodeControlInfo memberValue : values ) {
			String val = memberValue.getValue();
			countsByValue.merge(val, 1, Integer::sum);
		}
		Integer maxCount = null;
		String result = null;
		for ( Entry<String, Integer> e : countsByValue.entrySet() ) {
			// non-number values sort before numbers, so a number wins a tie against them
			if ( maxCount == null || e.getValue() > maxCount || (number && e.getValue().equals(maxCount)
					&& NumberUtils.compare(numberValue(e.getKey()), numberValue(result)) > 0) ) {
				result = e.getKey();
				maxCount = e.getValue();
			}
		}

		return groupValue(group, result);
	}

	private NodeControlInfo combineSum(NodeControlPropertyType type, NodeControlInfo group,
			List<MemberControlConfig> memberConfigs, Collection<NodeControlInfo> values) {
		BigDecimal total = BigDecimal.ZERO;
		for ( NodeControlInfo memberValue : values ) {
			final BigDecimal val = bigDecimalForNumber(numberValue(memberValue.getValue()));
			if ( val != null ) {
				total = total.add(val);
			}
		}
		if ( type == NodeControlPropertyType.Integer ) {
			total = total.setScale(0, RoundingMode.UP);
		}
		return groupValue(group, total.stripTrailingZeros().toPlainString());
	}

	private NodeControlInfo groupValue(NodeControlInfo group, @Nullable String value) {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId(group.getControlId())
				.withPropertyName(group.getPropertyName())
				.withReadonly(group.getReadonly())
				.withType(group.getType())
				.withUnit(group.getUnit())
				.withValue(value)
				.build()
				;
		// @formatter:on
	}

	@Override
	public @Nullable Map<String, Instruction> distribute(NodeControlInfo group,
			List<MemberControlConfig> memberConfigs, Instruction request,
			Collection<NodeControlProvider> controls) {
		final String topic = nonnull(request.getTopic(), "Instruction topic");
		if ( !(TOPIC_SET_CONTROL_PARAMETER.equals(topic) || TOPIC_DEMAND_BALANCE.equals(topic)) ) {
			return null;
		}
		final NodeControlPropertyType type = nonnull(group.getType(), "Control type");
		return switch (type) {
			case Boolean -> distributePassthrough(group, memberConfigs, request, controls);
			case Float, Integer -> distributeSum(type, group, memberConfigs, request, controls);
			case Percent -> distributePassthrough(group, memberConfigs, request, controls);
			case String -> distributePassthrough(group, memberConfigs, request, controls);
		};
	}

	/**
	 * Distribute a single value across all members.
	 *
	 * @param memberConfigs
	 *        the members to distribute to
	 * @param val
	 *        the value to distribute
	 * @return the resulting instructions, or {@code null} if {@code val} is
	 *         {@code null} or there are no valid members
	 */
	private @Nullable Map<String, Instruction> distributeValue(List<MemberControlConfig> memberConfigs,
			@Nullable String val) {
		if ( val == null ) {
			// no value provided
			return null;
		}
		final Map<String, Instruction> result = new LinkedHashMap<>(memberConfigs.size());
		for ( MemberControlConfig memberConfig : memberConfigs ) {
			if ( !memberConfig.isValid() ) {
				continue;
			}
			final String controlId = memberConfig.controlId();
			result.put(controlId, createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, controlId, val));
		}
		if ( result.isEmpty() ) {
			// no members to distribute to
			return null;
		}
		return result;
	}

	private @Nullable Map<String, Instruction> distributePassthrough(NodeControlInfo group,
			List<MemberControlConfig> memberConfigs, Instruction request,
			Collection<NodeControlProvider> controls) {
		// every member gets same value as group
		final String val = request.getParameterValue(nonnull(group.getControlId(), "Group control ID"));
		return distributeValue(memberConfigs, val);
	}

	private @Nullable Map<String, Instruction> distributeSum(NodeControlPropertyType type,
			NodeControlInfo group, List<MemberControlConfig> memberConfigs, Instruction request,
			Collection<NodeControlProvider> controls) {
		// every member gets the equal fraction of group sum
		final BigDecimal value = bigDecimalForNumber(numberValue(
				request.getParameterValue(nonnull(group.getControlId(), "Group control ID"))));
		if ( value == null ) {
			// no sum provided, or not a number
			return null;
		}

		// for Integer type round down to a whole number, so the allocations are whole numbers
		// that do not add up to more than the requested value
		final BigDecimal sum = (type == NodeControlPropertyType.Integer
				? value.setScale(0, RoundingMode.DOWN)
				: value);
		final Map<String, Instruction> result = new LinkedHashMap<>(memberConfigs.size());
		// a set so duplicate members only get one allocation
		final Set<String> controlIds = new LinkedHashSet<>(memberConfigs.size());
		for ( MemberControlConfig memberConfig : memberConfigs ) {
			final String controlId = memberConfig.getControlId();
			if ( controlId == null || controlId.isBlank() ) {
				continue;
			}
			controlIds.add(controlId);
		}

		final int memberCount = controlIds.size();
		if ( memberCount < 1 ) {
			// no members to distribute to
			return null;
		}

		// for Integer type round allocations to whole integers
		final int scale = (type == NodeControlPropertyType.Integer ? 0
				: sum.scale() > 0 ? sum.scale() : 9);

		final BigDecimal divisor = new BigDecimal(memberCount);
		BigDecimal remaining = sum;
		int i = 0;
		for ( String controlId : controlIds ) {
			i++;
			var allocation = (i == memberCount ? remaining
					: sum.divide(divisor, scale, RoundingMode.DOWN)).stripTrailingZeros();
			remaining = remaining.subtract(allocation);
			result.put(controlId, createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, controlId,
					allocation.toPlainString()));
		}

		return result;
	}

}
