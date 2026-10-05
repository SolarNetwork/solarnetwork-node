/* ==================================================================
 * DerAlarm.java - 5/10/2026 8:27:22 am
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * DER alarms.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerAlarm implements ModelEvent {

	/** Ground fault. */
	GroundFault(0, "Ground fault"),

	/** DC over voltage. */
	DcOverVoltage(1, "DC over voltage"),

	/** AC disconnect open. */
	AcDisconnect(2, "AC disconnect open"),

	/** DC disconnect open. */
	DcDisconnect(3, "DC disconnect open"),

	/** Grid disconnect. */
	GridDisconnect(4, "Grid disconnect"),

	/** Cabinet open. */
	CabinetOpen(5, "Cabinet open"),

	/** Manual shutdown. */
	ManualShutdown(6, "Manual shutdown"),

	/** Over temperature. */
	OverTemperature(7, "Over temperature"),

	/** Frequency above limit. */
	OverFrequency(8, "Frequency above limit"),

	/** Frequency under limit. */
	UnderFrequency(9, "Frequency under limit"),

	/** AC voltage above limit. */
	AcOverVoltage(10, "AC voltage above limit"),

	/** AC voltage under limit. */
	AcUnderVoltage(11, "AC voltage under limit"),

	/** Blown string fuse on input. */
	BlownStringFuse(12, "Blown string fuse on input"),

	/** Under temperature. */
	UnderTemperature(13, "Under temperature"),

	/** Generic memory or communication error (internal). */
	MemoryLoss(14, "Generic memory or communication error (internal)"),

	/** Hardware test failure. */
	HardwareTestFailure(15, "Hardware test failure"),

	/** Manufacturer alarm, see the manufacturer alarm information. */
	ManufacturerAlarm(16, "Manufacturer alarm"),

	;

	private final int index;
	private final String description;

	private DerAlarm(int index, String description) {
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
	 * Get a set of alarms from a bitmask.
	 *
	 * <p>
	 * SunSpec bitfields never have their most significant bit set, so a bitmask
	 * with that bit set, including the SunSpec "not implemented" value, results
	 * in an empty set.
	 * </p>
	 *
	 * @param bitmask
	 *        the bitmask
	 * @return the active alarms, never {@code null}
	 */
	public static Set<ModelEvent> forBitmask(long bitmask) {
		if ( bitmask == 0 || (bitmask & 0x80000000L) != 0 ) {
			return Collections.emptySet();
		}
		Set<ModelEvent> result = new LinkedHashSet<>(8);
		for ( DerAlarm e : DerAlarm.values() ) {
			if ( ((bitmask >> e.index) & 0x1) == 1 ) {
				result.add(e);
			}
		}
		return result;
	}

}
