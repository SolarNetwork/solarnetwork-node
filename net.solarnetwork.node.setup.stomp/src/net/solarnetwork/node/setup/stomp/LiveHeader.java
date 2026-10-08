/* ==================================================================
 * LiveHeader.java - 10/10/2026 1:42:00 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp;

/**
 * Headers used by live datum subscriptions.
 *
 * <p>
 * These are not part of {@link SetupHeader} because those names are stripped
 * from {@literal SEND} frame instruction parameters.
 * </p>
 *
 * @author elijah
 * @version 1.0
 * @since 4.1
 */
public enum LiveHeader {

	/** The source ID to stream datum for. */
	SourceId("source-id", "The source ID of the datum stream."),

	/** A comma-delimited list of datum property names to include. */
	Properties("A comma-delimited list of datum property names to include."),

	/** The minimum interval between messages, in milliseconds. */
	Interval("The minimum interval between messages, in milliseconds."),

	;

	private final String value;
	private final String description;

	private LiveHeader(String description) {
		this.value = this.name().toLowerCase();
		this.description = description;
	}

	private LiveHeader(String value, String description) {
		this.value = value;
		this.description = description;
	}

	/**
	 * Get the header value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get a description of the header.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
