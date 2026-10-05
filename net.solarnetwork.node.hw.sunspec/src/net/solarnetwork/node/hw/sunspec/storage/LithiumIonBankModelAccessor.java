/* ==================================================================
 * LithiumIonBankModelAccessor.java - 5/10/2026 9:05:44 pm
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
 * API for accessing SunSpec lithium-ion battery bank model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>803</b>. Setter methods
 * write to the device immediately.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface LithiumIonBankModelAccessor extends ModelAccessor {

	/**
	 * API for a single string in the bank.
	 */
	interface BatteryString {

		/**
		 * Get the string index.
		 *
		 * @return the index, starting from {@literal 1}
		 */
		int getIndex();

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
		 * Get the string state of charge.
		 *
		 * @return the state of charge, as a percentage, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getStateOfCharge();

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
		 * Get the reason the string is disabled.
		 *
		 * @return the reason, or {@code null} if not available
		 */
		@Nullable
		BatteryDisabledReason getDisabledReason();

		/**
		 * Get the closed contactors.
		 *
		 * @return the indexes of the closed contactors, starting from
		 *         {@literal 0}, never {@code null}
		 */
		Set<Integer> getClosedContactors();

		/**
		 * Get the active string events.
		 *
		 * @return the events, as {@link LithiumIonStringEvent} values, never
		 *         {@code null}
		 */
		Set<ModelEvent> getEvents();

		/**
		 * Get the active vendor events.
		 *
		 * <p>
		 * The two vendor event fields are presented as a single bit set, where
		 * the first bit of the second field is index {@literal 32}.
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
		void setEnableOperation(ModbusConnection conn, BatteryEnableOperation operation)
				throws IOException;

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

	}

	/**
	 * Get the number of strings in the bank.
	 *
	 * @return the number of strings, or {@code null} if not available
	 */
	@Nullable
	Integer getStringCount();

	/**
	 * Get the number of strings with their contactor closed.
	 *
	 * @return the number of strings, or {@code null} if not available
	 */
	@Nullable
	Integer getConnectedStringCount();

	/**
	 * Get the maximum module temperature in the bank.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getMaximumModuleTemperature();

	/**
	 * Get the index of the string containing the module with the maximum module
	 * temperature.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumModuleTemperatureStringIndex();

	/**
	 * Get the index of the module with the maximum module temperature.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumModuleTemperatureModuleIndex();

	/**
	 * Get the minimum module temperature in the bank.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getMinimumModuleTemperature();

	/**
	 * Get the index of the string containing the module with the minimum module
	 * temperature.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumModuleTemperatureStringIndex();

	/**
	 * Get the index of the module with the minimum module temperature.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumModuleTemperatureModuleIndex();

	/**
	 * Get the average module temperature in the bank.
	 *
	 * @return the temperature, in degrees Celsius, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getAverageModuleTemperature();

	/**
	 * Get the maximum string voltage in the bank.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumStringVoltage();

	/**
	 * Get the index of the string with the maximum string voltage.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumStringVoltageStringIndex();

	/**
	 * Get the minimum string voltage in the bank.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMinimumStringVoltage();

	/**
	 * Get the index of the string with the minimum string voltage.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumStringVoltageStringIndex();

	/**
	 * Get the average string voltage in the bank.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getAverageStringVoltage();

	/**
	 * Get the maximum string current in the bank.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumStringCurrent();

	/**
	 * Get the index of the string with the maximum string current.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumStringCurrentStringIndex();

	/**
	 * Get the minimum string current in the bank.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getMinimumStringCurrent();

	/**
	 * Get the index of the string with the minimum string current.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumStringCurrentStringIndex();

	/**
	 * Get the average string current in the bank.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getAverageStringCurrent();

	/**
	 * Get the number of cells currently being balanced.
	 *
	 * @return the number of cells, or {@code null} if not available
	 */
	@Nullable
	Integer getBalancingCellCount();

	/**
	 * Get the strings.
	 *
	 * <p>
	 * The number of strings is the string count, limited to the number of
	 * strings the model length allows for.
	 * </p>
	 *
	 * @return the strings, never {@code null}
	 */
	List<BatteryString> getStrings();

}
