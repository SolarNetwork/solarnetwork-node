/* ==================================================================
 * FloatingPointMeterModelAccessor_213_01Tests.java - 6/10/2026 4:58:30 pm
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

package net.solarnetwork.node.hw.sunspec.meter.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.meter.FloatingPointMeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelEvent;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link FloatingPointMeterModelAccessor} class.
 *
 * @author matt
 * @version 1.0
 */
public class FloatingPointMeterModelAccessor_213_01Tests {

	/** Synthetic data, as there is no capture from a device. */
	private static final String TEST_DATA = "test-data-213-01.txt";

	/** The model block address in the test data. */
	private static final int BLOCK_ADDRESS = 72;

	private MeterModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(MeterModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(FloatingPointMeterModelAccessor.class)));
	}

	@Test
	public void block() {
		MeterModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(70)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(MeterModelId.WyeConnectThreePhaseMeterFloat)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(124)));
		assertThat("Model length", model.getModelLength(), is(equalTo(124)));
	}

	@Test
	public void values() {
		MeterModelAccessor model = getTestModel();
		assertThat("Frequency", model.getFrequency(), is(equalTo(50.01f)));
		assertThat("Current", model.getCurrent(), is(equalTo(30.5f)));
		assertThat("Neutral current not in SunSpec", model.getNeutralCurrent(), is(nullValue()));
		assertThat("Voltage", model.getVoltage(), is(equalTo(230.4f)));
		assertThat("Line voltage", model.getLineVoltage(), is(equalTo(399.0f)));
		assertThat("Power factor", model.getPowerFactor(), is(equalTo(0.99f)));
		assertThat("Active power rounded", model.getActivePower(), is(equalTo(6951)));
		assertThat("Apparent power", model.getApparentPower(), is(equalTo(7020)));
		assertThat("Reactive power", model.getReactivePower(), is(equalTo(-980)));
		assertThat("Active energy exported", model.getActiveEnergyExported(), is(equalTo(1234567L)));
		assertThat("Active energy imported", model.getActiveEnergyImported(), is(equalTo(7654321L)));
		assertThat("Apparent energy exported", model.getApparentEnergyExported(), is(equalTo(1300000L)));
		assertThat("Apparent energy imported", model.getApparentEnergyImported(), is(equalTo(7700000L)));
		assertThat("Reactive energy imported from Q1 and Q2", model.getReactiveEnergyImported(),
				is(equalTo(1200L)));
		assertThat("Reactive energy exported from Q3 and Q4", model.getReactiveEnergyExported(),
				is(equalTo(700L)));
		assertThat("Active energy delivered is imported", model.getActiveEnergyDelivered(),
				is(equalTo(7654321L)));
		assertThat("Active energy received is exported", model.getActiveEnergyReceived(),
				is(equalTo(1234567L)));
		assertThat("Events", model.getEvents(), is(equalTo(
				Set.<ModelEvent> of(MeterModelEvent.PowerFailure, MeterModelEvent.LowPowerFactor))));
	}

	@Test
	public void phaseA() {
		MeterModelAccessor model = getTestModel().accessorForPhase(PhaseA);
		assertThat("Frequency", model.getFrequency(), is(equalTo(50.01f)));
		assertThat("Current", model.getCurrent(), is(equalTo(10.1f)));
		assertThat("Voltage", model.getVoltage(), is(equalTo(230.1f)));
		assertThat("Line voltage A-B", model.getLineVoltage(), is(equalTo(398.6f)));
		assertThat("Power factor", model.getPowerFactor(), is(equalTo(0.985f)));
		assertThat("Active power", model.getActivePower(), is(equalTo(2300)));
		assertThat("Apparent power", model.getApparentPower(), is(equalTo(2335)));
		assertThat("Reactive power", model.getReactivePower(), is(equalTo(-330)));
		assertThat("Active energy exported", model.getActiveEnergyExported(), is(equalTo(411522L)));
		assertThat("Active energy imported", model.getActiveEnergyImported(), is(equalTo(2551440L)));
		assertThat("Apparent energy exported", model.getApparentEnergyExported(), is(equalTo(433333L)));
		assertThat("Apparent energy imported", model.getApparentEnergyImported(), is(equalTo(2566667L)));
		assertThat("Reactive energy imported", model.getReactiveEnergyImported(), is(equalTo(399L)));
		assertThat("Reactive energy exported without Q4", model.getReactiveEnergyExported(),
				is(equalTo(100L)));
	}

	@Test
	public void phaseB() {
		MeterModelAccessor model = getTestModel().accessorForPhase(PhaseB);
		assertThat("Current", model.getCurrent(), is(equalTo(10.2f)));
		assertThat("Voltage", model.getVoltage(), is(equalTo(230.5f)));
		assertThat("Line voltage B-C", model.getLineVoltage(), is(equalTo(399.2f)));
		assertThat("Power factor", model.getPowerFactor(), is(equalTo(0.993f)));
		assertThat("Active power rounded", model.getActivePower(), is(equalTo(2326)));
		assertThat("Apparent power rounded", model.getApparentPower(), is(equalTo(2343)));
		assertThat("Reactive power rounded", model.getReactivePower(), is(equalTo(-326)));
		assertThat("Active energy exported", model.getActiveEnergyExported(), is(equalTo(411523L)));
		assertThat("Active energy imported", model.getActiveEnergyImported(), is(equalTo(2551441L)));
		assertThat("Reactive energy imported", model.getReactiveEnergyImported(), is(equalTo(401L)));
		assertThat("Reactive energy exported", model.getReactiveEnergyExported(), is(equalTo(233L)));
	}

	@Test
	public void phaseC() {
		MeterModelAccessor model = getTestModel().accessorForPhase(PhaseC);
		assertThat("Current", model.getCurrent(), is(equalTo(10.2f)));
		assertThat("Voltage", model.getVoltage(), is(equalTo(230.6f)));
		assertThat("Line voltage C-A", model.getLineVoltage(), is(equalTo(399.3f)));
		assertThat("Power factor not implemented", model.getPowerFactor(), is(nullValue()));
		assertThat("Active power", model.getActivePower(), is(equalTo(2325)));
		assertThat("Reactive power", model.getReactivePower(), is(equalTo(-324)));
		assertThat("Reactive energy exported", model.getReactiveEnergyExported(), is(equalTo(234L)));
	}

	@Test
	public void infiniteValue() {
		// GIVEN
		// W is +Infinity
		MeterModelAccessor model = ModelDataUtils.getModelDataInstanceWithRegisters(getClass(),
				TEST_DATA, BLOCK_ADDRESS + 26, 0x7F80, 0x0000).findTypedModel(MeterModelAccessor.class);

		// THEN
		assertThat("Infinite value not available", model.getActivePower(), is(nullValue()));
	}

}
