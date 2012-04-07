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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.weather;

import java.util.Date;
import java.util.TimeZone;

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.support.BaseDatum;

/**
 *  Domain object for day related data.
 * 
 * <p>The {@code day} property reflects the year/month/day of this datum
 * (e.g. a SQL date value). The {@code sunrise} and {@code sunset} properties 
 * reflect only time values for this day (e.g. a SQL time value).</p>
 * 
 * <p>The {@code latitude} and {@code longitude} may or may not be used, it 
 * depends on how granular the node wants to track day information.</p>
 * 
 * <p><b>Note:</b> the {@code day} property is internally stored on the 
 * {@link BaseDatum} {@code created} property, so externally this object
 * will appear to have both a {@code created} and {@code day} JavaBean
 * property, even though they are internally the same object.</p>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class DayDatum extends BaseDatum implements Datum {

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
	 * Construct with a primary key.
	 * 
	 * @param id the primary key
	 */
	public DayDatum(Long id) {
		super(id);
	}

	/**
	 * Construct with values.
	 * 
	 * @param day the day (year/month/day)
	 * @param sunrise the sunrise time
	 * @param sunset the sunset time
	 */
	public DayDatum(Date day, Date sunrise, Date sunset) {
		super();
		setCreated(day);
		this.sunrise = sunrise;
		this.sunset = sunset;
	}

	@Override
	public String toString() {
		return "DayDatum{day=" +getCreated()
			+",timeZoneId=" +this.timeZoneId
			+",sunrize=" +this.sunrise
			+",sunset=" +this.sunset
			+'}';
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result
				+ ((longitude == null) ? 0 : longitude.hashCode());
		result = prime * result + ((sunrise == null) ? 0 : sunrise.hashCode());
		result = prime * result + ((sunset == null) ? 0 : sunset.hashCode());
		result = prime * result
				+ ((timeZoneId == null) ? 0 : timeZoneId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		DayDatum other = (DayDatum) obj;
		if ( latitude == null ) {
			if ( other.latitude != null ) {
				return false;
			}
		} else if ( !latitude.equals(other.latitude) ) {
			return false;
		}
		if ( longitude == null ) {
			if ( other.longitude != null ) {
				return false;
			}
		} else if ( !longitude.equals(other.longitude) ) {
			return false;
		}
		if ( sunrise == null ) {
			if ( other.sunrise != null ) {
				return false;
			}
		} else if ( !sunrise.equals(other.sunrise) ) {
			return false;
		}
		if ( sunset == null ) {
			if ( other.sunset != null ) {
				return false;
			}
		} else if ( !sunset.equals(other.sunset) ) {
			return false;
		}
		if ( timeZoneId == null ) {
			if ( other.timeZoneId != null ) {
				return false;
			}
		} else if ( !timeZoneId.equals(other.timeZoneId) ) {
			return false;
		}
		return true;
	}

	/**
	 * @return the day
	 */
	public Date getDay() {
		return getCreated();
	}

	/**
	 * @param day the day to set
	 */
	public void setDay(Date day) {
		setCreated(day);
	}

	/**
	 * @return the sunrise
	 */
	public Date getSunrise() {
		return sunrise;
	}
	
	/**
	 * @param sunrise the sunrise to set
	 */
	public void setSunrise(Date sunrise) {
		this.sunrise = sunrise;
	}
	
	/**
	 * @return the sunset
	 */
	public Date getSunset() {
		return sunset;
	}
	
	/**
	 * @param sunset the sunset to set
	 */
	public void setSunset(Date sunset) {
		this.sunset = sunset;
	}
	
	/**
	 * @return the timeZoneId
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}
	
	/**
	 * @param timeZoneId the timeZoneId to set
	 */
	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
}
