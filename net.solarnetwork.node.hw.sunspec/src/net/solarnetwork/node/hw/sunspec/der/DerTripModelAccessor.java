/* ==================================================================
 * DerTripModelAccessor.java - 5/10/2026 6:24:51 pm
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
 * API for accessing DER trip model data.
 *
 * <p>
 * DER trip models store a number of trip curve sets. The first curve set is the
 * read-only active curve set. The other curve sets are stored settings: to
 * change the active curve set, update one of the stored curve sets and then
 * adopt it with {@link #adoptCurveSet(ModbusConnection, int)}, which copies the
 * stored curve set to the active curve set.
 * </p>
 *
 * <p>
 * Each model has its own sub-interface, so that
 * {@link net.solarnetwork.node.hw.sunspec.ModelData#findTypedModel(Class)} can
 * find a specific trip model when a device provides several.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerTripModelAccessor extends ModelAccessor {

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
	 * Get the number of curve sets.
	 *
	 * @return the number of curve sets, including the active curve set, or
	 *         {@code null} if not available
	 */
	@Nullable
	Integer getCurveSetCount();

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
	 * @return the index of the last curve set requested to be adopted, or
	 *         {@code null} if not available
	 */
	@Nullable
	Integer getAdoptCurveRequest();

	/**
	 * Adopt a stored curve set as the active curve set.
	 *
	 * <p>
	 * The device copies the curve set to the active curve set, and reports the
	 * result with {@link #getAdoptCurveResult()}.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param index
	 *        the index of the curve set to adopt, from {@literal 2} to the
	 *        curve set count
	 * @throws IllegalArgumentException
	 *         if {@code index} is outside the allowed range
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void adoptCurveSet(ModbusConnection conn, int index) throws IOException;

	/**
	 * Get the result of the last adopt curve request.
	 *
	 * @return the result, or {@code null} if not available
	 */
	@Nullable
	DerAdoptResult getAdoptCurveResult();

	/**
	 * Get the curve sets.
	 *
	 * <p>
	 * The number of curve sets is the curve set count, limited to the number of
	 * curve sets the model length allows for.
	 * </p>
	 *
	 * @return the curve sets, starting with the active curve set, never
	 *         {@code null}
	 */
	List<DerTripCurveSet> getCurveSets();

}
