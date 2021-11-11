/* ==================================================================
 * DeviceMessageSerializer.java - 14/11/2019 3:43:49 pm
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

import static net.solarnetwork.codec.JsonUtils.writeBitmaskValue;
import static net.solarnetwork.codec.JsonUtils.writeIso8601Timestamp;
import static net.solarnetwork.codec.JsonUtils.writeNumberField;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.node.io.gpsd.domain.DeviceMessage;

/**
 * Serializer for {@link DeviceMessage} objects.
 * 
 * @author matt
 * @version 2.0
 */
public class DeviceMessageSerializer extends AbstractGpsdMessageSerializer<DeviceMessage> {

	private static final long serialVersionUID = -1442236642469061759L;

	public static final String PATH_FIELD = "path";
	public static final String ACTIVATED_FIELD = "activated";
	public static final String FLAGS_FIELD = "flags";
	public static final String DRIVER_FIELD = "driver";
	public static final String SUBTYPE_FIELD = "subtype";
	public static final String BPS_FIELD = "bps";
	public static final String PARITY_FIELD = "parity";
	public static final String STOPBITS_FIELD = "stopbits";
	public static final String NATIVE_FIELD = "native";
	public static final String CYCLE_FIELD = "cycle";
	public static final String MINCYCLE_FIELD = "mincycle";

	/**
	 * Constructor.
	 */
	public DeviceMessageSerializer() {
		super(DeviceMessage.class);
	}

	@Override
	public void serializeFields(DeviceMessage value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value.getPath() != null && !value.getPath().isEmpty() ) {
			gen.writeStringField(PATH_FIELD, value.getPath());
		}
		writeIso8601Timestamp(gen, ACTIVATED_FIELD, value.getActivated());
		writeBitmaskValue(gen, FLAGS_FIELD, value.getFlags());
		if ( value.getDriver() != null && !value.getDriver().isEmpty() ) {
			gen.writeStringField(DRIVER_FIELD, value.getDriver());
		}
		if ( value.getSubtype() != null && !value.getSubtype().isEmpty() ) {
			gen.writeStringField(SUBTYPE_FIELD, value.getSubtype());
		}
		writeNumberField(gen, BPS_FIELD, value.getBitsPerSecond());
		if ( value.getParity() != null ) {
			gen.writeStringField(PARITY_FIELD, value.getParity().getKey());
		}
		writeNumberField(gen, STOPBITS_FIELD, value.getStopbits());
		gen.writeNumberField(NATIVE_FIELD, value.isNativeMode() ? 1 : 0);
		writeNumberField(gen, CYCLE_FIELD, value.getCycleSeconds());
		writeNumberField(gen, MINCYCLE_FIELD, value.getMinimumCycleSeconds());
	}

}
