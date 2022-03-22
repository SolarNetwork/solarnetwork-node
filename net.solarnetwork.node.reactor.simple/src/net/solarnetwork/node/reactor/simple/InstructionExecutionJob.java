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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;

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
 * {@literal true} the handler will be given the opportunity to handle the
 * instruction via {@link InstructionHandler#processInstruction(Instruction)}.
 * If the handler returns {@literal null} or {@link InstructionState#Received}
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
 * @version 1.1
 * @since 2.0
 */
public class InstructionExecutionJob extends BaseIdentifiable implements JobService {

	/**
	 * An instruction result error code when an instruction is declined from
	 * expiring due to the {@code maximumIncompleteHours} setting.
	 * 
	 * @since 1.1
	 */
	public static final String ERROR_CODE_INSTRUCTION_EXPIRED = "IEXJ.001";

	/**
	 * The {@code maximumIncompleteHours} property default value.
	 * 
	 * @since 1.1
	 */
	public static final int DEFAULT_MAXIMUM_INCOMPLETE_HOURS = 168;

	private final InstructionDao instructionDao;
	private final InstructionExecutionService service;
	private int maximumIncompleteHours = DEFAULT_MAXIMUM_INCOMPLETE_HOURS;

	/**
	 * Constructor.
	 * 
	 * @param instructionDao
	 *        the instruction DAO
	 * @param service
	 *        the service
	 */
	public InstructionExecutionJob(InstructionDao instructionDao, InstructionExecutionService service) {
		super();
		this.instructionDao = requireNonNullArgument(instructionDao, "instructionDao");
		this.service = requireNonNullArgument(service, "service");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.reactor.simple";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
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
				if ( isExpired(instruction, status) ) {
					status = expiredStatus(instruction);
				}
				if ( instructionDao.compareAndStoreInstructionStatus(instruction.getId(),
						instruction.getInstructorId(), InstructionState.Executing, status) ) {
					if ( log.isInfoEnabled()
							&& status.getInstructionState() != InstructionState.Received ) {
						log.info("Instruction {} {} status changed to {}", instruction.getId(),
								instruction.getTopic(), status.getInstructionState());
					}
				}
			}
		}
		expireExecutingInstructions();
	}

	private boolean isExpired(Instruction instruction, InstructionStatus status) {
		if ( maximumIncompleteHours > 0
				&& (status.getInstructionState() == InstructionState.Received
						|| status.getInstructionState() == InstructionState.Executing)
				&& ChronoUnit.HOURS.between(instruction.getInstructionDate(),
						Instant.now()) > maximumIncompleteHours ) {
			return true;
		}
		return false;
	}

	private InstructionStatus expiredStatus(Instruction instruction) {
		Map<String, Object> resultParams = new HashMap<>(2);
		resultParams.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, ERROR_CODE_INSTRUCTION_EXPIRED);
		resultParams.put(InstructionStatus.MESSAGE_RESULT_PARAM,
				String.format("Declining because unhandled within %d hours", maximumIncompleteHours));
		return InstructionUtils.createStatus(instruction, InstructionState.Declined, resultParams);
	}

	private void expireExecutingInstructions() {
		List<Instruction> instructions = instructionDao
				.findInstructionsForState(InstructionState.Executing);
		log.debug("Found {} instructions in Executing state", instructions.size());
		for ( Instruction instruction : instructions ) {
			if ( isExpired(instruction, instruction.getStatus()) ) {
				InstructionStatus declined = expiredStatus(instruction);
				if ( instructionDao.compareAndStoreInstructionStatus(instruction.getId(),
						instruction.getInstructorId(), InstructionState.Executing, declined) ) {
					if ( log.isInfoEnabled() ) {
						log.info("Instruction {} {} expired after {} hours to {}", instruction.getId(),
								instruction.getTopic(), maximumIncompleteHours,
								declined.getInstructionState());
					}
				}
			}
		}
	}

	/**
	 * Get the maximum number of hours before incomplete instructions can be
	 * declined.
	 * 
	 * @return the maximum hours; defaults to
	 *         {@link #DEFAULT_MAXIMUM_INCOMPLETE_HOURS}
	 */
	public int getMaximumIncompleteHours() {
		return maximumIncompleteHours;
	}

	/**
	 * Set the maximum number of hours before incomplete instructions can be
	 * declined.
	 * 
	 * @param maximumIncompleteHours
	 *        the hours to set; if less than {@literal 1} then instructions will
	 *        not expire
	 */
	public void setMaximumIncompleteHours(int maximumIncompleteHours) {
		this.maximumIncompleteHours = maximumIncompleteHours;
	}

}
