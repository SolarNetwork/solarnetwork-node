/* ==================================================================
 * InstructionAcknowledgeJob.java - Sep 30, 2011 9:15:19 PM
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

import java.io.IOException;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.setup.SetupException;

/**
 * Job to look for instructions to update the acknowledgment status for.
 * 
 * @author matt
 * @version 3.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class InstructionAcknowledgeJob extends AbstractJob {

	private InstructionDao instructionDao;
	private InstructionAcknowledgementService instructionAcknowledgementService;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		try {
			List<Instruction> instructions = instructionDao.findInstructionsForAcknowledgement();
			if ( instructions.size() > 0 ) {
				instructionAcknowledgementService.acknowledgeInstructions(instructions);
				for ( Instruction instruction : instructions ) {
					instructionDao.storeInstructionStatus(instruction.getId(),
							instruction.getInstructorId(),
							instruction.getStatus().newCopyWithAcknowledgedState(
									instruction.getStatus().getInstructionState()));
				}
			}
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				if ( log.isWarnEnabled() ) {
					log.warn("Network problem posting instruction acknowledgement ({}): {}",
							root.getClass().getSimpleName(), root.getMessage());
				}
			} else if ( root instanceof SetupException ) {
				log.warn("Unable to post instruction acknowledgement: {}", root.getMessage());
			} else {
				if ( log.isErrorEnabled() ) {
					log.error("Exception posting instruction acknowledgement", root);
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

	public InstructionAcknowledgementService getInstructionAcknowledgementService() {
		return instructionAcknowledgementService;
	}

	public void setInstructionAcknowledgementService(
			InstructionAcknowledgementService instructionAcknowledgementService) {
		this.instructionAcknowledgementService = instructionAcknowledgementService;
	}

}
