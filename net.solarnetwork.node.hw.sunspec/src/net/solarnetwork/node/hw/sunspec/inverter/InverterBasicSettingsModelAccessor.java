/* ==================================================================
 * InverterBasicSettingsModelAccessor.java - 15/10/2018 1:44:08 PM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import java.io.IOException;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.ApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ReactivePowerAction;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing inverter basic settings model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>121</b>. Setter methods
 * write to the device immediately, and throw {@link IllegalArgumentException}
 * if the value is not valid for the point, or {@link IllegalStateException} if
 * the model scale factors have not been read from the device or are not
 * implemented.
 * </p>
 *
 * @author matt
 * @version 2.1
 * @since 1.2
 */
public interface InverterBasicSettingsModelAccessor extends ModelAccessor {

	/**
	 * Get the maximum active power output, in W.
	 *
	 * @return the active power maximum
	 */
	@Nullable
	Integer getActivePowerMaximum();

	/**
	 * Set the maximum active power output.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the maximum active power, in W
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setActivePowerMaximum(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the voltage at the point of common coupling (PCC), in V.
	 *
	 * @return the PCC voltage
	 */
	@Nullable
	Float getPccVoltage();

	/**
	 * Set the voltage at the point of common coupling (PCC).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the voltage, in V
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPccVoltage(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the voltage offset from the PCC to the inverter, in V.
	 *
	 * @return the voltage offset
	 */
	@Nullable
	Float getPccVoltageOffset();

	/**
	 * Set the voltage offset from the PCC to the inverter.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the voltage offset, in V
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPccVoltageOffset(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the maximum voltage, in V.
	 *
	 * @return the maximum voltage
	 */
	@Nullable
	Float getVoltageMaximum();

	/**
	 * Set the maximum voltage.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the maximum voltage, in V
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setVoltageMaximum(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the minimum voltage, in V.
	 *
	 * @return the minimum voltage
	 */
	@Nullable
	Float getVoltageMinimum();

	/**
	 * Set the minimum voltage.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the minimum voltage, in V
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setVoltageMinimum(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the maximum apparent power output, in VA.
	 *
	 * @return the apparent power maximum
	 */
	@Nullable
	Integer getApparentPowerMaximum();

	/**
	 * Set the maximum apparent power output.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param voltAmps
	 *        the maximum apparent power, in VA
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setApparentPowerMaximum(ModbusConnection conn, int voltAmps) throws IOException;

	/**
	 * Get the maximum reactive power for EEI quadrant 1 (lagging, inductive),
	 * in VAR.
	 *
	 * @return the reactive power rating
	 */
	@Nullable
	Integer getReactivePowerQ1Maximum();

	/**
	 * Set the maximum reactive power for EEI quadrant 1 (lagging, inductive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the maximum reactive power, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setReactivePowerQ1Maximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the maximum reactive power for EEI quadrant 2 (leading, capacitive),
	 * in VAR.
	 *
	 * @return the reactive power rating
	 */
	@Nullable
	Integer getReactivePowerQ2Maximum();

	/**
	 * Set the maximum reactive power for EEI quadrant 2 (leading, capacitive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the maximum reactive power, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setReactivePowerQ2Maximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the maximum reactive power for EEI quadrant 3 (lagging, inductive),
	 * in VAR.
	 *
	 * @return the reactive power rating
	 */
	@Nullable
	Integer getReactivePowerQ3Maximum();

	/**
	 * Set the maximum reactive power for EEI quadrant 3 (lagging, inductive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the maximum reactive power, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setReactivePowerQ3Maximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the maximum reactive power for EEI quadrant 4 (leading, capacitive),
	 * in VAR.
	 *
	 * @return the reactive power rating
	 */
	@Nullable
	Integer getReactivePowerQ4Maximum();

	/**
	 * Set the maximum reactive power for EEI quadrant 4 (leading, capacitive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the maximum reactive power, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setReactivePowerQ4Maximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the ramp rate of change of active power due to commands or internal
	 * actions, in maximum active power percentage/sec.
	 *
	 * @return active power ramp rate percentage
	 */
	@Nullable
	Float getActivePowerRampRate();

	/**
	 * Set the ramp rate of change of active power due to commands or internal
	 * actions.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percentPerSecond
	 *        the ramp rate, in maximum active power percentage/sec
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setActivePowerRampRate(ModbusConnection conn, float percentPerSecond) throws IOException;

	/**
	 * Get the minimum power factor rating for EEI quadrant 1 (lagging,
	 * inductive), as a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getPowerFactorQ1Minimum();

	/**
	 * Set the minimum power factor for EEI quadrant 1 (lagging, inductive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor, as a decimal from -1.0 to 1.0
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPowerFactorQ1Minimum(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the minimum power factor rating for EEI quadrant 2 (leading,
	 * capacitive), as a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getPowerFactorQ2Minimum();

	/**
	 * Set the minimum power factor for EEI quadrant 2 (leading, capacitive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor, as a decimal from -1.0 to 1.0
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPowerFactorQ2Minimum(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the minimum power factor rating for EEI quadrant 3 (lagging,
	 * inductive), as a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getPowerFactorQ3Minimum();

	/**
	 * Set the minimum power factor for EEI quadrant 3 (lagging, inductive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor, as a decimal from -1.0 to 1.0
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPowerFactorQ3Minimum(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the minimum power factor rating for EEI quadrant 4 (leading,
	 * capacitive), as a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getPowerFactorQ4Minimum();

	/**
	 * Set the minimum power factor for EEI quadrant 4 (leading, capacitive).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the power factor, as a decimal from -1.0 to 1.0
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setPowerFactorQ4Minimum(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the action to take when changing between charging and discharging.
	 *
	 * @return the action
	 */
	@Nullable
	ReactivePowerAction getImportExportChangeReactivePowerAction();

	/**
	 * Set the action to take when changing between charging and discharging.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param action
	 *        the action
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setImportExportChangeReactivePowerAction(ModbusConnection conn, ReactivePowerAction action)
			throws IOException;

	/**
	 * Get the apparent power calculation method used.
	 *
	 * @return the apparent power calculation method
	 */
	@Nullable
	ApparentPowerCalculationMethod getApparentPowerCalculationMethod();

	/**
	 * Set the apparent power calculation method.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param method
	 *        the method
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setApparentPowerCalculationMethod(ModbusConnection conn, ApparentPowerCalculationMethod method)
			throws IOException;

	/**
	 * Get the ramp rate of change of active power due to intermittent PV
	 * generation, as a percentage of {@link #getActivePowerRampRate()}.
	 *
	 * @return active power ramp rate percentage
	 */
	@Nullable
	Float getActivePowerRampRateMaximum();

	/**
	 * Set the ramp rate of change of active power due to intermittent PV
	 * generation.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param percent
	 *        the ramp rate, as a percentage of
	 *        {@link #getActivePowerRampRate()}
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setActivePowerRampRateMaximum(ModbusConnection conn, float percent) throws IOException;

	/**
	 * Get the nominal frequency at the electrical connection point (ECP), in
	 * Hz.
	 *
	 * @return the ECP frequency
	 */
	@Nullable
	Float getEcpFrequency();

	/**
	 * Set the nominal frequency at the electrical connection point (ECP).
	 *
	 * @param conn
	 *        the connection to write to
	 * @param hertz
	 *        the frequency, in Hz
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setEcpFrequency(ModbusConnection conn, float hertz) throws IOException;

	/**
	 * Get the connected phase, for single phase inverters.
	 *
	 * @return the connected phase
	 */
	@Nullable
	AcPhase getConnectedPhase();

	/**
	 * Set the connected phase, for single phase inverters.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param phase
	 *        the connected phase, one of {@link AcPhase#PhaseA},
	 *        {@link AcPhase#PhaseB}, or {@link AcPhase#PhaseC}
	 * @throws IllegalArgumentException
	 *         if {@code phase} is not one of the supported phases
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	void setConnectedPhase(ModbusConnection conn, AcPhase phase) throws IOException;

}
