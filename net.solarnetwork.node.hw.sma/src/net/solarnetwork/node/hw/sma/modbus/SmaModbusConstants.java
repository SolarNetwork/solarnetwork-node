/* ==================================================================
 * SmaModbusConstants.java - 11/09/2020 8:52:39 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus;

import java.math.BigInteger;

/**
 * SMA defined constants for the Modbus protocol.
 * 
 * @author matt
 * @version 1.0
 */
public final class SmaModbusConstants {

	/** NaN value for Int16 number. */
	public static final short NAN_INT16 = (short) 0x8000;

	/** NaN value for UInt16 number. */
	public static final short NAN_UINT16 = (short) 0xFFFF;

	/** NaN value for Int32 value. */
	public static final int NAN_INT32 = 0x80000000;

	/** NaN value for UInt32 value. */
	public static final int NAN_UINT32 = 0xFFFFFFFF;

	/** NaN value for UInt64 value. */
	public static final BigInteger NAN_UINT64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

}
