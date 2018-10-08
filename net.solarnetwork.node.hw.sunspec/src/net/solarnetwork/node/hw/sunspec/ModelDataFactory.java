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

import net.solarnetwork.node.hw.sunspec.inverter.IntegerInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelId;
import net.solarnetwork.node.hw.sunspec.meter.IntegerMeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * A factory for creating concrete {@link ModelData} instances based on
 * discovery of SunSpec properties on a device.
 * 
 * @author matt
 * @version 1.0
 */
public class ModelDataFactory {

	/**
	 * Get a factory instance.
	 * 
	 * @return the factory
	 */
	public static ModelDataFactory getInstance() {
		// right now this is simple and uses hard-coded lookups for model instances;
		// in the future we could use some sort of lookup strategy to discover instances to use
		return new ModelDataFactory();
	}

	/**
	 * Default constructor.
	 */
	protected ModelDataFactory() {
		super();
	}

	private int findSunSpecBaseAddress(ModbusConnection conn) {
		for ( ModelRegister r : ModelRegister.BASE_ADDRESSES ) {
			String s = conn.readString(ModbusReadFunction.ReadHoldingRegister, r.getAddress(),
					r.getWordLength(), true, ModbusConnection.ASCII_CHARSET);
			if ( ModelRegister.BASE_ADDRESS_MAGIC_STRING.equals(s) ) {
				return r.getAddress();
			}
		}
		throw new RuntimeException("SunSpec ID 'SunS' not found at any known base address.");
	}

	/**
	 * Create a new model data instance by discovering the model from a device
	 * via a Modbus connection.
	 * 
	 * <p>
	 * This method calls {@link #getModelData(ModbusConnection, int)} with an
	 * unlimited read word count.
	 * </p>
	 * 
	 * @param conn
	 *        the modbus connection
	 * @return the data
	 * @throws RuntimeException
	 *         if no supported model data can be discovered
	 */
	public ModelData getModelData(ModbusConnection conn) {
		return getModelData(conn, Integer.MAX_VALUE);
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
			int[] words = conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister,
					nextModelAddress, 2);
			model = null;
			if ( words != null && words.length > 1 ) {
				if ( words[0] != ModelId.SUN_SPEC_END_ID ) {
					ModelAccessor accessor = createAccessor(data, nextModelAddress, words[0], words[1]);
					data.addModel(words[1], accessor);
					model = accessor;
				}
			}
		} while ( model != null );

		data.readModelData(conn);
		return data;
	}

	private ModelAccessor createAccessor(ModelData data, int baseAddress, int modelId, int modelLength) {
		// 100 series: inverter
		try {
			InverterModelId id = InverterModelId.forId(modelId);
			switch (id) {
				case SinglePhaseInverterInteger:
				case SplitPhaseInverterInteger:
				case ThreePhaseInverterInteger:
					return new IntegerInverterModelAccessor(data, baseAddress, id);

				default:
					return null;
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}

		// 200 series: meter
		try {
			MeterModelId id = MeterModelId.forId(modelId);
			switch (id) {
				case SinglePhaseMeterInteger:
				case SplitSinglePhaseMeterInteger:
				case WyeConnectThreePhaseMeterInteger:
				case DeltaConnectThreePhaseMeterInteger:
					return new IntegerMeterModelAccessor(data, baseAddress, id);

				default:
					return null;
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}

		// fall back to generic
		return new GenericModelAccessor(data, baseAddress, new GenericModelId(modelId));
	}

}
