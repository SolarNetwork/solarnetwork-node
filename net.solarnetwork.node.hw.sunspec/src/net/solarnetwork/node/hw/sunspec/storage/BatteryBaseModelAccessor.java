/* ==================================================================
 * BatteryBaseModelAccessor.java - 5/10/2026 7:58:02 pm
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
import java.time.LocalDate;
import java.util.BitSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.der.DerLocalRemoteControl;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing SunSpec battery base model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>802</b>. Setter methods
 * write to the device immediately, and throw {@link IllegalArgumentException}
 * if the value is not valid for the point, or {@link IllegalStateException} if
 * the model scale factors have not been read from the device or are not
 * implemented.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface BatteryBaseModelAccessor extends ModelAccessor {

	/**
	 * Get the nameplate charge capacity.
	 *
	 * @return the capacity, in Ah, or {@code null} if not available
	 */
	@Nullable
	Float getChargeCapacityRating();

	/**
	 * Get the nameplate energy capacity.
	 *
	 * @return the capacity, in Wh, or {@code null} if not available
	 */
	@Nullable
	Long getEnergyCapacityRating();

	/**
	 * Get the nameplate maximum charge rate.
	 *
	 * @return the rate, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getChargeRateMaximumRating();

	/**
	 * Get the nameplate maximum discharge rate.
	 *
	 * @return the rate, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getDischargeRateMaximumRating();

	/**
	 * Get the self discharge rate.
	 *
	 * @return the rate, as a percentage of the energy capacity discharged per
	 *         day, or {@code null} if not available
	 */
	@Nullable
	Float getSelfDischargeRate();

	/**
	 * Get the nameplate maximum state of charge.
	 *
	 * @return the state of charge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfChargeMaximumRating();

	/**
	 * Get the nameplate minimum state of charge.
	 *
	 * @return the state of charge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfChargeMinimumRating();

	/**
	 * Get the maximum reserve setting.
	 *
	 * @return the reserve, as a percentage of the nominal maximum storage, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getStateOfChargeReserveMaximum();

	/**
	 * Set the maximum reserve setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the reserve, as a percentage of the nominal maximum storage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setStateOfChargeReserveMaximum(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the minimum reserve setting.
	 *
	 * @return the reserve, as a percentage of the nominal maximum storage, or
	 *         {@code null} if not available
	 */
	@Nullable
	Float getStateOfChargeReserveMinimum();

	/**
	 * Set the minimum reserve setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the reserve, as a percentage of the nominal maximum storage
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setStateOfChargeReserveMinimum(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the state of charge.
	 *
	 * @return the state of charge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfCharge();

	/**
	 * Get the depth of discharge.
	 *
	 * <p>
	 * This is the charge removed from the battery as a percentage of its
	 * nameplate charge capacity.
	 * </p>
	 *
	 * @return the depth of discharge, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getDepthOfDischarge();

	/**
	 * Get the state of health.
	 *
	 * @return the state of health, as a percentage, or {@code null} if not
	 *         available
	 */
	@Nullable
	Float getStateOfHealth();

	/**
	 * Get the number of full discharge cycles executed.
	 *
	 * @return the cycle count, or {@code null} if not available
	 */
	@Nullable
	Long getCycleCount();

	/**
	 * Get the charge status.
	 *
	 * @return the status, or {@code null} if not available
	 */
	@Nullable
	BatteryChargeStatus getChargeStatus();

	/**
	 * Get the local or remote control mode.
	 *
	 * <p>
	 * In local mode all remote commands are refused, for example during
	 * maintenance.
	 * </p>
	 *
	 * @return the mode, or {@code null} if not available
	 */
	@Nullable
	DerLocalRemoteControl getLocalRemoteControl();

	/**
	 * Get the battery heartbeat.
	 *
	 * <p>
	 * The battery increments this value every second, with periodic resets to
	 * zero.
	 * </p>
	 *
	 * @return the heartbeat, or {@code null} if not available
	 */
	@Nullable
	Integer getBatteryHeartbeat();

	/**
	 * Get the controller heartbeat.
	 *
	 * @return the heartbeat, or {@code null} if not available
	 */
	@Nullable
	Integer getControllerHeartbeat();

	/**
	 * Set the controller heartbeat.
	 *
	 * <p>
	 * The controller is expected to increment this value every second, with
	 * periodic resets to zero.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param heartbeat
	 *        the heartbeat value to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setControllerHeartbeat(ModbusConnection conn, int heartbeat) throws IOException;

	/**
	 * Test if an alarm reset is in progress.
	 *
	 * <p>
	 * The battery clears the alarm reset when it has finished resetting latched
	 * alarms.
	 * </p>
	 *
	 * @return {@literal true} if an alarm reset is in progress, or {@code null}
	 *         if not available
	 */
	@Nullable
	Boolean isAlarmResetInProgress();

	/**
	 * Reset any latched alarms.
	 *
	 * @param conn
	 *        the connection to write to
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void resetAlarms(ModbusConnection conn) throws IOException;

	/**
	 * Get the battery type.
	 *
	 * @return the type, or {@code null} if not available
	 */
	@Nullable
	BatteryType getBatteryType();

	/**
	 * Get the battery bank state.
	 *
	 * @return the state, or {@code null} if not available
	 */
	@Nullable
	BatteryState getBatteryState();

	/**
	 * Get the vendor specific battery bank state.
	 *
	 * @return the state code, or {@code null} if not available
	 */
	@Nullable
	Integer getVendorBatteryState();

	/**
	 * Get the date the warranty expires.
	 *
	 * @return the date, or {@code null} if not available
	 */
	@Nullable
	LocalDate getWarrantyDate();

	/**
	 * Get the active battery events.
	 *
	 * @return the events, as {@link BatteryEvent} values, never {@code null}
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
	 * Get the DC bus voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getDCVoltage();

	/**
	 * Get the instantaneous maximum battery voltage limit.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumVoltage();

	/**
	 * Get the instantaneous minimum battery voltage limit.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMinimumVoltage();

	/**
	 * Get the maximum cell voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumCellVoltage();

	/**
	 * Get the index of the string containing the cell with the maximum voltage.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumCellVoltageStringIndex();

	/**
	 * Get the index of the module containing the cell with the maximum voltage.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMaximumCellVoltageModuleIndex();

	/**
	 * Get the minimum cell voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getMinimumCellVoltage();

	/**
	 * Get the index of the string containing the cell with the minimum voltage.
	 *
	 * @return the string index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumCellVoltageStringIndex();

	/**
	 * Get the index of the module containing the cell with the minimum voltage.
	 *
	 * @return the module index, or {@code null} if not available
	 */
	@Nullable
	Integer getMinimumCellVoltageModuleIndex();

	/**
	 * Get the average cell voltage.
	 *
	 * @return the voltage, in V, or {@code null} if not available
	 */
	@Nullable
	Float getAverageCellVoltage();

	/**
	 * Get the total DC current.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getDCCurrent();

	/**
	 * Get the instantaneous maximum DC charge current limit.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumChargeCurrent();

	/**
	 * Get the instantaneous maximum DC discharge current limit.
	 *
	 * @return the current, in A, or {@code null} if not available
	 */
	@Nullable
	Float getMaximumDischargeCurrent();

	/**
	 * Get the total DC power.
	 *
	 * @return the power, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getDCPower();

	/**
	 * Get the battery's request to start or stop the inverter.
	 *
	 * @return the request, or {@code null} if not available
	 */
	@Nullable
	BatteryInverterStateRequest getInverterStateRequest();

	/**
	 * Get the AC power requested by the battery.
	 *
	 * <p>
	 * A battery can request power in special states, such as string balancing.
	 * </p>
	 *
	 * @return the power, in W, or {@code null} if not available
	 */
	@Nullable
	Integer getPowerRequest();

	/**
	 * Get the last operation requested of the battery bank.
	 *
	 * @return the operation, or {@code null} if not available
	 */
	@Nullable
	BatteryOperation getOperation();

	/**
	 * Request the battery bank perform an operation.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param operation
	 *        the operation to perform
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setOperation(ModbusConnection conn, BatteryOperation operation) throws IOException;

	/**
	 * Get the inverter state given to the battery.
	 *
	 * @return the state, or {@code null} if not available
	 */
	@Nullable
	BatteryInverterState getInverterState();

	/**
	 * Set the inverter state given to the battery.
	 *
	 * <p>
	 * A controller is expected to update this as soon as it detects an inverter
	 * state change.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param state
	 *        the current inverter state
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setInverterState(ModbusConnection conn, BatteryInverterState state) throws IOException;

}
