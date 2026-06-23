/* ==================================================================
 * BasicInstruction.java - Feb 28, 2011 10:36:05 AM
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;

/**
 * Basic implementation of {@link Instruction}.
 *
 * @author matt
 * @version 2.2
 */
public class BasicInstruction extends net.solarnetwork.domain.BasicInstruction
		implements Instruction, Serializable {

	private static final long serialVersionUID = 3391977559465548659L;

	/** The instructor ID. */
	private final String instructorId;

	/**
	 * Create a new {@code BasicInstruction} from a common instruction.
	 *
	 * @param instr
	 *        the common instruction
	 * @param instructorId
	 *        the instructor ID
	 * @return the new instruction, or {@code null} if {@code instr} is
	 *         {@code null}
	 */
	public static @Nullable BasicInstruction from(net.solarnetwork.domain.@Nullable Instruction instr,
			String instructorId) {
		if ( instr == null ) {
			return null;
		}
		BasicInstruction result = new BasicInstruction(instr.getId(), instr.getTopic(),
				instr.getInstructionDate(), instructorId, null);
		copyParameters(instr, result);
		return result;
	}

	/**
	 * Copy the parameters from one instruction to another.
	 *
	 * @param instr
	 *        the input instruction to copy from
	 * @param dest
	 *        the destination instruction to copy to
	 * @since 2.1
	 */
	public static void copyParameters(net.solarnetwork.domain.@Nullable Instruction instr,
			BasicInstruction dest) {
		if ( instr == null || dest == null ) {
			return;
		}
		final Iterable<String> paramNames = instr.getParameterNames();
		if ( paramNames == null ) {
			return;
		}
		for ( String paramName : paramNames ) {
			String[] paramValues = instr.getAllParameterValues(paramName);
			if ( paramValues != null ) {
				dest.putParameters(paramName, Arrays.asList(paramValues));
			}
		}
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the local instruction ID
	 * @param topic
	 *        the instruction topic
	 * @param instructionDate
	 *        the instruction date
	 * @param instructorId
	 *        the instructor ID
	 * @param status
	 *        the status, or {@code null}
	 */
	public BasicInstruction(Long id, @Nullable String topic, @Nullable Instant instructionDate,
			String instructorId, @Nullable InstructionStatus status) {
		super(id, topic, instructionDate, status);
		this.instructorId = instructorId;
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the instruction to copy
	 * @param id
	 *        if provided, the local ID to use
	 * @param status
	 *        if provided, the new status to use
	 * @since 1.2
	 */
	public BasicInstruction(Instruction other, @Nullable Long id, @Nullable InstructionStatus status) {
		this((id != null ? id : other.getId()), other.getTopic(), other.getInstructionDate(),
				other.getInstructorId(), (status != null ? status : other.getStatus()));
		Map<String, List<String>> otherParams = other.getParameterMultiMap();
		if ( otherParams != null ) {
			for ( Entry<String, List<String>> me : otherParams.entrySet() ) {
				putParameters(me.getKey(), me.getValue());
			}
		}
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the instruction to copy
	 * @param status
	 *        if provided, the new status to use
	 * @since 1.2
	 */
	public BasicInstruction(Instruction other, InstructionStatus status) {
		this(other, null, status);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicInstruction{topic=");
		builder.append(getTopic());
		builder.append(",id=");
		builder.append(getId());
		builder.append(",instructorId=");
		builder.append(instructorId);
		builder.append(",status=");
		builder.append(getStatus());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public String getInstructorId() {
		return instructorId;
	}

	@Override
	public @Nullable InstructionStatus getStatus() {
		return (InstructionStatus) super.getStatus();
	}

}
