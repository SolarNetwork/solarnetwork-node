/* ==================================================================
 * MessageTypeTests.java - 20/09/2019 6:44:22 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;

/**
 * Test cases for the {@link MessageType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MessageTypeTests {

	@Test
	public void enumRoundtrip() {
		for ( MessageType type : MessageType.values() ) {
			String cmd = type.getCommand();
			MessageType resolved = MessageType.forCommand(cmd);
			assertThat("Round trip type <-> command", resolved, equalTo(type));
		}
	}

	@Test
	public void unknownCommand() {
		MessageType type = MessageType.forCommand("this is not a known command");
		assertThat("Unknown command resolves to null", type, nullValue());
	}

}
