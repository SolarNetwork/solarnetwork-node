/* ===================================================================
 * DayDatum.java
 * 
 * Created Dec 3, 2009 10:20:42 AM
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

package net.solarnetwork.node.weather;

import java.util.Date;
import java.util.TimeZone;
import net.solarnetwork.node.Datum;
import net.solarnetwork.node.support.BaseDatum;
import net.solarnetwork.node.support.BaseLocationDatum;

/**
 * Domain object for day related data.
 * 
 * <p>
 * The {@code day} property reflects the year/month/day of this datum (e.g. a
 * SQL date value). The {@code sunrise} and {@code sunset} properties reflect
 * only time values for this day (e.g. a SQL time value).
 * </p>
 * 
 * <p>
 * The {@code latitude} and {@code longitude} may or may not be used, it depends
 * on how granular the node wants to track day information.
 * </p>
 * 
 * <p>
 * <b>Note:</b> the {@code day} property is internally stored on the
 * {@link BaseDatum} {@code created} property, so externally this object will
 * appear to have both a {@code created} and {@code day} JavaBean property, even
 * though they are internally the same object.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class DayDatum extends BaseLocationDatum implements Datum {

	private String timeZoneId = TimeZone.getDefault().getID();
	private Double latitude = null;
	private Double longitude = null;
	private Date sunrise = null;
	private Date sunset = null;

	/**
	 * Default constructor.
	 */
	public DayDatum() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param day
	 *        the day (year/month/day)
	 * @param sunrise
	 *        the sunrise time
	 * @param sunset
	 *        the sunset time
	 */
	public DayDatum(Date day, Date sunrise, Date sunset) {
		super();
		setCreated(day);
		this.sunrise = sunrise;
		this.sunset = sunset;
	}

	@Override
	public String toString() {
		return "DayDatum{day=" + getCreated() + ",timeZoneId=" + this.timeZoneId + ",sunrize="
				+ this.sunrise + ",sunset=" + this.sunset + '}';
	}

	public Date getDay() {
		return getCreated();
	}

	public void setDay(Date day) {
		setCreated(day);
	}

	public Date getSunrise() {
		return sunrise;
	}

	public void setSunrise(Date sunrise) {
		this.sunrise = sunrise;
	}

	public Date getSunset() {
		return sunset;
	}

	public void setSunset(Date sunset) {
		this.sunset = sunset;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

}
