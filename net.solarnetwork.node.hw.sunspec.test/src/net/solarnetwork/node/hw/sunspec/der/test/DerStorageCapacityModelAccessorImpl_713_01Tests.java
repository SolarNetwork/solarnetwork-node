/* ==================================================================
 * DerStorageCapacityModelAccessorImpl_713_01Tests.java - 5/10/2026 10:42:15 am
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
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerStorageCapacityModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerStorageCapacityModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerStorageStatus;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link DerStorageCapacityModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerStorageCapacityModelAccessorImpl_713_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerStorageCapacityModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerStorageCapacityModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerStorageCapacityModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerStorageCapacityModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(1211)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(1213)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.StorageCapacity)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(7)));
		assertThat("Model length", model.getModelLength(), is(equalTo(7)));
	}

	@Test
	public void values() {
		DerStorageCapacityModelAccessor model = getTestModel();
		assertThat("Energy rating not implemented", model.getEnergyRating(), is(nullValue()));
		assertThat("Energy available not implemented", model.getEnergyAvailable(), is(nullValue()));
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(100.0f)));
		assertThat("State of health not implemented", model.getStateOfHealth(), is(nullValue()));
		assertThat("Status not implemented", model.getStorageStatus(), is(nullValue()));
	}

	@Test
	public void syntheticValues() {
		// GIVEN
		// replace the whole block: WHRtg, WHAvail, SoC, SoH, Sta, WH_SF, Pct_SF
		DerStorageCapacityModelAccessor model = ModelDataUtils
				.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, 1213, 1350, 1215, 900, 1000, 1,
						1, 0xFFFF)
				.findTypedModel(DerStorageCapacityModelAccessor.class);

		// THEN
		assertThat("Energy rating", model.getEnergyRating(), is(equalTo(13500L)));
		assertThat("Energy available", model.getEnergyAvailable(), is(equalTo(12150L)));
		assertThat("State of charge", model.getStateOfCharge(), is(equalTo(90.0f)));
		assertThat("State of health", model.getStateOfHealth(), is(equalTo(100.0f)));
		assertThat("Status", model.getStorageStatus(), is(equalTo(DerStorageStatus.Warning)));
	}

}
