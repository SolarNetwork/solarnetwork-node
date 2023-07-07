/* ==================================================================
 * Incline.java - 8/07/2023 8:32:43 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

/**
 * Incline data structure.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class Incline {

	private final Double x;
	private final Double y;
	private final Double z;

	/**
	 * Constructor.
	 * 
	 * @param x
	 *        the x-axis inclination, in degrees
	 * @param y
	 *        the y-axis inclination, in degrees
	 * @param z
	 *        the z-axis inclination, in degrees
	 */
	public Incline(Double x, Double y, Double z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        an array of inclination data, in x, y, z order; the array values
	 *        are copied
	 */
	public Incline(Double[] data) {
		super();
		this.x = (data != null && data.length > 0 ? data[0] : null);
		this.y = (data != null && data.length > 1 ? data[1] : null);
		this.z = (data != null && data.length > 2 ? data[2] : null);
	}

	/**
	 * Get the x-axis inclination, in degrees.
	 * 
	 * @return the inclination
	 */
	public Double getInclineX() {
		return x;
	}

	/**
	 * Get the y-axis inclination, in degrees.
	 * 
	 * @return the inclination
	 */
	public Double getInclineY() {
		return y;
	}

	/**
	 * Get the z-axis inclination, in degrees.
	 * 
	 * @return the inclination
	 */
	public Double getInclineZ() {
		return z;
	}

}
