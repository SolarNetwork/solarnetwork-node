/* ==================================================================
 * SimpleInstructionExecutionService.java - 8/06/2018 7:24:46 AM
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

package net.solarnetwork.node.reactor;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Default implementation of {@link InstructionExecutionService}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SimpleInstructionExecutionService implements InstructionExecutionService {

	/** Default value for the {@code executionReceivedHourLimit} property. */
	public static final int DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT = 24;

	private final List<InstructionHandler> handlers;
	private int executionReceivedHourLimit = DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param handlers
	 *        the handlers
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleInstructionExecutionService(List<InstructionHandler> handlers) {
		super();
		if ( handlers == null ) {
			throw new IllegalArgumentException("The handlers argument must not be null.");
		}
		this.handlers = handlers;
	}

	@Override
	public synchronized InstructionStatus executeInstruction(Instruction instruction) {
		if ( instruction == null ) {
			return null;
		}
		final long now = System.currentTimeMillis();
		final long timeLimitMs = executionReceivedHourLimit * 60 * 60 * 1000;
		final InstructionStatus startingStatus = instruction.getStatus();

		if ( handlers.isEmpty() ) {
			log.trace("No InstructionHandler instances available");
			return null;
		}
		final String topic = instruction.getTopic();
		log.trace("Passing instruction {} [{}] to handlers", instruction.getIdentifier(), topic);
		for ( InstructionHandler handler : handlers ) {
			if ( !handler.handlesTopic(topic) ) {
				continue;
			}
			try {
				InstructionStatus status = handler.processInstruction(instruction);
				if ( status != null && (startingStatus == null || !startingStatus.equals(status)) ) {
					log.info("Instruction {} [{}] state changed to {}", instruction.getIdentifier(),
							topic, status.getInstructionState());
					return status;
				}
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				String msg = String.format("Error handling instruction %s [%s] in handler %s: %s",
						instruction.getIdentifier(), topic, handler, root.getMessage());
				log.error(msg, e);
				throw new RuntimeException(msg, e);
			}
		}
		if ( instruction.getInstructionDate() != null ) {
			long diffMs = now - instruction.getInstructionDate().toEpochMilli();
			if ( diffMs > timeLimitMs ) {
				log.info("Instruction {} [{}] not handled within {} hours; declining",
						instruction.getIdentifier(), topic, executionReceivedHourLimit);
				return InstructionUtils.createStatus(instruction, InstructionState.Declined);
			}
		}

		// not handled
		return null;
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
