/* ==================================================================
 * TpvReportMessage.java - 12/11/2019 6:13:54 am
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

package net.solarnetwork.node.hw.gpsd.domain;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A time-position-velocity report message.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = TpvReportMessage.Builder.class)
public class TpvReportMessage extends AbstractGpsdMessage {

	private final String device;
	private final NmeaMode mode;
	private final Instant timestamp;
	private final Number timestampError;
	private final Number latitude;
	private final Number longitude;
	private final Number altitude;
	private final Number latitudeError;
	private final Number longitudeError;
	private final Number altitudeError;
	private final Number course;
	private final Number speed;
	private final Number climbRate;
	private final Number courseError;
	private final Number speedError;
	private final Number climbRateError;

	private TpvReportMessage(Builder builder) {
		super(GpsdMessageType.TpvReport, null);
		this.device = builder.device;
		this.mode = builder.mode;
		this.timestamp = builder.timestamp;
		this.timestampError = builder.timestampError;
		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.altitude = builder.altitude;
		this.latitudeError = builder.latitudeError;
		this.longitudeError = builder.longitudeError;
		this.altitudeError = builder.altitudeError;
		this.course = builder.course;
		this.speed = builder.speed;
		this.climbRate = builder.climbRate;
		this.courseError = builder.courseError;
		this.speedError = builder.speedError;
		this.climbRateError = builder.climbRateError;
	}

	/**
	 * Creates builder to build {@link TpvReportMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link TpvReportMessage}.
	 */
	public static final class Builder implements GpsdMessageJsonParser<TpvReportMessage> {

		private String device;
		private NmeaMode mode;
		private Instant timestamp;
		private Number timestampError;
		private Number latitude;
		private Number longitude;
		private Number altitude;
		private Number longitudeError;
		private Number latitudeError;
		private Number altitudeError;
		private Number course;
		private Number speed;
		private Number climbRate;
		private Number courseError;
		private Number speedError;
		private Number climbRateError;

		private Builder() {
		}

		public Builder withDevice(String device) {
			this.device = device;
			return this;
		}

		public Builder withMode(NmeaMode mode) {
			this.mode = mode;
			return this;
		}

		public Builder withTimestamp(String timestamp) {
			Instant ts = null;
			if ( timestamp != null && !timestamp.isEmpty() ) {
				try {
					ts = Instant.parse(timestamp);
				} catch ( DateTimeParseException e ) {
					// ignore
				}
			}
			return withTimestamp(ts);
		}

		public Builder withTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder withTimestampError(Number timestampError) {
			this.timestampError = timestampError;
			return this;
		}

		public Builder withLatitude(Number latitude) {
			this.latitude = latitude;
			return this;
		}

		public Builder withLongitude(Number longitude) {
			this.longitude = longitude;
			return this;
		}

		public Builder withAltitude(Number altitude) {
			this.altitude = altitude;
			return this;
		}

		public Builder withLongitudeError(Number longitudeError) {
			this.longitudeError = longitudeError;
			return this;
		}

		public Builder withLatitudeError(Number latitudeError) {
			this.latitudeError = latitudeError;
			return this;
		}

		public Builder withAltitudeError(Number altitudeError) {
			this.altitudeError = altitudeError;
			return this;
		}

		public Builder withCourse(Number course) {
			this.course = course;
			return this;
		}

		public Builder withSpeed(Number speed) {
			this.speed = speed;
			return this;
		}

		public Builder withClimbRate(Number climbRate) {
			this.climbRate = climbRate;
			return this;
		}

		public Builder withCourseError(Number courseError) {
			this.courseError = courseError;
			return this;
		}

		public Builder withSpeedError(Number speedError) {
			this.speedError = speedError;
			return this;
		}

		public Builder withClimbRateError(Number climbRateError) {
			this.climbRateError = climbRateError;
			return this;
		}

		public TpvReportMessage build() {
			return new TpvReportMessage(this);
		}

		@Override
		public TpvReportMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withDevice(json.path("device").textValue())
					.withMode(NmeaMode.forCode(json.path("mode").intValue()))
					.withTimestamp(json.path("time").textValue())
					.withTimestampError(json.path("ept").numberValue())
					.withLatitude(json.path("lat").numberValue())
					.withLongitude(json.path("lon").numberValue())
					.withAltitude(json.path("alt").numberValue())
					.withLongitudeError(json.path("epx").numberValue())
					.withLatitudeError(json.path("epy").numberValue())
					.withAltitudeError(json.path("epv").numberValue())
					.withCourse(json.path("track").numberValue())
					.withSpeed(json.path("speed").numberValue())
					.withClimbRate(json.path("climb").numberValue())
					.withCourseError(json.path("epd").numberValue())
					.withSpeedError(json.path("eps").numberValue())
					.withClimbRateError(json.path("epc").numberValue()).build();
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(device, mode, timestamp);
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
		if ( !(obj instanceof TpvReportMessage) ) {
			return false;
		}
		TpvReportMessage other = (TpvReportMessage) obj;
		return Objects.equals(device, other.device) && mode == other.mode
				&& Objects.equals(timestamp, other.timestamp);
	}

	/**
	 * Get the name of originating device.
	 * 
	 * @return the device name, or {@literal null}
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * Get the NMEA mode.
	 * 
	 * @return the mode, never {@literal null}
	 */
	public NmeaMode getMode() {
		return mode;
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the timestamp, or {@literal null}
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Get the estimated timestamp error in seconds, 95% confidence.
	 * 
	 * @return the timestamp error, or {@literal null}
	 */
	public Number getTimestampError() {
		return timestampError;
	}

	/**
	 * Get the latitude in degrees, where +/- signifies North/South.
	 * 
	 * @return the latitude, or {@literal null}
	 */
	public Number getLatitude() {
		return latitude;
	}

	/**
	 * Get the longitude in degrees, where +/- signifies East/West.
	 * 
	 * @return the longitude, or {@literal null}
	 */
	public Number getLongitude() {
		return longitude;
	}

	/**
	 * Get the altitude in meters.
	 * 
	 * @return the altitude, or {@literal null}
	 */
	public Number getAltitude() {
		return altitude;
	}

	/**
	 * Get the longitude error estimate in meters, 95% confidence.
	 * 
	 * @return the longitude error, or {@literal null}
	 */
	public Number getLongitudeError() {
		return longitudeError;
	}

	/**
	 * Get the latitude error estimate in meters, 95% confidence.
	 * 
	 * @return the latitude error, or {@literal null}
	 */
	public Number getLatitudeError() {
		return latitudeError;
	}

	/**
	 * Get the vertical error estimate in meters, 95% confidence.
	 * 
	 * @return the altitude error, or {@literal null}
	 */
	public Number getAltitudeError() {
		return altitudeError;
	}

	/**
	 * Get the course over ground, in degrees from true north.
	 * 
	 * @return the course, or {@literal null}
	 */
	public Number getCourse() {
		return course;
	}

	/**
	 * Get the speed over ground, in meters per second.
	 * 
	 * @return the speed, or {@literal null}
	 */
	public Number getSpeed() {
		return speed;
	}

	/**
	 * Get the climb (positive) or sink (negative) rate, in meters per second.
	 * 
	 * @return the climb rate, or {@literal null}
	 */
	public Number getClimbRate() {
		return climbRate;
	}

	/**
	 * Get the direction error estimate in degrees, 95% confidence.
	 * 
	 * @return the course error, or {@literal null}
	 */
	public Number getCourseError() {
		return courseError;
	}

	/**
	 * Get the speed error estimate in meters per second, 95% confidence.
	 * 
	 * @return the speed error, or {@literal null}
	 */
	public Number getSpeedError() {
		return speedError;
	}

	/**
	 * Get the climb/sink error estimate in meters per second, 95% confidence.
	 * 
	 * @return the climb rate error, or {@literal null}
	 */
	public Number getClimbRateError() {
		return climbRateError;
	}

}
