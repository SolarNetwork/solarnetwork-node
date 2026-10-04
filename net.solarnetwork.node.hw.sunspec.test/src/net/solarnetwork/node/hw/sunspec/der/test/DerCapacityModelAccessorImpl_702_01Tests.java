/* ==================================================================
 * DerCapacityModelAccessorImpl_702_01Tests.java - 5/10/2026 9:32:29 am
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
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerAbnormalOperatingCategory;
import net.solarnetwork.node.hw.sunspec.der.DerCapacityModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerCapacityModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerControlMode;
import net.solarnetwork.node.hw.sunspec.der.DerIntentionalIslandCategory;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerNormalOperatingCategory;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;

/**
 * Test cases for the {@link DerCapacityModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerCapacityModelAccessorImpl_702_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	private DerCapacityModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerCapacityModelAccessor.class);
	}

	private static DerCapacityModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn).findTypedModel(DerCapacityModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerCapacityModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerCapacityModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(332)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(334)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.Capacity)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(50)));
		assertThat("Model length", model.getModelLength(), is(equalTo(50)));
	}

	@Test
	public void powerRatings() {
		DerCapacityModelAccessor model = getTestModel();
		assertThat("Active power", model.getActivePowerMaximumRating(), is(equalTo(7680)));
		assertThat("Active power over-excited", model.getActivePowerOverExcitedRating(),
				is(equalTo(3072)));
		assertThat("Over-excited power factor", model.getOverExcitedPowerFactorRating(),
				is(equalTo(0.4f)));
		assertThat("Active power under-excited", model.getActivePowerUnderExcitedRating(),
				is(equalTo(3072)));
		assertThat("Under-excited power factor", model.getUnderExcitedPowerFactorRating(),
				is(equalTo(0.4f)));
		assertThat("Apparent power", model.getApparentPowerMaximumRating(), is(equalTo(7680)));
		assertThat("Reactive power injected", model.getReactivePowerInjectedMaximumRating(),
				is(equalTo(4070)));
		assertThat("Reactive power absorbed", model.getReactivePowerAbsorbedMaximumRating(),
				is(equalTo(4070)));
		assertThat("Active power charge rate", model.getActivePowerChargeRateMaximumRating(),
				is(equalTo(7680)));
		assertThat("Active power discharge rate", model.getActivePowerDischargeRateMaximumRating(),
				is(equalTo(7680)));
		assertThat("Apparent power charge rate", model.getApparentPowerChargeRateMaximumRating(),
				is(equalTo(7680)));
		assertThat("Apparent power discharge rate", model.getApparentPowerDischargeRateMaximumRating(),
				is(equalTo(7680)));
	}

	@Test
	public void otherRatings() {
		DerCapacityModelAccessor model = getTestModel();
		assertThat("Voltage nominal", model.getVoltageNominalRating(), is(equalTo(240.0f)));
		assertThat("Voltage maximum", model.getVoltageMaximumRating(), is(equalTo(264.0f)));
		assertThat("Voltage minimum", model.getVoltageMinimumRating(), is(equalTo(211.0f)));
		assertThat("Current maximum", model.getCurrentMaximumRating(), is(equalTo(32.0f)));
		assertThat("Reactive susceptance", model.getReactiveSusceptanceRating(), is(equalTo(0.0f)));
		assertThat("Normal operating category", model.getNormalOperatingCategory(),
				is(equalTo(DerNormalOperatingCategory.CategoryB)));
		assertThat("Abnormal operating category", model.getAbnormalOperatingCategory(),
				is(equalTo(DerAbnormalOperatingCategory.CategoryIII)));
		assertThat("Supported control modes", model.getSupportedControlModes(),
				is(equalTo(Set.of(DerControlMode.MaxActivePower, DerControlMode.FixedActivePower,
						DerControlMode.FixedReactivePower, DerControlMode.FixedPowerFactor,
						DerControlMode.VoltVar, DerControlMode.FrequencyWatt,
						DerControlMode.LowVoltageTrip, DerControlMode.HighVoltageTrip,
						DerControlMode.WattVar, DerControlMode.VoltWatt, DerControlMode.LowFrequencyTrip,
						DerControlMode.HighFrequencyTrip))));
		assertThat("Intentional island categories", model.getIntentionalIslandCategoriesRating(),
				is(equalTo(Set.of(DerIntentionalIslandCategory.IntentionalIslandCapable,
						DerIntentionalIslandCategory.BlackStartCapable,
						DerIntentionalIslandCategory.IsochronousCapable))));
	}

	@Test
	public void settings() {
		DerCapacityModelAccessor model = getTestModel();
		assertThat("Active power", model.getActivePowerMaximum(), is(nullValue()));
		assertThat("Over-excited power factor", model.getOverExcitedPowerFactor(), is(nullValue()));
		assertThat("Apparent power", model.getApparentPowerMaximum(), is(nullValue()));
		assertThat("Voltage nominal", model.getVoltageNominal(), is(nullValue()));
		assertThat("Current maximum", model.getCurrentMaximum(), is(nullValue()));
		assertThat("Intentional island categories", model.getIntentionalIslandCategories(),
				is(equalTo(Set.of())));
	}

	@Test
	public void writeSettings() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerCapacityModelAccessor model = discoverModel(conn);

		// WHEN
		model.setActivePowerMaximum(conn, 7000);
		model.setActivePowerOverExcited(conn, 3000);
		model.setOverExcitedPowerFactor(conn, 0.9f);
		model.setActivePowerUnderExcited(conn, 2900);
		model.setUnderExcitedPowerFactor(conn, 0.85f);
		model.setApparentPowerMaximum(conn, 7500);
		model.setReactivePowerInjectedMaximum(conn, 4000);
		model.setReactivePowerAbsorbedMaximum(conn, 3900);
		model.setActivePowerChargeRateMaximum(conn, 6000);
		model.setActivePowerDischargeRateMaximum(conn, 6500);
		model.setApparentPowerChargeRateMaximum(conn, 6100);
		model.setApparentPowerDischargeRateMaximum(conn, 6600);
		model.setVoltageNominal(conn, 240f);
		model.setVoltageMaximum(conn, 260f);
		model.setVoltageMinimum(conn, 210f);
		model.setCurrentMaximum(conn, 30f);
		model.setIntentionalIslandCategories(conn,
				Set.of(DerIntentionalIslandCategory.IntentionalIslandCapable,
						DerIntentionalIslandCategory.BlackStartCapable));

		// THEN
		assertThat("Model data updated", model.getActivePowerMaximum(), is(equalTo(7000)));

		DerCapacityModelAccessor device = discoverModel(conn);
		assertThat("Active power", device.getActivePowerMaximum(), is(equalTo(7000)));
		assertThat("Active power over-excited", device.getActivePowerOverExcited(), is(equalTo(3000)));
		assertThat("Over-excited power factor", device.getOverExcitedPowerFactor(), is(equalTo(0.9f)));
		assertThat("Active power under-excited", device.getActivePowerUnderExcited(), is(equalTo(2900)));
		assertThat("Under-excited power factor", device.getUnderExcitedPowerFactor(),
				is(equalTo(0.85f)));
		assertThat("Apparent power", device.getApparentPowerMaximum(), is(equalTo(7500)));
		assertThat("Reactive power injected", device.getReactivePowerInjectedMaximum(),
				is(equalTo(4000)));
		assertThat("Reactive power absorbed", device.getReactivePowerAbsorbedMaximum(),
				is(equalTo(3900)));
		assertThat("Active power charge rate", device.getActivePowerChargeRateMaximum(),
				is(equalTo(6000)));
		assertThat("Active power discharge rate", device.getActivePowerDischargeRateMaximum(),
				is(equalTo(6500)));
		assertThat("Apparent power charge rate", device.getApparentPowerChargeRateMaximum(),
				is(equalTo(6100)));
		assertThat("Apparent power discharge rate", device.getApparentPowerDischargeRateMaximum(),
				is(equalTo(6600)));
		assertThat("Voltage nominal", device.getVoltageNominal(), is(equalTo(240.0f)));
		assertThat("Voltage maximum", device.getVoltageMaximum(), is(equalTo(260.0f)));
		assertThat("Voltage minimum", device.getVoltageMinimum(), is(equalTo(210.0f)));
		assertThat("Current maximum", device.getCurrentMaximum(), is(equalTo(30.0f)));
		assertThat("Intentional island categories", device.getIntentionalIslandCategories(),
				is(equalTo(Set.of(DerIntentionalIslandCategory.IntentionalIslandCapable,
						DerIntentionalIslandCategory.BlackStartCapable))));
	}

	@Test
	public void writeSetting_outOfRange() throws IOException {
		// GIVEN
		StaticDataMapModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		DerCapacityModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setActivePowerMaximum(conn, 70000);
			fail("Value larger than uint16 should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Device not updated", discoverModel(conn).getActivePowerMaximum(), is(nullValue()));
	}

}
