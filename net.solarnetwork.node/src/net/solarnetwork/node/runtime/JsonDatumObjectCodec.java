/* ==================================================================
 * JsonDatumObjectCodec.java - 11/09/2023 3:30:16 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.codec.ObjectCodec;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.node.service.support.BaseIdentifiable;

/**
 * Simple {@link ObjectCodec} for {@link Datum} objects.
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public class JsonDatumObjectCodec extends BaseIdentifiable implements ObjectCodec {

	private final ObjectMapper mapper;

	/**
	 * Constructor.
	 */
	public JsonDatumObjectCodec() {
		super();
		setUid("JSON Datum Codec");
		setDisplayName("JSON Daum Codec");
		this.mapper = JsonUtils.newDatumObjectMapper();
	}

	@Override
	public byte[] encodeAsBytes(Object obj, Map<String, ?> parameters) throws IOException {
		return mapper.writeValueAsBytes(obj);
	}

	@Override
	public Object decodeFromBytes(byte[] data, Map<String, ?> parameters) throws IOException {
		return mapper.readValue(data, Datum.class);
	}

}
