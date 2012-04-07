/* ===================================================================
 * MockWeatherDatumDataSource.java
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.weather.mock;

import java.util.Calendar;
import java.util.Date;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.weather.WeatherDatum;

/**
 * Mock implementation of {@link DatumDataSource} for {@link WeatherDatum}
 * objects.
 * 
 * <p>This simple implementation returns an object with fixed data.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockWeatherDatumDataSource implements DatumDataSource<WeatherDatum> {

	public Class<? extends WeatherDatum> getDatumType() {
		return WeatherDatum.class;
	}

	public WeatherDatum readCurrentDatum() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		Date today = cal.getTime();
		WeatherDatum datum = new WeatherDatum();
		datum.setInfoDate(today);
		datum.setSkyConditions("Clear");
		datum.setTemperatureCelcius(24.0);
		datum.setHumidity(57.0);
		
		return datum;
	}

}
