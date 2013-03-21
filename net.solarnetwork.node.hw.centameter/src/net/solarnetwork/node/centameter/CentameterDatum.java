/* ==================================================================
 * CentameterDatum.java - Mar 21, 2013 1:02:16 PM
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

package net.solarnetwork.node.centameter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cent-a-meter datum bean.
 * 
 * @author matt
 * @version 1.0
 */
public class CentameterDatum implements Comparable<CentameterDatum> {

	private final long created;
	private final String address;
	private final Float amp1;
	private final Float amp2;
	private final Float amp3;

	public CentameterDatum(String address, Float amp1, Float amp2, Float amp3) {
		super();
		this.address = address;
		this.amp1 = amp1;
		this.amp2 = amp2;
		this.amp3 = amp3;
		this.created = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "CentameterDatum{" + getStatusMessage() + "}";
	}

	/**
	 * Get a status message of this datum, including the address, amp readings,
	 * and creation date.
	 * 
	 * @return a status message
	 */
	public String getStatusMessage() {
		return (address + ": 1 = " + (amp1 == null ? "N/A" : amp1) + ", 2 = "
				+ (amp2 == null ? "N/A" : amp2) + ", 3 = " + (amp3 == null ? "N/A" : amp3) + "; " + new SimpleDateFormat(
				"yyyy-MM-dd HH:mm").format(new Date(created)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
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
		CentameterDatum other = (CentameterDatum) obj;
		if ( address == null ) {
			if ( other.address != null )
				return false;
		} else if ( !address.equals(other.address) )
			return false;
		return true;
	}

	@Override
	public int compareTo(CentameterDatum o) {
		if ( o == null ) {
			return 1;
		}
		if ( address == null ) {
			return -1;
		}
		if ( o.address == null ) {
			return 1;
		}
		return address.compareTo(o.address);
	}

	public String getAddress() {
		return address;
	}

	public Float getAmp1() {
		return amp1;
	}

	public Float getAmp2() {
		return amp2;
	}

	public Float getAmp3() {
		return amp3;
	}

	public long getCreated() {
		return created;
	}

}
