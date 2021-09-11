/* ==================================================================
 * YrDayDatum.java - 20/05/2017 7:39:49 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.yr;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;

/**
 * Extension of {@link GeneralDayDatum} to support Yr data.
 * 
 * @author matt
 * @version 2.0
 */
public class YrDayDatum extends SimpleDayDatum {

	private static final long serialVersionUID = 8230332231861429332L;

	/**
	 * A {@link net.solarnetwork.domain.datum.DatumSamples} status sample key
	 * for the Yr sky conditions symbol.
	 */
	public static final String SYMBOL_VAR_KEY = "symbolVar";

	private final YrLocation location;

	/**
	 * Constructor.
	 * 
	 * @param timestamp
	 *        the timestamp
	 * @param location
	 *        the location
	 */
	public YrDayDatum(Instant timestamp, YrLocation location) {
		super(null, timestamp, new DatumSamples());
		this.location = location;
	}

	private LocalTime parseTime(String ts) throws DateTimeParseException {
		int idx = ts.indexOf('T');
		if ( idx != 0 ) {
			String time = ts.substring(idx + 1);
			String[] components = time.split(":", 3);
			if ( components.length > 2 ) {
				return LocalTime.of(Integer.valueOf(components[0]), Integer.valueOf(components[1]),
						Integer.valueOf(components[2]));
			} else {
				throw new DateTimeParseException("Cannot parse time", ts, idx + 1);
			}
		}
		return null;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location
	 */
	public YrLocation getLocation() {
		return location;
	}

	/**
	 * Set the sunrise time via a string.
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws DateTimeParseException
	 *         if a parsing error occurs
	 */
	public void setSunriseTimestamp(String ts) throws DateTimeParseException {
		setSunriseTime(parseTime(ts));
	}

	/**
	 * Set the sunset time via a string.
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws DateTimeParseException
	 *         if a parsing error occurs
	 */
	public void setSunsetTimestamp(String ts) throws DateTimeParseException {
		setSunsetTime(parseTime(ts));
	}
}
