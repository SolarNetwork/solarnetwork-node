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

import net.solarnetwork.node.util.DateUtils;
import org.joda.time.LocalTime;

/**
 * Extension of {@link GeneralLocationDatum} with {@link DayDatum} support.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralDayDatum extends GeneralLocationDatum implements DayDatum {

	@Override
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
	public String getSkyConditions() {
		return getStatusSampleString(SKY_CONDITIONS_KEY);
	}

	public void setSkyConditions(String value) {
		putStatusSampleValue(SKY_CONDITIONS_KEY, value);
	}

	@Override
	public Float getTemperatureMinimum() {
		return getInstantaneousSampleFloat(TEMPERATURE_MINIMUM_KEY);
	}

	public void setTemperatureMinimum(Float value) {
		putInstantaneousSampleValue(TEMPERATURE_MINIMUM_KEY, value);
	}

	@Override
	public Float getTemperatureMaximum() {
		return getInstantaneousSampleFloat(TEMPERATURE_MAXIMUM_KEY);
	}

	public void setTemperatureMaximum(Float value) {
		putInstantaneousSampleValue(TEMPERATURE_MAXIMUM_KEY, value);
	}

}
