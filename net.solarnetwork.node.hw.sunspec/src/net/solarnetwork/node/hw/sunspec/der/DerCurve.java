/* ==================================================================
 * DerCurve.java - 5/10/2026 4:58:20 pm
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

import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for a single curve of a DER curve model.
 *
 * <p>
 * Setter methods write to the device immediately, and throw
 * {@link UnsupportedOperationException} if the curve is read-only, as the
 * active curve (index {@literal 1}) always is.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerCurve {

	/**
	 * Get the curve index.
	 *
	 * @return the index, starting from {@literal 1}, which is the active curve
	 */
	int getIndex();

	/**
	 * Get the read-only setting.
	 *
	 * @return {@literal true} if the curve is read-only, or {@code null} if not
	 *         available
	 */
	@Nullable
	Boolean isReadOnly();

	/**
	 * Get the number of active points.
	 *
	 * @return the number of active points, or {@code null} if not available
	 */
	@Nullable
	Integer getActivePointCount();

	/**
	 * Set the number of active points.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param count
	 *        the number of active points, up to the model curve point count; at
	 *        least {@literal 1} is required, except for trip curves, which can
	 *        have none
	 * @throws IllegalArgumentException
	 *         if {@code count} is outside the allowed range
	 * @throws IllegalStateException
	 *         if the model curve point count is not available
	 * @throws UnsupportedOperationException
	 *         if the curve is read-only
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setActivePointCount(ModbusConnection conn, int count) throws IOException;

	/**
	 * Get the active points.
	 *
	 * @return the active points, never {@code null}
	 */
	List<DerCurvePoint> getPoints();

	/**
	 * Set the curve points.
	 *
	 * <p>
	 * The points are written in a single request, and then the number of active
	 * points is set to the number of points given. Any points after those are
	 * left unchanged.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param points
	 *        the points, up to the model curve point count; at least one point
	 *        is required, except for trip curves, which can have none
	 * @throws IllegalArgumentException
	 *         if the number of points is outside the allowed range, or a point
	 *         value is not valid
	 * @throws IllegalStateException
	 *         if the model curve point count is not available, or the model
	 *         scale factors have not been read or are not implemented
	 * @throws UnsupportedOperationException
	 *         if the curve is read-only
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setPoints(ModbusConnection conn, List<DerCurvePoint> points) throws IOException;

}
