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
 */

package net.solarnetwork.node.reactor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Basic implementation of {@link InstructionStatus}.
 * 
 * @author matt
 * @version 2.1
 */
public class BasicInstructionStatus extends net.solarnetwork.domain.BasicInstructionStatus
		implements InstructionStatus, Serializable {

	private static final long serialVersionUID = 2582625556107338247L;

	/** The acknowledged instruction state. */
	private final InstructionState acknowledgedInstructionState;

	/**
	 * Constructor.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param instructionState
	 *        the instruction state
	 * @param statusDate
	 *        the status date
	 */
	public BasicInstructionStatus(Long instructionId, InstructionState instructionState,
			Instant statusDate) {
		this(instructionId, instructionState, statusDate, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param instructionState
	 *        the instruction state
	 * @param statusDate
	 *        the status date
	 * @param ackInstructionState
	 *        the acknowledged state
	 */
	public BasicInstructionStatus(Long instructionId, InstructionState instructionState,
			Instant statusDate, InstructionState ackInstructionState) {
		this(instructionId, instructionState, statusDate, ackInstructionState, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param instructionState
	 *        the instruction state
	 * @param statusDate
	 *        the status date
	 * @param ackInstructionState
	 *        the acknowledged state
	 * @param resultParameters
	 *        the result parameters
	 * @since 1.2
	 */
	public BasicInstructionStatus(Long instructionId, InstructionState instructionState,
			Instant statusDate, InstructionState ackInstructionState, Map<String, ?> resultParameters) {
		super(instructionId, instructionState, statusDate, resultParameters);
		this.acknowledgedInstructionState = ackInstructionState;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicInstructionStatus{");
		if ( getInstructionId() != null ) {
			builder.append("instructionId=");
			builder.append(getInstructionId());
			builder.append(", ");
		}
		if ( getInstructionState() != null ) {
			builder.append("instructionState=");
			builder.append(getInstructionState());
			builder.append(", ");
		}
		if ( acknowledgedInstructionState != null ) {
			builder.append("acknowledgedInstructionState=");
			builder.append(acknowledgedInstructionState);
			builder.append(", ");
		}
		if ( getStatusDate() != null ) {
			builder.append("statusDate=");
			builder.append(getStatusDate());
			builder.append(", ");
		}
		if ( getResultParameters() != null ) {
			builder.append("resultParameters=");
			builder.append(getResultParameters());
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public InstructionStatus newCopyWithState(InstructionState newState,
			Map<String, ?> resultParameters) {
		return new BasicInstructionStatus(getInstructionId(), newState, getStatusDate(),
				this.acknowledgedInstructionState,
				resultParameters != null ? resultParameters : getResultParameters());
	}

	@Override
	public InstructionStatus newCopyWithAcknowledgedState(InstructionState newState,
			Map<String, ?> resultParameters) {
		return new BasicInstructionStatus(getInstructionId(), getInstructionState(), getStatusDate(),
				newState, resultParameters != null ? resultParameters : getResultParameters());
	}

	@Override
	public InstructionState getAcknowledgedInstructionState() {
		return acknowledgedInstructionState;
	}

}
