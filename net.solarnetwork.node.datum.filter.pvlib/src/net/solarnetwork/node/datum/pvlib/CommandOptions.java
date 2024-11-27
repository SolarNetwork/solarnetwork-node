/* ==================================================================
 * CommandOptions.java - 16/11/2024 6:22:01 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.pvlib;

/**
 * Enumeration of command options with associated metadata keys.
 *
 * @author matt
 * @version 1.1
 */
public enum CommandOptions {

	/** A decimal latitude value. */
	Latitude("--latitude", "lat"),

	/** a decimal longitude value. */
	Lonitude("--longitude", "lon"),

	/** A decimal altitude value, in meters above sea level. */
	Altitude("--altitude", "alt"),

	/** A time zone identifier, for example {@literal Pacific/Auckland}. */
	TimeZone("--zone", "zone"),

	/** A PV tilt angle value, in degrees from horizontal. */
	Tilt("--array-tilt", "pvArrayTilt"),

	/** A PV array angle value, in degrees clockwise from north. */
	Azimuth("--array-azimuth", "pvArrayAzimuth"),

	/**
	 * A minimum value of {@code cos(zenith)} to allow when calculating global
	 * clearness index.
	 */
	MinCosZenith("--min-cos-zenith", "minCosZenith"),

	/** A maximum zenith value to allow in DNI calculation. */
	MaxZenith("--max-zenith", "maxZenith"),

	/** The timestamp. */
	Date("--date", null),

	/** The GHI irradiance value. */
	Ghi("--irradiance", null),

	/**
	 * The transposition model to use; see {@link TranspositionModel} for
	 * supported values.
	 */
	TranspositionModel("--transpose", "transpositionModel"),

	;

	private final String option;
	private final String metadataKey;

	private CommandOptions(String option, String metadataKey) {
		this.option = option;
		this.metadataKey = metadataKey;
	}

	/**
	 * Get the command option.
	 *
	 * @return the command option
	 */
	public final String getOption() {
		return option;
	}

	/**
	 * Get the meadata key.
	 *
	 * @return the metadata key, or {@code null} if not supported
	 */
	public final String getMetadataKey() {
		return metadataKey;
	}

}
