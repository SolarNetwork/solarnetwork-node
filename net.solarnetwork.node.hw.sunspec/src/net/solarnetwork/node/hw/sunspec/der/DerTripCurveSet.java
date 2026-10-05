/* ==================================================================
 * DerTripCurveSet.java - 5/10/2026 6:24:51 pm
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

import org.jspecify.annotations.Nullable;

/**
 * API for a set of trip curves in a DER trip model.
 *
 * <p>
 * Each curve set has three curves, which define the region boundaries the DER
 * behaves differently in: when the must trip region is entered the DER must
 * trip, when the momentary cessation region is entered the DER must cease to
 * energize but not trip, and when the may trip region is entered the DER may
 * either continue operating or trip. Each curve reports the index and read-only
 * setting of its curve set, and can have no active points, as the may trip and
 * momentary cessation curves are optional.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerTripCurveSet {

	/**
	 * Get the curve set index.
	 *
	 * @return the index, starting from {@literal 1}, which is the active curve
	 *         set
	 */
	int getIndex();

	/**
	 * Get the read-only setting.
	 *
	 * @return {@literal true} if the curve set is read-only, or {@code null} if
	 *         not available
	 */
	@Nullable
	Boolean isReadOnly();

	/**
	 * Get the must trip curve.
	 *
	 * @return the curve, never {@code null}
	 */
	DerCurve getMustTripCurve();

	/**
	 * Get the may trip curve.
	 *
	 * @return the curve, never {@code null}
	 */
	DerCurve getMayTripCurve();

	/**
	 * Get the momentary cessation curve.
	 *
	 * @return the curve, never {@code null}
	 */
	DerCurve getMomentaryCessationCurve();

}
