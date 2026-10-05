/* ==================================================================
 * DerCurveModelAccessor.java - 5/10/2026 4:58:20 pm
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
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing DER curve model data.
 *
 * <p>
 * DER curve models, such as volt-var, store a number of piece-wise linear
 * curves. The first curve is the read-only active curve. The other curves are
 * stored settings: to change the active curve, update one of the stored curves
 * and then adopt it with {@link #adoptCurve(ModbusConnection, int)}, which
 * copies the stored curve to the active curve.
 * </p>
 *
 * <p>
 * Setter methods write to the device immediately, and throw
 * {@link IllegalArgumentException} if the value is not valid for the point, or
 * {@link IllegalStateException} if the model scale factors have not been read
 * from the device or are not implemented.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerCurveModelAccessor extends ModelAccessor {

	/**
	 * Get the function enable setting.
	 *
	 * @return {@literal true} if the function is enabled, or {@code null} if
	 *         not available
	 */
	@Nullable
	Boolean isEnabled();

	/**
	 * Set the function enable setting.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param enabled
	 *        {@literal true} to enable the function
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setEnabled(ModbusConnection conn, boolean enabled) throws IOException;

	/**
	 * Get the number of curves.
	 *
	 * @return the number of curves, including the active curve, or {@code null}
	 *         if not available
	 */
	@Nullable
	Integer getCurveCount();

	/**
	 * Get the number of points in each curve.
	 *
	 * @return the number of points, or {@code null} if not available
	 */
	@Nullable
	Integer getCurvePointCount();

	/**
	 * Get the last adopt curve request.
	 *
	 * @return the index of the last curve requested to be adopted, or
	 *         {@code null} if not available
	 */
	@Nullable
	Integer getAdoptCurveRequest();

	/**
	 * Adopt a stored curve as the active curve.
	 *
	 * <p>
	 * The device copies the curve to the active curve, and reports the result
	 * with {@link #getAdoptCurveResult()}.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param index
	 *        the index of the curve to adopt, from {@literal 2} to the curve
	 *        count
	 * @throws IllegalArgumentException
	 *         if {@code index} is outside the allowed range
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void adoptCurve(ModbusConnection conn, int index) throws IOException;

	/**
	 * Get the result of the last adopt curve request.
	 *
	 * @return the result, or {@code null} if not available
	 */
	@Nullable
	DerAdoptResult getAdoptCurveResult();

	/**
	 * Get the reversion timeout.
	 *
	 * @return the timeout, in seconds, where {@literal 0} means no reversion,
	 *         or {@code null} if not available
	 */
	@Nullable
	Long getReversionTime();

	/**
	 * Set the reversion timeout.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param seconds
	 *        the timeout, in seconds, or {@literal 0} for no reversion
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionTime(ModbusConnection conn, long seconds) throws IOException;

	/**
	 * Get the reversion time remaining.
	 *
	 * @return the time remaining, in seconds, or {@code null} if not available
	 */
	@Nullable
	Long getReversionTimeRemaining();

	/**
	 * Get the reversion curve.
	 *
	 * @return the index of the curve to adopt when the reversion timeout
	 *         expires, or {@code null} if not available
	 */
	@Nullable
	Integer getReversionCurve();

	/**
	 * Set the reversion curve.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param index
	 *        the index of the curve to adopt when the reversion timeout
	 *        expires, from {@literal 1} to the curve count
	 * @throws IllegalArgumentException
	 *         if {@code index} is outside the allowed range
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionCurve(ModbusConnection conn, int index) throws IOException;

	/**
	 * Get the curves.
	 *
	 * <p>
	 * The number of curves is the curve count, limited to the number of curves
	 * the model length allows for.
	 * </p>
	 *
	 * @return the curves, starting with the active curve, never {@code null}
	 */
	List<? extends DerCurve> getCurves();

}
