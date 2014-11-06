/* ==================================================================
 * InstructionSerializer.java - Aug 25, 2014 2:21:07 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.bulkjsonwebpost;

import java.io.IOException;
import net.solarnetwork.node.reactor.Instruction;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 * Serialize {@link Instruction} to JSON.
 * 
 * @author matt
 * @version 1.1
 */
public class InstructionSerializer extends StdScalarSerializer<Instruction> {

	/**
	 * Default constructor.
	 */
	public InstructionSerializer() {
		super(Instruction.class);
	}

	@Override
	public void serialize(Instruction instruction, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		if ( instruction.getTopic() == null || instruction.getStatus() == null ) {
			return;
		}
		generator.writeStartObject();
		generator.writeStringField("__type__", "InstructionStatus");
		if ( instruction.getId() != null ) {
			generator.writeStringField("id", instruction.getId().toString());
		}
		generator.writeStringField("instructionId", instruction.getRemoteInstructionId());
		generator.writeStringField("topic", instruction.getTopic());
		generator.writeStringField("status", instruction.getStatus().getInstructionState().toString());
		generator.writeEndObject();
	}

}
