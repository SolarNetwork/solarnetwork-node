/* ==================================================================
 * AllOrNothingControlGroupConsensusStrategy.java - 5 Nov 2026 3:27:12 pm
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

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * Consensus strategy that implements an all-or-nothing success result.
 *
 * <p>
 * The group result is {@code Completed} only if every member result is
 * {@code Completed}. Otherwise the group result is {@code Declined}, with a
 * message that describes every member that was not handled or not completed,
 * including the message from the member's own result, if it has one.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class AllOrNothingControlGroupConsensusStrategy implements ControlGroupConsensusStrategy {

	/** A default instance. */
	public static final ControlGroupConsensusStrategy INSTANCE = new AllOrNothingControlGroupConsensusStrategy();

	private static final MessageSource MESSAGE_SOURCE;
	static {
		var msg = new ResourceBundleMessageSource();
		msg.setBasename(AllOrNothingControlGroupConsensusStrategy.class.getName());
		msg.setBundleClassLoader(AllOrNothingControlGroupConsensusStrategy.class.getClassLoader());
		MESSAGE_SOURCE = msg;
	}

	/**
	 * Constructor.
	 */
	public AllOrNothingControlGroupConsensusStrategy() {
		super();
	}

	@Override
	public @Nullable InstructionStatus evaluate(NodeControlInfo group,
			List<MemberControlConfig> memberConfigs, Instruction request,
			Map<String, InstructionExecution> results) {
		final Map<String, String> updateValues = new LinkedHashMap<>(results.size());
		final List<String> failures = new ArrayList<>(results.size());
		for ( Entry<String, InstructionExecution> e : results.entrySet() ) {
			final String memberControlId = e.getKey();
			final Instruction instr = e.getValue().instruction();
			final String memberVal = instr.getParameterValue(memberControlId);
			final InstructionStatus status = e.getValue().result();
			if ( status == null ) {
				failures.add(MESSAGE_SOURCE.getMessage("error.update.notHandled",
						new Object[] { memberControlId, memberVal != null ? memberVal : "?" },
						"Update was not processed.", Locale.getDefault()));
			} else if ( status.getInstructionState() != Completed ) {
				final String cause = resultMessage(status);
				failures.add(MESSAGE_SOURCE.getMessage(
						cause != null ? "error.update.notCompletedWithCause"
								: "error.update.notCompleted",
						new Object[] { memberControlId, memberVal != null ? memberVal : "?",
								status.getInstructionState(), cause != null ? cause : "" },
						"Update was not completed.", Locale.getDefault()));
			} else if ( memberVal != null ) {
				updateValues.put(memberControlId, memberVal);
			}
		}
		if ( !failures.isEmpty() ) {
			return createStatus(request, Declined,
					createErrorResultParameters(String.join(" ", failures), null));
		}
		return createStatus(request, Completed, Map.of("updates", updateValues));
	}

	/**
	 * Get the message result parameter from a status.
	 *
	 * @param status
	 *        the status
	 * @return the message, or {@code null} if not available or blank
	 */
	private static @Nullable String resultMessage(InstructionStatus status) {
		final Map<String, ?> params = status.getResultParameters();
		if ( params == null ) {
			return null;
		}
		final Object msg = params.get(InstructionStatus.MESSAGE_RESULT_PARAM);
		if ( msg == null ) {
			return null;
		}
		final String result = msg.toString();
		return (result.isBlank() ? null : result);
	}

}
