/* ==================================================================
 * UsbRelayUtilsTests.java - 18/06/2019 9:42:09 am
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

package net.solarnetwork.node.hw.lctech.relay.test;

import static net.solarnetwork.node.hw.lctech.relay.UsbRelayUtils.DEFAULT_IDENTITY;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import java.io.IOException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.lctech.relay.UsbRelayUtils;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Test cases for the {@link UsbRelayUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UsbRelayUtilsTests {

	private SerialConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(SerialConnection.class);
	}

	private void replayAll() {
		EasyMock.replay(conn);
	}

	@After
	public void teardown() {
		EasyMock.verify(conn);
	}

	private static final byte IDENT = (byte) (DEFAULT_IDENTITY & 0xFF);

	@Test
	public void controlStateOpen1() throws IOException {
		// given
		expect(conn.getPortName()).andReturn("/dev/ttyTEST0");
		conn.writeMessage(aryEq(
				new byte[] { IDENT, (byte) 1, (byte) 1, (byte) ((DEFAULT_IDENTITY + 1 + 1) & 0xFF) }));

		// when
		replayAll();
		UsbRelayUtils.setRelayState(conn, IDENT, 1, true);
	}

	@Test
	public void controlStateClose1() throws IOException {
		// given
		expect(conn.getPortName()).andReturn("/dev/ttyTEST0");
		conn.writeMessage(aryEq(
				new byte[] { IDENT, (byte) 1, (byte) 0, (byte) ((DEFAULT_IDENTITY + 1 + 0) & 0xFF) }));

		// when
		replayAll();
		UsbRelayUtils.setRelayState(conn, IDENT, 1, false);
	}

	@Test
	public void controlStateOpen2() throws IOException {
		// given
		expect(conn.getPortName()).andReturn("/dev/ttyTEST0");
		conn.writeMessage(aryEq(
				new byte[] { IDENT, (byte) 2, (byte) 1, (byte) ((DEFAULT_IDENTITY + 2 + 1) & 0xFF) }));

		// when
		replayAll();
		UsbRelayUtils.setRelayState(conn, IDENT, 2, true);
	}

	@Test
	public void controlStateClose2() throws IOException {
		// given
		expect(conn.getPortName()).andReturn("/dev/ttyTEST0");
		conn.writeMessage(aryEq(
				new byte[] { IDENT, (byte) 2, (byte) 0, (byte) ((DEFAULT_IDENTITY + 2 + 0) & 0xFF) }));

		// when
		replayAll();
		UsbRelayUtils.setRelayState(conn, IDENT, 2, false);
	}
}
