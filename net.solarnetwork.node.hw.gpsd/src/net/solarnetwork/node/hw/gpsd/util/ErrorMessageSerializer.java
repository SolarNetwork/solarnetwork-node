/* ==================================================================
 * ErrorMessageSerializer.java - 14/11/2019 4:56:29 pm
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

package net.solarnetwork.node.hw.gpsd.util;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.node.hw.gpsd.domain.ErrorMessage;

/**
 * Serializer for {@link ErrorMessage} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class ErrorMessageSerializer extends AbstractGpsdMessageSerializer<ErrorMessage> {

	private static final long serialVersionUID = -1497497250207683344L;

	public static final String MESSAGE_FIELD = "message";

	/**
	 * Constructor.
	 */
	public ErrorMessageSerializer() {
		super(ErrorMessage.class);
	}

	@Override
	protected void serializeFields(ErrorMessage value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		if ( value.getMessage() != null && !value.getMessage().isEmpty() ) {
			gen.writeStringField(MESSAGE_FIELD, value.getMessage());
		}
	}

}
