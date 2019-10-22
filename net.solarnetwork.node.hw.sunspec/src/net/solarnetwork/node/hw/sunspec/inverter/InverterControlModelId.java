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

	NameplateRatings(
			120,
			"Inverter controls nameplate ratings",
			InverterNameplateRatingsModelAccessor.class),

	BasicSettings(121, "Inverter controls basic settings", InverterBasicSettingsModelAccessor.class),

	ExtendedMeasurements(122, "Inverter controls extended measurements and status"),

	ImmediateControls(123, "Inverter immediate controls"),

	BasicStorageControls(124, "Basic storage controls"),

	PricingSignal(125, "Pricing signal"),

	StaticVoltVarArrays(126, "Static volt-VAR arrays"),

	ParameterizedFrequencyWatt(127, "Parameterized frequency-watt"),

	DynamicReactiveCurrent(128, "Dynamic reactive current"),

	LvrtMustDisconnect(129, "LVRT must disconnect"),

	HvrtMustDisconnect(130, "HVRT must disconnect"),

	WattPowerFactor(131, "Watt-power factor"),

	VoltWatt(132, "Volt-watt"),

	BasicScheduling(133, "Basic scheduling"),

	CurveBasedFrequencyWatt(134, "Curve-based frequency-watt"),

	LowFrequencyRideThrough(135, "Low frequency ride-through"),

	HighFrequencyRideThrough(136, "High frequency ride-through"),

	LvrtMustRemainConnected(137, "LVRT must remain connected"),

	HvrtMustRemainConnected(138, "HVRT must remain connected"),

	LvrtExtendedCurve(139, "LVRT extended curve"),

	HvrtExtendedCurve(140, "HVRT extended curve"),

	LfrtMustRemainConnected(141, "LFRT must remain connected"),

	HfrtMustRemainConnected(142, "HFRT must remain connected"),

	LfrtEextendedCurve(143, "LFRT extended curve"),

	HfrtEextendedCurve(144, "HFRT extended curve"),

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
