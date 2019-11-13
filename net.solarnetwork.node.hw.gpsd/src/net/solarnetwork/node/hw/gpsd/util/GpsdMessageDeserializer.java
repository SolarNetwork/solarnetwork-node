/* ==================================================================
 * GpsdMessageDeserializer.java - 12/11/2019 6:43:57 am
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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessageJsonParser;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.hw.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.hw.gpsd.domain.UnknownMessage;
import net.solarnetwork.node.hw.gpsd.domain.VersionMessage;

/**
 * JSON deserializer for {@link GpsdMessage} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdMessageDeserializer extends StdScalarDeserializer<GpsdMessage> {

	private static final long serialVersionUID = 7535364248946369188L;

	/**
	 * Constructor.
	 */
	public GpsdMessageDeserializer() {
		super(GpsdMessage.class);
	}

	@Override
	public GpsdMessage deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		GpsdMessage result = null;
		if ( p.currentToken() == JsonToken.START_OBJECT ) {
			JsonNode json = p.readValueAsTree();
			String messageName = (json != null ? json.path("class").textValue() : null);
			GpsdMessageType messageType = GpsdMessageType.forName(messageName);
			GpsdMessageJsonParser<? extends GpsdMessage> parser = null;
			if ( messageType == GpsdMessageType.Unknown ) {
				result = new UnknownMessage(messageName, json);
			} else {
				switch (messageType) {
					case TpvReport:
						parser = TpvReportMessage.builder();
						break;

					case Version:
						parser = VersionMessage.builder();
						break;

					default:
						result = new UnknownMessage(messageType, json);
				}
			}
			if ( parser != null ) {
				result = parser.parseJsonTree(json);
			}
		}
		return result;
	}

}
