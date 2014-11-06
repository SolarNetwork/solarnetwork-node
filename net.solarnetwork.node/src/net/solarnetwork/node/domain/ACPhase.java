/* ==================================================================
 * ACPhase.java - Apr 2, 2014 10:05:08 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

/**
 * Enumeration of AC phase values.
 * 
 * @author matt
 * @version 1.0
 */
public enum ACPhase {

	/** The first phase. */
	PhaseA(1),

	/** The second phase. */
	PhaseB(2),

	/** The third phase. */
	PhaseC(3),

	Total(0);

	private final int number;

	private ACPhase(int n) {
		this.number = n;
	}

	/**
	 * Get the integer based value of the phase. The {@code PhaseA},
	 * {@code PhaseB}, and {@code PhaseC} phases are numbered <em>1</em>,
	 * <em>2</em>, and <em>3</em>. The {{@code Total} phase is numbered
	 * <em>0</em>.
	 * 
	 * @return the phase number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Get an ACPhase for a given number.
	 * 
	 * @param n
	 *        the number
	 * @return the ACPhase
	 * @see #getNumber()
	 * @throws IllegalArgumentException
	 *         if the number is not a valid phase value
	 */
	public ACPhase forNumber(final int n) {
		switch (n) {
			case 0:
				return Total;

			case 1:
				return PhaseA;

			case 2:
				return PhaseB;

			case 3:
				return PhaseC;

			default:
				throw new IllegalArgumentException("Number " + n + " is not a valid ACPhase");
		}
	}

}
