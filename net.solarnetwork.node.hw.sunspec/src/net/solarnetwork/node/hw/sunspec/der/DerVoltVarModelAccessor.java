/* ==================================================================
 * DerVoltVarModelAccessor.java - 5/10/2026 4:58:20 pm
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
 * API for accessing DER volt-var model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>705</b>. Each curve point
 * {@link DerCurvePoint#x()} is a voltage, as a percentage of nominal voltage,
 * and {@link DerCurvePoint#y()} is a reactive power, as a percentage of the
 * curve's {@link VoltVarCurve#getDependentReference()}.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerVoltVarModelAccessor extends DerCurveModelAccessor {

	/**
	 * API for a single volt-var curve.
	 */
	interface VoltVarCurve extends DerCurve {

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
		 *        the priority to set
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setPowerPriority(ModbusConnection conn, DerReactivePowerPriority priority)
				throws IOException;

		/**
		 * Get the voltage reference adjustment.
		 *
		 * @return the voltage reference, as a percentage of nominal voltage, or
		 *         {@code null} if not available
		 */
		@Nullable
		Float getVoltageReference();

		/**
		 * Set the voltage reference adjustment.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param percent
		 *        the voltage reference, as a percentage of nominal voltage
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setVoltageReference(ModbusConnection conn, float percent) throws IOException;

		/**
		 * Get the current autonomous voltage reference.
		 *
		 * @return the voltage reference, as a percentage of nominal voltage, or
		 *         {@code null} if not available
		 */
		@Nullable
		Float getAutonomousVoltageReference();

		/**
		 * Get the autonomous voltage reference enable setting.
		 *
		 * @return {@literal true} if the autonomous voltage reference is
		 *         enabled, or {@code null} if not available
		 */
		@Nullable
		Boolean isAutonomousVoltageReferenceEnabled();

		/**
		 * Set the autonomous voltage reference enable setting.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param enabled
		 *        {@literal true} to enable the autonomous voltage reference
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setAutonomousVoltageReferenceEnabled(ModbusConnection conn, boolean enabled)
				throws IOException;

		/**
		 * Get the autonomous voltage reference time constant.
		 *
		 * @return the time constant, in seconds, or {@code null} if not
		 *         available
		 */
		@Nullable
		Integer getAutonomousVoltageReferenceTimeConstant();

		/**
		 * Set the autonomous voltage reference time constant.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param seconds
		 *        the time constant, in seconds
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setAutonomousVoltageReferenceTimeConstant(ModbusConnection conn, int seconds)
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
	List<VoltVarCurve> getCurves();

}
