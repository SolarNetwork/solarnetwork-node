/* ==================================================================
 * WMBusNetworkTests.java - 25/06/2020 11:41:52 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.jmbus.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.openmuc.jmbus.wireless.WMBusMessageDecoder;
import net.solarnetwork.node.io.mbus.MBusDataRecord;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.io.mbus.WMBusConnection;
import net.solarnetwork.node.io.mbus.jmbus.JMBusConversion;
import net.solarnetwork.node.io.mbus.jmbus.JMBusSerialWMBusNetwork;
import net.solarnetwork.node.io.mbus.jmbus.JMBusWMBusNetwork;

/**
 * 
 * Test cases for the {@link JMBusSerialWMBusNetwork} class.
 * 
 * @author alex
 * @version 1.0
 */
public class WMBusNetworkTests {

	/**
	 * 
	 * Mock Network implementation
	 */
	private static class MockJMBusWMBusNetwork extends JMBusWMBusNetwork {

		private class MockConnection implements org.openmuc.jmbus.wireless.WMBusConnection {

			@Override
			public void close() throws IOException {
			}

			@Override
			public void addKey(SecondaryAddress address, byte[] key) {
			}

			@Override
			public void removeKey(SecondaryAddress address) {
			}

		}

		@Override
		protected org.openmuc.jmbus.wireless.WMBusConnection createJMBusConnection() {
			return new MockConnection();
		}

	}

	private static JMBusWMBusNetwork network = new MockJMBusWMBusNetwork();
	private static WMBusConnection conn;

	/**
	 * 
	 * Create and prime the connection with test data
	 * 
	 * @throws IOException
	 * @throws DecodingException
	 */
	@BeforeClass
	public static void prepareConnection() throws IOException, DecodingException {
		final byte[] bytes = WMBusNetworkTests.class.getResourceAsStream("wmbus-message.bin")
				.readAllBytes();
		final Map<SecondaryAddress, byte[]> keyMap = new HashMap<SecondaryAddress, byte[]>();
		final byte[] key = { (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
				(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
				(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB };
		final SecondaryAddress address = SecondaryAddress.newFromWMBusHeader(bytes, 2);
		keyMap.put(address, key);
		conn = network.createConnection(JMBusConversion.from(address), key);
		conn.open();
		final WMBusMessage msg = WMBusMessageDecoder.decode(bytes, 0, keyMap);
		msg.getVariableDataResponse().decode();
		network.newMessage(msg);
	}

	@Test
	public void readDouble() {
		final double expected = 0.027;
		final MBusDataRecord record = conn.getDataRecord(MBusDataType.Volume);
		assertNotNull(record);
		assertEquals(expected, record.getDoubleValue(), 0.0000000001);
	}

}
