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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.weather;

import java.util.Date;

import net.solarnetwork.node.support.BaseDatum;

/**
 * Domain object for weather related data.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class WeatherDatum extends BaseDatum {

	private Date infoDate;			// date weather info current as of
	private String skyConditions;
	private Double temperatureCelcius;
	private Double humidity;
	private Double barometricPressure;
	private String barometerDelta;
	private Double visibility;
	private Integer uvIndex;
	private Double dewPoint;
	
	/**
	 * Default constructor.
	 */
	public WeatherDatum() {
		this(null);
	}
	
	/**
	 * Construct with a primary key.
	 * 
	 * @param id the primary key
	 */
	public WeatherDatum(Long id) {
		super(id);
		setCreated(new Date());
	}

	@Override
	public String toString() {
		return "WeatherDatum{infoDate=" +this.infoDate
			+",temp=" +this.temperatureCelcius
			+",humidity=" +this.humidity
			+",barometricPressure=" +this.barometricPressure
			+",barometerDelta=" +this.barometerDelta
			+'}';
	}

	/**
	 * @return the skyConditions
	 */
	public String getSkyConditions() {
		return skyConditions;
	}
	
	/**
	 * @param skyConditions the skyConditions to set
	 */
	public void setSkyConditions(String skyConditions) {
		this.skyConditions = skyConditions;
	}
	
	/**
	 * @return the temperatureCelcius
	 */
	public Double getTemperatureCelcius() {
		return temperatureCelcius;
	}
	
	/**
	 * @param temperatureCelcius the temperatureCelcius to set
	 */
	public void setTemperatureCelcius(Double temperatureCelcius) {
		this.temperatureCelcius = temperatureCelcius;
	}
	
	/**
	 * @return the humidity
	 */
	public Double getHumidity() {
		return humidity;
	}
	
	/**
	 * @param humidity the humidity to set
	 */
	public void setHumidity(Double humidity) {
		this.humidity = humidity;
	}
	
	/**
	 * @return the barometricPressure
	 */
	public Double getBarometricPressure() {
		return barometricPressure;
	}
	
	/**
	 * @param barometricPressure the barometricPressure to set
	 */
	public void setBarometricPressure(Double barometricPressure) {
		this.barometricPressure = barometricPressure;
	}
	
	/**
	 * @return the barometerDelta
	 */
	public String getBarometerDelta() {
		return barometerDelta;
	}
	
	/**
	 * @param barometerDelta the barometerDelta to set
	 */
	public void setBarometerDelta(String barometerDelta) {
		this.barometerDelta = barometerDelta;
	}
	
	/**
	 * @return the visibility
	 */
	public Double getVisibility() {
		return visibility;
	}
	
	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(Double visibility) {
		this.visibility = visibility;
	}
	
	/**
	 * @return the uvIndex
	 */
	public Integer getUvIndex() {
		return uvIndex;
	}
	
	/**
	 * @param uvIndex the uvIndex to set
	 */
	public void setUvIndex(Integer uvIndex) {
		this.uvIndex = uvIndex;
	}
	
	/**
	 * @return the dewPoint
	 */
	public Double getDewPoint() {
		return dewPoint;
	}
	
	/**
	 * @param dewPoint the dewPoint to set
	 */
	public void setDewPoint(Double dewPoint) {
		this.dewPoint = dewPoint;
	}
	
	/**
	 * @return the infoDate
	 */
	public Date getInfoDate() {
		return infoDate;
	}

	/**
	 * @param infoDate the infoDate to set
	 */
	public void setInfoDate(Date infoDate) {
		this.infoDate = infoDate;
	}
	
}
