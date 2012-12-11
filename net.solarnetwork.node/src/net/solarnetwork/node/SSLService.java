/* ==================================================================
 * SSLService.java - Dec 11, 2012 9:21:03 AM
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

import javax.net.ssl.SSLSocketFactory;

/**
 * API for dealing with SSL connections.
 * 
 * @author matt
 * @version 1.0
 */
public interface SSLService {

	/**
	 * Get a SSLSocketFactory configured appropriately for the SolarIn
	 * application.
	 * 
	 * @return
	 */
	SSLSocketFactory getSolarInSocketFactory();

}
