/* ==================================================================
 * ModelData.java - 22/05/2018 6:40:27 AM
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

import java.util.ArrayList;
import java.util.List;
import bak.pcj.set.IntRange;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Base object for model data.
 * 
 * @author matt
 * @version 1.1
 */
public class ModelData extends ModbusData implements CommonModelAccessor {

	private final int baseAddress;
	private final int blockAddress;
	private int maxReadWordsCount;
	private List<ModelAccessor> models;

	/**
	 * Constructor.
	 */
	public ModelData(int baseAddress) {
		super();
		this.maxReadWordsCount = Integer.MAX_VALUE;
		this.baseAddress = baseAddress;
		this.blockAddress = baseAddress + 2;
		this.models = new ArrayList<>(1);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public ModelData(ModbusData other) {
		super(other);
		if ( other instanceof ModelData ) {
			ModelData md = (ModelData) other;
			this.maxReadWordsCount = md.maxReadWordsCount;
			this.baseAddress = md.baseAddress;
			this.blockAddress = md.blockAddress;
			this.models = new ArrayList<>(md.models);
		} else {
			this.maxReadWordsCount = Integer.MAX_VALUE;
			this.baseAddress = 0;
			this.blockAddress = 2;
			this.models = new ArrayList<>(1);
		}
	}

	@Override
	public ModbusData copy() {
		return new ModelData(this);
	}

	/**
	 * Get a snapshot copy of the model.
	 * 
	 * <p>
	 * This is essentially the same as {@link #copy()} but cast to
	 * {@code ModelData}.
	 * </p>
	 * 
	 * @return the snapshot
	 * @see #copy()
	 */
	public ModelData getSnapshot() {
		return (ModelData) this.copy();
	}

	/**
	 * Get the first-available model instance.
	 */
	public ModelAccessor getModel() {
		return (models != null && !models.isEmpty() ? models.get(0) : null);
	}

	/**
	 * Get the first-available model as a specific type.
	 * 
	 * @return the model
	 * @throws ClassCastException
	 *         if the model is not of the requested type
	 */
	public <T extends ModelAccessor> T getTypedModel() {
		@SuppressWarnings("unchecked")
		T result = (T) getModel();
		return result;
	}

	/**
	 * Find the first-available model of a specific type.
	 * 
	 * @param type
	 *        the type of model to get
	 * @return the found model, or {@literal null} if not found
	 * @since 1.1
	 */
	public <T extends ModelAccessor> T findTypedModel(Class<T> type) {
		if ( CommonModelAccessor.class.isAssignableFrom(type) ) {
			@SuppressWarnings("unchecked")
			T result = (T) this;
			return result;
		}
		List<ModelAccessor> list = getModels();
		if ( list != null ) {
			for ( ModelAccessor ma : list ) {
				if ( type.isAssignableFrom(ma.getClass()) ) {
					@SuppressWarnings("unchecked")
					T result = (T) ma;
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Get the list of model instances.
	 * 
	 * @return the model instances
	 */
	public List<ModelAccessor> getModels() {
		return models;
	}

	/**
	 * Get the maximum number of Modbus registers to read in one request.
	 * 
	 * @return the maximum read word count; defaults to
	 *         {@link Integer#MAX_VALUE}
	 */
	public int getMaxReadWordsCount() {
		return maxReadWordsCount;
	}

	/**
	 * Set the maximum number of Modbus registers to read in one request.
	 * 
	 * @param maxReadWordsCount
	 *        the maxReadWordsCount to set; anything less than {@literal 1} is
	 *        ignored; set to {@link Integer#MAX_VALUE} for no limit
	 */
	public void setMaxReadWordsCount(int maxReadWordsCount) {
		if ( maxReadWordsCount < 1 ) {
			return;
		}
		this.maxReadWordsCount = maxReadWordsCount;
	}

	@Override
	public int getBaseAddress() {
		return baseAddress;
	}

	@Override
	public int getBlockAddress() {
		return blockAddress;
	}

	@Override
	public ModelId getModelId() {
		return CommonModelId.CommonModel;
	}

	@Override
	public int getModelLength() {
		return getNumber(ModelRegister.ModelLength, baseAddress).intValue();
	}

	/**
	 * Update a mutable data object with data read from a Modbus connection,
	 * using the {@link ModbusReadFunction#ReadHoldingRegister} function.
	 * 
	 * @param conn
	 *        the connection
	 * @param m
	 *        the mutable data
	 * @param ranges
	 *        the list of register addresses to read
	 * @see #updateData(ModbusConnection, MutableModbusData, ModbusReadFunction,
	 *      IntRange[])
	 */
	protected static void updateData(ModbusConnection conn, MutableModbusData m, IntRange[] ranges) {
		updateData(conn, m, ModbusReadFunction.ReadHoldingRegister, ranges);
	}

	/**
	 * Update a mutable data object with data read from a Modbus connection.
	 * 
	 * <p>
	 * This method will read a set of Modbus registers, treating them as
	 * unsigned short values and storing them on {@code m} via
	 * {@link MutableModbusData#saveDataArray(int[], int)}.
	 * </p>
	 * 
	 * @param conn
	 *        the connection
	 * @param m
	 *        the mutable data
	 * @param function
	 *        the Modbus read function to use
	 * @param ranges
	 *        the list of register addresses to read
	 */
	protected static void updateData(ModbusConnection conn, MutableModbusData m,
			ModbusReadFunction function, IntRange[] ranges) {
		for ( IntRange r : ranges ) {
			int[] data = conn.readUnsignedShorts(function, r.first(), r.length());
			m.saveDataArray(data, r.first());
		}
	}

	/**
	 * Read the common model properties from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readCommonModelData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// load in our model header to find the common model length (65/66)
				int[] data = conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, baseAddress,
						2);
				m.saveDataArray(data, baseAddress);
				updateData(conn, m, getAddressRanges(maxReadWordsCount));
				return true;
			}
		});
	}

	/**
	 * Add a model accessor to this model.
	 * 
	 * @param modelId
	 *        the model ID
	 * @param modelLength
	 *        the model length
	 * @param accessor
	 *        the accessor to associate with this model
	 */
	public final void addModel(int modelLength, ModelAccessor accessor) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { accessor.getModelId().getId(), modelLength },
						accessor.getBaseAddress());
				models.add(accessor);
				return true;
			}
		});
	}

	/**
	 * Read the model properties from the device for all configured models.
	 * 
	 * <p>
	 * This method will iterate over all {@link ModelAccessor} instances that
	 * have been added via {@link #addModel(int, ModelAccessor)}, and read the
	 * data necessary for all their properties.
	 * </p>
	 * 
	 * @param conn
	 *        the connection
	 */
	public void readModelData(final ModbusConnection conn) {
		List<ModelAccessor> accessors = getModels();
		if ( accessors == null ) {
			return;
		}
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				for ( ModelAccessor accessor : accessors ) {
					updateData(conn, m, accessor.getAddressRanges(maxReadWordsCount));
				}
				return true;
			}
		});
	}

	@Override
	public String getManufacturer() {
		return getAsciiString(CommonModelRegister.Manufacturer, blockAddress, true);
	}

	@Override
	public String getModelName() {
		return getAsciiString(CommonModelRegister.Model, blockAddress, true);
	}

	@Override
	public String getOptions() {
		return getAsciiString(CommonModelRegister.Options, blockAddress, true);
	}

	@Override
	public String getVersion() {
		return getAsciiString(CommonModelRegister.Version, blockAddress, true);
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(CommonModelRegister.SerialNumber, blockAddress, true);
	}

	@Override
	public Integer getDeviceAddress() {
		return getNumber(CommonModelRegister.DeviceAddress, blockAddress).intValue();
	}

}
