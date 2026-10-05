/* ==================================================================
 * DerVoltVarModelAccessorImpl_705_01Tests.java - 5/10/2026 5:31:07 pm
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
import net.solarnetwork.node.hw.sunspec.der.DerAdoptResult;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerPriority;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerReference;
import net.solarnetwork.node.hw.sunspec.der.DerVoltVarModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerVoltVarModelAccessor.VoltVarCurve;
import net.solarnetwork.node.hw.sunspec.der.DerVoltVarModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerVoltVarModelAccessorImpl} class, including the
 * curve support shared by the other DER curve models.
 *
 * @author matt
 * @version 1.0
 */
public class DerVoltVarModelAccessorImpl_705_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 472;

	/** The curve 2 address: 13 fixed registers, then 18 registers per curve. */
	private static final int CURVE_2_ADDRESS = BLOCK_ADDRESS + 13 + 18;

	/** The curve 2 points address: 10 setting registers, then the points. */
	private static final int CURVE_2_POINTS_ADDRESS = CURVE_2_ADDRESS + 10;

	private DerVoltVarModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerVoltVarModelAccessor.class);
	}

	private RecordingModbusConnection writableConnection() {
		return ModelDataUtils.getWritableModbusConnection(getClass(), TEST_DATA);
	}

	private static DerVoltVarModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn).findTypedModel(DerVoltVarModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerVoltVarModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerVoltVarModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(470)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.VoltVar)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(13)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(18)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(3)));
		assertThat("Model length", model.getModelLength(), is(equalTo(67)));
	}

	@Test
	public void curveManagement() {
		DerVoltVarModelAccessor model = getTestModel();
		assertThat("Enabled", model.isEnabled(), is(equalTo(false)));
		assertThat("Curve count", model.getCurveCount(), is(equalTo(3)));
		assertThat("Curve point count", model.getCurvePointCount(), is(equalTo(4)));
		assertThat("Adopt curve request", model.getAdoptCurveRequest(), is(equalTo(0)));
		assertThat("Adopt curve result", model.getAdoptCurveResult(),
				is(equalTo(DerAdoptResult.InProgress)));
		assertThat("Reversion time not implemented", model.getReversionTime(), is(nullValue()));
		assertThat("Reversion time remaining not implemented", model.getReversionTimeRemaining(),
				is(nullValue()));
		assertThat("Reversion curve not implemented", model.getReversionCurve(), is(nullValue()));
	}

	@Test
	public void activeCurve() {
		List<VoltVarCurve> curves = getTestModel().getCurves();
		assertThat("Curves", curves, hasSize(3));

		VoltVarCurve curve = curves.get(0);
		assertThat("Index", curve.getIndex(), is(equalTo(1)));
		assertThat("Read-only", curve.isReadOnly(), is(equalTo(true)));
		assertThat("Active point count", curve.getActivePointCount(), is(equalTo(4)));
		assertThat("Dependent reference", curve.getDependentReference(),
				is(equalTo(DerReactivePowerReference.MaximumActivePowerPercent)));
		assertThat("Power priority", curve.getPowerPriority(),
				is(equalTo(DerReactivePowerPriority.ActivePower)));
		assertThat("Voltage reference", curve.getVoltageReference(), is(equalTo(100.0f)));
		assertThat("Autonomous voltage reference", curve.getAutonomousVoltageReference(),
				is(equalTo(100.0f)));
		assertThat("Autonomous voltage reference enabled", curve.isAutonomousVoltageReferenceEnabled(),
				is(equalTo(false)));
		assertThat("Autonomous voltage reference time constant",
				curve.getAutonomousVoltageReferenceTimeConstant(), is(equalTo(301)));
		assertThat("Open loop response time", curve.getOpenLoopResponseTime(), is(equalTo(10.0f)));
		assertThat("Points", curve.getPoints(),
				is(equalTo(List.of(new DerCurvePoint(90.0f, 25.0f), new DerCurvePoint(100.0f, 0.0f),
						new DerCurvePoint(100.0f, 0.0f), new DerCurvePoint(110.0f, -25.0f)))));
	}

	@Test
	public void storedCurves() {
		List<VoltVarCurve> curves = getTestModel().getCurves();
		for ( int i = 1; i < 3; i++ ) {
			VoltVarCurve curve = curves.get(i);
			String prefix = "Curve " + (i + 1);
			assertThat(prefix + " index", curve.getIndex(), is(equalTo(i + 1)));
			assertThat(prefix + " read-only", curve.isReadOnly(), is(equalTo(false)));
			assertThat(prefix + " points", curve.getPoints(),
					is(equalTo(List.of(new DerCurvePoint(0.0f, 0.0f), new DerCurvePoint(0.0f, 0.0f),
							new DerCurvePoint(0.0f, 0.0f), new DerCurvePoint(0.0f, 0.0f)))));
		}
	}

	@Test
	public void writeCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		curve.setDependentReference(conn, DerReactivePowerReference.MaximumReactivePowerPercent);
		curve.setPowerPriority(conn, DerReactivePowerPriority.ReactivePower);
		curve.setVoltageReference(conn, 101.5f);
		curve.setAutonomousVoltageReferenceEnabled(conn, true);
		curve.setAutonomousVoltageReferenceTimeConstant(conn, 120);
		curve.setOpenLoopResponseTime(conn, 5.5f);
		curve.setPoints(conn, List.of(new DerCurvePoint(92.0f, 30.0f), new DerCurvePoint(98.0f, 0.0f),
				new DerCurvePoint(102.0f, 0.0f), new DerCurvePoint(108.0f, -30.0f)));

		// THEN
		List<VoltVarCurve> curves = discoverModel(conn).getCurves();
		VoltVarCurve device = curves.get(1);
		assertThat("Dependent reference", device.getDependentReference(),
				is(equalTo(DerReactivePowerReference.MaximumReactivePowerPercent)));
		assertThat("Power priority", device.getPowerPriority(),
				is(equalTo(DerReactivePowerPriority.ReactivePower)));
		assertThat("Voltage reference", device.getVoltageReference(), is(equalTo(101.5f)));
		assertThat("Autonomous voltage reference enabled", device.isAutonomousVoltageReferenceEnabled(),
				is(equalTo(true)));
		assertThat("Autonomous voltage reference time constant",
				device.getAutonomousVoltageReferenceTimeConstant(), is(equalTo(120)));
		assertThat("Open loop response time", device.getOpenLoopResponseTime(), is(equalTo(5.5f)));
		assertThat("Points", device.getPoints(),
				is(equalTo(List.of(new DerCurvePoint(92.0f, 30.0f), new DerCurvePoint(98.0f, 0.0f),
						new DerCurvePoint(102.0f, 0.0f), new DerCurvePoint(108.0f, -30.0f)))));
		assertThat("Active curve unchanged", curves.get(0).getPoints().get(0),
				is(equalTo(new DerCurvePoint(90.0f, 25.0f))));
		assertThat("Curve 3 unchanged", curves.get(2).getPoints().get(0),
				is(equalTo(new DerCurvePoint(0.0f, 0.0f))));
	}

	@Test
	public void setPoints_requests() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		curve.setPoints(conn, List.of(new DerCurvePoint(92.0f, 30.0f), new DerCurvePoint(98.0f, 0.0f),
				new DerCurvePoint(102.0f, 0.0f), new DerCurvePoint(108.0f, -30.0f)));

		// THEN
		assertThat("Points written in one request, then the active point count", conn.getWrites(),
				is(equalTo(List.of(List.of(CURVE_2_POINTS_ADDRESS, 8), List.of(CURVE_2_ADDRESS, 1)))));
	}

	@Test
	public void setPoints_fewerThanPointCount() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		curve.setPoints(conn, List.of(new DerCurvePoint(95.0f, 10.0f), new DerCurvePoint(100.0f, 0.0f),
				new DerCurvePoint(105.0f, -10.0f)));

		// THEN
		VoltVarCurve device = discoverModel(conn).getCurves().get(1);
		assertThat("Active point count", device.getActivePointCount(), is(equalTo(3)));
		assertThat("Active points", device.getPoints(),
				is(equalTo(List.of(new DerCurvePoint(95.0f, 10.0f), new DerCurvePoint(100.0f, 0.0f),
						new DerCurvePoint(105.0f, -10.0f)))));
	}

	@Test
	public void setActivePointCount() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		curve.setActivePointCount(conn, 2);

		// THEN
		assertThat("Active point count written", conn.getWrites(),
				is(equalTo(List.of(List.of(CURVE_2_ADDRESS, 1)))));
		assertThat("Active point count", discoverModel(conn).getCurves().get(1).getPoints(), hasSize(2));
	}

	@Test
	public void setPoints_invalidCount() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);
		DerCurvePoint p = new DerCurvePoint(100.0f, 0.0f);

		// THEN
		for ( List<DerCurvePoint> points : List.of(List.<DerCurvePoint> of(), List.of(p, p, p, p, p)) ) {
			try {
				curve.setPoints(conn, points);
				fail("Point count " + points.size() + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		try {
			curve.setActivePointCount(conn, 5);
			fail("Active point count above the curve point count should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void setPoints_invalidValue() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(1);

		// WHEN
		try {
			curve.setPoints(conn,
					List.of(new DerCurvePoint(100.0f, 0.0f), new DerCurvePoint(7000.0f, 0.0f)));
			fail("Scaled value larger than uint16 should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void readOnlyCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		VoltVarCurve curve = discoverModel(conn).getCurves().get(0);

		// THEN
		try {
			curve.setPoints(conn, List.of(new DerCurvePoint(100.0f, 0.0f)));
			fail("Writing points to the read-only curve should be rejected.");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
		try {
			curve.setDependentReference(conn, DerReactivePowerReference.MaximumApparentPowerPercent);
			fail("Writing a setting to the read-only curve should be rejected.");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void adoptCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerVoltVarModelAccessor model = discoverModel(conn);

		// WHEN
		model.adoptCurve(conn, 2);

		// THEN
		assertThat("Adopt curve request written", conn.getWrites(),
				is(equalTo(List.of(List.of(BLOCK_ADDRESS + 1, 1)))));
		assertThat("Adopt curve request", discoverModel(conn).getAdoptCurveRequest(), is(equalTo(2)));
	}

	@Test
	public void adoptCurve_invalidIndex() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerVoltVarModelAccessor model = discoverModel(conn);

		// THEN
		for ( int index : new int[] { 0, 1, 4 } ) {
			try {
				model.adoptCurve(conn, index);
				fail("Curve index " + index + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void writeCurveManagement() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerVoltVarModelAccessor model = discoverModel(conn);

		// WHEN
		model.setEnabled(conn, true);
		model.setReversionTime(conn, 600);
		model.setReversionCurve(conn, 3);

		// THEN
		DerVoltVarModelAccessor device = discoverModel(conn);
		assertThat("Enabled", device.isEnabled(), is(equalTo(true)));
		assertThat("Reversion time", device.getReversionTime(), is(equalTo(600L)));
		assertThat("Reversion curve", device.getReversionCurve(), is(equalTo(3)));
	}

	@Test
	public void setReversionCurve_invalidIndex() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerVoltVarModelAccessor model = discoverModel(conn);

		// THEN
		for ( int index : new int[] { 0, 4 } ) {
			try {
				model.setReversionCurve(conn, index);
				fail("Curve index " + index + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

}
