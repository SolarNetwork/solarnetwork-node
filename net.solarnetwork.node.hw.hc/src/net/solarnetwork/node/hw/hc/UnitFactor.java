/* ==================================================================
 * UnitFactor.java - Mar 26, 2014 5:39:03 PM
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

package net.solarnetwork.node.hw.hc;

/**
 * Unit factors for EM65XX devices. If the value is a positive integer it
 * represents a divisor; if the value is negative it represents a multiple that
 * should be treated as an absolute factor (i.e. convert the factor to a
 * positive value and then multiply).
 * 
 * @author matt
 * @version 1.0
 */
public enum UnitFactor {

	/** Unit factors for EM5610. */
	EM5610(1, 1, 1),

	/** Unit factors for EM5630 in 5A (CT) mode. */
	EM5630_5A(100, 10000, 20),

	/** Unit factors for EM5630 in 30A mode. */
	EM5630_30A(100, 3000, -6);

	private final int u;
	private final int a;
	private final int p;

	private UnitFactor(int u, int a, int p) {
		this.u = u;
		this.a = a;
		this.p = p;
	}

	/**
	 * Get the U factor.
	 * 
	 * @return the U factor
	 */
	public int getU() {
		return u;
	}

	/**
	 * Get the A factor.
	 * 
	 * @return the A factor
	 */
	public int getA() {
		return a;
	}

	/**
	 * Get the P factor.
	 * 
	 * @return the P factor
	 */
	public int getP() {
		return p;
	}

}
