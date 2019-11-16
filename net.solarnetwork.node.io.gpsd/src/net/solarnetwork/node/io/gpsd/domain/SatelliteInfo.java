/* ==================================================================
 * SatelliteInfo.java - 15/11/2019 10:35:57 am
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

import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.AZIMUTH_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.ELEVATION_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.PRN_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.SIGNAL_STRENGTH_FIELD;
import static net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer.USED_FIELD;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.solarnetwork.node.io.gpsd.util.SatelliteInfoDeserializer;
import net.solarnetwork.node.io.gpsd.util.SatelliteInfoSerializer;

/**
 * Details on a GPS satellite.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(using = SatelliteInfoDeserializer.class)
@JsonSerialize(using = SatelliteInfoSerializer.class)
public class SatelliteInfo {

	private final Number prn;
	private final Number azimuth;
	private final Number elevation;
	private final Number signalStrength;
	private final boolean used;

	private SatelliteInfo(Builder builder) {
		this.prn = builder.prn;
		this.azimuth = builder.azimuth;
		this.elevation = builder.elevation;
		this.signalStrength = builder.signalStrength;
		this.used = builder.used;
	}

	/**
	 * Creates builder to build {@link SatelliteInfo}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link SatelliteInfo}.
	 */
	public static final class Builder implements GpsdJsonParser<SatelliteInfo> {

		private Number prn;
		private Number azimuth;
		private Number elevation;
		private Number signalStrength;
		private boolean used;

		private Builder() {
		}

		public Builder withPrn(Number prn) {
			this.prn = prn;
			return this;
		}

		public Builder withAzimuth(Number azimuth) {
			this.azimuth = azimuth;
			return this;
		}

		public Builder withElevation(Number elevation) {
			this.elevation = elevation;
			return this;
		}

		public Builder withSignalStrength(Number signalStrength) {
			this.signalStrength = signalStrength;
			return this;
		}

		public Builder withUsed(boolean used) {
			this.used = used;
			return this;
		}

		public SatelliteInfo build() {
			return new SatelliteInfo(this);
		}

		@Override
		public SatelliteInfo parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withPrn(json.path(PRN_FIELD).numberValue())
					.withAzimuth(json.path(AZIMUTH_FIELD).numberValue())
					.withElevation(json.path(ELEVATION_FIELD).numberValue())
					.withSignalStrength(json.path(SIGNAL_STRENGTH_FIELD).numberValue())
					.withUsed(json.path(USED_FIELD).booleanValue()).build();
		}

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("SatelliteInfo{prn=");
		buf.append(prn);
		buf.append(", azimuth=");
		buf.append(azimuth);
		buf.append(", elevation=");
		buf.append(elevation);
		buf.append(", signalStrength=");
		buf.append(signalStrength);
		buf.append(", used=");
		buf.append(used);
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(azimuth, elevation, prn, signalStrength, used);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof SatelliteInfo) ) {
			return false;
		}
		SatelliteInfo other = (SatelliteInfo) obj;
		return Objects.equals(azimuth, other.azimuth) && Objects.equals(elevation, other.elevation)
				&& Objects.equals(prn, other.prn) && Objects.equals(signalStrength, other.signalStrength)
				&& used == other.used;
	}

	/**
	 * Get the pseudo-random-number ID of the satellite.
	 * 
	 * <p>
	 * 1-63 are GNSS satellites, 64-96 are GLONASS satellites, 100-164 are SBAS
	 * satellites.
	 * </p>
	 * 
	 * @return the prn
	 */
	public Number getPrn() {
		return prn;
	}

	/**
	 * Get the degrees from true north.
	 * 
	 * @return the azimuth
	 */
	public Number getAzimuth() {
		return azimuth;
	}

	/**
	 * Get the elevation in degrees.
	 * 
	 * @return the elevation
	 */
	public Number getElevation() {
		return elevation;
	}

	/**
	 * Get the signal strength in dB.
	 * 
	 * @return the signal strength
	 */
	public Number getSignalStrength() {
		return signalStrength;
	}

	/**
	 * Get the flag indicating if the satellite info is used in the current
	 * solution.
	 * 
	 * @return the used
	 */
	public boolean isUsed() {
		return used;
	}

}
