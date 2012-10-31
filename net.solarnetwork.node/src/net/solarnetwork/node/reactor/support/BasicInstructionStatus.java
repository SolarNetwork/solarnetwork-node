/* ==================================================================
 * BasicInstructionStatus.java - Feb 28, 2011 11:28:09 AM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.reactor.support;

import java.io.Serializable;
import java.util.Date;

import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * Basic implementation of {@link InstructionStatus}.
 * 
 * @author matt
 * @version $Revision$
 */
public class BasicInstructionStatus implements InstructionStatus, Serializable {

	private static final long serialVersionUID = -2397174943316242336L;

	private final Long instructionId;
	private final InstructionState instructionState;
	private final InstructionState acknowledgedInstructionState;
	private final Date statusDate;
	
	/**
	 * Constructor.
	 * 
	 * @param instructionId the instruction ID
	 * @param instructionState the instruction state
	 * @param statusDate the status date
	 */
	public BasicInstructionStatus(Long instructionId,
			InstructionState instructionState, Date statusDate) {
		this(instructionId, instructionState, statusDate, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param instructionId the instruction ID
	 * @param instructionState the instruction state
	 * @param statusDate the status date
	 * @param ackInstructionState the acknowledged state
	 */
	public BasicInstructionStatus(Long instructionId,
			InstructionState instructionState, Date statusDate, 
			InstructionState ackInstructionState) {
		this.instructionId = instructionId;
		this.instructionState = instructionState;
		this.statusDate = (statusDate == null ? new Date() : statusDate);
		this.acknowledgedInstructionState = ackInstructionState;
	}
	
	@Override
	public InstructionStatus newCopyWithState(InstructionState newState) {
		return new BasicInstructionStatus(this.instructionId, newState, 
				this.statusDate, this.acknowledgedInstructionState);
	}
	
	@Override
	public InstructionStatus newCopyWithAcknowledgedState(InstructionState newState) {
		return new BasicInstructionStatus(
				this.instructionId, this.instructionState, this.statusDate, newState);
	}

	@Override
	public Long getInstructionId() {
		return instructionId;
	}

	@Override
	public InstructionState getInstructionState() {
		return instructionState;
	}

	@Override
	public Date getStatusDate() {
		return statusDate;
	}

	@Override
	public InstructionState getAcknowledgedInstructionState() {
		return acknowledgedInstructionState;
	}

}
