/* ==================================================================
 * TestDataUtils.java - 8/11/2019 11:16:37 am
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

package net.solarnetwork.node.hw.satcon.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.test.DataUtils;

/**
 * Helper utilities for unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class TestDataUtils {

	private static final Logger log = LoggerFactory.getLogger(TestDataUtils.class);

	/**
	 * Parse modbus test data.
	 * 
	 * <p>
	 * This calls {@link DataUtils#parseModbusHexRegisterMappingLines} so the
	 * address offsets are honored.
	 * </p>
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the parsed data
	 */
	public static Map<Integer, Integer> parseTestData(Class<?> clazz, String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(
					new BufferedReader(new InputStreamReader(clazz.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return new HashMap<>();
		}
	}

}
