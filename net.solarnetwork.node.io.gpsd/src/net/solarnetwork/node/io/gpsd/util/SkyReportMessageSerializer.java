/* ==================================================================
 * SkyReportMessageSerializer.java - 15/11/2019 11:05:31 am
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

import static net.solarnetwork.node.io.gpsd.util.JsonUtils.writeIso8601Timestamp;
import static net.solarnetwork.node.io.gpsd.util.JsonUtils.writeNumberField;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.node.io.gpsd.domain.SatelliteInfo;
import net.solarnetwork.node.io.gpsd.domain.SkyReportMessage;

/**
 * Serializer for {@link SkyReportMessage} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class SkyReportMessageSerializer extends AbstractGpsdMessageSerializer<SkyReportMessage> {

	private static final long serialVersionUID = 7398922403735597192L;

	public static final String DEVICE_FIELD = "device";
	public static final String TIMESTAMP_FIELD = "time";
	public static final String LONGITUDE_DOP_FIELD = "xdop";
	public static final String LATITUDE_DOP_FIELD = "ydop";
	public static final String ALTITUDE__DOP_FIELD = "vdop";
	public static final String TIMESTAMP_DOP_FIELD = "tdop";
	public static final String HORIZONTAL_DOP_FIELD = "hdop";
	public static final String SPHERICAL_DOP_FIELD = "pdop";
	public static final String HYPERSPHERICAL_DOP_FIELD = "gdop";
	public static final String SATELLITES_FIELD = "satellites";

	/**
	 * Constructor.
	 */
	public SkyReportMessageSerializer() {
		super(SkyReportMessage.class);
	}

	@Override
	protected void serializeFields(SkyReportMessage value, JsonGenerator gen,
			SerializerProvider provider) throws IOException {
		if ( value.getDevice() != null && !value.getDevice().isEmpty() ) {
			gen.writeStringField(DEVICE_FIELD, value.getDevice());
		}
		writeIso8601Timestamp(gen, TIMESTAMP_FIELD, value.getTimestamp());
		writeNumberField(gen, LONGITUDE_DOP_FIELD, value.getLongitudeDop());
		writeNumberField(gen, LATITUDE_DOP_FIELD, value.getLatitudeDop());
		writeNumberField(gen, ALTITUDE__DOP_FIELD, value.getAltitudeDop());
		writeNumberField(gen, TIMESTAMP_DOP_FIELD, value.getTimestampDop());
		writeNumberField(gen, HORIZONTAL_DOP_FIELD, value.getHorizontalDop());
		writeNumberField(gen, SPHERICAL_DOP_FIELD, value.getSphericalDop());
		writeNumberField(gen, HYPERSPHERICAL_DOP_FIELD, value.getHypersphericalDop());
		Iterable<SatelliteInfo> satellites = value.getSatellites();
		if ( satellites != null ) {
			gen.writeArrayFieldStart(SATELLITES_FIELD);
			for ( SatelliteInfo info : satellites ) {
				gen.writeObject(info);
			}
			gen.writeEndArray();
		}
	}

}
