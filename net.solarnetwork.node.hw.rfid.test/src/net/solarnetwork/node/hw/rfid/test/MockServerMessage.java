/* ==================================================================
 * MockServerMessage.java - 29/07/2016 6:53:56 PM
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

package net.solarnetwork.node.hw.rfid.test;

/**
 * Helper class for the mock RFID server.
 * 
 * @author matt
 * @version 1.0
 */
public class MockServerMessage {

	private final long pause;
	private final String message;

	public MockServerMessage(String message) {
		this(0, message);
	}

	public MockServerMessage(long pause, String message) {
		this.pause = pause;
		this.message = message;
	}

	public long getPause() {
		return pause;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return message;
	}

}
