/* ==================================================================
 * SwitchTest.java - Jun 27, 2011 1:12:27 PM
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

package net.solarnetwork.node.control.jf2.lata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import net.solarnetwork.node.control.jf2.lata.Converser;
import net.solarnetwork.node.control.jf2.lata.LATABusConverser;
import net.solarnetwork.node.control.jf2.lata.command.AddressableCommand;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandValidationException;
import net.solarnetwork.node.control.jf2.lata.command.ToggleMode;
import net.solarnetwork.node.io.rxtx.SerialPortConversationalDataCollector;
import net.solarnetwork.node.io.rxtx.SerialPortConversationalDataCollectorFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for controlling the JF2 LATA switch, 
 * to toggle it on and off.
 * 
 * @author matt
 * @version $Revision$
 */
public class SwitchTest extends BaseTestSupport {

	private static final String LONG_VERSION = "v0106\r";
	
	private static final String SHORT_VERSION = "V9999\r";
	
	private static final String SWITCH_1_IDENTIFIER = "100000BD";
	
	private static final String SWITCH_2_IDENTIFIER = "100000FD";
	
	@Autowired private SerialPortConversationalDataCollectorFactory spcdcf;
	
	private SerialPortConversationalDataCollector dc;
	
	@Before
	public void setUp() {
		dc = spcdcf.getObject();
	}
	
	@After
	public void cleanup() {
		if (dc != null) {
			dc.stopCollecting();
		}
	}
	
	@Test
	public void getVersionLong() throws Exception {
		String result = dc.collectData(new Converser(Command.GetVersionLong));
		assertNotNull(result);
		assertEquals(LONG_VERSION, result);
	}
	
	@Test
	public void getVersionShort() throws Exception {
		String result = dc.collectData(new Converser(Command.GetVersionShort));
		assertNotNull(result);
		assertEquals(SHORT_VERSION, result);
	}
	
	@Test
	public void validateIDTest() {
		
		//empty id invalid case
		try {
		   new AddressableCommand("", Command.SwitchOn);
		   fail();
		} catch (CommandValidationException ie) {
			//ignorable
		}
		
		//empty id short id
		try {
			new AddressableCommand("10000AB", Command.SwitchOn);
			fail();
		} catch (CommandValidationException ie) {
			//ignorable
		}
		
		//empty id invalid hex
		try {
			new AddressableCommand("ghijkl10", Command.SwitchOn);
			fail();
		} catch (CommandValidationException ie) {
			//ignorable
		}
		
		//empty id invalid hex
		try {
			new AddressableCommand("ghijkl10", Command.SwitchOn);
			fail();
		} catch (CommandValidationException ie) {
			//ignorable
		}

		//invalid more than 0x1FFFFFF
		try {
			new AddressableCommand("ABCD1234", Command.SwitchOn);
			fail();
		} catch (CommandValidationException ie) {
		}
		
		//invalid
		try {
			new AddressableCommand("abcd1234", Command.SwitchOn);
			fail();
		} catch (CommandValidationException ie) {
		}
		
		//valid
		try {
			new AddressableCommand("1FFFFFFF", Command.SwitchOn);
		} catch (CommandValidationException ie) {
			fail();
		}
		
		
		//valid
		try {
			new AddressableCommand("100000BD", Command.SwitchOn);
			
		} catch (CommandValidationException ie) {
			fail();
		}
		
		//valid
		try {
			new AddressableCommand("100000FD", Command.SwitchOn);
			
		} catch (CommandValidationException ie) {
			fail();
		}
	}
	
	@Test
	public void  toggleSwitch1Off() {
		String result;
		try {
			// turn switch off
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_1_IDENTIFIER, Command.SwitchOff)));
			//confirm switch is off;
			cleanup();
			setUp();
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_1_IDENTIFIER, Command.SwitchStatus)));
			assertNotNull(result);
			int resultLength = result.length();
			assertEquals(18, resultLength);
			assertEquals(result.substring(resultLength-3, resultLength-1), ToggleMode.OFF.hexString());
		} catch (CommandValidationException e) {
			fail();
		}
	}
	
	@Test
	public void  toggleSwitch2Off() {
		String result;
		try {
			// turn switch off
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_2_IDENTIFIER, Command.SwitchOff)));
			cleanup();
			setUp();
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_2_IDENTIFIER, Command.SwitchStatus)));
			assertNotNull(result);
			int resultLength = result.length();
			assertEquals(18, resultLength);
			assertEquals(result.substring(resultLength-3, resultLength-1), ToggleMode.OFF.hexString());
		} catch (CommandValidationException e) {
			fail();
		}
	}
	
	@Test
	public void  toggleSwitch1On() {
		String result;
		try {
			// turn switch on
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_1_IDENTIFIER, Command.SwitchOn)));
			cleanup();
			setUp();
			//confirm switch is on
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_1_IDENTIFIER, Command.SwitchStatus)));
			assertNotNull(result);
			int resultLength = result.length();
			assertEquals(18, resultLength);
			assertEquals(result.substring(resultLength-3, resultLength-1), ToggleMode.ON.hexString());
		} catch (CommandValidationException e) {
			fail();
		}
	}
	
	@Test
	public void  toggleSwitch2On() {
		String result;
		try {
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_2_IDENTIFIER, Command.SwitchOn)));
			//confirm switch is on
			cleanup();
			setUp();
			result = dc.collectData(new LATABusConverser(new AddressableCommand(SWITCH_2_IDENTIFIER,Command.SwitchStatus)));
			assertNotNull(result);
			int resultLength = result.length();
			assertEquals(18, resultLength);
			assertEquals(result.substring(resultLength-3, resultLength-1), ToggleMode.ON.hexString());
		} catch (CommandValidationException e) {
			fail();
		}
	}
}
