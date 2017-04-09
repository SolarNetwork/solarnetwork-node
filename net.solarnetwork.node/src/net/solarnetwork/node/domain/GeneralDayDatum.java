/* ==================================================================
 * GeneralDayDatum.java - Oct 22, 2014 2:46:53 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import java.math.BigDecimal;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.node.util.DateUtils;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Extension of {@link GeneralLocationDatum} with {@link DayDatum} support.
 * 
 * @author matt
 * @version 1.2
 */
public class GeneralDayDatum extends GeneralLocationDatum implements DayDatum {

	@Override
	@JsonIgnore
	@SerializeIgnore
	public LocalTime getSunrise() {
		String time = getStatusSampleString(SUNRISE_KEY);
		if ( time == null ) {
			return null;
		}
		return DateUtils.parseLocalTime(time);
	}

	public void setSunrise(LocalTime value) {
		putStatusSampleValue(SUNRISE_KEY, DateUtils.format(value));
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public LocalTime getSunset() {
		String time = getStatusSampleString(SUNSET_KEY);
		if ( time == null ) {
			return null;
		}
		return DateUtils.parseLocalTime(time);
	}

	public void setSunset(LocalTime value) {
		putStatusSampleValue(SUNSET_KEY, DateUtils.format(value));
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public LocalTime getMoonrise() {
		String time = getStatusSampleString(MOONRISE_KEY);
		if ( time == null ) {
			return null;
		}
		return DateUtils.parseLocalTime(time);
	}

	public void setMoonrise(LocalTime value) {
		putStatusSampleValue(MOONRISE_KEY, DateUtils.format(value));
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public LocalTime getMoonset() {
		String time = getStatusSampleString(MOONSET_KEY);
		if ( time == null ) {
			return null;
		}
		return DateUtils.parseLocalTime(time);
	}

	public void setMoonset(LocalTime value) {
		putStatusSampleValue(MOONSET_KEY, DateUtils.format(value));
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public String getSkyConditions() {
		return getStatusSampleString(SKY_CONDITIONS_KEY);
	}

	public void setSkyConditions(String value) {
		putStatusSampleValue(SKY_CONDITIONS_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public BigDecimal getTemperatureMinimum() {
		return getInstantaneousSampleBigDecimal(TEMPERATURE_MINIMUM_KEY);
	}

	public void setTemperatureMinimum(BigDecimal value) {
		putInstantaneousSampleValue(TEMPERATURE_MINIMUM_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public BigDecimal getTemperatureMaximum() {
		return getInstantaneousSampleBigDecimal(TEMPERATURE_MAXIMUM_KEY);
	}

	public void setTemperatureMaximum(BigDecimal value) {
		putInstantaneousSampleValue(TEMPERATURE_MAXIMUM_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public String getBriefOverview() {
		return getStatusSampleString(BRIEF_OVERVIEW_KEY);
	}

	public void setBriefOverview(String value) {
		putStatusSampleValue(BRIEF_OVERVIEW_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public BigDecimal getWindSpeed() {
		return getInstantaneousSampleBigDecimal(WIND_SPEED_KEY);
	}

	public void setWindSpeed(BigDecimal value) {
		putInstantaneousSampleValue(WIND_SPEED_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getWindDirection() {
		return getInstantaneousSampleInteger(WIND_DIRECTION_KEY);
	}

	public void setWindDirection(Integer value) {
		putInstantaneousSampleValue(WIND_DIRECTION_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getRain() {
		return getInstantaneousSampleInteger(RAIN_KEY);
	}

	public void setRain(Integer value) {
		putInstantaneousSampleValue(RAIN_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getSnow() {
		return getInstantaneousSampleInteger(SNOW_KEY);
	}

	public void setSnow(Integer value) {
		putInstantaneousSampleValue(SNOW_KEY, value);
	}

}
