/* ==================================================================
 * SunSpecUtils.java - 6 Oct 2026 10:09:54 am
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

package net.solarnetwork.node.hw.sunspec;

import java.util.Collections;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.Bitmaskable;

/**
 * General SunSpec helper methods.
 * 
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public final class SunSpecUtils {

	private SunSpecUtils() {
		// not available
	}

	/**
	 * Get a bitfield data property value as a set of enumeration values.
	 *
	 * <p>
	 * SunSpec bitfields never have their most significant bit set, so a value
	 * with that bit set, including the SunSpec "not implemented" value, is
	 * returned as an empty set. Bits without a corresponding enumeration value
	 * are ignored.
	 * </p>
	 *
	 * @param <T>
	 *        the enumeration type
	 * @param n
	 *        the bitmask number value
	 * @param wordLength
	 *        the number of 16-bit words the bitfield is comprised of
	 * @param type
	 *        the enumeration type
	 * @return the values, never {@code null}
	 */
	public static <T extends Enum<T> & Bitmaskable> Set<T> bitfieldValues(final @Nullable Number n,
			final int wordLength, final Class<T> type) {
		if ( n == null ) {
			return Collections.emptySet();
		}
		final long v = n.longValue();
		if ( (v & (1L << (wordLength * 16 - 1))) != 0 ) {
			return Collections.emptySet();
		}
		return Bitmaskable.setForBitmask((int) v, type);
	}

}
