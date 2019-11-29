/* ==================================================================
 * SatelliteInfoDeserializer.java - 15/11/2019 10:42:37 am
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

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.solarnetwork.node.io.gpsd.domain.SatelliteInfo;

/**
 * JSON deserializer for {@link SatelliteInfo} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class SatelliteInfoDeserializer extends StdScalarDeserializer<SatelliteInfo> {

	private static final long serialVersionUID = -12740633372900062L;

	public static final String PRN_FIELD = "PRN";
	public static final String AZIMUTH_FIELD = "az";
	public static final String ELEVATION_FIELD = "el";
	public static final String SIGNAL_STRENGTH_FIELD = "ss";
	public static final String USED_FIELD = "used";

	/**
	 * Constructor.
	 */
	public SatelliteInfoDeserializer() {
		super(SatelliteInfo.class);
	}

	@Override
	public SatelliteInfo deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		SatelliteInfo.Builder builder = null;
		if ( p.currentToken() == JsonToken.START_OBJECT ) {
			String field = null;
			while ( (field = p.nextFieldName()) != null ) {
				JsonToken t = p.nextToken();
				if ( t.isScalarValue() ) {
					if ( builder == null ) {
						builder = SatelliteInfo.builder();
					}
					if ( t.isNumeric() ) {
						switch (field) {
							case PRN_FIELD:
								builder.withPrn(p.getNumberValue());
								break;

							case AZIMUTH_FIELD:
								builder.withAzimuth(p.getNumberValue());
								break;

							case ELEVATION_FIELD:
								builder.withElevation(p.getNumberValue());
								break;

							case SIGNAL_STRENGTH_FIELD:
								builder.withSignalStrength(p.getNumberValue());
								break;

							default:
								// ignore
						}
					} else if ( t.isBoolean() && USED_FIELD.equals(field) ) {
						builder.withUsed(p.getBooleanValue());
					}
				}
			}
		}
		return (builder != null ? builder.build() : null);
	}

}
