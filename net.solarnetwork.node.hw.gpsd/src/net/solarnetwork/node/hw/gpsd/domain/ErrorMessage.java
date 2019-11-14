/* ==================================================================
 * ErrorMessage.java - 14/11/2019 4:54:36 pm
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

import static net.solarnetwork.node.hw.gpsd.util.ErrorMessageSerializer.MESSAGE_FIELD;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Message for the {@literal ERROR}.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = ErrorMessage.Builder.class)
@JsonSerialize(using = net.solarnetwork.node.hw.gpsd.util.ErrorMessageSerializer.class)
public class ErrorMessage extends AbstractGpsdMessage {

	private final String message;

	private ErrorMessage(Builder builder) {
		super(GpsdMessageType.Error);
		this.message = builder.message;
	}

	/**
	 * Creates builder to build {@link ErrorMessage}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link ErrorMessage}.
	 */
	public static final class Builder implements GpsdMessageJsonParser<ErrorMessage> {

		private String message;

		private Builder() {
		}

		public Builder withMessage(String message) {
			this.message = message;
			return this;
		}

		public ErrorMessage build() {
			return new ErrorMessage(this);
		}

		@Override
		public ErrorMessage parseJsonTree(TreeNode node) {
			if ( !(node instanceof JsonNode) ) {
				return null;
			}
			JsonNode json = (JsonNode) node;
			return withMessage(json.path(MESSAGE_FIELD).textValue()).build();
		}

	}

	/**
	 * Get the message.
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

}
