/* ==================================================================
 * DerDcPortAlarm.java - 5/10/2026 10:12:08 am
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

import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.SunSpecUtils;

/**
 * DER DC port alarms.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerDcPortAlarm implements ModelEvent {

	/** Ground fault. */
	GroundFault(0, "Ground fault"),

	/** Input over voltage. */
	InputOverVoltage(1, "Input over voltage"),

	/** DC disconnect. */
	DcDisconnect(3, "DC disconnect"),

	/** Cabinet open. */
	CabinetOpen(5, "Cabinet open"),

	/** Manual shutdown. */
	ManualShutdown(6, "Manual shutdown"),

	/** Over temperature. */
	OverTemperature(7, "Over temperature"),

	/** Blown fuse. */
	BlownFuse(12, "Blown fuse"),

	/** Under temperature. */
	UnderTemperature(13, "Under temperature"),

	/** Memory loss. */
	MemoryLoss(14, "Memory loss"),

	/** Arc detection. */
	ArcDetection(15, "Arc detection"),

	/** Reserved. */
	Reserved(19, "Reserved"),

	/** Test failed. */
	TestFailed(20, "Test failed"),

	/** Input under voltage. */
	InputUnderVoltage(21, "Input under voltage"),

	/** Input over current. */
	InputOverCurrent(22, "Input over current"),

	;

	private final int index;
	private final String description;

	private DerDcPortAlarm(int index, String description) {
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
	public static Set<DerDcPortAlarm> forBitmask(long bitmask) {
		return SunSpecUtils.bitfieldValues(bitmask, 2, DerDcPortAlarm.class);
	}

}
