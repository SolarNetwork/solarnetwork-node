/* ===================================================================
 * DataCollector.java
 * 
 * Created Jul 27, 2009 9:18:00 AM
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

package net.solarnetwork.node;

/**
 * API for object that can sample (collect) raw data from some device,
 * such as a serial port, and return it.
 * 
 * <p>This API is designed for collecting small amounts of data at a time,
 * geared towards reading statistics from generation or consumption data
 * sources.</p>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public interface DataCollector {
	
	/**
	 * Start collecting data.
	 * 
	 * <p>This method will start reading data from the connected
	 * data stream. Depending on the implementation, it may block until
	 * the desired data has been collected, or it may return immediately
	 * and collect data in a background thread.</p>
	 */
	public void collectData();

	/**
	 * Get the number of "good" bytes read so far.
	 * 
	 * @return byte count
	 */
	public int bytesRead();
	
	/**
	 * Get the bytes read.
	 * 
	 * @return the bytes of data collected thus far
	 */
	public byte[] getCollectedData();
	
	/**
	 * Get the bytes read as a String.
	 * 
	 * @return the bytes of data collected thus far, as a String
	 */
	public String getCollectedDataAsString();
	
	/**
	 * Stop collecting data, freeing up any appropriate resources
	 * (for example closing the underlying data stream).
	 */
	public void stopCollecting();
}
