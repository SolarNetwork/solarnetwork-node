/* ==================================================================
 * StompCommand.java - 17/08/2021 4:28:11 PM
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

package net.solarnetwork.node.setup.stomp;

/**
 * STOMP commands.
 * 
 * @author matt
 * @version 1.0
 */
public enum StompCommand {

	ABORT,

	ACK,

	BEGIN,

	COMMIT,

	CONNECT,

	CONNECTED(true),

	DISCONNECT,

	ERROR(true),

	MESSAGE(true),

	NACK,

	RECEIPT(true),

	SEND,

	SUBSCRIBE,

	STOMP,

	UNSUBSCRIBE,

	;

	private final boolean serverInitiated;

	private StompCommand() {
		this.serverInitiated = false;
	}

	private StompCommand(boolean serverInitiated) {
		this.serverInitiated = serverInitiated;
	}

	/**
	 * Get the header value.
	 * 
	 * @return the value, never {@literal null}
	 */
	public String getValue() {
		return name();
	}

	/**
	 * Get "server initiated" flag.
	 * 
	 * @return {@literal true} if the command is initiated by the server, or
	 *         {@literal false} if initiated by the client
	 */
	public boolean isServerInitiated() {
		return serverInitiated;
	}

}
