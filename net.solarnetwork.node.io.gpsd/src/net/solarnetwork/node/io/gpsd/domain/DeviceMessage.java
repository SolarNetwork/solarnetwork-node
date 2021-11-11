/* ==================================================================
 * DeviceMessage.java - 14/11/2019 2:52:44 pm
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

import static net.solarnetwork.codec.JsonUtils.iso8610Timestamp;
import static net.solarnetwork.domain.Bitmaskable.setForBitmask;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.ACTIVATED_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.BPS_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.CYCLE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.DRIVER_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.FLAGS_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.MINCYCLE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.NATIVE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.PARITY_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.PATH_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.STOPBITS_FIELD;
import static net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.SUBTYPE_FIELD;
import java.time.Instant;
import java.util.Set;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Message for the {@literal DEVICE} command.
 * 
 * @author matt
 * @version 2.0
 */
@JsonDeserialize(builder = DeviceMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.io.gpsd.util.DeviceMessageSerializer.class)
public class DeviceMessage extends AbstractGpsdMessage {

	private final String path;
	private final Instant activated;
	private final Set<DeviceFlags> flags;
	private final String driver;
	private final String subtype;
	private final Number bitsPerSecond;
	private final Parity parity;
	private final Number stopbits;
	private final boolean nativeMode;
	private final Number cycleSeconds;
	private final Number minimumCycleSeconds;

	private DeviceMessage(Builder builder) {
		super(GpsdMessageType.Device);
		this.path = builder.path;
		this.activated = builder.activated;
		this.flags = builder.flags;
		this.driver = builder.driver;
		this.subtype = builder.subtype;
		this.bitsPerSecond = builder.bitsPerSecond;
		this.parity = builder.parity;
		this.stopbits = builder.stopbits;
		this.nativeMode = builder.nativeMode;
		this.cycleSeconds = builder.cycleSeconds;
		this.minimumCycleSeconds = builder.minimumCycleSeconds;
	}

	/**
	 * Creates builder to build {@link DeviceMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link DeviceMessage}.
	 */
	public static final class Builder implements GpsdJsonParser<DeviceMessage> {

		private String path;
		private Instant activated;
		private Set<DeviceFlags> flags;
		private String driver;
		private String subtype;
		private Number bitsPerSecond;
		private Parity parity;
		private Number stopbits;
		private boolean nativeMode;
		private Number cycleSeconds;
		private Number minimumCycleSeconds;

		private Builder() {
		}

		public Builder withPath(String path) {
			this.path = path;
			return this;
		}

		public Builder withActivated(String timestamp) {
			return withActivated(iso8610Timestamp(timestamp));
		}

		public Builder withActivated(Instant activated) {
			this.activated = activated;
			return this;
		}

		public Builder withFlags(Number flags) {
			if ( flags == null ) {
				return this;
			}
			return withFlags(setForBitmask(flags.intValue(), DeviceFlags.class));
		}

		public Builder withFlags(Set<DeviceFlags> flags) {
			this.flags = flags;
			return this;
		}

		public Builder withDriver(String driver) {
			this.driver = driver;
			return this;
		}

		public Builder withSubtype(String subtype) {
			this.subtype = subtype;
			return this;
		}

		public Builder withBitsPerSecond(Number bitsPerSecond) {
			this.bitsPerSecond = bitsPerSecond;
			return this;
		}

		public Builder withParity(String parity) {
			try {
				withParity(Parity.forKey(parity));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
			return this;
		}

		public Builder withParity(Parity parity) {
			this.parity = parity;
			return this;
		}

		public Builder withStopbits(Number stopbits) {
			this.stopbits = stopbits;
			return this;
		}

		public Builder withNativeMode(Number nativeMode) {
			boolean b = (nativeMode != null && nativeMode.intValue() == 1);
			return withNativeMode(b);
		}

		public Builder withNativeMode(boolean nativeMode) {
			this.nativeMode = nativeMode;
			return this;
		}

		public Builder withCycleSeconds(Number cycleSeconds) {
			this.cycleSeconds = cycleSeconds;
			return this;
		}

		public Builder withMinimumCycleSeconds(Number minimumCycleSeconds) {
			this.minimumCycleSeconds = minimumCycleSeconds;
			return this;
		}

		public DeviceMessage build() {
			return new DeviceMessage(this);
		}

		@Override
		public DeviceMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withPath(json.path(PATH_FIELD).textValue())
					.withActivated(json.path(ACTIVATED_FIELD).textValue())
					.withFlags(json.path(FLAGS_FIELD).numberValue())
					.withDriver(json.path(DRIVER_FIELD).textValue())
					.withSubtype(json.path(SUBTYPE_FIELD).textValue())
					.withBitsPerSecond(json.path(BPS_FIELD).numberValue())
					.withParity(json.path(PARITY_FIELD).textValue())
					.withStopbits(json.path(STOPBITS_FIELD).numberValue())
					.withNativeMode(json.path(NATIVE_FIELD).numberValue())
					.withCycleSeconds(json.path(CYCLE_FIELD).numberValue())
					.withMinimumCycleSeconds(json.path(MINCYCLE_FIELD).numberValue()).build();
		}

	}

	/**
	 * Get the name the device for which the control bits are being reported, or
	 * for which they are to be applied.
	 * 
	 * <p>
	 * This attribute may be omitted only when there is exactly one subscribed
	 * channel.
	 * </p>
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the time the device was activated.
	 * 
	 * <p>
	 * If the device is inactive this attribute is absent.
	 * </p>
	 * 
	 * @return the activated
	 */
	public Instant getActivated() {
		return activated;
	}

	/**
	 * Get the set of property flags.
	 * 
	 * <p>
	 * Won't be reported if empty, e.g. before gpsd has seen identifiable
	 * packets from the device.
	 * </p>
	 * 
	 * @return the flags
	 */
	public Set<DeviceFlags> getFlags() {
		return flags;
	}

	/**
	 * Get GPSD's name for the device driver type.
	 * 
	 * <p>
	 * This won't be reported before gpsd has seen identifiable packets from the
	 * device.
	 * </p>
	 * 
	 * @return the driver
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * Get the version information the device returned.
	 * 
	 * @return the subtype
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * Get the device communication speed, in bits per second.
	 * 
	 * @return the speed
	 */
	public Number getBitsPerSecond() {
		return bitsPerSecond;
	}

	/**
	 * Get the parity.
	 * 
	 * @return the parity
	 */
	public Parity getParity() {
		return parity;
	}

	/**
	 * Get the number of stop bits
	 * 
	 * @return the number of stop bits
	 */
	public Number getStopbits() {
		return stopbits;
	}

	/**
	 * Get the NMEA versus alternate mode flag.
	 * 
	 * <p>
	 * Alternate mode means binary if it has one, for SiRF and Evermore chipsets
	 * in particular.
	 * </p>
	 * 
	 * @return {@literal true} for NMEA mode, {@literal false} for alternate
	 *         mode
	 */
	public boolean isNativeMode() {
		return nativeMode;
	}

	/**
	 * Get the device cycle time in seconds.
	 * 
	 * @return the seconds
	 */
	public Number getCycleSeconds() {
		return cycleSeconds;
	}

	/**
	 * Get the device minimum cycle time in seconds.
	 * 
	 * <p>
	 * Reported from {@literal ?DEVICE} when (and only when) the rate is
	 * switchable. It is read-only and not settable.
	 * </p>
	 * 
	 * @return the seconds
	 */
	public Number getMinimumCycleSeconds() {
		return minimumCycleSeconds;
	}

}
