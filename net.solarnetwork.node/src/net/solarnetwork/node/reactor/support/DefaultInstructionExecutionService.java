/* ==================================================================
 * DefaultInstructionExecutionService.java - 8/06/2018 7:24:46 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * Default implementation of {@link InstructionExecutionService}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.58
 */
public class DefaultInstructionExecutionService implements InstructionExecutionService {

	/** Default value for the {@code executionReceivedHourLimit} property. */
	public static final int DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT = 24;

	private List<InstructionHandler> handlers = Collections.emptyList();
	private List<FeedbackInstructionHandler> feedbackHandlers = Collections.emptyList();
	private int executionReceivedHourLimit = DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public synchronized InstructionStatus executeInstruction(Instruction instruction) {
		if ( instruction == null ) {
			return null;
		}
		final long now = System.currentTimeMillis();
		final long timeLimitMs = executionReceivedHourLimit * 60 * 60 * 1000;
		final InstructionStatus startingStatus = instruction.getStatus();
		final InstructionState startingState = (startingStatus != null
				? startingStatus.getInstructionState()
				: InstructionState.Received);

		List<InstructionHandler> allHandlers = new ArrayList<InstructionHandler>(handlers);
		allHandlers.addAll(feedbackHandlers);
		if ( allHandlers.isEmpty() ) {
			log.trace("No InstructionHandler instances available");
			return null;
		}
		final String topic = instruction.getTopic();
		log.trace("Passing instruction {} {} to {} handlers",
				new Object[] { instruction.getId(), topic, allHandlers.size() });
		for ( InstructionHandler handler : allHandlers ) {
			log.trace("Handler {} handles topic {}: {}",
					new Object[] { handler, topic, handler.handlesTopic(topic) });
			if ( handler.handlesTopic(topic) ) {
				try {
					if ( handler instanceof FeedbackInstructionHandler ) {
						InstructionStatus status = ((FeedbackInstructionHandler) handler)
								.processInstructionWithFeedback(instruction);
						if ( status != null
								&& (startingStatus == null || !startingStatus.equals(status)) ) {
							log.info("Instruction {} {} state changed to {}", instruction.getId(), topic,
									status.getInstructionState());
							return status;
						}
					} else {
						InstructionState state = handler.processInstruction(instruction);
						if ( state != null && !startingState.equals(state) ) {
							log.info("Instruction {} {} state changed to {}", instruction.getId(), topic,
									state);
							return (startingStatus != null ? startingStatus.newCopyWithState(state)
									: new BasicInstructionStatus(instruction.getId(), state,
											new Date()));
						}
					}
				} catch ( Exception e ) {
					log.error("Handler {} threw exception processing instruction {} ({})", handler,
							instruction.getId(), topic, e);
				}
			}
		}
		if ( instruction.getInstructionDate() != null ) {
			long diffMs = now - instruction.getInstructionDate().getTime();
			if ( diffMs > timeLimitMs ) {
				log.info("Instruction {} {} not handled within {} hours; declining", instruction.getId(),
						topic, executionReceivedHourLimit);
				return (startingStatus != null
						? startingStatus.newCopyWithState(InstructionState.Declined)
						: new BasicInstructionStatus(instruction.getId(), InstructionState.Declined,
								new Date()));
			}
		}

		// not handled
		return null;
	}

	/**
	 * Set a list of {@link InstructionHandler} instances to process
	 * instructions with.
	 * 
	 * <p>
	 * Note that {@link FeedbackInstructionHandler} instances <em>are</em>
	 * allowed to be configured here, and will be treated as such.
	 * </p>
	 * 
	 * @param handlers
	 *        the handlers
	 */
	public void setHandlers(List<InstructionHandler> handlers) {
		this.handlers = (handlers != null ? handlers : Collections.<InstructionHandler> emptyList());
	}

	/**
	 * Set a list of {@link FeedbackInstructionHandler} instances to process
	 * instructions with.
	 * 
	 * @param feedbackHandlers
	 *        the feedbackHandlers to set
	 */
	public void setFeedbackHandlers(List<FeedbackInstructionHandler> feedbackHandlers) {
		this.feedbackHandlers = (feedbackHandlers != null ? feedbackHandlers
				: Collections.<FeedbackInstructionHandler> emptyList());
	}

	/**
	 * Set the minimum amount of time to wait before forcing instructions into
	 * the {@link InstructionState#Declined} state. This prevents instructions
	 * not handled by any handler from sticking around on the node indefinitely.
	 * Defaults to {@link #DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT}.
	 * 
	 * @param executionReceivedHourLimit
	 *        the hour limit
	 */
	public void setExecutionReceivedHourLimit(int executionReceivedHourLimit) {
		this.executionReceivedHourLimit = executionReceivedHourLimit;
	}

}
