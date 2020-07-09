/* ==================================================================
 * WMBusConnection.java - 29/06/2020 11:51:53 AM
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

package net.solarnetwork.node.io.mbus;

import java.io.Closeable;
import java.io.IOException;

/**
 * High level Wireless M-Bus device connection API.
 * 
 * @author alex
 * @version 1.0
 */
public abstract class WMBusConnection implements Closeable, MBusMessageHandler {

	private MBusData partialData = null;
	private MBusData latestData = null;
	private final Object dataLock = new Object();

	@Override
	public void handleMessage(MBusMessage message) {
		synchronized ( dataLock ) {
			if ( message.moreRecordsFollow ) {
				if ( partialData == null ) {
					partialData = new MBusData(message);
				} else {
					partialData.addRecordsFrom(message);
				}
			} else {
				if ( partialData == null ) {
					latestData = new MBusData(message);
				} else {
					latestData = partialData;
					latestData.addRecordsFrom(message);
					partialData = null;
				}
			}
		}
	}

	/**
	 * Open the connection, if it is not already open. The connection must be
	 * opened before calling any of the other methods in this API.
	 * 
	 * @throws IOException
	 *         if the connection cannot be opened
	 */
	public abstract void open() throws IOException;

	public MBusData getData() {
		synchronized ( dataLock ) {
			return new MBusData(latestData);
		}
	}
}
