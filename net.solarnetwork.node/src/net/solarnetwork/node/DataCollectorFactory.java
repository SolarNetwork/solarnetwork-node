/* ==================================================================
 * DataCollectorFactory.java - Mar 24, 2012 8:19:45 PM
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
 */

package net.solarnetwork.node;

/**
 * API for a service that manages data collector instances.
 * 
 * @author matt
 * @version 1.1
 */
public interface DataCollectorFactory<T> extends Identifiable {

	/**
	 * Get a unique identifier for this factory. This should be meaningful to
	 * the factory implementation. For example a serial port based
	 * implementation could use the port identifier as the UID.
	 * 
	 * @return unique identifier
	 */
	@Override
	String getUID();

	/**
	 * Get a {@link DataCollector} instance.
	 * 
	 * @param params
	 *        the parameters to configure the collector with
	 * @return the instance
	 */
	DataCollector getDataCollectorInstance(T params);

	/**
	 * Get a {@link ConversationalDataCollector} instance.
	 * 
	 * @param params
	 *        the parameters to configure the collector with
	 * @return the instance
	 */
	ConversationalDataCollector getConversationalDataCollectorInstance(T params);

}
