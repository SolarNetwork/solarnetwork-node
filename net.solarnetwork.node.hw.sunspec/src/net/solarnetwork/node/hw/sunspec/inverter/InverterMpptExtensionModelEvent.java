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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * MPPT extension events.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public enum InverterMpptExtensionModelEvent implements ModelEvent {

	GroundFault(0, "Ground fault"),

	DcOverVoltage(1, "DC over voltage"),

	DcDisconnect(3, "DC disconnect open"),

	CabinetOpen(5, "Cabinet open"),

	ManualShutdown(6, "Manual shutdown"),

	OverTemperature(7, "Over temperature"),

	BlownStringFuse(12, "Blown fuse"),

	UnderTemperature(13, "Under temperature"),

	MemoryLoss(14, "Generic memory or communication error (internal)"),

	ArcDetection(15, "Arc detection"),

	Reserved(19, "Reserved"),

	HwTestFailure(20, "Hardware test failure"),

	DcUnderVoltage(21, "DC under voltage"),

	DcOverCurrent(22, "DC over current");

	final private int index;
	final private String description;

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
	 * @param bitmask
	 *        the bitmask
	 * @return the active events
	 */
	public static Set<ModelEvent> forBitmask(long bitmask) {
		if ( bitmask == 0 || (bitmask & ModelData.NAN_BITFIELD32) == ModelData.NAN_BITFIELD32 ) {
			return Collections.emptySet();
		}
		Set<ModelEvent> result = new LinkedHashSet<>(32);
		for ( InverterMpptExtensionModelEvent e : InverterMpptExtensionModelEvent.values() ) {
			int index = e.getIndex();
			if ( ((bitmask >> index) & 0x1) == 1 ) {
				result.add(e);
			}
		}
		return result;
	}

}
