/* ===================================================================
 * WeatherDotComWeatherDatumDataSource.java
 * 
 * Created Dec 3, 2009 1:27:59 PM
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.weather.WeatherDatum;

import org.springframework.beans.BeanWrapper;

/**
 * Weather.com implementation of {@link DatumDataSource} for 
 * {@link WeatherDatum} objects.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class WeatherDotComWeatherDatumDataSource extends WeatherDotComSupport
implements DatumDataSource<WeatherDatum> {

	/**
	 * Initialize this class after properties are set.
	 */
	@Override
	public void init() {
		if ( getDatumXPathMapping() == null ) {
			// apply defaults
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("infoDate", "/weather/cc/lsup/text()");
			map.put("skyConditions", "/weather/cc/t/text()");
			map.put("temperatureCelcius", 
				"/weather/cc/tmp[string(number(.)) != 'NaN']/text()");
			map.put("humidity", 
				"/weather/cc/hmid[string(number(.)) != 'NaN']/text()");
			map.put("barometricPressure", 
				"/weather/cc/bar/r[string(number(.)) != 'NaN']/text()");
			map.put("barometerDelta", "/weather/cc/bar/d/text()");
			map.put("visibility", "/weather/cc/vis/text()");
			map.put("uvIndex",
				"/weather/cc/uv/i[string(number(.)) != 'NaN']/text()");
			map.put("dewPoint", 
				"/weather/cc/dewp[string(number(.)) != 'NaN']/text()");
			setDatumXPathMapping(map);
		}
		
		super.init();
	}
	
	public Class<? extends WeatherDatum> getDatumType() {
		return WeatherDatum.class;
	}

	/* (non-Javadoc)
	 * @see net.solarnetwork.node.DatumDataSource#readCurrentDatum()
	 */
	public WeatherDatum readCurrentDatum() {
		WeatherDatum datum = new WeatherDatum();
		String theLocation = getLocation();
		datum.setSourceId(theLocation);
		populateDatum(theLocation, datum);
		return datum;
	}

	@Override
	protected void registerCustomEditors(BeanWrapper bean) {
		bean.registerCustomEditor(Date.class, "infoDate", newStandardDateEditor());
		bean.registerCustomEditor(Double.class, "visibility", new PropertyEditorSupport() {

			@Override
			public String getAsText() {
				return getValue() == null ? "" : getValue().toString();
			}

			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if ( text == null || text.trim().length() < 1 ) {
					setValue(null);
				} else if ( "unlimited".equalsIgnoreCase(text) ) {
					// set to Float.MAX_VALUE so can treat as Float centrally
					setValue(Double.valueOf(Float.MAX_VALUE));	
				} else {
					try {
						setValue(Double.parseDouble(text));
					} catch ( NumberFormatException e ) {
						if ( log.isTraceEnabled() ) {
							log.trace("Unable to parse double [" +text +']');
						}
						setValue(null);
					}
				}
			}
			
		});
	}
	
}
