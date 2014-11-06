/* ==================================================================
 * GeneralNodeDatumSerializer.java - Aug 25, 2014 2:05:01 PM
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
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 * Serialize {@link GeneralNodeDatum} to JSON. The {@link GeneralLocationDatum}
 * is also supported.
 * 
 * @author matt
 * @version 1.1
 */
public class GeneralNodeDatumSerializer extends StdScalarSerializer<GeneralNodeDatum> {

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumSerializer() {
		super(GeneralNodeDatum.class);
	}

	@Override
	public void serialize(GeneralNodeDatum datum, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		generator.writeStartObject();
		generator.writeNumberField("created", datum.getCreated().getTime());
		if ( datum instanceof GeneralLocationDatum ) {
			GeneralLocationDatum loc = (GeneralLocationDatum) datum;
			generator.writeNumberField("locationId", loc.getLocationId());
		}
		generator.writeStringField("sourceId", datum.getSourceId());
		generator.writeObjectField("samples", datum.getSamples());
		generator.writeEndObject();
	}

}
