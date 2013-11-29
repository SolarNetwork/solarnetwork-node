/* ==================================================================
 * CCDatum.java - Apr 23, 2013 3:30:30 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.currentcost;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A CurrentCost datum.
 * 
 * @author matt
 * @version 1.1
 */
public class CCDatum implements Comparable<CCDatum> {

	private final long created;
	private String deviceAddress;
	private String deviceName;
	private String deviceType;
	private String deviceSoftwareVersion;
	private Integer daysSinceBegin;
	private LocalTime time;
	private Integer channel1Watts;
	private Integer channel2Watts;
	private Integer channel3Watts;
	private Float temperature;

	/**
	 * Default constructor.
	 */
	public CCDatum() {
		super();
		created = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "CCDatum{" + getStatusMessage() + "}";
	}

	/**
	 * Secondary setter to set the time, to support both v1 and v2 message
	 * format mapping.
	 * 
	 * @param time
	 *        the time
	 */
	public void setLocalTime(String timeString) {
		LocalTime t = null;
		if ( timeString != null && timeString.length() > 5 ) {
			// the XPath mapping can result in a time like ::14:14:27, due to supporting old/new XML
			if ( timeString.startsWith(":") ) {
				timeString = timeString.replaceAll("^:+", "");
			}
			DateTimeFormatter dtf = DateTimeFormat.forPattern("HH:mm:ss");
			t = dtf.parseLocalTime(timeString);
		}
		this.time = t;
	}

	/**
	 * Get a status message of this datum, including the address, amp readings,
	 * and creation date.
	 * 
	 * @return a status message
	 */
	public String getStatusMessage() {
		return (deviceAddress + ": 1 = " + (channel1Watts == null ? "N/A" : channel1Watts) + ", 2 = "
				+ (channel2Watts == null ? "N/A" : channel2Watts) + ", 3 = "
				+ (channel3Watts == null ? "N/A" : channel3Watts) + (time == null ? "" : "; " + time)
				+ "; " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(created)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((deviceAddress == null) ? 0 : deviceAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		CCDatum other = (CCDatum) obj;
		if ( deviceAddress == null ) {
			if ( other.deviceAddress != null )
				return false;
		} else if ( !deviceAddress.equals(other.deviceAddress) )
			return false;
		return true;
	}

	@Override
	public int compareTo(CCDatum o) {
		if ( o == null ) {
			return 1;
		}
		if ( deviceAddress == null ) {
			return -1;
		}
		if ( o.deviceAddress == null ) {
			return 1;
		}
		return deviceAddress.compareTo(o.deviceAddress);
	}

	public String getDeviceAddress() {
		return deviceAddress;
	}

	public void setDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceSoftwareVersion() {
		return deviceSoftwareVersion;
	}

	public void setDeviceSoftwareVersion(String deviceSoftwareVersion) {
		this.deviceSoftwareVersion = deviceSoftwareVersion;
	}

	public Integer getDaysSinceBegin() {
		return daysSinceBegin;
	}

	public void setDaysSinceBegin(Integer daysSinceBegin) {
		this.daysSinceBegin = daysSinceBegin;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public Integer getChannel1Watts() {
		return channel1Watts;
	}

	public void setChannel1Watts(Integer channel1Watts) {
		this.channel1Watts = channel1Watts;
	}

	public Integer getChannel2Watts() {
		return channel2Watts;
	}

	public void setChannel2Watts(Integer channel2Watts) {
		this.channel2Watts = channel2Watts;
	}

	public Integer getChannel3Watts() {
		return channel3Watts;
	}

	public void setChannel3Watts(Integer channel3Watts) {
		this.channel3Watts = channel3Watts;
	}

	public Float getTemperature() {
		return temperature;
	}

	public void setTemperature(Float temperature) {
		this.temperature = temperature;
	}

}
