/* ==================================================================
 * KioskDataService.java - 23/10/2016 6:30:17 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.kiosk.web;

import java.util.Map;

/**
 * Service API for the collection of data needed to drive the kiosk display.
 * 
 * @author matt
 * @version 1.0
 */
public interface KioskDataService {

	/** A message topic for kiosk data changes. */
	String MESSAGE_TOPIC_KIOSK_DATA = "/pub/topic/ocpp/kiosk";

	/**
	 * Initialize the service. Call this once after all properties configured.
	 */
	void startup();

	/**
	 * Shutdown the service, releasing any associated resources.
	 */
	void shutdown();

	/**
	 * Get a map of all data necessary for the kiosk to display.
	 * 
	 * @return The data.
	 */
	Map<String, Object> getKioskData();

	/**
	 * Refresh the kiosk data.
	 * 
	 * This should be called periodically to keep the data updated.
	 */
	void refreshKioskData();

}
