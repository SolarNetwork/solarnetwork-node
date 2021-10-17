/* ==================================================================
 * MessageHandler.java - 14/09/2021 10:18:05 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

/**
 * API for handling RFXCOM messages.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface MessageHandler {

	/**
	 * Handle a message.
	 * 
	 * @param message
	 *        the message to handle
	 * @return {@literal true} to listen for more messages, {@literal false} to
	 *         stop listening
	 */
	boolean handleMessage(Message message);

}
