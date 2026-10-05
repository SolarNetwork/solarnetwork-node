/* ==================================================================
 * LithiumIonModuleModelAccessor.java - 5/10/2026 9:05:44 pm
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

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing SunSpec lithium-ion module model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>805</b>.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface LithiumIonModuleModelAccessor extends ModelAccessor {

	/**
	 * API for a single cell in the module.
	 */
	interface BatteryCell {

		/**
		 * Get the cell index.
		 *
		 * @return the index, starting from {@literal 1}
		 */
		int getIndex();

		/**
		 * Get the cell voltage.
		 *
		 * @return the voltage, in V, or {@code null} if not available
		 */
		@Nullable
		Float getVoltage();

		/**
		 * Get the cell temperature.
		 *
		 * @return the temperature, in degrees Celsius, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getTemperature();

		/**
		 * Get the cell status.
		 *
		 * @return the status flags, never {@code null}
		 */
		Set<LithiumIonCellStatus> getStatus();

	}

	/**
	 * Get the index of the string containing the module.
	 *
	 * @return the index, starting from {@literal 1}, or {@code null} if not
	 *         available
	 */
	@Nullable
	Integer getStringIndex();

	/**
	 * Get the index of the module within the string.
	 *
	 * @return the index, starting from {@literal 1}, or {@code null} if not
	 *         available
	 */
	@Nullable
	Integer getModuleIndex();

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
	 * Get the module depth of discharge.
	 *
	 * @return the depth of discharge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getDepthOfDischarge();

	/**
	 * Get the module state of health.
	 *
	 * @return the state of health, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfHealth();

	/**
	 * Get the number of cycles executed.
	 *
	 * @return the cycle count, or {@code null} if not available
	 */
	@Nullable
	Long getCycleCount();

	/**
	 * Get the module voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getDCVoltage();

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

	/**
	 * Get the number of cells in the module currently being balanced.
	 *
	 * @return the number of cells, or {@code null} if not available
	 */
	@Nullable
	Integer getBalancingCellCount();

	/**
	 * Get the module serial number.
	 *
	 * @return the serial number, or {@code null} if not available
	 */
	@Nullable
	String getSerialNumber();

	/**
	 * Get the cells.
	 *
	 * <p>
	 * The number of cells is determined by the model length.
	 * </p>
	 *
	 * @return the cells, never {@code null}
	 */
	List<BatteryCell> getCells();

}
