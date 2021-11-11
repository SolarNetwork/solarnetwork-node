/* ==================================================================
 * VersionMessageSerializer.java - 14/11/2019 3:58:06 pm
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

package net.solarnetwork.node.io.gpsd.util;

import static net.solarnetwork.codec.JsonUtils.writeNumberField;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;

/**
 * Serializer for {@link VersionMessage} objects.
 * 
 * @author matt
 * @version 2.0
 */
public class VersionMessageSerializer extends AbstractGpsdMessageSerializer<VersionMessage> {

	private static final long serialVersionUID = -4464195785843259035L;

	public static final String RELEASE_FIELD = "release";
	public static final String REVISION_FIELD = "rev";
	public static final String PROTOCOL_MAJOR_FIELD = "proto_major";
	public static final String PROTOCOL_MINOR_FIELD = "proto_minor";
	public static final String REMOTE_URL_FIELD = "remote";

	/**
	 * Constructor.
	 */
	public VersionMessageSerializer() {
		super(VersionMessage.class);
	}

	@Override
	protected void serializeFields(VersionMessage value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value.getRelease() != null && !value.getRelease().isEmpty() ) {
			gen.writeStringField(RELEASE_FIELD, value.getRelease());
		}
		if ( value.getRevision() != null && !value.getRevision().isEmpty() ) {
			gen.writeStringField(REVISION_FIELD, value.getRevision());
		}
		writeNumberField(gen, PROTOCOL_MAJOR_FIELD, value.getProtocolMajor());
		writeNumberField(gen, PROTOCOL_MINOR_FIELD, value.getProtocolMinor());
		if ( value.getRemoteUrl() != null && !value.getRemoteUrl().isEmpty() ) {
			gen.writeStringField(REMOTE_URL_FIELD, value.getRemoteUrl());
		}
	}

}
