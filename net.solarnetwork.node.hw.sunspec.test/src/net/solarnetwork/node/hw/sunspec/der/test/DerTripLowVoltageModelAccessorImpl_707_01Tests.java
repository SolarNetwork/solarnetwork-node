/* ==================================================================
 * DerTripLowVoltageModelAccessorImpl_707_01Tests.java - 5/10/2026 6:48:10 pm
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
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerAdoptResult;
import net.solarnetwork.node.hw.sunspec.der.DerCurve;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerTripCurveSet;
import net.solarnetwork.node.hw.sunspec.der.DerTripLowVoltageModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerTripLowVoltageModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerTripLowVoltageModelAccessorImpl} class,
 * including the curve set support shared by the other DER trip models.
 *
 * @author matt
 * @version 1.0
 */
public class DerTripLowVoltageModelAccessorImpl_707_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 583;

	/**
	 * The curve set 2 address: 7 fixed registers, then 67 registers per set.
	 */
	private static final int SET_2_ADDRESS = BLOCK_ADDRESS + 7 + 67;

	/**
	 * The curve set 2 must trip curve address, after the read-only register.
	 */
	private static final int SET_2_MUST_TRIP_ADDRESS = SET_2_ADDRESS + 1;

	/** The curve set 2 may trip curve address, after the 22 register curve. */
	private static final int SET_2_MAY_TRIP_ADDRESS = SET_2_MUST_TRIP_ADDRESS + 22;

	private static final List<DerCurvePoint> MUST_TRIP_POINTS = List.of(new DerCurvePoint(0.0f, 0.16f),
			new DerCurvePoint(45.0f, 0.16f), new DerCurvePoint(45.0f, 10.0f),
			new DerCurvePoint(70.0f, 10.0f), new DerCurvePoint(70.0f, 10.0f));

	private static final List<DerCurvePoint> MOM_CESS_POINTS = List.of(new DerCurvePoint(0.0f, 0.08f),
			new DerCurvePoint(50.0f, 0.08f));

	private static final List<DerCurvePoint> NEW_MUST_TRIP_POINTS = List.of(
			new DerCurvePoint(0.0f, 0.16f), new DerCurvePoint(50.0f, 0.16f),
			new DerCurvePoint(50.0f, 2.0f), new DerCurvePoint(88.0f, 2.0f),
			new DerCurvePoint(88.0f, 21.0f));

	private DerTripLowVoltageModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerTripLowVoltageModelAccessor.class);
	}

	private RecordingModbusConnection writableConnection() {
		return ModelDataUtils.getWritableModbusConnection(getClass(), TEST_DATA);
	}

	private static DerTripLowVoltageModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerTripLowVoltageModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerTripLowVoltageModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerTripLowVoltageModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(581)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.TripLowVoltage)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(7)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(67)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(2)));
		assertThat("Model length", model.getModelLength(), is(equalTo(141)));
	}

	@Test
	public void curveManagement() {
		DerTripLowVoltageModelAccessor model = getTestModel();
		assertThat("Enabled", model.isEnabled(), is(equalTo(true)));
		assertThat("Curve set count", model.getCurveSetCount(), is(equalTo(2)));
		assertThat("Curve point count", model.getCurvePointCount(), is(equalTo(7)));
		assertThat("Adopt curve request", model.getAdoptCurveRequest(), is(equalTo(0)));
		assertThat("Adopt curve result", model.getAdoptCurveResult(),
				is(equalTo(DerAdoptResult.InProgress)));
	}

	@Test
	public void curveSets() {
		List<DerTripCurveSet> sets = getTestModel().getCurveSets();
		assertThat("Curve sets", sets, hasSize(2));
		for ( DerTripCurveSet set : sets ) {
			String prefix = "Set " + set.getIndex();
			boolean active = set.getIndex() == 1;
			assertThat(prefix + " read-only", set.isReadOnly(), is(equalTo(active)));

			DerCurve mustTrip = set.getMustTripCurve();
			assertThat(prefix + " must trip index", mustTrip.getIndex(), is(equalTo(set.getIndex())));
			assertThat(prefix + " must trip read-only", mustTrip.isReadOnly(), is(equalTo(active)));
			assertThat(prefix + " must trip active points", mustTrip.getActivePointCount(),
					is(equalTo(5)));
			assertThat(prefix + " must trip points", mustTrip.getPoints(),
					is(equalTo(MUST_TRIP_POINTS)));

			DerCurve mayTrip = set.getMayTripCurve();
			assertThat(prefix + " may trip active points not implemented", mayTrip.getActivePointCount(),
					is(nullValue()));
			assertThat(prefix + " may trip points", mayTrip.getPoints(),
					is(equalTo(Collections.emptyList())));

			DerCurve momCess = set.getMomentaryCessationCurve();
			assertThat(prefix + " momentary cessation active points", momCess.getActivePointCount(),
					is(equalTo(2)));
			assertThat(prefix + " momentary cessation points", momCess.getPoints(),
					is(equalTo(MOM_CESS_POINTS)));
		}
	}

	@Test
	public void writeMustTripCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerCurve curve = discoverModel(conn).getCurveSets().get(1).getMustTripCurve();

		// WHEN
		curve.setPoints(conn, NEW_MUST_TRIP_POINTS);

		// THEN
		assertThat("Points written in one request, then the active point count", conn.getWrites(),
				is(equalTo(List.of(List.of(SET_2_MUST_TRIP_ADDRESS + 1, 15),
						List.of(SET_2_MUST_TRIP_ADDRESS, 1)))));

		List<DerTripCurveSet> sets = discoverModel(conn).getCurveSets();
		assertThat("Must trip points", sets.get(1).getMustTripCurve().getPoints(),
				is(equalTo(NEW_MUST_TRIP_POINTS)));
		assertThat("Momentary cessation unchanged", sets.get(1).getMomentaryCessationCurve().getPoints(),
				is(equalTo(MOM_CESS_POINTS)));
		assertThat("Active set unchanged", sets.get(0).getMustTripCurve().getPoints(),
				is(equalTo(MUST_TRIP_POINTS)));
	}

	@Test
	public void clearMayTripCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerCurve curve = discoverModel(conn).getCurveSets().get(1).getMayTripCurve();

		// WHEN
		curve.setPoints(conn, List.of());

		// THEN
		assertThat("Only the active point count written", conn.getWrites(),
				is(equalTo(List.of(List.of(SET_2_MAY_TRIP_ADDRESS, 1)))));
		DerCurve device = discoverModel(conn).getCurveSets().get(1).getMayTripCurve();
		assertThat("Active point count", device.getActivePointCount(), is(equalTo(0)));
		assertThat("Points", device.getPoints(), is(equalTo(Collections.emptyList())));
	}

	@Test
	public void setActivePointCount() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerCurve curve = discoverModel(conn).getCurveSets().get(1).getMomentaryCessationCurve();

		// WHEN
		curve.setActivePointCount(conn, 1);

		// THEN
		assertThat("Points",
				discoverModel(conn).getCurveSets().get(1).getMomentaryCessationCurve().getPoints(),
				is(equalTo(MOM_CESS_POINTS.subList(0, 1))));
	}

	@Test
	public void invalidPointCount() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerCurve curve = discoverModel(conn).getCurveSets().get(1).getMustTripCurve();
		DerCurvePoint p = new DerCurvePoint(50.0f, 1.0f);

		// THEN
		try {
			curve.setPoints(conn, List.of(p, p, p, p, p, p, p, p));
			fail("More points than the curve point count should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}
		for ( int count : new int[] { -1, 8 } ) {
			try {
				curve.setActivePointCount(conn, count);
				fail("Active point count " + count + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void readOnlyCurveSet() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerTripCurveSet set = discoverModel(conn).getCurveSets().get(0);

		// THEN
		try {
			set.getMustTripCurve().setPoints(conn, NEW_MUST_TRIP_POINTS);
			fail("Writing points to the read-only curve set should be rejected.");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
		try {
			set.getMayTripCurve().setActivePointCount(conn, 0);
			fail("Writing the active point count to the read-only curve set should be rejected.");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void adoptCurveSet() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerTripLowVoltageModelAccessor model = discoverModel(conn);

		// WHEN
		for ( int index : new int[] { 1, 3 } ) {
			try {
				model.adoptCurveSet(conn, index);
				fail("Curve set index " + index + " should be rejected.");
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
		model.adoptCurveSet(conn, 2);
		model.setEnabled(conn, false);

		// THEN
		assertThat("Writes", conn.getWrites(),
				is(equalTo(List.of(List.of(BLOCK_ADDRESS + 1, 1), List.of(BLOCK_ADDRESS, 1)))));
		DerTripLowVoltageModelAccessor device = discoverModel(conn);
		assertThat("Adopt curve request", device.getAdoptCurveRequest(), is(equalTo(2)));
		assertThat("Enabled", device.isEnabled(), is(equalTo(false)));
	}

}
