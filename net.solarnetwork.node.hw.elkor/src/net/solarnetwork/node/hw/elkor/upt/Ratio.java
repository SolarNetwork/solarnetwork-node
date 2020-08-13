/* ==================================================================
 * Ratio.java - 14/08/2020 10:28:25 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.elkor.upt;

import java.util.Objects;

/**
 * A transformer ratio.
 * 
 * @author matt
 * @version 1.0
 */
public class Ratio {

	private final int primary;
	private final int secondary;

	/**
	 * Constructor.
	 * 
	 * @param primary
	 *        the primary value (dividend)
	 * @param secondary
	 *        the second value (divisor)
	 */
	public Ratio(int primary, int secondary) {
		super();
		this.primary = primary;
		this.secondary = secondary;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(primary);
		builder.append(":");
		builder.append(secondary);
		return builder.toString();
	}

	/**
	 * Get the primary value (dividend).
	 * 
	 * @return the primary value
	 */
	public int getPrimary() {
		return primary;
	}

	/**
	 * Get the secondary value (divisor).
	 * 
	 * @return the secondary value
	 */
	public int getSecondary() {
		return secondary;
	}

	@Override
	public int hashCode() {
		return Objects.hash(primary, secondary);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof Ratio) ) {
			return false;
		}
		Ratio other = (Ratio) obj;
		return primary == other.primary && secondary == other.secondary;
	}

}
