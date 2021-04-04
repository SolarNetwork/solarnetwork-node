/* ==================================================================
 * ModelDataUtils.java - 9/10/2018 7:08:14 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.sunspec.inverter.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;

/**
 * Helper utility methods for model data testing.
 * 
 * @author matt
 * @version 2.0
 */
public final class ModelDataUtils {

	private static final Logger log = LoggerFactory.getLogger(ModelDataUtils.class);

	/**
	 * Parse modbus test data.
	 * 
	 * <p>
	 * This calls {@link DataUtils#parseModbusHexRegisterLines} so the address
	 * offsets are ignored and the returned array's data will always start at
	 * index {@literal 0}.
	 * </p>
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the parsed data
	 */
	public static int[] parseTestData(Class<?> clazz, String resource) {
		try {
			return DataUtils.parseModbusHexRegisterLines(
					new BufferedReader(new InputStreamReader(clazz.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading data resource [{}]", resource, e);
			return new int[0];
		}
	}

	/**
	 * Get a connection backed by a static, read-only set of data loaded from a
	 * class-path resource of modbus test data.
	 * 
	 * <p>
	 * This calls {@link #parseTestData(Class, String)} to parse a modbus data
	 * text file.
	 * </p>
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the connection backed by the static data
	 * @see #parseTestData(Class, String)
	 */
	public static ModbusConnection getStaticDataConnection(Class<?> clazz, String resource) {
		return new StaticDataMapReadonlyModbusConnection(parseTestData(clazz, resource));
	}

	/**
	 * Get a static, read-only model data instance using a class-path resource
	 * of modbus test data.
	 * 
	 * <p>
	 * This calls {@link #getStaticDataConnection(Class, String)} to parse a
	 * modbus data text file and then
	 * {@link ModelDataFactory#getModelData(ModbusConnection)} to read the data
	 * into a {@link ModelData} instance.
	 * </p>
	 * 
	 * @param clazz
	 *        the class to load the resource from
	 * @param resource
	 *        the data resource to load
	 * @return the model data
	 * @see #parseTestData(Class, String)
	 */
	public static ModelData getModelDataInstance(Class<?> clazz, String resource) {
		ModbusConnection conn = getStaticDataConnection(clazz, resource);
		try {
			return ModelDataFactory.getInstance().getModelData(conn);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

}
