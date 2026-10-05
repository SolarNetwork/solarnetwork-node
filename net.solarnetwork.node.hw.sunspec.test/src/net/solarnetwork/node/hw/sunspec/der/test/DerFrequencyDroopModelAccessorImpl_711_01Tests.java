/* ==================================================================
 * DerFrequencyDroopModelAccessorImpl_711_01Tests.java - 5/10/2026 7:12:33 pm
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerAdoptCurveResult;
import net.solarnetwork.node.hw.sunspec.der.DerFrequencyDroopModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerFrequencyDroopModelAccessor.FrequencyDroopControl;
import net.solarnetwork.node.hw.sunspec.der.DerFrequencyDroopModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerFrequencyDroopModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerFrequencyDroopModelAccessorImpl_711_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 1107;

	private DerFrequencyDroopModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerFrequencyDroopModelAccessor.class);
	}

	private RecordingModbusConnection writableConnection() {
		return ModelDataUtils.getWritableModbusConnection(getClass(), TEST_DATA);
	}

	private static DerFrequencyDroopModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerFrequencyDroopModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerFrequencyDroopModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerFrequencyDroopModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(1105)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.FrequencyDroop)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(12)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(10)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(3)));
		assertThat("Model length", model.getModelLength(), is(equalTo(42)));
	}

	@Test
	public void controlManagement() {
		DerFrequencyDroopModelAccessor model = getTestModel();
		assertThat("Enabled", model.isEnabled(), is(equalTo(false)));
		assertThat("Control count", model.getControlCount(), is(equalTo(3)));
		assertThat("Adopt control request", model.getAdoptControlRequest(), is(equalTo(0)));
		assertThat("Adopt control result", model.getAdoptControlResult(),
				is(equalTo(DerAdoptCurveResult.InProgress)));
		assertThat("Reversion time not implemented", model.getReversionTime(), is(nullValue()));
		assertThat("Reversion time remaining not implemented", model.getReversionTimeRemaining(),
				is(nullValue()));
		assertThat("Reversion control not implemented", model.getReversionControl(), is(nullValue()));
	}

	@Test
	public void controls() {
		List<FrequencyDroopControl> controls = getTestModel().getControls();
		assertThat("Controls", controls, hasSize(3));

		FrequencyDroopControl control = controls.get(0);
		assertThat("Index", control.getIndex(), is(equalTo(1)));
		assertThat("Read-only", control.isReadOnly(), is(equalTo(true)));
		assertThat("Over-frequency deadband", control.getOverFrequencyDeadband(), is(equalTo(0.036f)));
		assertThat("Under-frequency deadband", control.getUnderFrequencyDeadband(), is(equalTo(0.036f)));
		assertThat("Over-frequency change ratio", control.getOverFrequencyChangeRatio(),
				is(equalTo(0.05f)));
		assertThat("Under-frequency change ratio", control.getUnderFrequencyChangeRatio(),
				is(equalTo(0.05f)));
		assertThat("Open loop response time", control.getOpenLoopResponseTime(), is(equalTo(5.0f)));
		assertThat("Minimum active power", control.getMinimumActivePower(), is(equalTo(0)));

		for ( int i = 1; i < 3; i++ ) {
			FrequencyDroopControl stored = controls.get(i);
			String prefix = "Control " + (i + 1);
			assertThat(prefix + " index", stored.getIndex(), is(equalTo(i + 1)));
			assertThat(prefix + " read-only", stored.isReadOnly(), is(equalTo(false)));
			assertThat(prefix + " over-frequency deadband", stored.getOverFrequencyDeadband(),
					is(equalTo(0.0f)));
		}
	}

	@Test
	public void writeControl() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		FrequencyDroopControl control = discoverModel(conn).getControls().get(1);

		// WHEN
		control.setOverFrequencyDeadband(conn, 0.017f);
		control.setUnderFrequencyDeadband(conn, 0.025f);
		control.setOverFrequencyChangeRatio(conn, 0.04f);
		control.setUnderFrequencyChangeRatio(conn, 0.03f);
		control.setOpenLoopResponseTime(conn, 10.5f);
		control.setMinimumActivePower(conn, -50);

		// THEN
		List<FrequencyDroopControl> controls = discoverModel(conn).getControls();
		FrequencyDroopControl device = controls.get(1);
		assertThat("Over-frequency deadband", device.getOverFrequencyDeadband(), is(equalTo(0.017f)));
		assertThat("Under-frequency deadband", device.getUnderFrequencyDeadband(), is(equalTo(0.025f)));
		assertThat("Over-frequency change ratio", device.getOverFrequencyChangeRatio(),
				is(equalTo(0.04f)));
		assertThat("Under-frequency change ratio", device.getUnderFrequencyChangeRatio(),
				is(equalTo(0.03f)));
		assertThat("Open loop response time", device.getOpenLoopResponseTime(), is(equalTo(10.5f)));
		assertThat("Minimum active power", device.getMinimumActivePower(), is(equalTo(-50)));
		assertThat("Control 3 unchanged", controls.get(2).getOpenLoopResponseTime(), is(equalTo(0.0f)));
	}

	@Test
	public void setMinimumActivePower_outOfRange() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		FrequencyDroopControl control = discoverModel(conn).getControls().get(1);

		// THEN
		for ( int percent : new int[] { -101, 101 } ) {
			try {
				control.setMinimumActivePower(conn, percent);
				fail("Minimum active power " + percent + "% should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void readOnlyControl() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		FrequencyDroopControl control = discoverModel(conn).getControls().get(0);

		// WHEN
		try {
			control.setOverFrequencyDeadband(conn, 0.017f);
			fail("Writing to the read-only control should be rejected.");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void adoptControl() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerFrequencyDroopModelAccessor model = discoverModel(conn);

		// WHEN
		for ( int index : new int[] { -1, 4 } ) {
			try {
				model.adoptControl(conn, index);
				fail("Control index " + index + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		model.adoptControl(conn, 0);
		model.adoptControl(conn, 2);

		// THEN
		assertThat("Writes", conn.getWrites(),
				is(equalTo(List.of(List.of(BLOCK_ADDRESS + 1, 1), List.of(BLOCK_ADDRESS + 1, 1)))));
		assertThat("Adopt control request", discoverModel(conn).getAdoptControlRequest(),
				is(equalTo(2)));
	}

	@Test
	public void writeControlManagement() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerFrequencyDroopModelAccessor model = discoverModel(conn);

		// WHEN
		for ( int index : new int[] { 0, 4 } ) {
			try {
				model.setReversionControl(conn, index);
				fail("Reversion control index " + index + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		model.setEnabled(conn, true);
		model.setReversionTime(conn, 300);
		model.setReversionControl(conn, 3);

		// THEN
		DerFrequencyDroopModelAccessor device = discoverModel(conn);
		assertThat("Enabled", device.isEnabled(), is(equalTo(true)));
		assertThat("Reversion time", device.getReversionTime(), is(equalTo(300L)));
		assertThat("Reversion control", device.getReversionControl(), is(equalTo(3)));
	}

}
