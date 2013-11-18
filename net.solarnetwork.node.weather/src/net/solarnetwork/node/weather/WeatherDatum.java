/* ===================================================================
 * WeatherDatum.java
 * 
 * Created Dec 3, 2009 10:39:22 AM
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
import net.solarnetwork.node.support.BaseLocationDatum;

/**
 * Domain object for weather related data.
 * 
 * @author matt
 * @version 1.1
 */
public class WeatherDatum extends BaseLocationDatum {

	private String skyConditions;
	private Double temperatureCelsius;
	private Double humidity;
	private Double barometricPressure;
	private String barometerDelta;
	private Double visibility;
	private Integer uvIndex;
	private Double dewPoint;

	@Override
	public String toString() {
		return "WeatherDatum{infoDate=" + getCreated() + ",temp=" + this.temperatureCelsius
				+ ",humidity=" + this.humidity + ",barometricPressure=" + this.barometricPressure
				+ ",barometerDelta=" + this.barometerDelta + '}';
	}

	public String getSkyConditions() {
		return skyConditions;
	}

	public void setSkyConditions(String skyConditions) {
		this.skyConditions = skyConditions;
	}

	public Double getTemperatureCelsius() {
		return temperatureCelsius;
	}

	public void setTemperatureCelsius(Double temperatureCelsius) {
		this.temperatureCelsius = temperatureCelsius;
	}

	@Deprecated
	public void setTemperatureCelcius(Double temperatureCelcius) {
		setTemperatureCelsius(temperatureCelcius);
	}

	public Double getHumidity() {
		return humidity;
	}

	public void setHumidity(Double humidity) {
		this.humidity = humidity;
	}

	public Double getBarometricPressure() {
		return barometricPressure;
	}

	public void setBarometricPressure(Double barometricPressure) {
		this.barometricPressure = barometricPressure;
	}

	public String getBarometerDelta() {
		return barometerDelta;
	}

	public void setBarometerDelta(String barometerDelta) {
		this.barometerDelta = barometerDelta;
	}

	public Double getVisibility() {
		return visibility;
	}

	public void setVisibility(Double visibility) {
		this.visibility = visibility;
	}

	public Integer getUvIndex() {
		return uvIndex;
	}

	public void setUvIndex(Integer uvIndex) {
		this.uvIndex = uvIndex;
	}

	public Double getDewPoint() {
		return dewPoint;
	}

	public void setDewPoint(Double dewPoint) {
		this.dewPoint = dewPoint;
	}

	@Deprecated
	public Date getInfoDate() {
		return getCreated();
	}

	@Deprecated
	public void setInfoDate(Date infoDate) {
		setCreated(infoDate);
	}

}
