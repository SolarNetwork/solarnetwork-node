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
 */

package net.solarnetwork.node.rfxcom;

import java.io.IOException;
import net.solarnetwork.service.Identifiable;

/**
 * API for working with RFXCOM transceivers.
 * 
 * @author matt
 * @version 2.0
 */
public interface RFXCOM extends Identifiable {

	/**
	 * Listen for messages.
	 * 
	 * @param handler
	 *        the handler to accept messages
	 * @throws IOException
	 *         if an error occurs
	 */
	void listenForMessages(MessageHandler handler) throws IOException;

}
