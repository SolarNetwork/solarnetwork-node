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

import java.math.BigDecimal;

/**
 * Unit factors for EM65XX devices. The values represent a multiple of a raw
 * Modbus register value. The factors are stored as {@link BigDecimal} objects
 * to no precision is lost.
 * 
 * @author matt
 * @version 1.0
 */
public enum UnitFactor {

	/** Unit factors for EM5610. */
	EM5610("1", "1", "1", "5610"),

	/** Unit factors for EM5630 in 5A (CT) mode. */
	EM5630_5A("0.01", "0.0001", "0.2", "5630 (5A CT mode)"),

	/** Unit factors for EM5630 in 30A mode. */
	EM5630_30A("0.01", "0.003", "6", "5630");

	private final BigDecimal u;
	private final BigDecimal a;
	private final BigDecimal p;
	private final String name;

	private UnitFactor(String u, String a, String p, String name) {
		this.u = new BigDecimal(u);
		this.a = new BigDecimal(a);
		this.p = new BigDecimal(p);
		this.name = name;
	}

	/**
	 * Get the U factor.
	 * 
	 * @return the U factor
	 */
	public BigDecimal getU() {
		return u;
	}

	/**
	 * Get the A factor.
	 * 
	 * @return the A factor
	 */
	public BigDecimal getA() {
		return a;
	}

	/**
	 * Get the P factor.
	 * 
	 * @return the P factor
	 */
	public BigDecimal getP() {
		return p;
	}

	/**
	 * Get a display name for this factor.
	 * 
	 * @return the name
	 */
	public String getDisplayName() {
		return name;
	}
}
