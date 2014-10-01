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

package net.solarnetwork.node.upload.bulkjsonwebpost;

import java.io.IOException;
import net.solarnetwork.domain.NodeControlInfo;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

/**
 * Serialize {@link NodeControlInfo} to JSON.
 * 
 * @author matt
 * @version 1.1
 */
public class NodeControlInfoSerializer extends SerializerBase<NodeControlInfo> {

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
		generator.writeStringField("unit", info.getUnit());
		generator.writeStringField("value", info.getValue());
		generator.writeEndObject();
	}

}
