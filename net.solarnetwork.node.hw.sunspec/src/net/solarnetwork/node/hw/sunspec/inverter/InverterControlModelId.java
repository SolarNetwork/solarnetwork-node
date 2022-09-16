/* ==================================================================
 * InverterControlModelId.java - 15/10/2018 11:01:56 AM
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

import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Enumeration of SunSpec inverter control model IDs.
 * 
 * @author matt
 * @version 1.1
 */
public enum InverterControlModelId implements ModelId {

	/** Nameplate ratings. */
	NameplateRatings(
			120,
			"Inverter controls nameplate ratings",
			InverterNameplateRatingsModelAccessor.class),

	/** Basic settings. */
	BasicSettings(121, "Inverter controls basic settings", InverterBasicSettingsModelAccessor.class),

	/** Extended measurements. */
	ExtendedMeasurements(122, "Inverter controls extended measurements and status"),

	/** Immediate controls. */
	ImmediateControls(123, "Inverter immediate controls"),

	/** Basic storage controls. */
	BasicStorageControls(124, "Basic storage controls"),

	/** Pricing signal. */
	PricingSignal(125, "Pricing signal"),

	/** Static volt-VAR arrays. */
	StaticVoltVarArrays(126, "Static volt-VAR arrays"),

	/** Parameterized frequency-watt. */
	ParameterizedFrequencyWatt(127, "Parameterized frequency-watt"),

	/** Dynamic reactive current. */
	DynamicReactiveCurrent(128, "Dynamic reactive current"),

	/** LVRT must disconnect. */
	LvrtMustDisconnect(129, "LVRT must disconnect"),

	/** HVRT must disconnect. */
	HvrtMustDisconnect(130, "HVRT must disconnect"),

	/** Watt-power factor. */
	WattPowerFactor(131, "Watt-power factor"),

	/** Volt-watt. */
	VoltWatt(132, "Volt-watt"),

	/** Basic scheduling. */
	BasicScheduling(133, "Basic scheduling"),

	/** Curve based frequency-watt. */
	CurveBasedFrequencyWatt(134, "Curve-based frequency-watt"),

	/** Low frequency ride-through. */
	LowFrequencyRideThrough(135, "Low frequency ride-through"),

	/** High frequency ride-through. */
	HighFrequencyRideThrough(136, "High frequency ride-through"),

	/** LVRT must remain connected. */
	LvrtMustRemainConnected(137, "LVRT must remain connected"),

	/** HVRT must remain connected. */
	HvrtMustRemainConnected(138, "HVRT must remain connected"),

	/** LVRT extended curve. */
	LvrtExtendedCurve(139, "LVRT extended curve"),

	/** HVRT extended curve. */
	HvrtExtendedCurve(140, "HVRT extended curve"),

	/** LFRT must remain connected. */
	LfrtMustRemainConnected(141, "LFRT must remain connected"),

	/** HFRT must remain connected. */
	HfrtMustRemainConnected(142, "HFRT must remain connected"),

	/** LFRT extended curve. */
	LfrtEextendedCurve(143, "LFRT extended curve"),

	/** HFRT extended curve. */
	HfrtEextendedCurve(144, "HFRT extended curve"),

	/** Inverter controls extended settings. */
	ExtendedSettings(145, "Inverter controls extended settings");

	private final int id;
	private final String description;
	private final Class<? extends ModelAccessor> accessorType;

	private InverterControlModelId(int id, String description) {
		this(id, description, ModelAccessor.class);
	}

	private InverterControlModelId(int id, String description,
			Class<? extends ModelAccessor> accessorType) {
		this.id = id;
		this.description = description;
		this.accessorType = accessorType;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Class<? extends ModelAccessor> getModelAccessorType() {
		return accessorType;
	}

	/**
	 * Get an enumeration for an ID value.
	 * 
	 * @param id
	 *        the ID to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code id} is not supported
	 */
	public static InverterControlModelId forId(int id) {
		for ( InverterControlModelId e : InverterControlModelId.values() ) {
			if ( e.id == id ) {
				return e;
			}
		}
		throw new IllegalArgumentException("ID [" + id + "] not supported");
	}
}
