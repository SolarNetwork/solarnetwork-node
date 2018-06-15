/* ==================================================================
 * NodeControlInfoSerializer.java - Aug 25, 2014 1:54:33 PM
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
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import net.solarnetwork.domain.NodeControlInfo;

/**
 * Serialize {@link NodeControlInfo} to JSON.
 * 
 * @author matt
 * @version 1.0
 * @since 1.58
 */
public class NodeControlInfoSerializer extends StdScalarSerializer<NodeControlInfo>
		implements Serializable {

	private static final long serialVersionUID = 3250639159449218754L;

	/**
	 * Default constructor.
	 */
	public NodeControlInfoSerializer() {
		super(NodeControlInfo.class);
	}

	@Override
	public void serialize(NodeControlInfo info, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		generator.writeStartObject();
		generator.writeStringField("__type__", "NodeControlInfo");
		generator.writeStringField("controlId", info.getControlId());
		generator.writeStringField("type", info.getType().toString());
		if ( info.getPropertyName() != null ) {
			generator.writeStringField("propertyName", info.getPropertyName());
		}
		if ( info.getUnit() != null ) {
			generator.writeStringField("unit", info.getUnit());
		}
		generator.writeStringField("value", info.getValue());
		generator.writeEndObject();
	}

}
