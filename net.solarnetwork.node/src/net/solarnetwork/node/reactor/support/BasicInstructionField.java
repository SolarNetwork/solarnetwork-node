/* ==================================================================
 * BasicInstructionField.java - 11/08/2021 3:22:37 PM
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.codec.IndexedField;
import net.solarnetwork.codec.JsonDateUtils.InstantDeserializer;
import net.solarnetwork.codec.JsonDateUtils.InstantSerializer;

/**
 * Fields for {@link BasicInstructionSerializer}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public enum BasicInstructionField implements IndexedField {

	Topic(0, "topic"),

	Id(1, "id"),

	InstructionDate(2, "instructionDate"),

	Params(3, "params"),

	Parameters(4, "parameters"),

	;

	/** A field map. */
	public static final Map<String, BasicInstructionField> FIELD_MAP = IndexedField
			.fieldMap(BasicInstructionField.class);

	private final int index;
	private final String fieldName;

	private BasicInstructionField(int index, String fieldName) {
		this.index = index;
		this.fieldName = fieldName;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public Object parseValue(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		switch (this) {
			case Topic:
			case Id:
				return parser.nextTextValue();

			case InstructionDate:
				parser.nextToken();
				return Date.from(InstantDeserializer.INSTANCE.deserialize(parser, ctxt));

			case Params:
			case Parameters:
				return parseParameters(parser, ctxt);

			default:
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeValue(JsonGenerator generator, SerializerProvider provider, Object value)
			throws IOException, JsonProcessingException {
		if ( value == null ) {
			return;
		}
		switch (this) {
			case Topic:
			case Id:
				generator.writeStringField(fieldName, value.toString());
				break;

			case InstructionDate:
				generator.writeFieldName(fieldName);
				InstantSerializer.INSTANCE.serialize(value != null ? ((Date) value).toInstant() : null,
						generator, provider);
				break;

			case Params:
			case Parameters:
				writeParameters((Map<String, List<String>>) value, generator, provider);
				break;

			default:
				// nothing
		}
	}

	private static Map<String, List<String>> parseParameters(JsonParser p, DeserializationContext ctxt)
			throws IOException {
		JsonToken t = p.nextToken();
		switch (t) {
			case START_ARRAY:
				return parseParameterList(p, ctxt);

			case START_OBJECT:
				return parseParameterMap(p, ctxt);

			default:
				return null;
		}
	}

	private static Map<String, List<String>> parseParameterList(JsonParser p,
			DeserializationContext ctxt) throws IOException {
		assert p.currentToken() == JsonToken.START_ARRAY;
		Map<String, List<String>> result = new LinkedHashMap<>(8);
		JsonToken t = null;
		String paramName = null;
		String paramValue = null;
		while ( (t = p.nextToken()) != JsonToken.END_ARRAY ) {
			if ( t == JsonToken.START_OBJECT ) {
				String f;
				while ( (f = p.nextFieldName()) != null ) {
					String v = p.nextTextValue();
					switch (f) {
						case "name":
							paramName = v;
							break;

						case "value":
							paramValue = v;
							break;
					}
				}
				if ( paramName != null && paramValue != null ) {
					result.computeIfAbsent(paramName, k -> new ArrayList<>(1)).add(paramValue);
				}
			}
		}
		return result;
	}

	private static Map<String, List<String>> parseParameterMap(JsonParser p, DeserializationContext ctxt)
			throws IOException {
		assert p.currentToken() == JsonToken.START_OBJECT;
		Map<String, List<String>> result = new LinkedHashMap<>(8);
		String f;
		while ( (f = p.nextFieldName()) != null ) {
			String s = p.nextTextValue();
			if ( s != null ) {
				result.computeIfAbsent(f, k -> new ArrayList<>(1)).add(s);
			}
		}
		return result;
	}

	private void writeParameters(Map<String, List<String>> value, JsonGenerator generator,
			SerializerProvider provider) throws IOException {
		if ( value == null || value.isEmpty() ) {
			return;
		}
		generator.writeFieldName(fieldName);
		switch (this) {
			case Params:
				writeParameterList(value, generator, provider);
				break;

			case Parameters:
				writeParameterMap(value, generator, provider);
				break;

			default:
				// nothing to do
		}
	}

	private static void writeParameterList(Map<String, List<String>> value, JsonGenerator generator,
			SerializerProvider provider) throws IOException {
		assert value != null;
		int size = 0;
		for ( List<String> l : value.values() ) {
			size += l.size();
		}
		generator.writeStartArray(size);
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

	private static void writeParameterMap(Map<String, List<String>> value, JsonGenerator generator,
			SerializerProvider provider) throws IOException {
		assert value != null;
		generator.writeStartObject(value, value.size());
		for ( Entry<String, List<String>> me : value.entrySet() ) {
			String v = (me.getValue() != null && !me.getValue().isEmpty() ? me.getValue().get(0) : null);
			if ( v == null ) {
				continue;
			}
			generator.writeStringField(me.getKey(), v);
		}
		generator.writeEndObject();
	}

}
