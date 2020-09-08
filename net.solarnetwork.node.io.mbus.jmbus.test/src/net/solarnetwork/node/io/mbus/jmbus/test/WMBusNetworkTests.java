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

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.openmuc.jmbus.wireless.WMBusMessageDecoder;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataRecord;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.io.mbus.MBusMessage;
import net.solarnetwork.node.io.mbus.MBusMessageHandler;
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

		@Override
		protected String getNetworkDescription() {
			return "mock";
		}

	}

	private static JMBusWMBusNetwork network = new MockJMBusWMBusNetwork();
	private static WMBusConnection conn;
	private static Map<SecondaryAddress, byte[]> keyMap = new HashMap<SecondaryAddress, byte[]>();

	/**
	 * 
	 * Create and prime the connection with test data
	 * 
	 * @throws IOException
	 * @throws DecodingException
	 */
	@BeforeClass
	public static void prepareConnection() throws IOException, DecodingException {
		final byte[] bytes = copyToByteArray(
				WMBusNetworkTests.class.getResourceAsStream("wmbus-message.bin"));
		final byte[] key = { (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
				(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB,
				(byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB, (byte) 0xAB };
		final SecondaryAddress address = SecondaryAddress.newFromWMBusHeader(bytes, 2);
		keyMap.put(address, key);
		conn = network.createConnection(JMBusConversion.from(address), key);
	}

	@Test
	public void receiveMessage() throws IOException, DecodingException {
		final byte[] bytes = copyToByteArray(
				WMBusNetworkTests.class.getResourceAsStream("wmbus-message.bin"));
		final WMBusMessage msg = WMBusMessageDecoder.decode(bytes, 0, keyMap);

		final MBusData expected = new MBusData(new Date());
		expected.dataRecords
				.add(new MBusDataRecord(MBusDataDescription.Volume, MBusDataType.BCD, 27L, -3));
		expected.dataRecords
				.add(new MBusDataRecord(MBusDataDescription.DateTime, new Date(1593064440000L)));

		MBusMessageHandler messageHandler = EasyMock.createMock(MBusMessageHandler.class);
		messageHandler.handleMessage(new MBusMessage(expected));
		expectLastCall();
		replay(messageHandler);
		conn.open(messageHandler);
		network.newMessage(msg);
		verify(messageHandler);
	}

}
