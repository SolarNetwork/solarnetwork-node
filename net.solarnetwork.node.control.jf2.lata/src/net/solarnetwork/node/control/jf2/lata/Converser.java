/* ==================================================================
 * Converser.java - Jun 27, 2011 1:06:01 PM
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

package net.solarnetwork.node.control.jf2.lata;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.control.jf2.lata.command.CommandInterface;

/**
 * ConversationalDataCollector.Moderator for LATA switch.
 * 
 * @author matt
 * @version $Revision$
 */
public class Converser implements ConversationalDataCollector.Moderator<String> {

	private final CommandInterface command;
	
	/**
	 * Constructor.
	 * 
	 * @param command the command to execute.
	 */
	public Converser(CommandInterface command) {
		this.command = command;
	}
	
	@Override
	public String conductConversation(ConversationalDataCollector dataCollector) {
		// depending on command, use speak() or speakAndListen()
		dataCollector.speakAndListen(command.getCommandData());
		return dataCollector.getCollectedDataAsString();
	}
	
	public CommandInterface getCommand() {
		return command;
	}

}
