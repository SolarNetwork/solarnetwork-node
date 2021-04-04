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
 * @version 1.1
 */
public enum ACPhase {

	/** The first phase. */
	PhaseA(1, 'a'),

	/** The second phase. */
	PhaseB(2, 'b'),

	/** The third phase. */
	PhaseC(3, 'c'),

	Total(0, 't');

	private final int number;
	private final char key;

	private ACPhase(int n, char key) {
		this.number = n;
		this.key = key;
	}

	/**
	 * Get the integer based value of the phase.
	 * 
	 * <p>
	 * The {@code PhaseA}, {@code PhaseB}, and {@code PhaseC} phases are
	 * numbered <em>1</em>, <em>2</em>, and <em>3</em>. The {{@code Total} phase
	 * is numbered <em>0</em>.
	 * </p>
	 * 
	 * @return the phase number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Get the key value of the phase.
	 * 
	 * <p>
	 * The keys are {@literal a}, {@literal b}, {@literal c}, and {@literal t}.
	 * </p>
	 * 
	 * @return the key value
	 * @since 1.1
	 */
	public char getKey() {
		return key;
	}

	/**
	 * Get a string with a key suffix added.
	 * 
	 * <p>
	 * This will take {@code value} and append <i>_P</i>, where {@literal P} is
	 * the key.
	 * </p>
	 * 
	 * @param value
	 *        the value to append the key to
	 * @return the value with a key suffix added
	 * @since 1.1
	 */
	public String withKey(String value) {
		return (value != null ? value : "") + '_' + key;
	}

	/**
	 * Get a key value for a line phase, with this phase as the leading phase.
	 * 
	 * <p>
	 * The keys are {@literal ab}, {@literal bc}, {@literal ca}, and
	 * {@literal t}.
	 * </p>
	 * 
	 * @return the line key
	 * @since 1.1
	 */
	public String getLineKey() {
		switch (this) {
			case Total:
				return "t";

			case PhaseA:
				return "ab";

			case PhaseB:
				return "bc";

			case PhaseC:
				return "ca";

			default:
				return "";
		}
	}

	/**
	 * Get a string with a line key suffix added.
	 * 
	 * <p>
	 * This will take {@code value} and append <i>_P</i>, where {@literal P} is
	 * the line key.
	 * </p>
	 * 
	 * @param value
	 *        the value to append the line key to
	 * @return the value with a line key suffix added
	 * @since 1.1
	 */
	public String withLineKey(String value) {
		return (value != null ? value : "") + '_' + getLineKey();
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

	/**
	 * Get an ACPhase for a given key.
	 * 
	 * @param key
	 *        the key
	 * @return the ACPhase
	 * @see #getKey()
	 * @throws IllegalArgumentException
	 *         if the key is not a valid phase value
	 */
	public ACPhase forKey(final char key) {
		switch (key) {
			case 't':
				return Total;

			case 'a':
				return PhaseA;

			case 'b':
				return PhaseB;

			case 'c':
				return PhaseC;

			default:
				throw new IllegalArgumentException("Key " + key + " is not a valid ACPhase");
		}
	}

}
