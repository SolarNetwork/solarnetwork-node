/* ==================================================================
 * LithiumIonStringModelAccessor.java - 5/10/2026 9:05:44 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.storage;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing SunSpec lithium-ion string model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>804</b>. Setter methods
 * write to the device immediately.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface LithiumIonStringModelAccessor extends ModelAccessor {

	/**
	 * API for a summary of a single module in the string.
	 */
	interface BatteryModule {

		/**
		 * Get the module index.
		 *
		 * @return the index, starting from {@literal 1}
		 */
		int getIndex();

		/**
		 * Get the number of cells in the module.
		 *
		 * @return the number of cells, or {@code null} if not available
		 */
		@Nullable
		Integer getCellCount();

		/**
		 * Get the module state of charge.
		 *
		 * @return the state of charge, as a percentage, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getStateOfCharge();

		/**
		 * Get the module state of health.
		 *
		 * @return the state of health, as a percentage, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getStateOfHealth();

		/**
		 * Get the maximum cell voltage in the module.
		 *
		 * @return the voltage, in V, or {@code null} if not available
		 */
		@Nullable
		Float getMaximumCellVoltage();

		/**
		 * Get the index of the cell with the maximum cell voltage.
		 *
		 * @return the cell index, or {@code null} if not available
		 */
		@Nullable
		Integer getMaximumCellVoltageCellIndex();

		/**
		 * Get the minimum cell voltage in the module.
		 *
		 * @return the voltage, in V, or {@code null} if not available
		 */
		@Nullable
		Float getMinimumCellVoltage();

		/**
		 * Get the index of the cell with the minimum cell voltage.
		 *
		 * @return the cell index, or {@code null} if not available
		 */
		@Nullable
		Integer getMinimumCellVoltageCellIndex();

		/**
		 * Get the average cell voltage in the module.
		 *
		 * @return the voltage, in V, or {@code null} if not available
		 */
		@Nullable
		Float getAverageCellVoltage();

		/**
		 * Get the maximum cell temperature in the module.
		 *
		 * @return the temperature, in degrees Celsius, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getMaximumCellTemperature();

		/**
		 * Get the index of the cell with the maximum cell temperature.
		 *
		 * @return the cell index, or {@code null} if not available
		 */
		@Nullable
		Integer getMaximumCellTemperatureCellIndex();

		/**
		 * Get the minimum cell temperature in the module.
		 *
		 * @return the temperature, in degrees Celsius, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getMinimumCellTemperature();

		/**
		 * Get the index of the cell with the minimum cell temperature.
		 *
		 * @return the cell index, or {@code null} if not available
		 */
		@Nullable
		Integer getMinimumCellTemperatureCellIndex();

		/**
		 * Get the average cell temperature in the module.
		 *
		 * @return the temperature, in degrees Celsius, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getAverageCellTemperature();

	}

	/**
	 * Get the index of the string within the bank.
	 *
	 * @return the index, starting from {@literal 1}, or {@code null} if not
	 *         available
	 */
	@Nullable
	Integer getStringIndex();

	/**
	 * Get the number of modules in the string.
	 *
	 * @return the number of modules, or {@code null} if not available
	 */
	@Nullable
	Integer getModuleCount();

	/**
	 * Get the string status.
	 *
	 * @return the status flags, never {@code null}
	 */
	Set<BatteryConnectionStatus> getStatus();

	/**
	 * Get the reason the string failed to connect when the battery was last
	 * asked to connect.
	 *
	 * @return the reason, or {@code null} if not available
	 */
	@Nullable
	BatteryConnectionFailure getConnectionFailure();

	/**
	 * Get the number of cells in the string currently being balanced.
	 *
	 * @return the number of cells, or {@code null} if not available
	 */
	@Nullable
	Integer getBalancingCellCount();

	/**
	 * Get the string state of charge.
	 *
	 * @return the state of charge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfCharge();

	/**
	 * Get the string depth of discharge.
	 *
	 * @return the depth of discharge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getDepthOfDischarge();

	/**
	 * Get the number of discharge cycles executed.
	 *
	 * @return the cycle count, or {@code null} if not available
	 */
	@Nullable
	Long getCycleCount();

	/**
	 * Get the string state of health.
	 *
	 * @return the state of health, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfHealth();

	/**
	 * Get the string current.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getDCCurrent();

	/**
	 * Get the string voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getDCVoltage();

	/**
	 * Get the maximum cell voltage in the string.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumCellVoltage();

	/**
	 * Get the index of the module containing the cell with the maximum cell
	 * voltage.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumCellVoltageModuleIndex();

	/**
	 * Get the minimum cell voltage in the string.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMinimumCellVoltage();

	/**
	 * Get the index of the module containing the cell with the minimum cell
	 * voltage.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumCellVoltageModuleIndex();

	/**
	 * Get the average cell voltage in the string.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getAverageCellVoltage();

	/**
	 * Get the maximum module temperature in the string.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getMaximumModuleTemperature();

	/**
	 * Get the index of the module with the maximum module temperature.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumModuleTemperatureModuleIndex();

	/**
	 * Get the minimum module temperature in the string.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getMinimumModuleTemperature();

	/**
	 * Get the index of the module with the minimum module temperature.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumModuleTemperatureModuleIndex();

	/**
	 * Get the average module temperature in the string.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getAverageModuleTemperature();

	/**
	 * Get the closed contactors.
	 *
	 * @return the indexes of the closed contactors, starting from {@literal 0},
	 *         never {@code null}
	 */
	Set<Integer> getClosedContactors();

	/**
	 * Get the active string events.
	 *
	 * @return the events, as {@link LithiumIonStringEvent} values, never
	 *         {@code null}
	 */
	Set<? extends ModelEvent> getEvents();

	/**
	 * Get the active vendor events.
	 *
	 * <p>
	 * The two vendor event fields are presented as a single bit set, where the
	 * first bit of the second field is index {@literal 32}.
	 * </p>
	 *
	 * @return the vendor events, never {@code null}
	 */
	BitSet getVendorEvents();

	/**
	 * Get the string enable or disable operation in progress.
	 *
	 * @return the operation, which the device clears when complete, or
	 *         {@code null} if not available
	 */
	@Nullable
	BatteryEnableOperation getEnableOperation();

	/**
	 * Request the string be enabled or disabled.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param operation
	 *        the operation to perform
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setEnableOperation(ModbusConnection conn, BatteryEnableOperation operation) throws IOException;

	/**
	 * Get the string connect or disconnect operation in progress.
	 *
	 * @return the operation, which the device clears when complete, or
	 *         {@code null} if not available
	 */
	@Nullable
	BatteryOperation getConnectOperation();

	/**
	 * Request the string be connected or disconnected.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param operation
	 *        the operation to perform
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setConnectOperation(ModbusConnection conn, BatteryOperation operation) throws IOException;

	/**
	 * Get the modules.
	 *
	 * <p>
	 * The number of modules is the module count, limited to the number of
	 * modules the model length allows for.
	 * </p>
	 *
	 * @return the modules, never {@code null}
	 */
	List<BatteryModule> getModules();

}
