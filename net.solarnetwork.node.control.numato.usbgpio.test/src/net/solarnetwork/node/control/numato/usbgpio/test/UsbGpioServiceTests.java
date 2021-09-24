/* ==================================================================
 * UsbGpioServiceTests.java - 24/09/2021 4:16:20 PM
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

package net.solarnetwork.node.control.numato.usbgpio.test;

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.control.numato.usbgpio.UsbGpioService;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link UsbGpioService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UsbGpioServiceTests {

	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	private SerialNetwork network;
	private SerialConnection conn;
	private UsbGpioService service;

	@Before
	public void setup() {
		network = EasyMock.createMock(SerialNetwork.class);
		conn = EasyMock.createMock(SerialConnection.class);
		service = new UsbGpioService(new StaticOptionalService<>(network));
	}

	@After
	public void teardown() {
		EasyMock.verify(network, conn);
	}

	private void replayAll() {
		EasyMock.replay(network, conn);
	}

	private <T> void doConnAction() throws IOException {
		Object[] actionResult = new Object[1];
		expect(network.performAction(assertWith(new Assertion<SerialConnectionAction<T>>() {

			@Override
			public void check(SerialConnectionAction<T> argument) throws Throwable {
				actionResult[0] = argument.doWithConnection(conn);
			}

		}))).andAnswer(new IAnswer<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T answer() throws Throwable {
				return (T) actionResult[0];
			}
		});
	}

	@Test
	public void readVersion() throws Exception {
		// GIVEN
		final String version = "1.2.3";
		doConnAction();
		conn.writeMessage(aryEq("ver".getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(version.getBytes(US_ASCII));

		// WHEN
		replayAll();
		String result = service.getDeviceVersion();

		// THEN
		assertThat("Version returned", result, is(version));
	}

	@Test
	public void readId() throws Exception {
		// GIVEN
		final String id = "12345678";
		doConnAction();
		conn.writeMessage(aryEq("id get".getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(id.getBytes(US_ASCII));

		// WHEN
		replayAll();
		String result = service.getId();

		// THEN
		assertThat("ID returned", result, is(id));
	}

	@Test
	public void setId() throws Exception {
		// GIVEN
		final String id = "12345678";
		doConnAction();
		conn.writeMessage(aryEq(String.format("id set %s", id).getBytes(US_ASCII)));

		// WHEN
		replayAll();
		service.setId(id);

		// THEN
	}

	@Test(expected = IllegalArgumentException.class)
	public void setId_invalidLength() throws Exception {
		// GIVEN
		final String id = "123";

		// WHEN
		replayAll();
		service.setId(id);

		// THEN
	}

	@Test
	public void readOne_on() throws Exception {
		// GIVEN
		final int addr = 3;
		final String status = "on";
		doConnAction();
		conn.writeMessage(aryEq(String.format("gpio read %d", addr).getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(status.getBytes(US_ASCII));

		// WHEN
		replayAll();
		boolean result = service.read(addr);

		// THEN
		assertThat("IO is enabled", result, is(true));
	}

	@Test
	public void readOne_off() throws Exception {
		// GIVEN
		final int addr = 3;
		final String status = "off";
		doConnAction();
		conn.writeMessage(aryEq(String.format("gpio read %d", addr).getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(status.getBytes(US_ASCII));

		// WHEN
		replayAll();
		boolean result = service.read(addr);

		// THEN
		assertThat("IO is disabled", result, is(false));
	}

	@Test
	public void readAnalog() throws Exception {
		// GIVEN
		final int addr = 5;
		final String status = "123";
		doConnAction();
		conn.writeMessage(aryEq(String.format("adc read %d", addr).getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(status.getBytes(US_ASCII));

		// WHEN
		replayAll();
		int result = service.readAnalog(addr);

		// THEN
		assertThat("Analog value returned", result, is(123));
	}

	@Test
	public void setOne_on() throws Exception {
		// GIVEN
		final int addr = 3;
		doConnAction();
		conn.writeMessage(aryEq(String.format("gpio set %s", addr).getBytes(US_ASCII)));

		// WHEN
		replayAll();
		service.set(addr, true);

		// THEN
	}

	@Test
	public void setOne_off() throws Exception {
		// GIVEN
		final int addr = 3;
		doConnAction();
		conn.writeMessage(aryEq(String.format("gpio clear %s", addr).getBytes(US_ASCII)));

		// WHEN
		replayAll();
		service.set(addr, false);

		// THEN
	}

	@Test
	public void configureMask() throws Exception {
		// GIVEN
		doConnAction();
		conn.writeMessage(aryEq("gpio iomask 92".getBytes(US_ASCII)));

		// WHEN
		replayAll();
		BitSet set = new BitSet(); // 0x92
		set.set(1);
		set.set(4);
		set.set(7);
		service.configureWriteMask(set);

		// THEN
	}

	@Test
	public void configureMask_onlyLowerBits() throws Exception {
		// GIVEN
		doConnAction();
		conn.writeMessage(aryEq("gpio iomask 02".getBytes(US_ASCII)));

		// WHEN
		replayAll();
		BitSet set = new BitSet(); // 0x02
		set.set(1);
		service.configureWriteMask(set);

		// THEN
	}

	@Test
	public void configureIoDirection() throws Exception {
		// GIVEN
		doConnAction();
		conn.writeMessage(aryEq("gpio iodir 45".getBytes(US_ASCII)));

		// WHEN
		replayAll();
		BitSet set = new BitSet(); // 0x45
		set.set(0);
		set.set(2);
		set.set(6);
		service.configureIoDirection(set);

		// THEN
	}

	@Test
	public void writeAll() throws Exception {
		// GIVEN
		doConnAction();
		conn.writeMessage(aryEq("gpio writeall 23".getBytes(US_ASCII)));

		// WHEN
		replayAll();
		BitSet set = new BitSet(); // 0x23
		set.set(0);
		set.set(1);
		set.set(5);
		service.writeAll(set);

		// THEN
	}

	@Test
	public void writeAll_onlyLowerBits() throws Exception {
		// GIVEN
		doConnAction();
		conn.writeMessage(aryEq("gpio writeall 01".getBytes(US_ASCII)));

		// WHEN
		replayAll();
		BitSet set = new BitSet(); // 0x01
		set.set(0);
		service.writeAll(set);

		// THEN
	}

	@Test
	public void readAll() throws Exception {
		// GIVEN
		final String status = "DA";
		doConnAction();
		conn.writeMessage(aryEq("gpio readall".getBytes(US_ASCII)));
		expect(conn.drainInputBuffer()).andReturn(status.getBytes(US_ASCII));

		// WHEN
		replayAll();
		BitSet result = service.readAll();

		// THEN
		BitSet expected = new BitSet();
		expected.set(1);
		expected.set(3);
		expected.set(4);
		expected.set(6);
		expected.set(7);
		assertThat("Readall set returned", result, is(expected));
	}

}
