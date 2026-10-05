/* ==================================================================
 * DerWattVarModelAccessorImpl_712_01Tests.java - 5/10/2026 5:31:07 pm
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
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerPriority;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerReference;
import net.solarnetwork.node.hw.sunspec.der.DerWattVarModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerWattVarModelAccessor.WattVarCurve;
import net.solarnetwork.node.hw.sunspec.der.DerWattVarModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerWattVarModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerWattVarModelAccessorImpl_712_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private static final List<DerCurvePoint> NEW_POINTS = List.of(new DerCurvePoint(-100.0f, 20.0f),
			new DerCurvePoint(-50.0f, 10.0f), new DerCurvePoint(-10.0f, 0.0f),
			new DerCurvePoint(10.0f, 0.0f), new DerCurvePoint(50.0f, -10.0f),
			new DerCurvePoint(100.0f, -20.0f));

	private DerWattVarModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerWattVarModelAccessor.class);
	}

	private static DerWattVarModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn).findTypedModel(DerWattVarModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerWattVarModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerWattVarModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(1149)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(1151)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.WattVar)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(12)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(16)));
		assertThat("Model length", model.getModelLength(), is(equalTo(60)));
	}

	@Test
	public void curves() {
		DerWattVarModelAccessor model = getTestModel();
		assertThat("Enabled", model.isEnabled(), is(equalTo(false)));
		assertThat("Curve count", model.getCurveCount(), is(equalTo(3)));
		assertThat("Curve point count", model.getCurvePointCount(), is(equalTo(6)));

		List<WattVarCurve> curves = model.getCurves();
		assertThat("Curves", curves, hasSize(3));
		WattVarCurve curve = curves.get(0);
		assertThat("Read-only", curve.isReadOnly(), is(equalTo(true)));
		assertThat("Dependent reference", curve.getDependentReference(),
				is(equalTo(DerReactivePowerReference.MaximumActivePowerPercent)));
		assertThat("Power priority", curve.getPowerPriority(),
				is(equalTo(DerReactivePowerPriority.ActivePower)));
		assertThat("Points", curve.getPoints(),
				is(equalTo(List.of(new DerCurvePoint(-100.0f, 0.0f), new DerCurvePoint(-50.0f, 0.0f),
						new DerCurvePoint(-20.0f, 0.0f), new DerCurvePoint(20.0f, 0.0f),
						new DerCurvePoint(50.0f, 0.0f), new DerCurvePoint(100.0f, -25.0f)))));
	}

	@Test
	public void writeCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		WattVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		curve.setDependentReference(conn, DerReactivePowerReference.AvailableReactivePowerPercent);
		curve.setPowerPriority(conn, DerReactivePowerPriority.ReactivePower);
		curve.setPoints(conn, NEW_POINTS);

		// THEN
		WattVarCurve device = discoverModel(conn).getCurves().get(1);
		assertThat("Dependent reference", device.getDependentReference(),
				is(equalTo(DerReactivePowerReference.AvailableReactivePowerPercent)));
		assertThat("Power priority", device.getPowerPriority(),
				is(equalTo(DerReactivePowerPriority.ReactivePower)));
		assertThat("Points", device.getPoints(), is(equalTo(NEW_POINTS)));
	}

	@Test
	public void setPowerPriority_vendor() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		WattVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		try {
			curve.setPowerPriority(conn, DerReactivePowerPriority.Vendor);
			fail("The vendor priority is not supported by the watt-var model.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

}
