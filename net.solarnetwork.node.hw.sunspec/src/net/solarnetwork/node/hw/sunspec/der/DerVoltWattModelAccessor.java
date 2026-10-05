/* ==================================================================
 * DerVoltWattModelAccessor.java - 5/10/2026 4:58:20 pm
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
 * API for accessing DER volt-watt model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>706</b>. Each curve point
 * {@link DerCurvePoint#x()} is a voltage, as a percentage of nominal voltage,
 * and {@link DerCurvePoint#y()} is an active power, as a percentage of the
 * curve's {@link VoltWattCurve#getDependentReference()}.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerVoltWattModelAccessor extends DerCurveModelAccessor {

	/**
	 * API for a single volt-watt curve.
	 */
	interface VoltWattCurve extends DerCurve {

		/**
		 * Get the dependent reference, which the active power of each point is
		 * a percentage of.
		 *
		 * @return the reference, or {@code null} if not available
		 */
		@Nullable
		DerActivePowerReference getDependentReference();

		/**
		 * Set the dependent reference, which the active power of each point is
		 * a percentage of.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param reference
		 *        the reference to set
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setDependentReference(ModbusConnection conn, DerActivePowerReference reference)
				throws IOException;

		/**
		 * Get the open loop response time.
		 *
		 * @return the response time, in seconds, or {@code null} if not
		 *         available
		 */
		@Nullable
		Float getOpenLoopResponseTime();

		/**
		 * Set the open loop response time.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param seconds
		 *        the response time, in seconds
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setOpenLoopResponseTime(ModbusConnection conn, float seconds) throws IOException;

	}

	@Override
	List<VoltWattCurve> getCurves();

}
