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

package net.solarnetwork.node.io.gpsd.domain;

import static net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.PROTOCOL_MAJOR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.PROTOCOL_MINOR_FIELD;
import static net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.RELEASE_FIELD;
import static net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.REMOTE_URL_FIELD;
import static net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.REVISION_FIELD;
import java.util.Objects;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Message for the {@literal VERSION} command.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = VersionMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.io.gpsd.util.VersionMessageSerializer.class)
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
	public static final class Builder implements GpsdJsonParser<VersionMessage> {

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
			return withRelease(json.path(RELEASE_FIELD).textValue())
					.withRevision(json.path(REVISION_FIELD).textValue())
					.withProtocolMajor(json.path(PROTOCOL_MAJOR_FIELD).numberValue())
					.withProtocolMinor(json.path(PROTOCOL_MINOR_FIELD).numberValue())
					.withRemoteUrl(json.path(REMOTE_URL_FIELD).textValue()).build();
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
	public String getRelease() {
		return release;
	}

	/**
	 * Get the internal revision-control level.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return revision;
	}

	/**
	 * Get the API major revision level.
	 * 
	 * @return the protocol major revision
	 */
	public Number getProtocolMajor() {
		return protocolMajor;
	}

	/**
	 * Get the API minor revision level.
	 * 
	 * @return the protocol minor revision
	 */
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
	public String getRemoteUrl() {
		return remoteUrl;
	}

}
