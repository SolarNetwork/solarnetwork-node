/* ==================================================================
 * SatelliteInfoSerializer.java - 15/11/2019 11:12:48 am
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

package net.solarnetwork.node.io.gpsd.util;

import static net.solarnetwork.node.io.gpsd.util.JsonUtils.writeNumberField;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.AZIMUTH_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.ELEVATION_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.PRN_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.SIGNAL_STRENGTH_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.USED_FIELD;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.node.io.gpsd.domain.SatelliteInfo;

/**
 * JSON serializer for {@link SatelliteInfo} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class SatelliteInfoSerializer extends StdSerializer<SatelliteInfo> {

	private static final long serialVersionUID = 7121472655567709875L;

	/**
	 * Constructor.
	 */
	public SatelliteInfoSerializer() {
		super(SatelliteInfo.class);
	}

	@Override
	public void serialize(SatelliteInfo value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}
		gen.writeStartObject();
		writeNumberField(gen, PRN_FIELD, value.getPrn());
		writeNumberField(gen, AZIMUTH_FIELD, value.getAzimuth());
		writeNumberField(gen, ELEVATION_FIELD, value.getElevation());
		writeNumberField(gen, SIGNAL_STRENGTH_FIELD, value.getSignalStrength());
		gen.writeBooleanField(USED_FIELD, value.isUsed());
		gen.writeEndObject();
	}

}
