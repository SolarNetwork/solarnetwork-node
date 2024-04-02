/* ==================================================================
 * Instruction.java - Feb 28, 2011 10:16:35 AM
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * API for a single, immutable instruction with associated parameters.
 *
 * <p>
 * An instruction is like a single name (the {@code topic}) with an arbitrary
 * number of named key/value parameters. All parameter names and values are
 * Strings. Each parameter can have any number of associated values.
 * </p>
 *
 * <p>
 * An Instruction is considered equal to another Instruction only if the
 * {@code instructionId} and {@code instructorId} values are equal.
 * </p>
 *
 * @author matt
 * @version 2.3
 */
public interface Instruction extends net.solarnetwork.domain.Instruction {

	/**
	 * An ID to use for locally-initiated instructions, in
	 * {@link #getInstructorId()}.
	 */
	String LOCAL_INSTRUCTION_ID = "LOCAL";

	/**
	 * The name of an instruction parameter that represents the desired
	 * execution date for the instruction.
	 *
	 * <p>
	 * The parameter value can be either a {@code long} epoch millisecond value
	 * or an ISO 8601 instant.
	 * </p>
	 *
	 * @since 2.1
	 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
	 */
	String PARAM_EXECUTION_DATE = "executionDate";

	/**
	 * The task instruction parameter for a "parent" instruction ID.
	 *
	 * <p>
	 * This parameter can be used to track instructions created by other
	 * instructions.
	 * </p>
	 *
	 * @since 2.2
	 */
	String PARAM_PARENT_INSTRUCTION_ID = "parentInstructionId";

	/**
	 * The task instruction parameter for the "parent" instructor ID.
	 *
	 * <p>
	 * This parameter can be used to track instructions created by other
	 * instructions.
	 * </p>
	 *
	 * @since 2.2
	 */
	String PARAM_PARENT_INSTRUCTOR_ID = "parentInstructorId";

	/**
	 * Get an identifier for this instruction.
	 *
	 * <p>
	 * This can be used for logging, display, etc.
	 * </p>
	 *
	 * @return an identifier, never {@literal null}
	 */
	default String getIdentifier() {
		Long id = getId();
		return (id != null ? id.toString() : LOCAL_INSTRUCTION_ID);
	}

	/**
	 * Get the unique ID for the sender of the instruction, for example the DN
	 * of the sender's certificate.
	 *
	 * @return the instructor ID
	 */
	String getInstructorId();

	/**
	 * Get the instruction state.
	 *
	 * @return the state, or {@literal null} if none available
	 */
	@Override
	InstructionStatus getStatus();

	/**
	 * Get the instruction execution date, if available.
	 *
	 * <p>
	 * This method looks for the {@link #PARAM_EXECUTION_DATE} parameter and
	 * tries to parse that as a date. The parameter value can be either a
	 * {@code long} epoch millisecond value or an ISO 8601 instant.
	 * </p>
	 *
	 * @return the instruction execution date, or {@literal null} if one is not
	 *         available or cannot be parsed
	 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
	 * @see #timestampParameterValue(String)
	 * @since 2.1
	 */
	default Instant getExecutionDate() {
		return timestampParameterValue(PARAM_EXECUTION_DATE);
	}

	/**
	 * Get a date parameter value, if available.
	 *
	 * <p>
	 * This method supports the same date format as described in the
	 * {@link #PARAM_EXECUTION_DATE} parameter: the parameter value can be
	 * either a {@code long} epoch millisecond value or an ISO 8601 instant.
	 * </p>
	 *
	 * @param parameterName
	 *        the name of the parameter to parse as a timestamp
	 * @return the parameter value parsed as a timestamp, or {@literal null} if
	 *         one is not available or cannot be parsed
	 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
	 * @since 2.2
	 */
	default Instant timestampParameterValue(String parameterName) {
		if ( parameterName == null ) {
			return null;
		}
		final String val = getParameterValue(parameterName);
		if ( val == null ) {
			return null;
		}
		Instant result = null;
		try {
			result = Instant.ofEpochMilli(Long.parseLong(val));
		} catch ( NumberFormatException | DateTimeException e ) {
			try {
				result = DateTimeFormatter.ISO_INSTANT.parse(val, Instant::from);
			} catch ( DateTimeParseException e2 ) {
				// ignore and bail
			}
		}
		return result;
	}

	/**
	 * Test if this instruction is a local instruction.
	 *
	 * @return {@literal true} if {@link #getInstructorId()} is
	 *         {@link #LOCAL_INSTRUCTION_ID}
	 * @since 2.2
	 */
	default boolean isLocal() {
		return LOCAL_INSTRUCTION_ID.equals(getInstructorId());
	}

	/**
	 * Get the instruction parameters as a single-valued map.
	 *
	 * <p>
	 * All multi-value parameters will be concatenated into a single final
	 * value.
	 * </p>
	 *
	 * @return the parameters as a map, or {@literal null} if
	 *         {@link #getParameterMultiMap()} is {@literal null}
	 * @since 2.3
	 */
	default public Map<String, String> params() {
		Map<String, List<String>> mm = getParameterMultiMap();
		if ( mm == null ) {
			return null;
		}
		Map<String, String> params = new LinkedHashMap<>(mm.size());
		for ( Entry<String, List<String>> e : mm.entrySet() ) {
			if ( e.getKey() != null && e.getValue() != null ) {
				for ( String val : e.getValue() ) {
					if ( val != null ) {
						params.merge(e.getKey(), val, String::concat);
					}
				}
			}
		}
		return params;
	}

}
