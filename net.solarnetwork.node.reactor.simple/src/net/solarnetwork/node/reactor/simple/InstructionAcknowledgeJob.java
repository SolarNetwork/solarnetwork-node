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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Job to look for instructions to update the acknowledgment status for.
 * 
 * @author matt
 * @version 3.0
 */
public class InstructionAcknowledgeJob extends BaseIdentifiable implements JobService {

	private final InstructionDao instructionDao;
	private final InstructionAcknowledgementService instructionAcknowledgementService;

	/**
	 * Constructor.
	 * 
	 * @param instructionDao
	 *        the instruction DAO
	 * @param instructionAcknowledgementService
	 *        the acknowledgement service
	 */
	public InstructionAcknowledgeJob(InstructionDao instructionDao,
			InstructionAcknowledgementService instructionAcknowledgementService) {
		super();
		this.instructionDao = requireNonNullArgument(instructionDao, "instructionDao");
		this.instructionAcknowledgementService = requireNonNullArgument(
				instructionAcknowledgementService, "instructionAcknowledgementService");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.reactor.simple.ack";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public void executeJobService() throws Exception {
		try {
			List<Instruction> instructions = instructionDao.findInstructionsForAcknowledgement();
			if ( instructions.size() > 0 ) {
				List<InstructionStatus> statuses = instructions.stream().map(Instruction::getStatus)
						.collect(Collectors.toList());
				instructionAcknowledgementService.acknowledgeInstructions(statuses);
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

}
