/* ==================================================================
 * InverterNameplateRatingsModelAccessor.java - 15/10/2018 9:32:08 AM
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

import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.roundDown;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.hw.sunspec.DistributedEnergyResourceType;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.util.NullRemoveMap;

/**
 * API for accessing inverter nameplate ratings model data.
 *
 * @author matt
 * @version 1.1
 * @since 1.2
 */
public interface InverterNameplateRatingsModelAccessor extends ModelAccessor {

	/**
	 * Key for the {@link DistributedEnergyResourceType} name, as a String.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_DER_TYPE = "derType";

	/**
	 * Key for the {@link DistributedEnergyResourceType} code, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_DER_TYPE_CODE = "derTypeCode";

	/**
	 * Key for the active power rating in W, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_ACTIVE_POWER_RATING = "activePowerRating";

	/**
	 * Key for the apparent power rating in W, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_APPARENT_POWER_RATING = "apparentPowerRating";

	/**
	 * Key for the reactive power Q1 rating in VAR, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_REACTIVE_POWER_Q1_RATING = "reactivePowerQ1Rating";

	/**
	 * Key for the reactive power Q2 rating in VAR, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_REACTIVE_POWER_Q2_RATING = "reactivePowerQ2Rating";

	/**
	 * Key for the reactive power Q3 rating in VAR, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_REACTIVE_POWER_Q3_RATING = "reactivePowerQ3Rating";

	/**
	 * Key for the reactive power Q4 rating in VAR, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_REACTIVE_POWER_Q4_RATING = "reactivePowerQ4Rating";

	/**
	 * Key for the current rating in A, as a Float.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_CURRENT_RATING = "currentRating";

	/**
	 * Key for the power factor Q1 rating, as a Float.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_POWER_FACTOR_Q1_RATING = "powerFactorQ1Rating";

	/**
	 * Key for the power factor Q2 rating, as a Float.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_POWER_FACTOR_Q2_RATING = "powerFactorQ2Rating";

	/**
	 * Key for the power factor Q3 rating, as a Float.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_POWER_FACTOR_Q3_RATING = "powerFactorQ3Rating";

	/**
	 * Key for the power factor Q4 rating, as a Float.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_POWER_FACTOR_Q4_RATING = "powerFactorQ4Rating";

	/**
	 * Key for the stored energy inport power rating in Wh, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_STORED_ENERGY_RATING = "storedEnergyRating";

	/**
	 * Key for the stored charge capacity rating in Wh, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_STORED_CHARGE_CAPACITY = "storedChargeCapacity";

	/**
	 * Key for the stored energy inport power rating in W, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_STORED_ENERGY_IMPORT_POWER_RATING = "storedEnergyImportPowerRating";

	/**
	 * Key for the stored energy export power rating in W, as an Integer.
	 *
	 * @since 1.1
	 */
	String INFO_KEY_STORED_ENERGY_EXPORT_POWER_RATING = "storedEnergyExportPowerRating";

	/**
	 * Get the DER type.
	 *
	 * @return the type
	 */
	DistributedEnergyResourceType getDerType();

	/**
	 * Get the continuous active power capability, in W.
	 *
	 * @return the active power rating
	 */
	Integer getActivePowerRating();

	/**
	 * Get the continuous apparent power capability, in VA.
	 *
	 * @return the apparent power rating
	 */
	Integer getApparentPowerRating();

	/**
	 * Get the continuous reactive power capability for EEI quadrant 1 (lagging,
	 * inductive), in VAR.
	 *
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ1Rating();

	/**
	 * Get the continuous reactive power capability for EEI quadrant 2 (leading,
	 * capacitive), in VAR.
	 *
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ2Rating();

	/**
	 * Get the continuous reactive power capability for EEI quadrant 3 (lagging,
	 * inductive), in VAR.
	 *
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ3Rating();

	/**
	 * Get the continuous reactive power capability for EEI quadrant 4 (leading,
	 * capacitive), in VAR.
	 *
	 * @return the reactive power rating
	 */
	Integer getReactivePowerQ4Rating();

	/**
	 * Get the maximum RMS AC current capability of the inverter, in A.
	 *
	 * @return the current rating
	 */
	Float getCurrentRating();

	/**
	 * Get the power factor rating for EEI quadrant 1 (lagging, inductive), as a
	 * decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	Float getPowerFactorQ1Rating();

	/**
	 * Get the power factor rating for EEI quadrant 2 (leading, capacitive), as
	 * a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	Float getPowerFactorQ2Rating();

	/**
	 * Get the power factor rating for EEI quadrant 3 (lagging, inductive), as a
	 * decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	Float getPowerFactorQ3Rating();

	/**
	 * Get the power factor rating for EEI quadrant 4 (leading, capacitive), as
	 * a decimal from -1.0 to 1.0.
	 *
	 * @return the power factor
	 */
	Float getPowerFactorQ4Rating();

	/**
	 * Get the maximum rated stored energy of the battery storage system, in Wh.
	 *
	 * @return the maximum rated energy of the battery storage
	 */
	Integer getStoredEnergyRating();

	/**
	 * Get the maximum rated stored charge of the battery storage system, in Ah.
	 *
	 * @return the maximum rated charge of the battery storage
	 */
	Integer getStoredChargeCapacity();

	/**
	 * Get the maximum rate of charge for the battery storage system, in W.
	 *
	 * @return the maximum charge rate power
	 */
	Integer getStoredEnergyImportPowerRating();

	/**
	 * Get the maximum rate of discharge for the battery storage system, in W.
	 *
	 * @return the minimum discharge rate power
	 */
	Integer getStoredEnergyExportPowerRating();

	/**
	 * Get an information mapping if the nameplate ratings.
	 *
	 * @return the information mapping
	 * @since 1.1
	 */
	default Map<String, Object> nameplateRatingsInfo() {
		Map<String, Object> result = new NullRemoveMap<>(new LinkedHashMap<>(17));

		DistributedEnergyResourceType derType = getDerType();
		if ( derType != null ) {
			result.put(INFO_KEY_DER_TYPE, derType.toString());
			result.put(INFO_KEY_DER_TYPE_CODE, derType.getCode());
		}

		result.put(INFO_KEY_ACTIVE_POWER_RATING, getActivePowerRating());
		result.put(INFO_KEY_APPARENT_POWER_RATING, getApparentPowerRating());
		result.put(INFO_KEY_REACTIVE_POWER_Q1_RATING, getReactivePowerQ1Rating());
		result.put(INFO_KEY_REACTIVE_POWER_Q2_RATING, getReactivePowerQ2Rating());
		result.put(INFO_KEY_REACTIVE_POWER_Q3_RATING, getReactivePowerQ3Rating());
		result.put(INFO_KEY_REACTIVE_POWER_Q4_RATING, getReactivePowerQ4Rating());
		result.put(INFO_KEY_CURRENT_RATING, narrow(roundDown(getCurrentRating(), 1), 2));
		result.put(INFO_KEY_POWER_FACTOR_Q1_RATING, narrow(roundDown(getPowerFactorQ1Rating(), 3), 2));
		result.put(INFO_KEY_POWER_FACTOR_Q2_RATING, narrow(roundDown(getPowerFactorQ2Rating(), 3), 2));
		result.put(INFO_KEY_POWER_FACTOR_Q3_RATING, narrow(roundDown(getPowerFactorQ3Rating(), 3), 2));
		result.put(INFO_KEY_POWER_FACTOR_Q4_RATING, narrow(roundDown(getPowerFactorQ4Rating(), 3), 2));
		result.put(INFO_KEY_STORED_ENERGY_RATING, getStoredEnergyRating());
		result.put(INFO_KEY_STORED_CHARGE_CAPACITY, getStoredChargeCapacity());
		result.put(INFO_KEY_STORED_ENERGY_IMPORT_POWER_RATING, getStoredEnergyImportPowerRating());
		result.put(INFO_KEY_STORED_ENERGY_EXPORT_POWER_RATING, getStoredEnergyExportPowerRating());

		return result;
	}
}
