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

package net.solarnetwork.node.reactor.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * Basic implementation of {@link Instruction}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicInstruction implements Instruction, Serializable {

	private static final long serialVersionUID = 5522509637377814131L;

	private final Long id;
	private final String topic;
	private final Date instructionDate;
	private final String remoteInstructionId;
	private final String instructorId;
	private final InstructionStatus status;
	private final Map<String, List<String>> parameters;

	public BasicInstruction(String topic, Date instructionDate, String remoteInstructionId,
			String instructorId, InstructionStatus status) {
		this(null, topic, instructionDate, remoteInstructionId, instructorId, status);
	}

	public BasicInstruction(Long id, String topic, Date instructionDate, String remoteInstructionId,
			String instructorId, InstructionStatus status) {
		this.id = id;
		this.topic = topic;
		this.instructionDate = instructionDate;
		this.remoteInstructionId = remoteInstructionId;
		this.instructorId = instructorId;
		this.status = status;
		this.parameters = new LinkedHashMap<String, List<String>>();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicInstruction{topic=");
		builder.append(topic);
		builder.append(",remoteInstructionId=");
		builder.append(remoteInstructionId);
		builder.append(",status=");
		builder.append(status);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getTopic() {
		return topic;
	}

	@Override
	public Date getInstructionDate() {
		return instructionDate;
	}

	@Override
	public String getRemoteInstructionId() {
		return remoteInstructionId;
	}

	@Override
	public String getInstructorId() {
		return instructorId;
	}

	@Override
	public Iterable<String> getParameterNames() {
		return Collections.unmodifiableSet(parameters.keySet());
	}

	@Override
	public boolean isParameterAvailable(String parameterName) {
		List<String> values = parameters.get(parameterName);
		return values != null;
	}

	@Override
	public String getParameterValue(String parameterName) {
		List<String> values = parameters.get(parameterName);
		return values == null ? null : values.get(0);
	}

	@Override
	public String[] getAllParameterValues(String parameterName) {
		List<String> values = parameters.get(parameterName);
		if ( values != null ) {
			return values.toArray(new String[values.size()]);
		}
		return null;
	}

	@Override
	public InstructionStatus getStatus() {
		return status;
	}

	/**
	 * Add a new parameter value.
	 * 
	 * @param name
	 *        the parameter name
	 * @param value
	 *        the parameter value
	 */
	public void addParameter(String name, String value) {
		assert name != null && value != null;
		List<String> values = parameters.get(name);
		if ( values == null ) {
			values = new ArrayList<String>(3);
			parameters.put(name, values);
		}
		values.add(value);
	}

}
