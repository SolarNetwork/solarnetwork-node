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

package net.solarnetwork.node.reactor.support;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * Basic implementation of {@link InstructionStatus}.
 * 
 * @author matt
 * @version 1.2
 */
public class BasicInstructionStatus implements InstructionStatus, Serializable {

	private static final long serialVersionUID = 4584466858414350595L;

	private final Long instructionId;
	private final InstructionState instructionState;
	private final InstructionState acknowledgedInstructionState;
	private final Date statusDate;
	private final Map<String, ?> resultParameters;

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
			Date statusDate) {
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
	public BasicInstructionStatus(Long instructionId, InstructionState instructionState, Date statusDate,
			InstructionState ackInstructionState) {
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
	public BasicInstructionStatus(Long instructionId, InstructionState instructionState, Date statusDate,
			InstructionState ackInstructionState, Map<String, ?> resultParameters) {
		this.instructionId = instructionId;
		this.instructionState = instructionState;
		this.statusDate = (statusDate == null ? new Date() : statusDate);
		this.acknowledgedInstructionState = ackInstructionState;
		this.resultParameters = resultParameters;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicInstructionStatus{");
		if ( instructionId != null ) {
			builder.append("instructionId=");
			builder.append(instructionId);
			builder.append(", ");
		}
		if ( instructionState != null ) {
			builder.append("instructionState=");
			builder.append(instructionState);
			builder.append(", ");
		}
		if ( acknowledgedInstructionState != null ) {
			builder.append("acknowledgedInstructionState=");
			builder.append(acknowledgedInstructionState);
			builder.append(", ");
		}
		if ( statusDate != null ) {
			builder.append("statusDate=");
			builder.append(statusDate);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public InstructionStatus newCopyWithState(InstructionState newState) {
		return new BasicInstructionStatus(this.instructionId, newState, this.statusDate,
				this.acknowledgedInstructionState, this.resultParameters);
	}

	@Override
	public InstructionStatus newCopyWithAcknowledgedState(InstructionState newState) {
		return new BasicInstructionStatus(this.instructionId, this.instructionState, this.statusDate,
				newState, this.resultParameters);
	}

	@Override
	public InstructionStatus newCopyWithState(InstructionState newState,
			Map<String, ?> resultParameters) {
		return new BasicInstructionStatus(this.instructionId, newState, this.statusDate,
				this.acknowledgedInstructionState, resultParameters);
	}

	@Override
	public InstructionStatus newCopyWithAcknowledgedState(InstructionState newState,
			Map<String, ?> resultParameters) {
		return new BasicInstructionStatus(this.instructionId, this.instructionState, this.statusDate,
				newState, resultParameters);
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

	@Override
	public Map<String, ?> getResultParameters() {
		return resultParameters;
	}

}
