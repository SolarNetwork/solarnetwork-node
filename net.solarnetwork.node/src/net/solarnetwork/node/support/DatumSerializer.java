/* ==================================================================
 * DatumSerializer.java - Aug 25, 2014 2:27:41 PM
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

package net.solarnetwork.node.support;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.util.ClassUtils;

/**
 * Serialize {@link Datum} to JSON.
 * 
 * @author matt
 * @version 1.0
 * @since 1.58
 */
public class DatumSerializer extends StdScalarSerializer<Datum> implements Serializable {

	private static final long serialVersionUID = -523673923281012956L;

	/**
	 * Default constructor.
	 */
	public DatumSerializer() {
		super(Datum.class);
	}

	@Override
	public void serialize(Datum datum, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		generator.writeStartObject();
		generator.writeStringField("__type__", datum.getClass().getSimpleName());
		Map<String, Object> props = ClassUtils.getBeanProperties(datum, null, true);
		for ( Map.Entry<String, Object> me : props.entrySet() ) {
			Object val = me.getValue();
			if ( val instanceof Number ) {
				if ( val instanceof Integer ) {
					generator.writeNumberField(me.getKey(), (Integer) val);
				} else if ( val instanceof Long ) {
					generator.writeNumberField(me.getKey(), (Long) val);
				} else if ( val instanceof Double ) {
					generator.writeNumberField(me.getKey(), (Double) val);
				} else if ( val instanceof Float ) {
					generator.writeNumberField(me.getKey(), (Float) val);
				} else if ( val instanceof BigDecimal ) {
					generator.writeNumberField(me.getKey(), (BigDecimal) val);
				}
			} else if ( val instanceof Date ) {
				generator.writeNumberField(me.getKey(), ((Date) val).getTime());
			} else if ( val instanceof String ) {
				generator.writeStringField(me.getKey(), (String) val);
			}
		}
		generator.writeEndObject();
	}

}
