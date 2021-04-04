/* ==================================================================
 * InverterNameplateRatingsModelAccessorImpl.java - 15/10/2018 9:23:28 AM
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

import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.DistributedEnergyResourceType;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Data access object for an inverter nameplate ratings model.
 * 
 * @author matt
 * @version 1.0
 */
public class InverterNameplateRatingsModelAccessorImpl extends BaseModelAccessor
		implements InverterNameplateRatingsModelAccessor {

	/** The inverter nameplate ratings model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 26;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public InverterNameplateRatingsModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@link InverterControlModelId} class will be used as the
	 * {@code ModelId} instance.
	 * </p>
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public InverterNameplateRatingsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, InverterControlModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public DistributedEnergyResourceType getDerType() {
		Number n = getData().getNumber(InverterNameplateRatingsRegister.DerType, getBlockAddress());
		if ( n == null ) {
			return null;
		}
		return InverterDerType.forCode(n.intValue());
	}

	@Override
	public Integer getActivePowerRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ActivePowerRating,
				InverterNameplateRatingsRegister.ScaleFactorActivePowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getApparentPowerRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ApparentPowerRating,
				InverterNameplateRatingsRegister.ScaleFactorApparentPowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ1Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ReactivePowerQ1Rating,
				InverterNameplateRatingsRegister.ScaleFactorReactivePowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ2Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ReactivePowerQ2Rating,
				InverterNameplateRatingsRegister.ScaleFactorReactivePowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ3Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ReactivePowerQ3Rating,
				InverterNameplateRatingsRegister.ScaleFactorReactivePowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getReactivePowerQ4Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.ReactivePowerQ4Rating,
				InverterNameplateRatingsRegister.ScaleFactorReactivePowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Float getCurrentRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.CurrentRating,
				InverterNameplateRatingsRegister.ScaleFactorCurrentRating);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ1Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.PowerFactorQ1Rating,
				InverterNameplateRatingsRegister.ScaleFactorPowerFactorRating);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ2Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.PowerFactorQ2Rating,
				InverterNameplateRatingsRegister.ScaleFactorPowerFactorRating);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ3Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.PowerFactorQ3Rating,
				InverterNameplateRatingsRegister.ScaleFactorPowerFactorRating);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactorQ4Rating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.PowerFactorQ4Rating,
				InverterNameplateRatingsRegister.ScaleFactorPowerFactorRating);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getStoredEnergyRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.StoredEnergyRating,
				InverterNameplateRatingsRegister.ScaleFactorStoredEnergyRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getStoredChargeCapacity() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.StoredChargeCapacity,
				InverterNameplateRatingsRegister.ScaleFactorStoredChargeCapacity);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getStoredEnergyImportPowerRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.StoredEnergyImportPowerRating,
				InverterNameplateRatingsRegister.ScaleFactorStoredEnergyImportPowerRating);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getStoredEnergyExportPowerRating() {
		Number n = getScaledValue(InverterNameplateRatingsRegister.StoredEnergyExportPowerRating,
				InverterNameplateRatingsRegister.ScaleFactorStoredEnergyExportPowerRating);
		return (n != null ? n.intValue() : null);
	}

}
