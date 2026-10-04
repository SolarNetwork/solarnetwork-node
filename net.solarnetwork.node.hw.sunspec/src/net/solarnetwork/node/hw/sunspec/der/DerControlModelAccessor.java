/* ==================================================================
 * DerControlModelAccessor.java - 5/10/2026 10:31:52 am
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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * API for accessing DER control model data.
 *
 * <p>
 * This API corresponds to the SunSpec model number <b>715</b>. Setter methods
 * write to the device immediately, and throw {@link IllegalArgumentException}
 * if the value is not valid for the point.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public interface DerControlModelAccessor extends ModelAccessor {

	/**
	 * Get the local or remote control mode.
	 *
	 * <p>
	 * Local mode is used for manual or maintenance operations, and must be
	 * explicitly exited for the DER to be controlled remotely.
	 * </p>
	 *
	 * @return the mode, or {@code null} if not available
	 */
	@Nullable
	DerLocalRemoteControl getLocalRemoteControl();

	/**
	 * Get the DER heartbeat.
	 *
	 * <p>
	 * The DER increments this value every second, with periodic resets to
	 * zero.
	 * </p>
	 *
	 * @return the heartbeat, or {@code null} if not available
	 */
	@Nullable
	Long getDerHeartbeat();

	/**
	 * Get the controller heartbeat.
	 *
	 * @return the heartbeat, or {@code null} if not available
	 */
	@Nullable
	Long getControllerHeartbeat();

	/**
	 * Set the controller heartbeat.
	 *
	 * <p>
	 * The controller is expected to increment this value every second, with
	 * periodic resets to zero.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param heartbeat
	 *        the heartbeat value to set
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setControllerHeartbeat(ModbusConnection conn, long heartbeat) throws IOException;

	/**
	 * Reset any latched alarms.
	 *
	 * @param conn
	 *        the connection to write to
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void resetAlarms(ModbusConnection conn) throws IOException;

	/**
	 * Get the operation command.
	 *
	 * @return the command, or {@code null} if not available
	 */
	@Nullable
	DerOperationCommand getOperationCommand();

	/**
	 * Set the operation command.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param command
	 *        the command to send
	 * @throws IOException
	 *         if any communication error occurs
	 */
	void setOperationCommand(ModbusConnection conn, DerOperationCommand command) throws IOException;

}
