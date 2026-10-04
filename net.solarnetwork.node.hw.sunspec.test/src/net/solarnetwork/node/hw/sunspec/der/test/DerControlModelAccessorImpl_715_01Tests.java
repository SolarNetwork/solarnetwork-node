/* ==================================================================
 * DerControlModelAccessorImpl_715_01Tests.java - 5/10/2026 10:42:15 am
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

package net.solarnetwork.node.hw.sunspec.der.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerControlModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerControlModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerControlModelRegister;
import net.solarnetwork.node.hw.sunspec.der.DerLocalRemoteControl;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerOperationCommand;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;

/**
 * Test cases for the {@link DerControlModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerControlModelAccessorImpl_715_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerControlModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerControlModelAccessor.class);
	}

	private static DerControlModelAccessorImpl discoverModel(ModbusConnection conn) {
		return (DerControlModelAccessorImpl) ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerControlModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerControlModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerControlModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(1265)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(1267)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.Control)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(7)));
		assertThat("Model length", model.getModelLength(), is(equalTo(7)));
	}

	@Test
	public void values() {
		DerControlModelAccessor model = getTestModel();
		assertThat("Local or remote control", model.getLocalRemoteControl(),
				is(equalTo(DerLocalRemoteControl.Remote)));
		assertThat("DER heartbeat", model.getDerHeartbeat(), is(equalTo(10615L)));
		assertThat("Controller heartbeat not implemented", model.getControllerHeartbeat(),
				is(nullValue()));
		assertThat("Operation command", model.getOperationCommand(),
				is(equalTo(DerOperationCommand.Start)));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerControlModelAccessor model = discoverModel(conn);

		// WHEN
		model.setControllerHeartbeat(conn, 123456L);
		model.setOperationCommand(conn, DerOperationCommand.EnterStandby);
		model.resetAlarms(conn);

		// THEN
		assertThat("Model data updated", model.getOperationCommand(),
				is(equalTo(DerOperationCommand.EnterStandby)));

		DerControlModelAccessorImpl device = discoverModel(conn);
		assertThat("Controller heartbeat", device.getControllerHeartbeat(), is(equalTo(123456L)));
		assertThat("Operation command", device.getOperationCommand(),
				is(equalTo(DerOperationCommand.EnterStandby)));
		assertThat("Alarm reset", device.getIntegerValue(DerControlModelRegister.AlarmReset),
				is(equalTo(1)));
		assertThat("DER heartbeat unchanged", device.getDerHeartbeat(), is(equalTo(10615L)));
	}

	@Test
	public void writeControllerHeartbeat_notImplementedValue() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerControlModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setControllerHeartbeat(conn, 0xFFFFFFFFL);
			fail("The uint32 not implemented value should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Device not updated", discoverModel(conn).getControllerHeartbeat(), is(nullValue()));
	}

}
