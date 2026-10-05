/* ==================================================================
 * DerWattVarModelAccessor.java - 5/10/2026 4:58:20 pm
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
 * API for accessing DER watt-var model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>712</b>. Each curve point
 * {@link DerCurvePoint#x()} is an active power, as a percentage of maximum
 * active power, and {@link DerCurvePoint#y()} is a reactive power, as a
 * percentage of the curve's {@link WattVarCurve#getDependentReference()}.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerWattVarModelAccessor extends DerCurveModelAccessor {

	/**
	 * API for a single watt-var curve.
	 */
	interface WattVarCurve extends DerCurve {

		/**
		 * Get the dependent reference, which the reactive power of each point
		 * is a percentage of.
		 *
		 * @return the reference, or {@code null} if not available
		 */
		@Nullable
		DerReactivePowerReference getDependentReference();

		/**
		 * Set the dependent reference, which the reactive power of each point
		 * is a percentage of.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param reference
		 *        the reference to set
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setDependentReference(ModbusConnection conn, DerReactivePowerReference reference)
				throws IOException;

		/**
		 * Get the power priority.
		 *
		 * <p>
		 * The watt-var model supports only the
		 * {@link DerReactivePowerPriority#ActivePower} and
		 * {@link DerReactivePowerPriority#ReactivePower} priorities.
		 * </p>
		 *
		 * @return the priority, or {@code null} if not available
		 */
		@Nullable
		DerReactivePowerPriority getPowerPriority();

		/**
		 * Set the power priority.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param priority
		 *        the priority to set, either
		 *        {@link DerReactivePowerPriority#ActivePower} or
		 *        {@link DerReactivePowerPriority#ReactivePower}
		 * @throws IllegalArgumentException
		 *         if {@code priority} is not supported by the watt-var model
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setPowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
				throws IOException;

	}

	@Override
	List<WattVarCurve> getCurves();

}
