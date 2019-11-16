/* ==================================================================
 * GpsdMessageType.java - 11/11/2019 9:19:44 pm
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

import java.util.HashMap;
import java.util.Map;

/**
 * GSPd commands.
 * 
 * @author matt
 * @version 1.0
 */
public enum GpsdMessageType {

	Unknown("!"),

	/** A single device. */
	Device("DEVICE"),

	/** A list of devices. */
	Devices("DEVICES"),

	/** An error. */
	Error("ERROR"),

	/** Most recent data from connected devices. */
	Poll("POLL"),

	/** SkyReport view of GPS positions. */
	SkyReport("SKY"),

	/** A time-position-velocity report. */
	TpvReport("TPV"),

	/** GPSd version information. */
	Version("VERSION"),

	/** Watch mode configuration. */
	Watch("WATCH");

	private static final Map<String, GpsdMessageType> nameMapping = createNameMapping();

	private static Map<String, GpsdMessageType> createNameMapping() {
		GpsdMessageType[] types = GpsdMessageType.values();
		Map<String, GpsdMessageType> m = new HashMap<>(types.length);
		for ( GpsdMessageType t : types ) {
			if ( t == Unknown ) {
				continue;
			}
			m.put(t.name, t);
		}
		return m;
	}

	private final String name;

	private GpsdMessageType(String name) {
		this.name = name;
	}

	/**
	 * Get the message name value.
	 * 
	 * @return the message name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get an enumeration instance for a name value.
	 * 
	 * <p>
	 * This method will return {@link GpsdMessageType#Unknown} for any
	 * {@code name} that is not a known message name value.
	 * </p>
	 * 
	 * @param name
	 *        the name value to get the enumeration for
	 * @return the enumeration, never {@literal null}
	 */
	public static GpsdMessageType forName(String name) {
		GpsdMessageType type = nameMapping.get(name);
		return (type != null ? type : Unknown);
	}

}
