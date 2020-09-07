/* ==================================================================
 * WMBusMessageDecoder.java - 01/07/2020 10:27:10 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package org.openmuc.jmbus.wireless;

import java.util.Map;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;

/**
 * 
 * Make the {@link WMBusMessage.decode} method publicly available
 * 
 * @author alex
 * @version 1.0
 */
public class WMBusMessageDecoder {

	public static WMBusMessage decode(byte[] buffer, Integer signalStrengthInDBm,
			Map<SecondaryAddress, byte[]> keyMap) throws DecodingException {
		return WMBusMessage.decode(buffer, signalStrengthInDBm, keyMap);
	}

}
