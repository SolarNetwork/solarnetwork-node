/* ===================================================================
 * SmaChannelType.java
 * 
 * Created Sep 7, 2009 10:26:58 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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

package net.solarnetwork.node.hw.sma.sunnynet;

/**
 * An SMA channel type.
 *
 * @author matt
 * @version 1.0 $Date$
 */
public enum SmaChannelType {
	
	/** Analog type. */
	Analog(1),
	
	/** Digital type. */
	Digital(2),
	
	/** Counter type. */
	Counter(4),
	
	/** Status type. */
	Status(8),
	
	/** Unknown type. */
	Unknown(-1);
	
	private short code;
	
	private SmaChannelType(int code) {
		this.code = (short)code;
	}
	
	/**
	 * Get the channel type code value.
	 * 
	 * @return code value
	 */
	public short getCode() {
		return this.code;
	}
	
	/**
	 * Get a SmaChannelType instance from a code value.
	 * 
	 * @param code the code value
	 * @return the SmaChannelType
	 */
	public static SmaChannelType forCode(int code) {
		switch ( code ) {
			case 1:
				return Analog;
				
			case 2:
				return Digital;
				
			case 4:
				return Counter;
				
			case 8:
				return Status;
			
			default:
				return Unknown;
		}
	}
}