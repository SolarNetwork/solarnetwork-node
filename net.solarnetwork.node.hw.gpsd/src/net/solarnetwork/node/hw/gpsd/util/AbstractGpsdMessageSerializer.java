/* ==================================================================
 * AbstractGpsdMessageSerializer.java - 14/11/2019 3:48:53 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.gpsd.util;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;

/**
 * Base class to support JSON serialization of {@link GpsdMessage} objects.
 * 
 * @param <T>
 *        the message type
 * @author matt
 * @version 1.0
 */
public abstract class AbstractGpsdMessageSerializer<T extends GpsdMessage> extends StdSerializer<T> {

	public static final String CLASS_FIELD = "class";

	private static final long serialVersionUID = -5959210124908880965L;

	/**
	 * Constructor.
	 * 
	 * @param clazz
	 *        the message object class handled by this serializer
	 */
	public AbstractGpsdMessageSerializer(Class<T> clazz) {
		super(clazz);
	}

	@Override
	public final void serialize(T value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value == null ) {
			gen.writeNull();
		}
		gen.writeStartObject(value);
		gen.writeStringField(CLASS_FIELD, value.getMessageName());
		serializeFields(value, gen, provider);
		gen.writeEndObject();
	}

	/**
	 * Serialize the message object fields.
	 * 
	 * <p>
	 * This method is called from
	 * {@link #serialize(GpsdMessage, JsonGenerator, SerializerProvider)} after
	 * a JSON object with a {@literal class} field has been generated already.
	 * This method should generate all remaining fields for the given value.
	 * </p>
	 * 
	 * @param value
	 *        the value to generate JSON for
	 * @param gen
	 *        the JSON generator
	 * @param provider
	 *        the provider
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected abstract void serializeFields(T value, JsonGenerator gen, SerializerProvider provider)
			throws IOException;

}
