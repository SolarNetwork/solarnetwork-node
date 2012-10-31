/* ==================================================================
 * TransceiverType.java - Jul 7, 2012 4:08:36 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.rfxcom;

/**
 * FIXME
 * 
 * <p>TODO</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt></dt>
 *   <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public enum TransceiverType {
	
	Unknown(0x0, "Unknown"),

	Type310(0x50, "310 MHz"),
	
	Type315(0x51, "315 MHz"),
	
	Type43392(0x52, "433.92 MHz"),
	
	Type43392a(0x53, "433.92 MHz"),
	
	Type868(0x55, "868 MHz"),
	
	Type868F(0x56, "868 MHz FSK"),
	
	Type86830(0x57, "868.30 MHz"),
	
	Type86830F(0x58, "868.30 MHz FSK"),
	
	Type86835(0x59, "868.35 MHz"),

	Type86835F(0x5a, "868.35 MHz FSK"),

	Type86895(0x5b, "868.95 MHz");

	private final byte value;
	private final String description;
	
	private TransceiverType(int value, String description) {
		this.value = (byte)value;
		this.description = description;
	}
	
	/**
	 * Parse a byte into an enumerated value.
	 * 
	 * @param b the byte to parse
	 * @return the value
	 * @throws IllegalArgumentException if the byte is not a supported type
	 */
	public static TransceiverType valueOf(byte b) {
		switch ( b ) {
			case 0x50: return Type310;
			case 0x51: return Type315;
			case 0x52: return Type43392;
			case 0x53: return Type43392a;
			case 0x55: return Type868;
			case 0x56: return Type868F;
			case 0x57: return Type86830;
			case 0x58: return Type86830F;
			case 0x59: return Type86835;
			case 0x5a: return Type86835F;
			case 0x5b: return Type86895;			
			default: return Unknown;
		}
	}

	public byte getMessageValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}
	
}
