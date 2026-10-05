/* ==================================================================
 * LithiumIonStringEvent.java - 5/10/2026 9:05:44 pm
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * Lithium-ion battery string events (alarms and warnings).
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum LithiumIonStringEvent implements ModelEvent {

	/** Communication error. */
	CommunicationError(0, "Communication error"),

	/** Over temperature alarm. */
	OverTemperatureAlarm(1, "Over temperature alarm"),

	/** Over temperature warning. */
	OverTemperatureWarning(2, "Over temperature warning"),

	/** Under temperature alarm. */
	UnderTemperatureAlarm(3, "Under temperature alarm"),

	/** Under temperature warning. */
	UnderTemperatureWarning(4, "Under temperature warning"),

	/** Over charge current alarm. */
	OverChargeCurrentAlarm(5, "Over charge current alarm"),

	/** Over charge current warning. */
	OverChargeCurrentWarning(6, "Over charge current warning"),

	/** Over discharge current alarm. */
	OverDischargeCurrentAlarm(7, "Over discharge current alarm"),

	/** Over discharge current warning. */
	OverDischargeCurrentWarning(8, "Over discharge current warning"),

	/** Over voltage alarm. */
	OverVoltageAlarm(9, "Over voltage alarm"),

	/** Over voltage warning. */
	OverVoltageWarning(10, "Over voltage warning"),

	/** Under voltage alarm. */
	UnderVoltageAlarm(11, "Under voltage alarm"),

	/** Under voltage warning. */
	UnderVoltageWarning(12, "Under voltage warning"),

	/** Under minimum state of charge alarm. */
	UnderSocMinimumAlarm(13, "Under minimum state of charge alarm"),

	/** Under minimum state of charge warning. */
	UnderSocMinimumWarning(14, "Under minimum state of charge warning"),

	/** Over maximum state of charge alarm. */
	OverSocMaximumAlarm(15, "Over maximum state of charge alarm"),

	/** Over maximum state of charge warning. */
	OverSocMaximumWarning(16, "Over maximum state of charge warning"),

	/** Voltage imbalance warning. */
	VoltageImbalanceWarning(17, "Voltage imbalance warning"),

	/** Temperature imbalance alarm. */
	TemperatureImbalanceAlarm(18, "Temperature imbalance alarm"),

	/** Temperature imbalance warning. */
	TemperatureImbalanceWarning(19, "Temperature imbalance warning"),

	/** Contactor error. */
	ContactorError(20, "Contactor error"),

	/** Fan error. */
	FanError(21, "Fan error"),

	/** Ground fault. */
	GroundFault(22, "Ground fault"),

	/** Open door error. */
	OpenDoorError(23, "Open door error"),

	/** Reserved. */
	Reserved1(24, "Reserved"),

	/** Other alarm. */
	OtherAlarm(25, "Other alarm"),

	/** Other warning. */
	OtherWarning(26, "Other warning"),

	/** Reserved. */
	Reserved2(27, "Reserved"),

	/** Configuration alarm. */
	ConfigurationAlarm(28, "Configuration alarm"),

	/** Configuration warning. */
	ConfigurationWarning(29, "Configuration warning"),

	;

	private final int index;
	private final String description;

	private LithiumIonStringEvent(int index, String description) {
		this.index = index;
		this.description = description;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get a set of alarms for a bitmask value.
	 *
	 * <p>
	 * If the most significant bit of the 32-bit value is set, the alarms are
	 * not implemented and an empty set is returned.
	 * </p>
	 *
	 * @param bitmask
	 *        the bitmask value
	 * @return the alarms, never {@code null}
	 */
	public static Set<ModelEvent> forBitmask(long bitmask) {
		if ( bitmask == 0 || (bitmask & 0x80000000L) != 0 ) {
			return Collections.emptySet();
		}
		Set<ModelEvent> result = new LinkedHashSet<>(8);
		for ( LithiumIonStringEvent e : LithiumIonStringEvent.values() ) {
			if ( ((bitmask >> e.index) & 0x1) == 1 ) {
				result.add(e);
			}
		}
		return result;
	}

}
