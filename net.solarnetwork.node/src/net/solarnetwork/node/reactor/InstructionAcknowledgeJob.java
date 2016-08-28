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

package net.solarnetwork.node.reactor;

import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import net.solarnetwork.node.job.AbstractJob;

/**
 * Job to look for instructions to update the acknowledgment status for.
 * 
 * @author matt
 * @version 2.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class InstructionAcknowledgeJob extends AbstractJob {

	private InstructionDao instructionDao;
	private InstructionAcknowledgementService instructionAcknowledgementService;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		List<Instruction> instructions = instructionDao.findInstructionsForAcknowledgement();
		if ( instructions.size() > 0 ) {
			instructionAcknowledgementService.acknowledgeInstructions(instructions);
			for ( Instruction instruction : instructions ) {
				instructionDao.storeInstructionStatus(instruction.getId(), instruction.getStatus()
						.newCopyWithAcknowledgedState(instruction.getStatus().getInstructionState()));
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
