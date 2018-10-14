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

import net.solarnetwork.node.hw.sunspec.DistributedEnergyResourceType;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing inverter nameplate ratings model data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public interface InverterNameplateRatingsModelAccessor extends ModelAccessor {

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
}
