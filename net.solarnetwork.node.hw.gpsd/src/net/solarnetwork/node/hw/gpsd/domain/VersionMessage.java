/* ==================================================================
 * VersionMessage.java - 13/11/2019 2:49:10 pm
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

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Message for the {@literal VERSION} command.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = VersionMessage.Builder.class)
public class VersionMessage extends AbstractGpsdMessage {

	private final String release;
	private final String revision;
	private final Number protocolMajor;
	private final Number protocolMinor;
	private final String remoteUrl;

	private VersionMessage(Builder builder) {
		super(GpsdMessageType.Version);
		this.release = builder.release;
		this.revision = builder.revision;
		this.protocolMajor = builder.protocolMajor;
		this.protocolMinor = builder.protocolMinor;
		this.remoteUrl = builder.remoteUrl;
	}

	/**
	 * Creates builder to build {@link VersionMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link VersionMessage}.
	 */
	public static final class Builder implements GpsdMessageJsonParser<VersionMessage> {

		private String release;
		private String revision;
		private Number protocolMajor;
		private Number protocolMinor;
		private String remoteUrl;

		private Builder() {
		}

		public Builder withRelease(String release) {
			this.release = release;
			return this;
		}

		public Builder withRevision(String revision) {
			this.revision = revision;
			return this;
		}

		public Builder withProtocolMajor(Number protocolMajor) {
			this.protocolMajor = protocolMajor;
			return this;
		}

		public Builder withProtocolMinor(Number protocolMinor) {
			this.protocolMinor = protocolMinor;
			return this;
		}

		public Builder withRemoteUrl(String remoteUrl) {
			this.remoteUrl = remoteUrl;
			return this;
		}

		public VersionMessage build() {
			return new VersionMessage(this);
		}

		@Override
		public VersionMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withRelease(json.path("release").textValue())
					.withRevision(json.path("rev").textValue())
					.withProtocolMajor(json.path("proto_major").numberValue())
					.withProtocolMinor(json.path("proto_minor").numberValue())
					.withRemoteUrl(json.path("remote").textValue()).build();
		}

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("VersionMessage{release=");
		buf.append(release);
		buf.append(", revision=");
		buf.append(revision);
		buf.append(", protocolMajor=");
		buf.append(protocolMajor);
		buf.append(", protocolMinor=");
		buf.append(protocolMinor);
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
				+ Objects.hash(protocolMajor, protocolMinor, release, remoteUrl, revision);
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
		if ( !(obj instanceof VersionMessage) ) {
			return false;
		}
		VersionMessage other = (VersionMessage) obj;
		return Objects.equals(protocolMajor, other.protocolMajor)
				&& Objects.equals(protocolMinor, other.protocolMinor)
				&& Objects.equals(release, other.release) && Objects.equals(remoteUrl, other.remoteUrl)
				&& Objects.equals(revision, other.revision);
	}

	/**
	 * Get the public release version.
	 * 
	 * @return the release
	 */
	@JsonProperty("release")
	public String getRelease() {
		return release;
	}

	/**
	 * Get the internal revision-control level.
	 * 
	 * @return the revision
	 */
	@JsonProperty("rev")
	public String getRevision() {
		return revision;
	}

	/**
	 * Get the API major revision level.
	 * 
	 * @return the protocol major revision
	 */
	@JsonProperty("proto_major")
	public Number getProtocolMajor() {
		return protocolMajor;
	}

	/**
	 * Get the API minor revision level.
	 * 
	 * @return the protocol minor revision
	 */
	@JsonProperty("proto_minor")
	public Number getProtocolMinor() {
		return protocolMinor;
	}

	/**
	 * Get the URL of the remote daemon reporting this version.
	 * 
	 * <p>
	 * If empty, this is the version of the local daemon.
	 * </p>
	 * 
	 * @return the remote URL
	 */
	@JsonProperty("remote")
	public String getRemoteUrl() {
		return remoteUrl;
	}

}
