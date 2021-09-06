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

package net.solarnetwork.node.reactor.simple;

import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;

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
 * @author matt
 * @version 1.0
 * @since 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class InstructionExecutionJob extends AbstractJob {

	private InstructionDao instructionDao;
	private InstructionExecutionService service;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		List<Instruction> instructions = instructionDao
				.findInstructionsForState(InstructionState.Received);
		log.debug("Found {} instructions in Received state", instructions.size());
		for ( Instruction instruction : instructions ) {
			InstructionStatus receivedStatus = instruction.getStatus();

			final InstructionStatus execStatus = receivedStatus
					.newCopyWithState(InstructionStatus.InstructionState.Executing);
			InstructionStatus status = null;
			boolean canExecute = false;
			try {
				// update state to Executing
				canExecute = instructionDao.compareAndStoreInstructionStatus(instruction.getId(),
						instruction.getInstructorId(), InstructionState.Received, execStatus);
				if ( canExecute ) {
					status = service.executeInstruction(instruction);
				}
			} catch ( Exception e ) {
				log.error("Execution of instruction {} {} threw exception", instruction.getId(),
						instruction.getTopic(), e);
			} finally {
				if ( status == null ) {
					// roll back to received status to try again later
					status = receivedStatus;
				}
				if ( instructionDao.compareAndStoreInstructionStatus(instruction.getId(),
						instruction.getInstructorId(), InstructionState.Executing, status) ) {
					if ( log.isInfoEnabled()
							&& status.getInstructionState() != InstructionState.Received ) {
						log.info("Instruction {} {}  status changed to {}", instruction.getId(),
								instruction.getTopic(), status.getInstructionState());
					}
				}
			}
		}
	}

	/**
	 * Get the instruction DAO.
	 * 
	 * @return the instruction DAO
	 */
	public InstructionDao getInstructionDao() {
		return instructionDao;
	}

	/**
	 * Set the {@link InstructionDao} to manage {@link Instruction} entities
	 * with.
	 * 
	 * @param instructionDao
	 *        the DAO to use
	 */
	public void setInstructionDao(InstructionDao instructionDao) {
		this.instructionDao = instructionDao;
	}

	/**
	 * Set the instruction execution service to use.
	 * 
	 * @param service
	 *        the service
	 * @since 2.3
	 */
	public void setInstructionExecutionService(InstructionExecutionService service) {
		if ( service == null ) {
			throw new IllegalArgumentException("InstructionExecutionService may not be null");
		}
		this.service = service;
	}

}
