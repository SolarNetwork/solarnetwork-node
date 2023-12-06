/* ==================================================================
 * BasicInstructionSerializer.java - 17/01/2023 3:32:53 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.codec.BasicInstructionField;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;

/**
 * Serializer for {@link Instruction} objects.
 * 
 * <p>
 * This serializer is used to provide API compatibility with the SolarUser API.
 * The {@link net.solarnetwork.codec.BasicInstructionSerializer} class
 * serializes the parameter array as a {@code params} field, while SolarUser
 * serializes the same as {@code parameters}. This class thus uses
 * {@code parameters} to maintain compatibility with SolarUser.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 3.3
 */
public class BasicInstructionSerializer extends StdSerializer<BasicInstruction> {

	private static final long serialVersionUID = 7971213870751023282L;

	/** A default instance. */
	public static final JsonSerializer<BasicInstruction> INSTANCE = new BasicInstructionSerializer();

	/**
	 * Constructor.
	 */
	public BasicInstructionSerializer() {
		super(BasicInstruction.class);
	}

	@Override
	public void serialize(BasicInstruction value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}
		gen.writeStartObject(value, 5);
		BasicInstructionField.Id.writeValue(gen, provider, value.getId());
		BasicInstructionField.Topic.writeValue(gen, provider, value.getTopic());
		BasicInstructionField.InstructionDate.writeValue(gen, provider, value.getInstructionDate());
		writeParameterList(gen, provider, value.getParameterMultiMap());
		BasicInstructionField.Status.writeValue(gen, provider, value.getStatus());
		gen.writeEndObject();
	}

	private static void writeParameterList(JsonGenerator generator, SerializerProvider provider,
			Map<String, List<String>> value) throws IOException {
		if ( value == null || value.isEmpty() ) {
			return;
		}
		generator.writeFieldName(BasicInstructionField.Parameters.getFieldName());

		int size = 0;
		for ( List<String> l : value.values() ) {
			size += l.size();
		}
		generator.writeStartArray(value, size);
		for ( Entry<String, List<String>> me : value.entrySet() ) {
			for ( String v : me.getValue() ) {
				generator.writeStartObject();
				generator.writeStringField("name", me.getKey());
				generator.writeStringField("value", v);
				generator.writeEndObject();
			}
		}
		generator.writeEndArray();
	}

}
