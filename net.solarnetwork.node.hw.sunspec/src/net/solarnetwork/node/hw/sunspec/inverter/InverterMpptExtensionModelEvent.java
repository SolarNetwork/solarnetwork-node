/* ==================================================================
 * InverterMpptExtensionModelEvent.java - 6/09/2019 5:33:58 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.SunSpecUtils;

/**
 * MPPT extension events.
 *
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public enum InverterMpptExtensionModelEvent implements ModelEvent {

	/** Ground fault. */
	GroundFault(0, "Ground fault"),

	/** DC over voltage. */
	DcOverVoltage(1, "DC over voltage"),

	/** DC disconnect open. */
	DcDisconnect(3, "DC disconnect open"),

	/** Cabinet open. */
	CabinetOpen(5, "Cabinet open"),

	/** Manual shutdown. */
	ManualShutdown(6, "Manual shutdown"),

	/** Over temperature. */
	OverTemperature(7, "Over temperature"),

	/** Blown fuse. */
	BlownStringFuse(12, "Blown fuse"),

	/** Under temperature. */
	UnderTemperature(13, "Under temperature"),

	/** Generic memory or communication error (internal). */
	MemoryLoss(14, "Generic memory or communication error (internal)"),

	/** Arc detection. */
	ArcDetection(15, "Arc detection"),

	/** Reserved. */
	Reserved(19, "Reserved"),

	/** Hardware test failure. */
	HwTestFailure(20, "Hardware test failure"),

	/** DC under voltage. */
	DcUnderVoltage(21, "DC under voltage"),

	/** DC over current. */
	DcOverCurrent(22, "DC over current");

	private final int index;
	private final String description;

	private InverterMpptExtensionModelEvent(int index, String description) {
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
	 * Get an enumeration for an index value.
	 *
	 * @param index
	 *        the ID to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code index} is not supported
	 */
	public static InverterMpptExtensionModelEvent forIndex(int index) {
		for ( InverterMpptExtensionModelEvent e : InverterMpptExtensionModelEvent.values() ) {
			if ( e.index == index ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Index [" + index + "] not supported");
	}

	/**
	 * Get a set of events from a bitmask.
	 *
	 * <p>
	 * SunSpec bitfields never have their most significant bit set, so a 32-bit
	 * bitmask with that bit set, including the SunSpec "not implemented" value,
	 * results in an empty set.
	 * </p>
	 *
	 * @param bitmask
	 *        the 32-bit bitmask
	 * @return the active events
	 */
	public static Set<InverterMpptExtensionModelEvent> forBitmask(long bitmask) {
		return SunSpecUtils.bitfieldValues(bitmask, 2, InverterMpptExtensionModelEvent.class);
	}

}
