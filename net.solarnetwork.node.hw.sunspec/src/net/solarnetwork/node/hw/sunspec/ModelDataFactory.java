/* ==================================================================
 * ModelDataFactory.java - 22/05/2018 9:40:11 AM
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

package net.solarnetwork.node.hw.sunspec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * A factory for creating concrete {@link ModelData} instances based on
 * discovery of SunSpec properties on a device.
 * 
 * <p>
 * This factory looks for a
 * {@literal net/solarnetwork/node/hw/sunspec/model-accessors.properties}
 * resource that contains a mapping of SunSpec model IDs to associated
 * {@link ModalAccessor} classes. The classes are expected to provide a public
 * constructor that accepts the following arguments:
 * </p>
 * 
 * <ol>
 * <li>a {@link ModalData} instance</li>
 * <li>an {@code int} base Modbus address for the associated model data</li>
 * <li>an {@code int} model ID value</li>
 * </ol>
 * 
 * <p>
 * If no {@literal model-accessors.properties} resource is available, the
 * factory falls back to using the
 * {@literal net/solarnetwork/node/hw/sunspec/model-accessors-default.properties}
 * resource provided by this bundle.
 * </p>
 * 
 * @author matt
 * @version 1.4
 */
public class ModelDataFactory {

	/**
	 * The default value for the maximum number of Modbus words to read at one
	 * time.
	 * 
	 * @since 1.1
	 */
	public static final int DEFAULT_MAX_READ_WORDS_COUNT = 124;

	/**
	 * The name of the class-path resource with the {@code ModelAccessor}
	 * properties mapping.
	 * 
	 * @since 1.2
	 */
	public static final String MODEL_ACCESSOR_PROPERTIES_RESOURCE_NAME = "net/solarnetwork/node/hw/sunspec/model-accessors.properties";

	/**
	 * The name of the class-path resource with the built-in default
	 * {@code ModelAccessor} properties mapping.
	 * 
	 * @since 1.2
	 */
	public static final String DEFAULT_MODEL_ACCESSOR_PROPERTIES_RESOURCE_NAME = "net/solarnetwork/node/hw/sunspec/model-accessors-default.properties";

	private static final Logger log = LoggerFactory.getLogger(ModelDataFactory.class);

	private Properties accessorProperties = null;

	/**
	 * Get a factory instance.
	 * 
	 * <p>
	 * This will trigger the loading of the model accessor properties resource,
	 * as described in {@link #getModelAccessorProperties()}.
	 * </p>
	 * 
	 * @return the factory
	 */
	public static ModelDataFactory getInstance() {
		ModelDataFactory factory = new ModelDataFactory();
		factory.loadModelAccessorProperties();
		return factory;
	}

	/**
	 * Default constructor.
	 */
	protected ModelDataFactory() {
		super();
	}

	private int findSunSpecBaseAddress(ModbusConnection conn) {
		for ( ModelRegister r : ModelRegister.BASE_ADDRESSES ) {
			try {
				String s = conn.readString(ModbusReadFunction.ReadHoldingRegister, r.getAddress(),
						r.getWordLength(), true, ModbusDataUtils.ASCII_CHARSET);
				if ( ModelRegister.BASE_ADDRESS_MAGIC_STRING.equals(s) ) {
					return r.getAddress();
				}
			} catch ( RuntimeException e ) {
				// in case device throws error reading from address that is not base address, keep looking
				log.warn("Error looking for SunSpec ID at base address {}: {}", r.getAddress(),
						e.toString());
			}
		}
		throw new RuntimeException("SunSpec ID 'SunS' not found at any known base address.");
	}

	/**
	 * Create a new model data instance by discovering the model from a device
	 * via a Modbus connection.
	 * 
	 * <p>
	 * This method calls {@link #getModelData(ModbusConnection, int)} with a
	 * {@link #DEFAULT_MAX_READ_WORDS_COUNT} maximum read word count.
	 * </p>
	 * 
	 * @param conn
	 *        the modbus connection
	 * @return the data
	 * @throws RuntimeException
	 *         if no supported model data can be discovered
	 */
	public ModelData getModelData(ModbusConnection conn) {
		return getModelData(conn, DEFAULT_MAX_READ_WORDS_COUNT);
	}

	/**
	 * Create a new model data instance by discovering the model from a device
	 * via a Modbus connection.
	 * 
	 * @param conn
	 *        the modbus connection
	 * @param maxReadWordsCount
	 *        the maxReadWordsCount to set; anything less than {@literal 1} is
	 *        ignored; pass {@link Integer#MAX_VALUE} for no limit
	 * @return the data
	 * @throws RuntimeException
	 *         if no supported model data can be discovered
	 */
	public ModelData getModelData(ModbusConnection conn, int maxReadWordsCount) {
		final int sunSpecBaseAddress = findSunSpecBaseAddress(conn);
		ModelData data = new ModelData(sunSpecBaseAddress + 2);
		data.setMaxReadWordsCount(maxReadWordsCount);
		data.readCommonModelData(conn);

		ModelAccessor model = data;
		do {
			int nextModelAddress = model.getBlockAddress() + model.getModelLength();
			short[] words = conn.readWords(ModbusReadFunction.ReadHoldingRegister,
					nextModelAddress, 2);
			model = null;
			if ( words != null && words.length > 1 ) {
				if ( (words[0] & 0xFFFF) != ModelId.SUN_SPEC_END_ID ) {
					ModelAccessor accessor = createAccessor(data, nextModelAddress, words[0], words[1]);
					data.addModel(words[1], accessor);
					model = accessor;
				}
			}
		} while ( model != null );

		data.readModelData(conn);
		return data;
	}

	private void loadModelAccessorProperties() {
		accessorProperties = getModelAccessorProperties();
	}

	/**
	 * Load the {@link ModalAccessor} properties.
	 * 
	 * <p>
	 * This factory looks for a
	 * {@literal net/solarnetwork/node/hw/sunspec/model-accessors.properties}
	 * resource that contains a mapping of SunSpec model IDs to associated
	 * {@link ModalAccessor} classes. If that resource is not available, then
	 * the
	 * {@literal net/solarnetwork/node/hw/sunspec/model-accessors-default.properties}
	 * resource provided by this bundle will be used. Here's an example of the
	 * format of this resource:
	 * </p>
	 * 
	 * <pre>
	 * 101 = net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor
	 * 201 = net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelAccessor
	 * </pre>
	 * 
	 * @return the model accessor properties
	 */
	protected Properties getModelAccessorProperties() {
		Properties props = null;

		// try to load mapping with context class loader, if available
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl != null ) {
			props = propertiesWithClassLoader(cl);
		}

		// fall back to this class' class loader if not found
		if ( props == null ) {
			cl = getClass().getClassLoader();
			props = propertiesWithClassLoader(cl);
		}

		return props;
	}

	private Properties propertiesWithClassLoader(ClassLoader cl) {
		for ( String rsrcName : new String[] { MODEL_ACCESSOR_PROPERTIES_RESOURCE_NAME,
				DEFAULT_MODEL_ACCESSOR_PROPERTIES_RESOURCE_NAME } ) {
			try (InputStream in = cl.getResourceAsStream(rsrcName)) {
				if ( in != null ) {
					Properties props = new Properties();
					props.load(in);
					log.debug("Loaded SunSpec ModelAccessor mappings from {}", rsrcName);
					return props;
				}
			} catch ( IOException e ) {
				// ignore
			}
		}
		return null;
	}

	private ModelAccessor createAccessor(ModelData data, int baseAddress, int modelId, int modelLength) {
		Properties props = accessorProperties;
		if ( props != null ) {
			String accessorClassName = props.getProperty(String.valueOf(modelId));
			if ( accessorClassName != null ) {
				for ( ClassLoader cl : new ClassLoader[] {
						Thread.currentThread().getContextClassLoader(),
						data.getClass().getClassLoader() } ) {
					if ( cl == null ) {
						continue;
					}
					try {
						Class<?> clazz = cl.loadClass(accessorClassName);
						if ( clazz != null ) {
							Class<? extends ModelAccessor> maClass = clazz
									.asSubclass(ModelAccessor.class);
							Constructor<? extends ModelAccessor> constr = maClass
									.getConstructor(ModelData.class, Integer.TYPE, Integer.TYPE);
							return constr.newInstance(data, baseAddress, modelId);
						}
					} catch ( ClassNotFoundException | NoSuchMethodException | SecurityException
							| InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e ) {
						log.warn(
								"Error loading SunSpec ModelAccessor class {} for model {} using class loader {}: {}",
								accessorClassName, modelId, cl, e.toString());
					}
				}
			}
		}

		// fall back to generic
		return new GenericModelAccessor(data, baseAddress, new GenericModelId(modelId));
	}

}
