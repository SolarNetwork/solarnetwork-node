/* ===================================================================
 * SmaUserDataField.java
 * 
 * Created Sep 7, 2009 10:27:22 AM
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

package net.solarnetwork.node.power.impl.sma;

/**
 * A user data field enumeration.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public enum SmaUserDataField {
	
	/** The device serial number. */
	DeviceSerialNumber,
	
	/** The device type. */
	DeviceType,
	
	/** A List of {@link SmaChannel} objects. */
	Channels,
	
	/** The channel index number. */
	ChannelIndex,
	
	/** The channel type1 value. */
	ChannelType1,
	
	/** The channel type2 value. */
	ChannelType2,
	
	/** A data value. */
	Value,
	
	/** The number of sets of data. */
	DataSets,
	
	/** The seconds since value. */
	SecondsSince,
	
	/** A time basis. */
	TimeBasis,
	
	/** The "low" text value. */
	TextLow,
	
	/** The "high" text value. */
	TextHigh,
	
	/** An error. */
	Error;
}