/* ==================================================================
 * TestUtils.java - Jul 7, 2012 8:10:40 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.util.IntShortMap;

/**
 * Helper methods for unit tests.
 * 
 * @author matt
 * @version 1.1
 */
public final class TestUtils {

	private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

	/**
	 * Convert a hex-encoded string into bytes.
	 * 
	 * <p>
	 * Non hexadecimal characters are stripped from the input string, for
	 * example to remove spaces.
	 * </p>
	 * 
	 * @param str
	 *        the string
	 * @return the bytes
	 */
	public static byte[] bytesFromHexString(String str) {
		try {
			return Hex.decodeHex(str.replaceAll("[^0-9a-fA-F]", "").toCharArray());
		} catch ( DecoderException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parse modbus test data.
	 * 
	 * <p>
	 * This calls
	 * {@link DataUtils#parseModbusHexRegisterMappingLines(BufferedReader)} so
	 * the address offsets are honored.
	 * </p>
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the parsed data
	 */
	public static Map<Integer, Integer> parseTestData(Class<?> clazz, String resource) {
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource)))) {
			return DataUtils.parseModbusHexRegisterMappingLines(in);
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a {@link ModbusData} of test data.
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the connection
	 * @see #parseTestData(Class, String)
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static ModbusData testData(Class<?> clazz, String resource) {
		Map<Integer, Integer> map = parseTestData(clazz, resource);
		ModbusData d = new ModbusData();
		try {
			d.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) throws IOException {
					m.saveDataMap(map);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return d;
	}

	/**
	 * Get a {@link ModbusConnection} to test data.
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the connection
	 * @see #parseTestData(Class, String)
	 */
	public static ModbusConnection testDataConnection(Class<?> clazz, String resource) {
		Map<Integer, Integer> map = parseTestData(clazz, resource);
		IntShortMap ism = new IntShortMap(map.size());
		map.entrySet().stream().forEach((e) -> {
			ism.put(e.getKey(), (short) (e.getValue().intValue() & 0xFFFF));
		});
		return new StaticDataMapReadonlyModbusConnection(ism);
	}

}
