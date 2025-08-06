/* ==================================================================
 * BaseCommandHandler.java - 22/02/2019 9:56:10 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl;

import com.automatak.dnp3.AnalogOutputDouble64;
import com.automatak.dnp3.AnalogOutputFloat32;
import com.automatak.dnp3.AnalogOutputInt16;
import com.automatak.dnp3.AnalogOutputInt32;
import com.automatak.dnp3.CommandHandler;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.Database;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.OperateType;

/**
 * Base implementation of {@link CommandHandler}.
 *
 * <p>
 * This implementation is configured with a "default" command status value that
 * all methods return. Extending classes should override methods to perform
 * useful work.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public class BaseCommandHandler implements CommandHandler {

	private CommandStatus defaultStatus;

	/**
	 * Constructor.
	 *
	 * @param defaultStatus
	 *        the default status
	 */
	public BaseCommandHandler(CommandStatus defaultStatus) {
		super();
		setDefaultStatus(defaultStatus);
	}

	/**
	 * Set the default status value.
	 *
	 * @param defaultStatus
	 *        the default status to set
	 */
	protected void setDefaultStatus(CommandStatus defaultStatus) {
		this.defaultStatus = defaultStatus;
	}

	@Override
	public void begin() {
		// nothing to do
	}

	@Override
	public void end() {
		// nothing to do
	}

	@Override
	public CommandStatus select(ControlRelayOutputBlock command, int index) {
		return defaultStatus;
	}

	@Override
	public CommandStatus select(AnalogOutputInt32 command, int index) {
		return defaultStatus;
	}

	@Override
	public CommandStatus select(AnalogOutputInt16 command, int index) {
		return defaultStatus;
	}

	@Override
	public CommandStatus select(AnalogOutputFloat32 command, int index) {
		return defaultStatus;
	}

	@Override
	public CommandStatus select(AnalogOutputDouble64 command, int index) {
		return defaultStatus;
	}

	@Override
	public CommandStatus operate(ControlRelayOutputBlock command, int index, Database database,
			OperateType opType) {
		return defaultStatus;
	}

	@Override
	public CommandStatus operate(AnalogOutputInt32 command, int index, Database database,
			OperateType opType) {
		return defaultStatus;
	}

	@Override
	public CommandStatus operate(AnalogOutputInt16 command, int index, Database database,
			OperateType opType) {
		return defaultStatus;
	}

	@Override
	public CommandStatus operate(AnalogOutputFloat32 command, int index, Database database,
			OperateType opType) {
		return defaultStatus;
	}

	@Override
	public CommandStatus operate(AnalogOutputDouble64 command, int index, Database database,
			OperateType opType) {
		return defaultStatus;
	}

}
