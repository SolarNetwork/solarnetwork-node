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

package net.solarnetwork.node.io.gpsd.domain;

import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.ALTITUDE_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.ALTITUDE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.CLIMB_RATE_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.CLIMB_RATE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.COURSE_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.COURSE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.DEVICE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.LATITUDE_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.LATITUDE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.LONGITUDE_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.LONGITUDE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.MODE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.SPEED_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.SPEED_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.TIMESTAMP_ERROR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.TIMESTAMP_FIELD;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.solarnetwork.node.io.gpsd.util.JsonUtils;

/**
 * A time-position-velocity report message.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = TpvReportMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.io.gpsd.util.TpvReportMessageSerializer.class)
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
	public static final class Builder implements GpsdJsonParser<TpvReportMessage> {

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
			return withTimestamp(JsonUtils.iso8610Timestamp(timestamp));
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
			return withDevice(json.path(DEVICE_FIELD).textValue())
					.withMode(NmeaMode.forCode(json.path(MODE_FIELD).intValue()))
					.withTimestamp(json.path(TIMESTAMP_FIELD).textValue())
					.withTimestampError(json.path(TIMESTAMP_ERROR_FIELD).numberValue())
					.withLatitude(json.path(LATITUDE_FIELD).numberValue())
					.withLongitude(json.path(LONGITUDE_FIELD).numberValue())
					.withAltitude(json.path(ALTITUDE_FIELD).numberValue())
					.withLongitudeError(json.path(LONGITUDE_ERROR_FIELD).numberValue())
					.withLatitudeError(json.path(LATITUDE_ERROR_FIELD).numberValue())
					.withAltitudeError(json.path(ALTITUDE_ERROR_FIELD).numberValue())
					.withCourse(json.path(COURSE_FIELD).numberValue())
					.withSpeed(json.path(SPEED_FIELD).numberValue())
					.withClimbRate(json.path(CLIMB_RATE_FIELD).numberValue())
					.withCourseError(json.path(COURSE_ERROR_FIELD).numberValue())
					.withSpeedError(json.path(SPEED_ERROR_FIELD).numberValue())
					.withClimbRateError(json.path(CLIMB_RATE_ERROR_FIELD).numberValue()).build();
		}

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("TpvReportMessage{");
		if ( device != null ) {
			buf.append("device=");
			buf.append(device);
			buf.append(", ");
		}
		if ( mode != null ) {
			buf.append("mode=");
			buf.append(mode);
			buf.append(", ");
		}
		if ( timestamp != null ) {
			buf.append("timestamp=");
			buf.append(timestamp);
			buf.append(", ");
		}
		if ( timestampError != null ) {
			buf.append("timestampError=");
			buf.append(timestampError);
			buf.append(", ");
		}
		if ( latitude != null ) {
			buf.append("latitude=");
			buf.append(latitude);
			buf.append(", ");
		}
		if ( longitude != null ) {
			buf.append("longitude=");
			buf.append(longitude);
			buf.append(", ");
		}
		if ( altitude != null ) {
			buf.append("altitude=");
			buf.append(altitude);
			buf.append(", ");
		}
		if ( latitudeError != null ) {
			buf.append("latitudeError=");
			buf.append(latitudeError);
			buf.append(", ");
		}
		if ( longitudeError != null ) {
			buf.append("longitudeError=");
			buf.append(longitudeError);
			buf.append(", ");
		}
		if ( altitudeError != null ) {
			buf.append("altitudeError=");
			buf.append(altitudeError);
			buf.append(", ");
		}
		if ( course != null ) {
			buf.append("course=");
			buf.append(course);
			buf.append(", ");
		}
		if ( speed != null ) {
			buf.append("speed=");
			buf.append(speed);
			buf.append(", ");
		}
		if ( climbRate != null ) {
			buf.append("climbRate=");
			buf.append(climbRate);
			buf.append(", ");
		}
		if ( courseError != null ) {
			buf.append("courseError=");
			buf.append(courseError);
			buf.append(", ");
		}
		if ( speedError != null ) {
			buf.append("speedError=");
			buf.append(speedError);
			buf.append(", ");
		}
		if ( climbRateError != null ) {
			buf.append("climbRateError=");
			buf.append(climbRateError);
		}
		buf.append("}");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(altitude, altitudeError, climbRate, climbRateError,
				course, courseError, device, latitude, latitudeError, longitude, longitudeError, mode,
				speed, speedError, timestamp, timestampError);
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
		return Objects.equals(altitude, other.altitude)
				&& Objects.equals(altitudeError, other.altitudeError)
				&& Objects.equals(climbRate, other.climbRate)
				&& Objects.equals(climbRateError, other.climbRateError)
				&& Objects.equals(course, other.course) && Objects.equals(courseError, other.courseError)
				&& Objects.equals(device, other.device) && Objects.equals(latitude, other.latitude)
				&& Objects.equals(latitudeError, other.latitudeError)
				&& Objects.equals(longitude, other.longitude)
				&& Objects.equals(longitudeError, other.longitudeError) && mode == other.mode
				&& Objects.equals(speed, other.speed) && Objects.equals(speedError, other.speedError)
				&& Objects.equals(timestamp, other.timestamp)
				&& Objects.equals(timestampError, other.timestampError);
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
