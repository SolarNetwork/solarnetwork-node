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

import java.text.ParseException;
import org.joda.time.LocalTime;
import net.solarnetwork.node.domain.GeneralDayDatum;

/**
 * Extension of {@link GeneralDayDatum} to support Yr data.
 * 
 * @author matt
 * @version 1.0
 */
public class YrDayDatum extends GeneralDayDatum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for the Yr sky conditions symbol.
	 */
	public static final String SYMBOL_VAR_KEY = "symbolVar";

	private final YrLocation location;

	public YrDayDatum(YrLocation location) {
		super();
		this.location = location;
		setSamples(newSamplesInstance());
	}

	private LocalTime parseTime(String ts) throws ParseException {
		int idx = ts.indexOf('T');
		if ( idx != 0 ) {
			String time = ts.substring(idx + 1);
			String[] components = time.split(":", 3);
			if ( components.length > 2 ) {
				return new LocalTime(Integer.valueOf(components[0]), Integer.valueOf(components[1]),
						Integer.valueOf(components[2]));
			} else {
				throw new ParseException("Cannot parse time from [" + ts + "]", idx + 1);
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
	 * @throws ParseException
	 *         if a parsing error occurs
	 */
	public void setSunriseTimestamp(String ts) throws ParseException {
		LocalTime t = parseTime(ts);
		if ( t != null ) {
			setSunrise(t);
		}
	}

	/**
	 * Set the sunset time via a string.
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws ParseException
	 *         if a parsing error occurs
	 */
	public void setSunsetTimestamp(String ts) throws ParseException {
		LocalTime t = parseTime(ts);
		if ( t != null ) {
			setSunset(t);
		}
	}
}
