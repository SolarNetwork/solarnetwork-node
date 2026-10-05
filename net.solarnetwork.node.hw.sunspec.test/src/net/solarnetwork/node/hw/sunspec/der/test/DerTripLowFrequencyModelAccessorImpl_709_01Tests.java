/* ==================================================================
 * DerTripLowFrequencyModelAccessorImpl_709_01Tests.java - 5/10/2026 6:48:10 pm
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
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerTripCurveSet;
import net.solarnetwork.node.hw.sunspec.der.DerTripLowFrequencyModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerTripLowFrequencyModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerCurve;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;

/**
 * Test cases for the {@link DerTripLowFrequencyModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerTripLowFrequencyModelAccessorImpl_709_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerTripLowFrequencyModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerTripLowFrequencyModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerTripLowFrequencyModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerTripLowFrequencyModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(831)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(833)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.TripLowFrequency)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(7)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(64)));
		assertThat("Model length", model.getModelLength(), is(equalTo(135)));
	}

	@Test
	public void activeCurveSet() {
		List<DerTripCurveSet> sets = getTestModel().getCurveSets();
		assertThat("Curve sets", sets, hasSize(2));
		DerTripCurveSet set = sets.get(0);
		assertThat("Read-only", set.isReadOnly(), is(equalTo(true)));
		assertThat("Must trip points", set.getMustTripCurve().getPoints(),
				is(equalTo(List.of(new DerCurvePoint(0.0f, 0.16f), new DerCurvePoint(56.5f, 0.16f),
						new DerCurvePoint(56.5f, 300.0f), new DerCurvePoint(58.5f, 300.0f),
						new DerCurvePoint(58.5f, 0.0f)))));
		assertThat("May trip active point count", set.getMayTripCurve().getActivePointCount(),
				is(equalTo(0)));
		assertThat("May trip points", set.getMayTripCurve().getPoints(),
				is(equalTo(Collections.emptyList())));
		assertThat("Momentary cessation active point count",
				set.getMomentaryCessationCurve().getActivePointCount(), is(equalTo(0)));
		assertThat("Momentary cessation points", set.getMomentaryCessationCurve().getPoints(),
				is(equalTo(Collections.emptyList())));
		assertThat("Stored set read-only", sets.get(1).isReadOnly(), is(equalTo(false)));
		assertThat("Stored set must trip points", sets.get(1).getMustTripCurve().getPoints(),
				is(equalTo(Collections.emptyList())));
	}

	@Test
	public void writeMustTripCurve() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerCurve curve = ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerTripLowFrequencyModelAccessor.class).getCurveSets().get(1)
				.getMustTripCurve();
		List<DerCurvePoint> points = List.of(new DerCurvePoint(0.0f, 0.16f),
				new DerCurvePoint(57.0f, 0.16f), new DerCurvePoint(57.0f, 299.0f),
				new DerCurvePoint(58.5f, 299.0f));

		// WHEN
		curve.setPoints(conn, points);

		// THEN
		// set 2 starts after the 7 register fixed block and 64 register set 1, and
		// each frequency point has a 2 register frequency and a 2 register time
		int mustTripAddress = 833 + 7 + 64 + 1;
		assertThat("Points written in one request, then the active point count", conn.getWrites(),
				is(equalTo(List.of(List.of(mustTripAddress + 1, 16), List.of(mustTripAddress, 1)))));
		assertThat("Points",
				ModelDataUtils.getModelDataInstance(conn)
						.findTypedModel(DerTripLowFrequencyModelAccessor.class).getCurveSets().get(1)
						.getMustTripCurve().getPoints(),
				is(equalTo(points)));
	}

}
