/* ==================================================================
 * DerControlModelAccessorImpl.java - 5/10/2026 10:31:52 am
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
import java.util.Collection;
import java.util.EnumSet;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation of {@link DerControlModelAccessor}.
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public class DerControlModelAccessorImpl extends BaseModelAccessor implements DerControlModelAccessor {

	/** The DER control model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 7;

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public DerControlModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link DerModelId} class will be used as the {@code ModelId}
	 * instance.
	 * </p>
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public DerControlModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, DerModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	protected Collection<? extends ModbusReference> getFixedBlockRegisters() {
		return EnumSet.allOf(DerControlModelRegister.class);
	}

	@Override
	public @Nullable DerLocalRemoteControl getLocalRemoteControl() {
		return getCodedValue(DerControlModelRegister.LocalRemoteControl, DerLocalRemoteControl.class);
	}

	@Override
	public @Nullable Long getDerHeartbeat() {
		return getLongValue(DerControlModelRegister.DerHeartbeat);
	}

	@Override
	public @Nullable Long getControllerHeartbeat() {
		return getLongValue(DerControlModelRegister.ControllerHeartbeat);
	}

	@Override
	public void setControllerHeartbeat(ModbusConnection conn, long heartbeat) throws IOException {
		writeValue(conn, DerControlModelRegister.ControllerHeartbeat, heartbeat);
	}

	@Override
	public void resetAlarms(ModbusConnection conn) throws IOException {
		writeValue(conn, DerControlModelRegister.AlarmReset, 1);
	}

	@Override
	public @Nullable DerOperationCommand getOperationCommand() {
		return getCodedValue(DerControlModelRegister.OperationCommand, DerOperationCommand.class);
	}

	@Override
	public void setOperationCommand(ModbusConnection conn, DerOperationCommand command)
			throws IOException {
		writeValue(conn, DerControlModelRegister.OperationCommand, command.getCode());
	}

}
