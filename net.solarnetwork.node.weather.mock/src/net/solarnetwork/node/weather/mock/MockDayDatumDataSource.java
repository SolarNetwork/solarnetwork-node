/* ===================================================================
 * MockDayDatumDataSource.java
 * 
 * Created Dec 2, 2009 11:21:21 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.weather.mock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.weather.DayDatum;

/**
 * Mock implementation of {@link DatumDataSource} for {@link DayDatum} objects.
 * 
 * <p>
 * This simple implementation returns a value for the current day with fixed
 * sunrise/sunset values set according to the {@code timeDayStart} and
 * {@code timeNightStart} properties.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>timeDayStart</dt>
 * <dd>The time to use for sunrise. Must be in the form {@code HH:mm}. Defaults
 * to {@link #DEFAULT_TIME_DAY_START}.</dd>
 * 
 * <dt>timeNightStart</dt>
 * <dd>The time to ues for sunset. Must be in the form {@code HH:mm}. Defaults
 * to {@link #DEFAULT_TIME_NIGHT_START}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class MockDayDatumDataSource implements DatumDataSource<DayDatum> {

	/** The default value for the {@code timeDayStart} property. */
	public static final String DEFAULT_TIME_DAY_START = "08:00";

	/** The default value for the {@code timeNightStart} property. */
	public static final String DEFAULT_TIME_NIGHT_START = "20:00";

	private String timeDayStart = DEFAULT_TIME_DAY_START;
	private String timeNightStart = DEFAULT_TIME_NIGHT_START;

	@Override
	public Class<? extends DayDatum> getDatumType() {
		return DayDatum.class;
	}

	@Override
	public DayDatum readCurrentDatum() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		Date today = cal.getTime();
		Date sunrise = null;
		Date sunset = null;
		try {
			sunrise = sdf.parse(timeDayStart);
			sunset = sdf.parse(timeNightStart);
		} catch ( ParseException e ) {
			throw new RuntimeException(e);
		}

		return new DayDatum(today, sunrise, sunset);
	}

	public String getTimeDayStart() {
		return timeDayStart;
	}

	public void setTimeDayStart(String timeDayStart) {
		this.timeDayStart = timeDayStart;
	}

	public String getTimeNightStart() {
		return timeNightStart;
	}

	public void setTimeNightStart(String timeNightStart) {
		this.timeNightStart = timeNightStart;
	}

	@Override
	public String getUID() {
		return "MockSource";
	}

	@Override
	public String getGroupUID() {
		return "Mock";
	}

}
