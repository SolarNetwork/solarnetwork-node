/* ==================================================================
 * DerTripHighVoltageModelAccessorImpl_708_01Tests.java - 5/10/2026 6:48:10 pm
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
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.der.DerCurvePoint;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerTripCurveSet;
import net.solarnetwork.node.hw.sunspec.der.DerTripHighVoltageModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerTripHighVoltageModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerTripLowVoltageModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerTripModelAccessor;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link DerTripHighVoltageModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerTripHighVoltageModelAccessorImpl_708_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerTripHighVoltageModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerTripHighVoltageModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerTripHighVoltageModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerTripHighVoltageModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(724)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(726)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.TripHighVoltage)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(7)));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				is(equalTo(49)));
		assertThat("Model length", model.getModelLength(), is(equalTo(105)));
	}

	@Test
	public void activeCurveSet() {
		List<DerTripCurveSet> sets = getTestModel().getCurveSets();
		assertThat("Curve sets", sets, hasSize(2));
		DerTripCurveSet set = sets.get(0);
		assertThat("Read-only", set.isReadOnly(), is(equalTo(true)));
		assertThat("Must trip points", set.getMustTripCurve().getPoints(),
				is(equalTo(List.of(new DerCurvePoint(0.0f, 0.16f), new DerCurvePoint(120.0f, 0.16f),
						new DerCurvePoint(120.0f, 2.0f), new DerCurvePoint(110.0f, 2.0f),
						new DerCurvePoint(110.0f, 13.0f)))));
		assertThat("May trip active point count", set.getMayTripCurve().getActivePointCount(),
				is(equalTo(0)));
		assertThat("May trip points", set.getMayTripCurve().getPoints(),
				is(equalTo(Collections.emptyList())));
		assertThat("Momentary cessation active point count",
				set.getMomentaryCessationCurve().getActivePointCount(), is(equalTo(2)));
		assertThat("Momentary cessation points", set.getMomentaryCessationCurve().getPoints(),
				is(equalTo(List.of(new DerCurvePoint(0.0f, 0.0f), new DerCurvePoint(120.0f, 0.08f)))));
		assertThat("Stored set read-only", sets.get(1).isReadOnly(), is(equalTo(false)));
	}

	@Test
	public void findTypedModel_distinctFromLowVoltage() {
		ModelData data = ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA);
		assertThat("High voltage model found by its own type",
				data.findTypedModel(DerTripHighVoltageModelAccessor.class).getModelId(),
				is(equalTo(DerModelId.TripHighVoltage)));
		assertThat("Low voltage model found by its own type",
				data.findTypedModel(DerTripLowVoltageModelAccessor.class).getModelId(),
				is(equalTo(DerModelId.TripLowVoltage)));
		assertThat("Shared type finds the first trip model",
				data.findTypedModel(DerTripModelAccessor.class).getModelId(),
				is(equalTo(DerModelId.TripLowVoltage)));
	}

}
