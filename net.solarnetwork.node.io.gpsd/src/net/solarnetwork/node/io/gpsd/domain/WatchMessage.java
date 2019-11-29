/* ==================================================================
 * WatchMessage.java - 12/11/2019 10:59:36 am
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

package net.solarnetwork.node.io.gpsd.domain;

import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.DEVICE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.DUMP_JSON_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.DUMP_NMEA_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.DUMP_RAW_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.ENABLE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.PPS_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.REMOTE_URL_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.SCALED_FIELD;
import static net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.SPLIT24_FIELD;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Message for the {@literal WATCH} command.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = WatchMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.io.gpsd.util.WatchMessageSerializer.class)
public class WatchMessage extends AbstractGpsdMessage {

	private final boolean enable;
	private final boolean dumpJson;
	private final boolean dumpNmea;
	private final Number dumpRaw;
	private final boolean scaled;
	private final boolean split24;
	private final boolean pps;
	private final String device;
	private final String remoteUrl;

	private WatchMessage(Builder builder) {
		super(GpsdMessageType.Watch, null);
		this.enable = builder.enable;
		this.dumpJson = builder.dumpJson;
		this.dumpNmea = builder.dumpNmea;
		this.dumpRaw = builder.dumpRaw;
		this.scaled = builder.scaled;
		this.split24 = builder.split24;
		this.pps = builder.pps;
		this.device = builder.device;
		this.remoteUrl = builder.remoteUrl;
	}

	/**
	 * Creates builder to build {@link WatchMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link WatchMessage}.
	 */
	public static final class Builder implements GpsdJsonParser<WatchMessage> {

		private boolean enable = true;
		private boolean dumpJson = false;
		private boolean dumpNmea = false;
		private Number dumpRaw;
		private boolean scaled = false;
		private boolean split24 = false;
		private boolean pps = false;
		private String device;
		private String remoteUrl;

		private Builder() {
		}

		public Builder withEnable(boolean enable) {
			this.enable = enable;
			return this;
		}

		public Builder withDumpJson(boolean dumpJson) {
			this.dumpJson = dumpJson;
			return this;
		}

		public Builder withDumpNmea(boolean dumpNmea) {
			this.dumpNmea = dumpNmea;
			return this;
		}

		public Builder withDumpRaw(Number dumpRaw) {
			this.dumpRaw = dumpRaw;
			return this;
		}

		public Builder withScaled(boolean scaled) {
			this.scaled = scaled;
			return this;
		}

		public Builder withSplit24(boolean split24) {
			this.split24 = split24;
			return this;
		}

		public Builder withPps(boolean pps) {
			this.pps = pps;
			return this;
		}

		public Builder withDevice(String device) {
			this.device = device;
			return this;
		}

		public Builder withRemoteUrl(String remoteUrl) {
			this.remoteUrl = remoteUrl;
			return this;
		}

		public WatchMessage build() {
			return new WatchMessage(this);
		}

		@Override
		public WatchMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withEnable(json.path(ENABLE_FIELD).booleanValue())
					.withDumpJson(json.path(DUMP_JSON_FIELD).booleanValue())
					.withDumpNmea(json.path(DUMP_NMEA_FIELD).booleanValue())
					.withDumpRaw(json.path(DUMP_RAW_FIELD).numberValue())
					.withScaled(json.path(SCALED_FIELD).booleanValue())
					.withSplit24(json.path(SPLIT24_FIELD).booleanValue())
					.withPps(json.path(PPS_FIELD).booleanValue())
					.withDevice(json.path(DEVICE_FIELD).textValue())
					.withRemoteUrl(json.path(REMOTE_URL_FIELD).textValue()).build();
		}

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("WatchMessage{enable=");
		buf.append(enable);
		buf.append(", dumpJson=");
		buf.append(dumpJson);
		buf.append(", dumpNmea=");
		buf.append(dumpNmea);
		buf.append(", dumpRaw=");
		buf.append(dumpRaw);
		buf.append(", scaled=");
		buf.append(scaled);
		buf.append(", split24=");
		buf.append(split24);
		buf.append(", device=");
		buf.append(device);
		buf.append(", remoteUrl=");
		buf.append(remoteUrl);
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ Objects.hash(device, dumpJson, dumpNmea, dumpRaw, enable, remoteUrl, scaled, split24);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof WatchMessage) ) {
			return false;
		}
		WatchMessage other = (WatchMessage) obj;
		return Objects.equals(device, other.device) && dumpJson == other.dumpJson
				&& dumpNmea == other.dumpNmea && Objects.equals(dumpRaw, other.dumpRaw)
				&& enable == other.enable && Objects.equals(remoteUrl, other.remoteUrl)
				&& scaled == other.scaled && split24 == other.split24;
	}

	/**
	 * Get the enable (true) or disable (false) watcher mode.
	 * 
	 * @return the enable mode; defaults to {@literal true}
	 */
	public boolean isEnable() {
		return enable;
	}

	/**
	 * Get the enable (true) or disable (false) dumping of JSON reports.
	 * 
	 * @return the dump JSON mode; defaults to {@literal false}
	 */
	public boolean isDumpJson() {
		return dumpJson;
	}

	/**
	 * Get the enable (true) or disable(false) dumping of NMEA reports.
	 * 
	 * @return the dumpNmea the dump NMEA mode; defaults to {@literal false}
	 */
	public boolean isDumpNmea() {
		return dumpNmea;
	}

	/**
	 * Set the "raw" dump mode.
	 * 
	 * @return the raw dump mode
	 */
	public Number getDumpRaw() {
		return dumpRaw;
	}

	/**
	 * If true, apply scaling divisors to output before dumping.
	 * 
	 * @return the scaled mode; defaults to {@literal false}
	 */
	public boolean isScaled() {
		return scaled;
	}

	/**
	 * Get the specific device to watch, rather than all devices.
	 * 
	 * @return the device to watch, or {@literal null}
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * Get the AIS "type24" aggregate mode.
	 * 
	 * @return when {@literal true} aggregate AIS type24 sentence parts,
	 *         otherwise report each part as a separate JSON object, leaving the
	 *         client to match MMSIs and aggregate; defaults to {@literal false}
	 */
	public boolean isSplit24() {
		return split24;
	}

	/**
	 * Get the PPS mode.
	 * 
	 * @return wheN {@literal true} then emit the {@literal TOFF} JSON message
	 *         on each cycle and a PPS JSON message when the device issues
	 *         {@literal 1PPS}; defaults to {@literal false}
	 */
	public boolean isPps() {
		return pps;
	}

	/**
	 * Get the remote URL.
	 * 
	 * @return the remote GPSd URL, or {@literal null}
	 */
	public String getRemoteUrl() {
		return remoteUrl;
	}

}
