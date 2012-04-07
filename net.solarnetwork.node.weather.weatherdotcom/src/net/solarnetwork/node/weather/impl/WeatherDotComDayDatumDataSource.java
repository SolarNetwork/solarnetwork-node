/* ===================================================================
 * WeatherDotComDayDatumDataSource.java
 * 
 * Created Dec 3, 2009 11:57:49 AM
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

package net.solarnetwork.node.weather.impl;

import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.weather.DayDatum;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.propertyeditors.CustomDateEditor;

/**
 * Weather.com implementation of {@link DatumDataSource} for {@link DayDatum}
 * objects.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>sunriseSunsetDateFormat</dt>
 *   <dd>A time format for parsing sunrise/sunset dates with. Defaults
 *   to {@code h:mm a}.</dd>
 *   
 *   <dt>maxDayDatumCacheSize</dt>
 *   <dd>The maximum number of DayDatum to cache. The cache is used so the
 *   Weather.com service does not need to be called repeatedly on the same 
 *   day for the same day information. Defaults to 5.</dd>
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class WeatherDotComDayDatumDataSource extends WeatherDotComSupport
implements DatumDataSource<DayDatum> {

	private String sunriseSunsetDateFormat = "h:mm a";
	private int maxDayDatumCacheSize = 5;

	private Map<String, DayDatum> lastDayDatum = null;

	/**
	 * Initialize this class after properties are set.
	 */
	@Override
	public void init() {
		lastDayDatum = new ConcurrentHashMap<String, DayDatum>(maxDayDatumCacheSize);
		if ( getDatumXPathMapping() == null ) {
			// apply defaults
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("day", "/weather/cc/lsup/text()");
			map.put("timeZoneId", "/weather/loc/zone/text()");
			map.put("latitude", "/weather/loc/lat/text()");
			map.put("longitude", "/weather/loc/lon/text()");
			map.put("sunrise", "/weather/loc/sunr/text()");
			map.put("sunset", "/weather/loc/suns/text()");
			setDatumXPathMapping(map);
		}
		
		super.init();
	}
	
	public Class<? extends DayDatum> getDatumType() {
		return DayDatum.class;
	}

	public DayDatum readCurrentDatum() {
		DayDatum datum = null;
		String theLocation = getLocation();
		
		// check cache first
		datum = lastDayDatum.get(theLocation);
		if ( datum != null ) {
			Calendar now = Calendar.getInstance();
			Calendar datumCal = Calendar.getInstance();
			datumCal.setTime(datum.getDay());
			if ( now.get(Calendar.YEAR) == datumCal.get(Calendar.YEAR)
					&& now.get(Calendar.DAY_OF_YEAR) == datumCal.get(Calendar.DAY_OF_YEAR) ) {
				// cached data is for same date, so return that
				return datum;
			}
			
			// invalid cached data, remove now
			lastDayDatum.remove(theLocation);
		}
		
		datum = new DayDatum();
		datum.setSourceId(theLocation);
		populateDatum(theLocation, datum);
		
		// cache datum now
		synchronized ( lastDayDatum ) {
			if ( lastDayDatum.size() >= this.maxDayDatumCacheSize ) {
				// remove one from the cache, in unknown order
				Iterator<String> keys = lastDayDatum.keySet().iterator();
				keys.next();
				keys.remove();
			}
			lastDayDatum.put(theLocation, datum);
		}
		
		return datum;
	}

	@Override
	protected void registerCustomEditors(BeanWrapper bean) {
		bean.registerCustomEditor(String.class, "timeZoneId", new PropertyEditorSupport() {

			@Override
			public String getAsText() {
				return getValue() == null ? "" : getValue().toString();
			}

			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if ( text == null || text.trim().length() < 1 ) {
					setValue(null);
				} else {
					// format offset e.g. GMT+12:00
					int hourOffset = Integer.parseInt(text);
					String val = String.format("GMT%s%02d:00", 
							(hourOffset < 0 ? "-" : "+"), hourOffset);
					setValue(val);
				}
			}
			
		});
		bean.registerCustomEditor(Date.class, "day", newStandardDateEditor());
		bean.registerCustomEditor(Date.class, "sunrise", new CustomDateEditor(
				new SimpleDateFormat(this.sunriseSunsetDateFormat), false));
		bean.registerCustomEditor(Date.class, "sunset", new CustomDateEditor(
				new SimpleDateFormat(this.sunriseSunsetDateFormat), false));
	}
	
	/**
	 * @return the sunriseSunsetDateFormat
	 */
	public String getSunriseSunsetDateFormat() {
		return sunriseSunsetDateFormat;
	}

	/**
	 * @param sunriseSunsetDateFormat the sunriseSunsetDateFormat to set
	 */
	public void setSunriseSunsetDateFormat(String sunriseSunsetDateFormat) {
		this.sunriseSunsetDateFormat = sunriseSunsetDateFormat;
	}

}
