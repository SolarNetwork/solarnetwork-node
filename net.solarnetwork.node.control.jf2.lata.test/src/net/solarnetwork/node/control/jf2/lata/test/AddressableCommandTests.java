/* ==================================================================
 * AddressableCommandTests.java - Oct 27, 2014 2:21:01 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.jf2.lata.test;

import static org.junit.Assert.fail;
import net.solarnetwork.node.control.jf2.lata.command.AddressableCommand;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandValidationException;
import org.junit.Test;

/**
 * Unit tests for the {@link AddressableCommand} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AddressableCommandTests {

	@Test
	public void validateIDTest() {

		//empty id invalid case
		try {
			new AddressableCommand("", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
			//ignorable
		}

		//empty id short id
		try {
			new AddressableCommand("10000AB", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
			//ignorable
		}

		//empty id invalid hex
		try {
			new AddressableCommand("ghijkl10", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
			//ignorable
		}

		//empty id invalid hex
		try {
			new AddressableCommand("ghijkl10", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
			//ignorable
		}

		//invalid more than 0x1FFFFFF
		try {
			new AddressableCommand("ABCD1234", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
		}

		//invalid
		try {
			new AddressableCommand("abcd1234", Command.SwitchOn);
			fail();
		} catch ( CommandValidationException ie ) {
		}

		//valid
		try {
			new AddressableCommand("1FFFFFFF", Command.SwitchOn);
		} catch ( CommandValidationException ie ) {
			fail();
		}

		//valid
		try {
			new AddressableCommand("100000BD", Command.SwitchOn);

		} catch ( CommandValidationException ie ) {
			fail();
		}

		//valid
		try {
			new AddressableCommand("100000FD", Command.SwitchOn);

		} catch ( CommandValidationException ie ) {
			fail();
		}
	}

}
