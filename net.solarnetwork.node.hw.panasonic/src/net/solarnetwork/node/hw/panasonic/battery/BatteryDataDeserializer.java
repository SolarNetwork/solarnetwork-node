/* ==================================================================
 * BatteryDataDeserializer.java - 16/02/2016 8:33:48 am
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.panasonic.battery;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.solarnetwork.codec.JsonUtils;

/**
 * Parse JSON into BatteryData instances.
 * 
 * @author matt
 * @version 2.0
 */
public class BatteryDataDeserializer extends StdScalarDeserializer<BatteryData> {

	private static final long serialVersionUID = 7986330830501564619L;

	/** The date + time pattern used by the API. */
	public static final String DATE_TIME_PATTERN = "yyyyMMdd_HHmmss";

	/** The {@link DateTimeFormatter} for parsing dates. */
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN).withZone(ZoneOffset.UTC);

	/**
	 * Default constructor.
	 */
	public BatteryDataDeserializer() {
		super(BatteryData.class);
	}

	private String getString(TreeNode node, String key) {
		TreeNode n = node.get(key);
		return (n instanceof JsonNode ? ((JsonNode) n).textValue() : null);
	}

	private Integer getInteger(TreeNode node, String key) {
		TreeNode n = node.get(key);
		Number num = (n instanceof JsonNode ? ((JsonNode) n).numberValue() : null);
		return (num != null ? Integer.valueOf(num.intValue()) : null);
	}

	@Override
	public BatteryData deserialize(JsonParser parser, DeserializationContext context)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = parser.getCodec();
		TreeNode node = oc.readTree(parser);

		String deviceId = getString(node, "DeviceID");
		Instant date = JsonUtils.parseDateAttribute(node, "ReportDate", TIMESTAMP_FORMATTER,
				Instant::from);
		String status = getString(node, "Status");
		Integer availableCapacity = getInteger(node, "CurrentCapacity");
		Integer totalCapacity = getInteger(node, "TotalCapacity");

		return new BatteryData(deviceId, (date != null ? date : Instant.now()), status,
				availableCapacity, totalCapacity);
	}

}
