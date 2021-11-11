/* ==================================================================
 * TpvReportMessageSerializer.java - 15/11/2019 9:49:21 am
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

import static net.solarnetwork.codec.JsonUtils.writeIso8601Timestamp;
import static net.solarnetwork.codec.JsonUtils.writeNumberField;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;

/**
 * Serializer for {@link TpvReportMessage} objects.
 * 
 * @author matt
 * @version 2.0
 */
public class TpvReportMessageSerializer extends AbstractGpsdMessageSerializer<TpvReportMessage> {

	private static final long serialVersionUID = -8857382035746492617L;

	public static final String DEVICE_FIELD = "device";
	public static final String MODE_FIELD = "mode";
	public static final String TIMESTAMP_FIELD = "time";
	public static final String TIMESTAMP_ERROR_FIELD = "ept";
	public static final String LATITUDE_FIELD = "lat";
	public static final String LONGITUDE_FIELD = "lon";
	public static final String ALTITUDE_FIELD = "alt";
	public static final String LONGITUDE_ERROR_FIELD = "epx";
	public static final String LATITUDE_ERROR_FIELD = "epy";
	public static final String ALTITUDE_ERROR_FIELD = "epv";
	public static final String COURSE_FIELD = "track";
	public static final String SPEED_FIELD = "speed";
	public static final String CLIMB_RATE_FIELD = "climb";
	public static final String COURSE_ERROR_FIELD = "epd";
	public static final String SPEED_ERROR_FIELD = "eps";
	public static final String CLIMB_RATE_ERROR_FIELD = "epc";

	/**
	 * Constructor.
	 */
	public TpvReportMessageSerializer() {
		super(TpvReportMessage.class);
	}

	@Override
	protected void serializeFields(TpvReportMessage value, JsonGenerator gen,
			SerializerProvider provider) throws IOException {
		if ( value.getDevice() != null && !value.getDevice().isEmpty() ) {
			gen.writeStringField(DEVICE_FIELD, value.getDevice());
		}
		writeNumberField(gen, MODE_FIELD, value.getMode() != null ? value.getMode().getCode() : 0);
		writeIso8601Timestamp(gen, TIMESTAMP_FIELD, value.getTimestamp());
		writeNumberField(gen, TIMESTAMP_ERROR_FIELD, value.getTimestampError());
		writeNumberField(gen, LATITUDE_FIELD, value.getLatitude());
		writeNumberField(gen, LONGITUDE_FIELD, value.getLongitude());
		writeNumberField(gen, ALTITUDE_FIELD, value.getAltitude());
		writeNumberField(gen, LONGITUDE_ERROR_FIELD, value.getLongitudeError());
		writeNumberField(gen, LATITUDE_ERROR_FIELD, value.getLatitudeError());
		writeNumberField(gen, ALTITUDE_ERROR_FIELD, value.getAltitudeError());
		writeNumberField(gen, COURSE_FIELD, value.getCourse());
		writeNumberField(gen, SPEED_FIELD, value.getSpeed());
		writeNumberField(gen, CLIMB_RATE_FIELD, value.getClimbRate());
		writeNumberField(gen, COURSE_ERROR_FIELD, value.getCourseError());
		writeNumberField(gen, SPEED_ERROR_FIELD, value.getSpeedError());
		writeNumberField(gen, CLIMB_RATE_ERROR_FIELD, value.getClimbRateError());
	}

}
