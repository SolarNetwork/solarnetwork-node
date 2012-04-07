/* ==================================================================
 * Command.java - Jun 27, 2011 12:58:21 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.jf2.lata.command;


/**
 * Enumeration of supported commands.
 * 
 * @author matt
 * @version $Revision$
 */
public enum Command implements CommandInterface {

	GetVersionLong("v\r", true),
	
	GetVersionShort("V\r", true),
	
	StartOperationalMode("O\r"),
	
	SetSpeed("S4\r"),
	
	SwitchOn("T%s26464\r"),
	
	SwitchOff("T%s26400\r"),
	
	SwitchStatus("T%s26500\r", true);
	
	private String data;
	private boolean response;
	
	private Command(String data) {
		this(data, false);
	}
	
	private Command(String data, boolean response) {
		this.data = data;
		this.response = response;
	}
	
	public byte[] getCommandData() {
		return this.data.getBytes();
	}
	
	public String getData() {
		return this.data;
	}

	@Override
	public boolean includesResponse() {
		return response;
	}
	
}
