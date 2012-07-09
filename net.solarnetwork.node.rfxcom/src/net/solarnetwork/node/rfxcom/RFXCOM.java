/* ==================================================================
 * RFXCOM.java - Jul 9, 2012 3:37:58 PM
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

import net.solarnetwork.node.ConversationalDataCollector;

/**
 * API for working with RFXCOM transceivers.
 * 
 * @author matt
 * @version $Revision$
 */
public interface RFXCOM {

	/**
	 * Get the unique ID of this instance.
	 * 
	 * <p>The unique ID is related to the serial port this instance is configured
	 * to use.</p>
	 * 
	 * @return unique ID
	 */
	String getUID();
	
	/**
	 * Get a {@link ConversationalDataCollector} configured to work with
	 * this RFXCOM instance.
	 * 
	 * <p>Note this may return <em>null</em>, if the {@link DataCollector}
	 * is not available. This can happen if the RFXCOM instance has not
	 * been configured yet, for example assigned a serial port.</p>
	 * 
	 * @return a new ConverstaionalDataCollector instance, or <em>null</em>
	 */
	ConversationalDataCollector getDataCollectorInstance();
	
}
