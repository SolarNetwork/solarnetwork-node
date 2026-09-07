/* ==================================================================
 * SimpleReactorService.java - Mar 1, 2011 4:46:31 PM
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

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Received;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.util.StringUtils;

/**
 * Simple implementation of {@link ReactorService}.
 *
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public class SimpleReactorService implements ReactorService, InstructionHandler {

	private final InstructionDao instructionDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param instructionDao
	 *        the instruction DAO to use
	 */
	public SimpleReactorService(InstructionDao instructionDao) {
		super();
		this.instructionDao = instructionDao;
	}

	@Override
	public boolean handlesTopic(@Nullable String topic) {
		return TOPIC_CANCEL_INSTRUCTION.equals(topic);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public @Nullable InstructionStatus processInstruction(Instruction instruction) {
		if ( TOPIC_CANCEL_INSTRUCTION.equals(instruction.getTopic()) ) {
			return handleCancelInstruction(instruction);
		}

		// look for an existing status...
		Instruction instr = instructionDao.getInstruction(instruction.getId(),
				instruction.getInstructorId());
		if ( instr == null ) {
			// persist new status
			instructionDao.storeInstruction(instruction);
			instr = instructionDao.getInstruction(instruction.getId(), instruction.getInstructorId());
		}
		return (instr != null ? instr.getStatus() : null);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInstruction(Instruction instruction) {
		if ( instruction.getStatus() != null ) {
			// see if storing with status
			Instruction existing = instructionDao.getInstruction(instruction.getId(),
					instruction.getInstructorId());
			if ( existing != null ) {
				instructionDao.storeInstructionStatus(instruction.getId(), instruction.getInstructorId(),
						instruction.getStatus());
				return;
			}
		}
		instructionDao.storeInstruction(instruction);
	}

	private static InstructionState errorResultState(boolean ignoreErrors) {
		return (ignoreErrors ? Completed : Declined);
	}

	private InstructionStatus handleCancelInstruction(Instruction instruction) {
		final boolean ignoreErrors = StringUtils
				.parseBoolean(instruction.getParameterValue(PARAM_IGNORE_ERRORS));
		final boolean force = StringUtils.parseBoolean(instruction.getParameterValue(PARAM_FORCE));
		final String instructionIdVal = instruction.getParameterValue(PARAM_ID);
		if ( instructionIdVal == null ) {
			return createStatus(instruction, errorResultState(ignoreErrors),
					createErrorResultParameters("Missing 'id' parameter.", "SRS.0001"));
		}
		final Long instructionId;
		try {
			instructionId = Long.valueOf(instructionIdVal);
		} catch ( NumberFormatException e ) {
			return createStatus(instruction, errorResultState(ignoreErrors),
					createErrorResultParameters("Invalid 'id' parameter (not a number).", "SRS.0002"));
		}
		boolean updated = false;
		try {
			final Instruction instr = instructionDao.getInstruction(instructionId,
					instruction.getInstructorId());
			if ( instr == null ) {
				return createStatus(instruction, errorResultState(ignoreErrors),
						createErrorResultParameters("Instruction with given 'id' not found.",
								"SRS.0003"));
			}
			updated = instructionDao
					.compareAndStoreInstructionStatus(instructionId, instruction.getInstructorId(),
							Received,
							createStatus(instruction, errorResultState(ignoreErrors),
									createErrorResultParameters(
											String.format("Instruction cancelled by %s Instruction %d",
													instruction.getTopic(), instruction.getId()),
											null)));
			if ( !updated ) {
				return createStatus(instruction, errorResultState(ignoreErrors),
						createErrorResultParameters("Instruction failed to update state.", "SRS.0004"));
			}
			log.info("Cancelled instruction {} because of {} instruction {}", instructionId,
					instruction.getTopic(), instruction.getId());
		} finally {
			if ( updated || force ) {
				// also cancel any available children
				cancelChildInstructions(instruction, instructionId);
			}
		}
		return createStatus(instruction, Completed);
	}

	private void cancelChildInstructions(final Instruction instruction, final Long parentInstructionId) {
		List<Instruction> children = instructionDao.findInstructionsForStateAndParent(Received,
				instruction.getInstructorId(), parentInstructionId);
		for ( Instruction child : children ) {
			boolean updated = instructionDao.compareAndStoreInstructionStatus(child.getId(),
					child.getInstructorId(), Received,
					createStatus(child, Declined, createErrorResultParameters(String.format(
							"Instruction cancelled by %s instruction %d on parent instruction %d",
							instruction.getTopic(), instruction.getId(), parentInstructionId), null)));
			if ( updated ) {
				log.info("Cancelled instruction {} because of {} on parent instruction {}",
						child.getId(), instruction.getTopic(), parentInstructionId);
			}
		}
	}

}
