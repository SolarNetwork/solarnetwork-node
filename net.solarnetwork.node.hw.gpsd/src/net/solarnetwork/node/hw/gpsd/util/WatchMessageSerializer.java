/* ==================================================================
 * WatchMessageSerializer.java - 15/11/2019 9:13:11 am
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
import net.solarnetwork.node.hw.gpsd.domain.WatchMessage;

/**
 * Serializer for {@link WatchMessage} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class WatchMessageSerializer extends AbstractGpsdMessageSerializer<WatchMessage> {

	private static final long serialVersionUID = 8551922132996636816L;

	public static final String ENABLE_FIELD = "enable";
	public static final String DUMP_JSON_FIELD = "json";
	public static final String DUMP_NMEA_FIELD = "nmea";
	public static final String DUMP_RAW_FIELD = "raw";
	public static final String SCALED_FIELD = "scaled";
	public static final String SPLIT24_FIELD = "split24";
	public static final String PPS_FIELD = "pps";
	public static final String DEVICE_FIELD = "device";
	public static final String REMOTE_URL_FIELD = "remote";

	/**
	 * Constructor.
	 */
	public WatchMessageSerializer() {
		super(WatchMessage.class);
	}

	@Override
	protected void serializeFields(WatchMessage value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		gen.writeBooleanField(ENABLE_FIELD, value.isEnable());
		gen.writeBooleanField(DUMP_JSON_FIELD, value.isDumpJson());
		gen.writeBooleanField(DUMP_NMEA_FIELD, value.isDumpNmea());
		writeNumberField(gen, DUMP_RAW_FIELD, value.getDumpRaw());
		gen.writeBooleanField(SCALED_FIELD, value.isScaled());
		gen.writeBooleanField(SPLIT24_FIELD, value.isSplit24());
		gen.writeBooleanField(PPS_FIELD, value.isPps());
		if ( value.getDevice() != null && !value.getDevice().isEmpty() ) {
			gen.writeStringField(DEVICE_FIELD, value.getDevice());
		}
		if ( value.getRemoteUrl() != null && !value.getRemoteUrl().isEmpty() ) {
			gen.writeStringField(REMOTE_URL_FIELD, value.getRemoteUrl());
		}
	}

}
