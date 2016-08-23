/* ==================================================================
 * InstructionExecutionJob.java - Oct 1, 2011 8:11:50 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * Job to look for received instructions and pass to handlers for execution.
 * 
 * <p>
 * This job uses the {@link InstructionDao} to query for all {@link Instruction}
 * entities with a state of {@link InstructionState#Received}. For each
 * {@link Instruction} found, it will attempt to find an
 * {@link InstructionHandler} that can execute the instruction.
 * </p>
 * 
 * <p>
 * This job will pass {@link Instruction#getTopic()} to every configured
 * {@link InstructionHandler#handlesTopic(String)} and if that returns
 * <em>true</em> the handler will be given the opportunity to handle the
 * instruction via {@link InstructionHandler#processInstruction(Instruction)}.
 * If the handler returns <em>null</em> or {@link InstructionState#Received}
 * (because it does not support the given Instruction) this job will move on to
 * the next configured handler and try it.
 * </p>
 * 
 * <p>
 * The first handler to return an {@link InstructionState} other than
 * {@link InstructionState#Received} will cause this job to persist the updated
 * Instruction state and not pass the instruction to any other handlers.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>instructionDao</dt>
 * <dd>The {@link InstructionDao} to manage {@link Instruction} entities
 * with.</dd>
 * 
 * <dt>executionReceivedHourLimit</dt>
 * <dd>The minimum amount of time to wait before forcing instructions into the
 * {@link InstructionState#Declined} state. This prevents instructions not
 * handled by any handler from sticking around on the node indefinitely.
 * Defaults to {@link #DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT}.</dd>
 * 
 * <dt>handlers</dt>u
 * <dd>List of {@link InstructionHandler} instances
 * </dl>
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class InstructionExecutionJob extends AbstractJob {

	/** Default value for the {@code executionReceivedHourLimit} property. */
	public static final int DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT = 24;

	private InstructionDao instructionDao;
	private List<InstructionHandler> handlers;
	private int executionReceivedHourLimit = DEFAULT_EXECUTION_RECEIVED_HOUR_LIMIT;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		List<Instruction> instructions = instructionDao
				.findInstructionsForState(InstructionState.Received);
		log.debug("Found {} instructions in Received state", instructions.size());
		final long now = System.currentTimeMillis();
		final long timeLimitMs = executionReceivedHourLimit * 60 * 60 * 1000;
		for ( Instruction instruction : instructions ) {
			boolean handled = false;
			log.trace("Passing instruction {} {} to {} handlers",
					new Object[] { instruction.getId(), instruction.getTopic(), handlers.size() });
			for ( InstructionHandler handler : handlers ) {
				log.trace("Handler {} handles topic {}: {}", new Object[] { handler,
						instruction.getTopic(), handler.handlesTopic(instruction.getTopic()) });
				if ( handler.handlesTopic(instruction.getTopic()) ) {
					InstructionState state = handler.processInstruction(instruction);
					if ( state != null && !InstructionState.Received.equals(state) ) {
						log.info("Instruction {} state changed to {}", instruction.getId(), state);
						instructionDao.storeInstructionStatus(instruction.getId(),
								instruction.getStatus().newCopyWithState(state));
						handled = true;
					}
				}
			}
			if ( !handled && instruction.getInstructionDate() != null ) {
				long diffMs = now - instruction.getInstructionDate().getTime();
				if ( diffMs > timeLimitMs ) {
					log.info("Instruction {} not handled within {} hours; declining",
							instruction.getId(), executionReceivedHourLimit);
					instructionDao.storeInstructionStatus(instruction.getId(),
							instruction.getStatus().newCopyWithState(InstructionState.Declined));
				}
			}
		}
	}

	public InstructionDao getInstructionDao() {
		return instructionDao;
	}

	public void setInstructionDao(InstructionDao instructionDao) {
		this.instructionDao = instructionDao;
	}

	public List<InstructionHandler> getHandlers() {
		return handlers;
	}

	public void setHandlers(List<InstructionHandler> handlers) {
		this.handlers = handlers;
	}

	public int getExecutionReceivedHourLimit() {
		return executionReceivedHourLimit;
	}

	public void setExecutionReceivedHourLimit(int executionReceivedHourLimit) {
		this.executionReceivedHourLimit = executionReceivedHourLimit;
	}

}
