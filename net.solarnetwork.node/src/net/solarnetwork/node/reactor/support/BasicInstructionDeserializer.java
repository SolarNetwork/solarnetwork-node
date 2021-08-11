/* ==================================================================
 * BasicInstructionDeserializer.java - 5/08/2021 1:39:21 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.solarnetwork.node.reactor.Instruction;

/**
 * Deserializer for {@link Instruction} instances.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public class BasicInstructionDeserializer extends StdScalarDeserializer<Instruction>
		implements Serializable {

	/** A default instance using {@link Instruction#LOCAL_INSTRUCTION_ID}. */
	public static final BasicInstructionDeserializer LOCAL_INSTANCE = new BasicInstructionDeserializer(
			Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID);

	private static final long serialVersionUID = -1844182290390256234L;

	private final String instructorId;
	private final String instructionId;

	/**
	 * Constructor.
	 * 
	 * @param instructorId
	 *        the instructor ID to use
	 * @param instructionId
	 *        the instruction ID to use
	 */
	public BasicInstructionDeserializer(String instructorId, String instructionId) {
		super(Instruction.class);
		this.instructorId = instructorId;
		this.instructionId = instructionId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BasicInstruction deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( p.isExpectedStartObjectToken() ) {
			String topic = null;
			String instructionId = this.instructionId;
			String instructorId = this.instructorId;
			Instant date = null;
			Map<String, List<String>> parameters = null;

			String f;
			while ( (f = p.nextFieldName()) != null ) {
				BasicInstructionField field = BasicInstructionField.FIELD_MAP.get(f);
				if ( field == null ) {
					continue;
				}
				Object v = field.parseValue(p, ctxt);
				switch (field) {
					case Topic:
						topic = (String) v;
						break;

					case Id:
						instructionId = (String) v;
						break;

					case InstructionDate:
						date = (Instant) v;
						break;

					case Params:
					case Parameters:
						parameters = (Map<String, List<String>>) v;
						break;
				}
			}

			// jump to end object
			while ( (t = p.currentToken()) != JsonToken.END_OBJECT ) {
				t = p.nextToken();
			}

			BasicInstruction result = new BasicInstruction(topic,
					date != null ? Date.from(date) : new Date(), instructionId, instructorId, null);
			if ( parameters != null ) {
				for ( Entry<String, List<String>> e : parameters.entrySet() ) {
					final String paramName = e.getKey();
					for ( String paramValue : e.getValue() ) {
						result.addParameter(paramName, paramValue);
					}
				}
			}
			return result;
		}
		throw new JsonParseException(p, "Unable to parse Instruction (not an object)");
	}

}
