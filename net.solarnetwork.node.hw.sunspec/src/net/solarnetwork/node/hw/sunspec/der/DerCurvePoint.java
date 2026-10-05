/* ==================================================================
 * DerCurvePoint.java - 5/10/2026 4:58:20 pm
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

package net.solarnetwork.node.hw.sunspec.der;

/**
 * A point on a DER piece-wise linear curve.
 *
 * <p>
 * The meaning and units of each coordinate depend on the curve model, as
 * described by each model's accessor API. A coordinate that is not available is
 * {@link Float#NaN}.
 * </p>
 *
 * @param x
 *        the x coordinate
 * @param y
 *        the y coordinate
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public record DerCurvePoint(float x, float y) {

}
