/* ==================================================================
 * SkyReportMessage.java - 15/11/2019 10:29:59 am
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

import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.ALTITUDE__DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.DEVICE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.HORIZONTAL_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.HYPERSPHERICAL_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.LATITUDE_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.LONGITUDE_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.SATELLITES_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.SPHERICAL_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.TIMESTAMP_DOP_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.TIMESTAMP_FIELD;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.solarnetwork.node.io.gpsd.util.JsonUtils;

/**
 * A GPS satellite sky view report message.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = SkyReportMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.io.gpsd.util.SkyReportMessageSerializer.class)
public class SkyReportMessage extends AbstractGpsdReportMessage {

	private final String device;
	private final Number longitudeDop;
	private final Number latitudeDop;
	private final Number altitudeDop;
	private final Number timestampDop;
	private final Number horizontalDop;
	private final Number sphericalDop;
	private final Number hypersphericalDop;
	private final Iterable<SatelliteInfo> satellites;

	private SkyReportMessage(Builder builder) {
		super(GpsdMessageType.SkyReport, builder.timestamp);
		this.device = builder.device;
		this.longitudeDop = builder.longitudeDop;
		this.latitudeDop = builder.latitudeDop;
		this.altitudeDop = builder.altitudeDop;
		this.timestampDop = builder.timestampDop;
		this.horizontalDop = builder.horizontalDop;
		this.sphericalDop = builder.sphericalDop;
		this.hypersphericalDop = builder.hypersphericalDop;
		this.satellites = builder.satellites;
	}

	/**
	 * Creates builder to build {@link SkyReportMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public GpsdReportMessage withTimestamp(Instant timestamp) {
		return new Builder(this).withTimestamp(timestamp).build();
	}

	/**
	 * Builder to build {@link SkyReportMessage}.
	 */
	public static final class Builder implements GpsdJsonParser<SkyReportMessage> {

		private String device;
		private Instant timestamp;
		private Number longitudeDop;
		private Number latitudeDop;
		private Number altitudeDop;
		private Number timestampDop;
		private Number horizontalDop;
		private Number sphericalDop;
		private Number hypersphericalDop;
		private Iterable<SatelliteInfo> satellites = Collections.emptyList();

		private Builder() {
		}

		private Builder(SkyReportMessage other) {
			if ( other == null ) {
				return;
			}
			withDevice(other.device);
			withTimestamp(other.getTimestamp());
			withLongitudeDop(other.longitudeDop);
			withLatitudeDop(other.latitudeDop);
			withAltitudeDop(other.altitudeDop);
			withTimestampDop(other.timestampDop);
			withHorizontalDop(other.horizontalDop);
			withSphericalDop(other.hypersphericalDop);
			withHypersphericalDop(other.hypersphericalDop);
			withSatellites(other.satellites);
		}

		public Builder withDevice(String device) {
			this.device = device;
			return this;
		}

		public Builder withTimestamp(String timestamp) {
			return withTimestamp(JsonUtils.iso8610Timestamp(timestamp));
		}

		public Builder withTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder withLongitudeDop(Number longitudeDop) {
			this.longitudeDop = longitudeDop;
			return this;
		}

		public Builder withLatitudeDop(Number latitudeDop) {
			this.latitudeDop = latitudeDop;
			return this;
		}

		public Builder withAltitudeDop(Number altitudeDop) {
			this.altitudeDop = altitudeDop;
			return this;
		}

		public Builder withTimestampDop(Number timestampDop) {
			this.timestampDop = timestampDop;
			return this;
		}

		public Builder withHorizontalDop(Number horizontalDop) {
			this.horizontalDop = horizontalDop;
			return this;
		}

		public Builder withSphericalDop(Number sphericalDop) {
			this.sphericalDop = sphericalDop;
			return this;
		}

		public Builder withHypersphericalDop(Number hypersphericalDop) {
			this.hypersphericalDop = hypersphericalDop;
			return this;
		}

		public Builder withSatellites(Iterable<SatelliteInfo> satellites) {
			this.satellites = satellites;
			return this;
		}

		public SkyReportMessage build() {
			return new SkyReportMessage(this);
		}

		@Override
		public SkyReportMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			withDevice(json.path(DEVICE_FIELD).textValue())
					.withTimestamp(json.path(TIMESTAMP_FIELD).textValue())
					.withLongitudeDop(json.path(LONGITUDE_DOP_FIELD).numberValue())
					.withLatitudeDop(json.path(LATITUDE_DOP_FIELD).numberValue())
					.withAltitudeDop(json.path(ALTITUDE__DOP_FIELD).numberValue())
					.withTimestampDop(json.path(TIMESTAMP_DOP_FIELD).numberValue())
					.withHorizontalDop(json.path(HORIZONTAL_DOP_FIELD).numberValue())
					.withSphericalDop(json.path(SPHERICAL_DOP_FIELD).numberValue())
					.withHypersphericalDop(json.path(HYPERSPHERICAL_DOP_FIELD).numberValue());
			JsonNode sats = json.path(SATELLITES_FIELD);
			if ( sats.isArray() ) {
				List<SatelliteInfo> infos = new ArrayList<>(sats.size());
				for ( JsonNode sat : sats ) {
					infos.add(SatelliteInfo.builder().parseJsonTree(sat));
				}
				withSatellites(Collections.unmodifiableList(infos));
			}
			return build();
		}

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("SkyReportMessage{");
		if ( device != null ) {
			buf.append("device=");
			buf.append(device);
			buf.append(", ");
		}
		if ( getTimestamp() != null ) {
			buf.append("timestamp=");
			buf.append(getTimestamp());
			buf.append(", ");
		}
		if ( longitudeDop != null ) {
			buf.append("longitudeDop=");
			buf.append(longitudeDop);
			buf.append(", ");
		}
		if ( latitudeDop != null ) {
			buf.append("latitudeDop=");
			buf.append(latitudeDop);
			buf.append(", ");
		}
		if ( altitudeDop != null ) {
			buf.append("altitudeDop=");
			buf.append(altitudeDop);
			buf.append(", ");
		}
		if ( timestampDop != null ) {
			buf.append("timestampDop=");
			buf.append(timestampDop);
			buf.append(", ");
		}
		if ( horizontalDop != null ) {
			buf.append("horizontalDop=");
			buf.append(horizontalDop);
			buf.append(", ");
		}
		if ( sphericalDop != null ) {
			buf.append("sphericalDop=");
			buf.append(sphericalDop);
			buf.append(", ");
		}
		if ( hypersphericalDop != null ) {
			buf.append("hypersphericalDop=");
			buf.append(hypersphericalDop);
			buf.append(", ");
		}
		if ( satellites instanceof Collection<?> ) {
			buf.append("satelliteCount=");
			buf.append(((Collection<?>) satellites).size());
		}
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(altitudeDop, device, horizontalDop, hypersphericalDop,
				latitudeDop, longitudeDop, satellites, sphericalDop, timestampDop);
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
		if ( !(obj instanceof SkyReportMessage) ) {
			return false;
		}
		SkyReportMessage other = (SkyReportMessage) obj;
		return Objects.equals(altitudeDop, other.altitudeDop) && Objects.equals(device, other.device)
				&& Objects.equals(horizontalDop, other.horizontalDop)
				&& Objects.equals(hypersphericalDop, other.hypersphericalDop)
				&& Objects.equals(latitudeDop, other.latitudeDop)
				&& Objects.equals(longitudeDop, other.longitudeDop)
				&& Objects.equals(satellites, other.satellites)
				&& Objects.equals(sphericalDop, other.sphericalDop)
				&& Objects.equals(timestampDop, other.timestampDop);
	}

	/**
	 * Get the name of originating device.
	 * 
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * Get the longitudinal dilution of precision, a dimensionless factor which
	 * should be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the longitude DOP
	 */
	public Number getLongitudeDop() {
		return longitudeDop;
	}

	/**
	 * Get the latitudinal dilution of precision, a dimensionless factor which
	 * should be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the latitude DOP
	 */
	public Number getLatitudeDop() {
		return latitudeDop;
	}

	/**
	 * Get the altitude dilution of precision, a dimensionless factor which
	 * should be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the altitude DOP
	 */
	public Number getAltitudeDop() {
		return altitudeDop;
	}

	/**
	 * Get the time dilution of precision, a dimensionless factor which should
	 * be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the timestamp DOP
	 */
	public Number getTimestampDop() {
		return timestampDop;
	}

	/**
	 * Get the horizontal dilution of precision, a dimensionless factor which
	 * should be multiplied by a base UERE to get a circular error estimate.
	 * 
	 * @return the horizontal DOP
	 */
	public Number getHorizontalDop() {
		return horizontalDop;
	}

	/**
	 * Get the spherical dilution of precision, a dimensionless factor which
	 * should be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the spherical DOP
	 */
	public Number getSphericalDop() {
		return sphericalDop;
	}

	/**
	 * Get the hyperspherical dilution of precision, a dimensionless factor
	 * which should be multiplied by a base UERE to get an error estimate.
	 * 
	 * @return the hyperspherical DOP
	 */
	public Number getHypersphericalDop() {
		return hypersphericalDop;
	}

	/**
	 * Get the list of satellite objects in sky view.
	 * 
	 * @return the satellites
	 */
	public Iterable<SatelliteInfo> getSatellites() {
		return satellites;
	}

}
