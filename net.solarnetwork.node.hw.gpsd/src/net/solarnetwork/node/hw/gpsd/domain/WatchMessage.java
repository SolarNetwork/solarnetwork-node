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

package net.solarnetwork.node.hw.gpsd.domain;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Message for the {@literal WATCH} command.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = WatchMessage.Builder.class)
public class WatchMessage extends AbstractGpsdMessage {

	private final boolean enable;
	private final boolean dumpJson;
	private final boolean scaled;
	private final String device;

	private WatchMessage(Builder builder) {
		super(GpsdMessageType.Watch, null);
		this.enable = builder.enable;
		this.dumpJson = builder.dumpJson;
		this.scaled = builder.scaled;
		this.device = builder.device;
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
	public static final class Builder implements GpsdMessageJsonParser<WatchMessage> {

		private boolean enable = true;
		private boolean dumpJson = false;
		private boolean scaled = false;
		private String device;

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

		public Builder withScaled(boolean scaled) {
			this.scaled = scaled;
			return this;
		}

		public Builder withDevice(String device) {
			this.device = device;
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
			return withDevice(json.path("device").textValue())
					.withEnable(json.path("enable").booleanValue())
					.withDumpJson(json.path("json").booleanValue())
					.withScaled(json.path("scaled").booleanValue()).build();
		}

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

}
