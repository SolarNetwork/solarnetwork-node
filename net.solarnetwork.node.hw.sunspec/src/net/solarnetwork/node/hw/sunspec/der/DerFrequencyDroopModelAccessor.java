/* ==================================================================
 * DerFrequencyDroopModelAccessor.java - 5/10/2026 7:12:33 pm
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
 * API for accessing DER frequency droop model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>711</b>, which provides
 * the IEEE 1547-2018 frequency droop (frequency-watt) settings. The model
 * stores a number of controls, each a set of frequency droop settings, and the
 * device uses the control selected with
 * {@link #adoptControl(ModbusConnection, int)}.
 * </p>
 *
 * <p>
 * Setter methods write to the device immediately, and throw
 * {@link IllegalArgumentException} if the value is not valid for the point,
 * {@link IllegalStateException} if the model scale factors have not been read
 * from the device or are not implemented, or
 * {@link UnsupportedOperationException} if the control is read-only.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerFrequencyDroopModelAccessor extends ModelAccessor {

	/**
	 * API for a single frequency droop control.
	 */
	interface FrequencyDroopControl {

		/**
		 * Get the control index.
		 *
		 * @return the index, starting from {@literal 1}
		 */
		int getIndex();

		/**
		 * Get the read-only setting.
		 *
		 * @return {@literal true} if the control is read-only, or {@code null}
		 *         if not available
		 */
		@Nullable
		Boolean isReadOnly();

		/**
		 * Get the over-frequency deadband.
		 *
		 * @return the deadband, in hertz, or {@code null} if not available
		 */
		@Nullable
		Float getOverFrequencyDeadband();

		/**
		 * Set the over-frequency deadband.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param hertz
		 *        the deadband, in hertz
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setOverFrequencyDeadband(ModbusConnection conn, float hertz) throws IOException;

		/**
		 * Get the under-frequency deadband.
		 *
		 * @return the deadband, in hertz, or {@code null} if not available
		 */
		@Nullable
		Float getUnderFrequencyDeadband();

		/**
		 * Set the under-frequency deadband.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param hertz
		 *        the deadband, in hertz
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setUnderFrequencyDeadband(ModbusConnection conn, float hertz) throws IOException;

		/**
		 * Get the over-frequency change ratio.
		 *
		 * <p>
		 * This is the per-unit frequency change, for over-frequency conditions,
		 * corresponding to a 1 per-unit power output change.
		 * </p>
		 *
		 * @return the change ratio, or {@code null} if not available
		 */
		@Nullable
		Float getOverFrequencyChangeRatio();

		/**
		 * Set the over-frequency change ratio.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param ratio
		 *        the change ratio
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setOverFrequencyChangeRatio(ModbusConnection conn, float ratio) throws IOException;

		/**
		 * Get the under-frequency change ratio.
		 *
		 * <p>
		 * This is the per-unit frequency change, for under-frequency
		 * conditions, corresponding to a 1 per-unit power output change.
		 * </p>
		 *
		 * @return the change ratio, or {@code null} if not available
		 */
		@Nullable
		Float getUnderFrequencyChangeRatio();

		/**
		 * Set the under-frequency change ratio.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param ratio
		 *        the change ratio
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setUnderFrequencyChangeRatio(ModbusConnection conn, float ratio) throws IOException;

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

		/**
		 * Get the minimum active power.
		 *
		 * <p>
		 * This is the minimum active power output due to DER prime mover
		 * constraints.
		 * </p>
		 *
		 * @return the minimum active power, as a percentage of the active power
		 *         rating, or {@code null} if not available
		 */
		@Nullable
		Integer getMinimumActivePower();

		/**
		 * Set the minimum active power.
		 *
		 * @param conn
		 *        the connection to write to
		 * @param percent
		 *        the minimum active power, as a percentage of the active power
		 *        rating, from {@literal -100} to {@literal 100}
		 * @throws IllegalArgumentException
		 *         if {@code percent} is outside the allowed range
		 * @throws IOException
		 *         if any communication error occurs
		 */
		void setMinimumActivePower(ModbusConnection conn, int percent) throws IOException;

	}

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
	 * Get the number of controls.
	 *
	 * @return the number of controls, or {@code null} if not available
	 */
	@Nullable
	Integer getControlCount();

	/**
	 * Get the last adopt control request.
	 *
	 * @return the index of the last control requested to be made active, where
	 *         {@literal 0} means no active control, or {@code null} if not
	 *         available
	 */
	@Nullable
	Integer getAdoptControlRequest();

	/**
	 * Request a control be made active.
	 *
	 * <p>
	 * The device reports the result with {@link #getAdoptControlResult()}.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param index
	 *        the index of the control to make active, from {@literal 1} to the
	 *        control count, or {@literal 0} for no active control
	 * @throws IllegalArgumentException
	 *         if {@code index} is outside the allowed range
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void adoptControl(ModbusConnection conn, int index) throws IOException;

	/**
	 * Get the result of the last adopt control request.
	 *
	 * @return the result, or {@code null} if not available
	 */
	@Nullable
	DerAdoptResult getAdoptControlResult();

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
	 * Get the reversion control.
	 *
	 * @return the index of the control to adopt when the reversion timeout
	 *         expires, or {@code null} if not available
	 */
	@Nullable
	Integer getReversionControl();

	/**
	 * Set the reversion control.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param index
	 *        the index of the control to adopt when the reversion timeout
	 *        expires, from {@literal 1} to the control count
	 * @throws IllegalArgumentException
	 *         if {@code index} is outside the allowed range
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setReversionControl(ModbusConnection conn, int index) throws IOException;

	/**
	 * Get the controls.
	 *
	 * <p>
	 * The number of controls is the control count, limited to the number of
	 * controls the model length allows for.
	 * </p>
	 *
	 * @return the controls, never {@code null}
	 */
	List<FrequencyDroopControl> getControls();

}
