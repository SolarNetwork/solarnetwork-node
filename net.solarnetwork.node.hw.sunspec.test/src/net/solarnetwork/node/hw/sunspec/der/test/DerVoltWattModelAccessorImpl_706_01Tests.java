/* ==================================================================
 * DerVoltWattModelAccessorImpl_706_01Tests.java - 5/10/2026 5:31:07 pm
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
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerActivePowerReference;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerVoltWattModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerVoltWattModelAccessor.VoltWattCurve;
import net.solarnetwork.node.hw.sunspec.der.DerVoltWattModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerVoltWattModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerVoltWattModelAccessorImpl_706_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 541;

	/** The curve 3 address: 13 fixed registers, then 9 registers per curve. */
	private static final int CURVE_3_ADDRESS = BLOCK_ADDRESS + 13 + 9 * 2;

	private DerVoltWattModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerVoltWattModelAccessor.class);
	}

	private static DerVoltWattModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn).findTypedModel(DerVoltWattModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerVoltWattModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerVoltWattModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(539)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.VoltWatt)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(13)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(9)));
		assertThat("Model length", model.getModelLength(), is(equalTo(40)));
	}

	@Test
	public void curves() {
		DerVoltWattModelAccessor model = getTestModel();
		assertThat("Enabled", model.isEnabled(), is(equalTo(true)));
		assertThat("Curve count", model.getCurveCount(), is(equalTo(3)));
		assertThat("Curve point count", model.getCurvePointCount(), is(equalTo(2)));

		List<VoltWattCurve> curves = model.getCurves();
		assertThat("Curves", curves, hasSize(3));
		VoltWattCurve curve = curves.get(0);
		assertThat("Read-only", curve.isReadOnly(), is(equalTo(true)));
		assertThat("Dependent reference", curve.getDependentReference(),
				is(equalTo(DerActivePowerReference.MaximumActivePowerPercent)));
		assertThat("Open loop response time", curve.getOpenLoopResponseTime(), is(equalTo(10.0f)));
		assertThat("Points", curve.getPoints(), is(
				equalTo(List.of(new DerCurvePoint(106.0f, 100.0f), new DerCurvePoint(110.0f, 20.0f)))));
		assertThat("Stored curve read-only", curves.get(2).isReadOnly(), is(equalTo(false)));
	}

	@Test
	public void writeCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		VoltWattCurve curve = discoverModel(conn).getCurves().get(2);

		// WHEN
		curve.setDependentReference(conn, DerActivePowerReference.AvailableActivePowerPercent);
		curve.setOpenLoopResponseTime(conn, 2.5f);
		curve.setPoints(conn,
				List.of(new DerCurvePoint(105.0f, 100.0f), new DerCurvePoint(109.0f, 0.0f)));

		// THEN
		assertThat("Writes", conn.getWrites(),
				is(equalTo(List.of(List.of(CURVE_3_ADDRESS + 1, 1), List.of(CURVE_3_ADDRESS + 2, 2),
						List.of(CURVE_3_ADDRESS + 5, 4), List.of(CURVE_3_ADDRESS, 1)))));

		VoltWattCurve device = discoverModel(conn).getCurves().get(2);
		assertThat("Dependent reference", device.getDependentReference(),
				is(equalTo(DerActivePowerReference.AvailableActivePowerPercent)));
		assertThat("Open loop response time", device.getOpenLoopResponseTime(), is(equalTo(2.5f)));
		assertThat("Points", device.getPoints(), is(
				equalTo(List.of(new DerCurvePoint(105.0f, 100.0f), new DerCurvePoint(109.0f, 0.0f)))));
	}

}
