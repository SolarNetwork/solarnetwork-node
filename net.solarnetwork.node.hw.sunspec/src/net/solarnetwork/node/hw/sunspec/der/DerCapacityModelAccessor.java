/* ==================================================================
 * DerCapacityModelAccessor.java - 5/10/2026 9:32:29 am
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

package net.solarnetwork.node.hw.sunspec.der;

import java.io.IOException;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing DER capacity model data.
 *
 * <p>
 * The nameplate ratings are read-only, and the settings used to adjust the
 * ratings are writable. Setter methods write to the device immediately, and
 * throw {@link IllegalArgumentException} if the value is not valid for the
 * point, or {@link IllegalStateException} if the model scale factors have not
 * been read from the device.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerCapacityModelAccessor extends ModelAccessor {

	/**
	 * Get the maximum active power rating at unity power factor, in W.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getActivePowerMaximumRating();

	/**
	 * Get the active power rating at the over-excited power factor rating, in
	 * W.
	 *
	 * @return the rating
	 * @see #getOverExcitedPowerFactorRating()
	 */
	@Nullable
	Integer getActivePowerOverExcitedRating();

	/**
	 * Get the over-excited power factor rating.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getOverExcitedPowerFactorRating();

	/**
	 * Get the active power rating at the under-excited power factor rating, in
	 * W.
	 *
	 * @return the rating
	 * @see #getUnderExcitedPowerFactorRating()
	 */
	@Nullable
	Integer getActivePowerUnderExcitedRating();

	/**
	 * Get the under-excited power factor rating.
	 *
	 * @return the power factor
	 */
	@Nullable
	Float getUnderExcitedPowerFactorRating();

	/**
	 * Get the maximum apparent power rating, in VA.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getApparentPowerMaximumRating();

	/**
	 * Get the maximum injected reactive power rating, in VAR.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getReactivePowerInjectedMaximumRating();

	/**
	 * Get the maximum absorbed reactive power rating, in VAR.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getReactivePowerAbsorbedMaximumRating();

	/**
	 * Get the maximum active power charge rate rating, in W.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getActivePowerChargeRateMaximumRating();

	/**
	 * Get the maximum active power discharge rate rating, in W.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getActivePowerDischargeRateMaximumRating();

	/**
	 * Get the maximum apparent power charge rate rating, in VA.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getApparentPowerChargeRateMaximumRating();

	/**
	 * Get the maximum apparent power discharge rate rating, in VA.
	 *
	 * @return the rating
	 */
	@Nullable
	Integer getApparentPowerDischargeRateMaximumRating();

	/**
	 * Get the nominal AC voltage rating, in V.
	 *
	 * <p>
	 * Voltages are line to neutral for single phase DER, and line to line for
	 * split phase and three phase DER.
	 * </p>
	 *
	 * @return the rating
	 */
	@Nullable
	Float getVoltageNominalRating();

	/**
	 * Get the maximum AC voltage rating, in V.
	 *
	 * @return the rating
	 * @see #getVoltageNominalRating()
	 */
	@Nullable
	Float getVoltageMaximumRating();

	/**
	 * Get the minimum AC voltage rating, in V.
	 *
	 * @return the rating
	 * @see #getVoltageNominalRating()
	 */
	@Nullable
	Float getVoltageMinimumRating();

	/**
	 * Get the maximum AC current rating, in A.
	 *
	 * @return the rating
	 */
	@Nullable
	Float getCurrentMaximumRating();

	/**
	 * Get the reactive susceptance that remains connected to the area electric
	 * power system in the cease to energize and trip state, in S.
	 *
	 * @return the reactive susceptance
	 */
	@Nullable
	Float getReactiveSusceptanceRating();

	/**
	 * Get the normal operating performance category.
	 *
	 * @return the category
	 */
	@Nullable
	DerNormalOperatingCategory getNormalOperatingCategory();

	/**
	 * Get the abnormal operating performance category.
	 *
	 * @return the category
	 */
	@Nullable
	DerAbnormalOperatingCategory getAbnormalOperatingCategory();

	/**
	 * Get the supported control modes.
	 *
	 * @return the control modes, never {@code null}
	 */
	Set<DerControlMode> getSupportedControlModes();

	/**
	 * Get the intentional island categories rating.
	 *
	 * @return the categories, never {@code null}
	 */
	Set<DerIntentionalIslandCategory> getIntentionalIslandCategoriesRating();

	/**
	 * Get the maximum active power setting, in W.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getActivePowerMaximum();

	/**
	 * Set the maximum active power setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setting, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerMaximum(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the active power setting at the over-excited power factor, in W.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getActivePowerOverExcited();

	/**
	 * Set the active power setting at the over-excited power factor.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setting, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerOverExcited(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the over-excited power factor setting.
	 *
	 * @return the setting
	 */
	@Nullable
	Float getOverExcitedPowerFactor();

	/**
	 * Set the over-excited power factor setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the setting
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setOverExcitedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the active power setting at the under-excited power factor, in W.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getActivePowerUnderExcited();

	/**
	 * Set the active power setting at the under-excited power factor.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setting, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerUnderExcited(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the under-excited power factor setting.
	 *
	 * @return the setting
	 */
	@Nullable
	Float getUnderExcitedPowerFactor();

	/**
	 * Set the under-excited power factor setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param powerFactor
	 *        the setting
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setUnderExcitedPowerFactor(ModbusConnection conn, float powerFactor) throws IOException;

	/**
	 * Get the maximum apparent power setting, in VA.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getApparentPowerMaximum();

	/**
	 * Set the maximum apparent power setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param voltAmps
	 *        the setting, in VA
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setApparentPowerMaximum(ModbusConnection conn, int voltAmps) throws IOException;

	/**
	 * Get the maximum injected reactive power setting, in VAR.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getReactivePowerInjectedMaximum();

	/**
	 * Set the maximum injected reactive power setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the setting, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerInjectedMaximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the maximum absorbed reactive power setting, in VAR.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getReactivePowerAbsorbedMaximum();

	/**
	 * Set the maximum absorbed reactive power setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param vars
	 *        the setting, in VAR
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReactivePowerAbsorbedMaximum(ModbusConnection conn, int vars) throws IOException;

	/**
	 * Get the maximum active power charge rate setting, in W.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getActivePowerChargeRateMaximum();

	/**
	 * Set the maximum active power charge rate setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setting, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerChargeRateMaximum(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the maximum active power discharge rate setting, in W.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getActivePowerDischargeRateMaximum();

	/**
	 * Set the maximum active power discharge rate setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param watts
	 *        the setting, in W
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePowerDischargeRateMaximum(ModbusConnection conn, int watts) throws IOException;

	/**
	 * Get the maximum apparent power charge rate setting, in VA.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getApparentPowerChargeRateMaximum();

	/**
	 * Set the maximum apparent power charge rate setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param voltAmps
	 *        the setting, in VA
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setApparentPowerChargeRateMaximum(ModbusConnection conn, int voltAmps) throws IOException;

	/**
	 * Get the maximum apparent power discharge rate setting, in VA.
	 *
	 * @return the setting
	 */
	@Nullable
	Integer getApparentPowerDischargeRateMaximum();

	/**
	 * Set the maximum apparent power discharge rate setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param voltAmps
	 *        the setting, in VA
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setApparentPowerDischargeRateMaximum(ModbusConnection conn, int voltAmps) throws IOException;

	/**
	 * Get the nominal AC voltage setting, in V.
	 *
	 * @return the setting
	 * @see #getVoltageNominalRating()
	 */
	@Nullable
	Float getVoltageNominal();

	/**
	 * Set the nominal AC voltage setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the setting, in V
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setVoltageNominal(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the maximum AC voltage setting, in V.
	 *
	 * @return the setting
	 * @see #getVoltageNominalRating()
	 */
	@Nullable
	Float getVoltageMaximum();

	/**
	 * Set the maximum AC voltage setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the setting, in V
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setVoltageMaximum(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the minimum AC voltage setting, in V.
	 *
	 * @return the setting
	 * @see #getVoltageNominalRating()
	 */
	@Nullable
	Float getVoltageMinimum();

	/**
	 * Set the minimum AC voltage setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param volts
	 *        the setting, in V
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setVoltageMinimum(ModbusConnection conn, float volts) throws IOException;

	/**
	 * Get the maximum AC current setting, in A.
	 *
	 * @return the setting
	 */
	@Nullable
	Float getCurrentMaximum();

	/**
	 * Set the maximum AC current setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param amps
	 *        the setting, in A
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setCurrentMaximum(ModbusConnection conn, float amps) throws IOException;

	/**
	 * Get the intentional island categories setting.
	 *
	 * @return the categories, never {@code null}
	 */
	Set<DerIntentionalIslandCategory> getIntentionalIslandCategories();

	/**
	 * Set the intentional island categories setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param categories
	 *        the categories
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setIntentionalIslandCategories(ModbusConnection conn,
			Set<DerIntentionalIslandCategory> categories) throws IOException;

}
