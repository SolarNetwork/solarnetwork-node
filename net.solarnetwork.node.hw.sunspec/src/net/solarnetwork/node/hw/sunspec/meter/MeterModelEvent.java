/* ==================================================================
 * MeterModelEvent.java - 22/05/2018 5:57:49 AM
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

package net.solarnetwork.node.hw.sunspec.meter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;

/**
 * Meter type events.
 * 
 * @author matt
 * @version 1.1
 */
public enum MeterModelEvent implements ModelEvent {

	/** Loss of power or phase. */
	PowerFailure(2, "Loss of power or phase"),

	/** Voltage below threshold (phase loss). */
	UnderVoltage(3, "Voltage below threshold (phase loss)"),

	/** Power factor below threshold. */
	LowPowerFactor(4, "Power factor below threshold"),

	/** Current input over threshold. */
	OverCurrent(5, "Current input over threshold"),

	/** Voltage input over threshold. */
	OverVoltage(6, "Voltage input over threshold"),

	/** Sensor not connected. */
	MissingSensor(7, "Sensor not connected"),

	/** Reserved 1. */
	Reserved_01(8, "Reserved 1"),

	/** Reserved 2. */
	Reserved_02(9, "Reserved 2"),

	/** Reserved 3. */
	Reserved_03(10, "Reserved 3"),

	/** Reserved 4. */
	Reserved_04(11, "Reserved 4"),

	/** Reserved 5. */
	Reserved_05(12, "Reserved 5"),

	/** Reserved 6. */
	Reserved_06(13, "Reserved 6"),

	/** Reserved 7. */
	Reserved_07(14, "Reserved 7"),

	/** Reserved 8. */
	Reserved_08(15, "Reserved 8"),

	/** OEM 1. */
	OEM_01(16, "OEM 1"),

	/** OEM 2. */
	OEM_02(17, "OEM 2"),

	/** OEM 3. */
	OEM_03(18, "OEM 3"),

	/** OEM 4. */
	OEM_04(19, "OEM 4"),

	/** OEM 5. */
	OEM_05(20, "OEM 5"),

	/** OEM 6. */
	OEM_06(21, "OEM 6"),

	/** OEM 7. */
	OEM_07(22, "OEM 7"),

	/** OEM 8. */
	OEM_08(23, "OEM 8"),

	/** OEM 9. */
	OEM_09(24, "OEM 9"),

	/** OEM 10. */
	OEM_10(25, "OEM 10"),

	/** OEM 11. */
	OEM_11(26, "OEM 11"),

	/** OEM 12. */
	OEM_12(27, "OEM 12"),

	/** OEM 13. */
	OEM_13(28, "OEM 13"),

	/** OEM 14. */
	OEM_14(29, "OEM 14"),

	/** OEM 15. */
	OEM_15(30, "OEM 15"),

	/** OEM 16. */
	OEM_16(31, "OEM 16");

	private final int index;
	private final String description;

	private MeterModelEvent(int index, String description) {
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
	public static MeterModelEvent forIndex(int index) {
		for ( MeterModelEvent e : MeterModelEvent.values() ) {
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
		for ( MeterModelEvent e : MeterModelEvent.values() ) {
			int index = e.getIndex();
			if ( ((bitmask >> index) & 0x1) == 1 ) {
				result.add(e);
			}
		}
		return result;
	}

}
