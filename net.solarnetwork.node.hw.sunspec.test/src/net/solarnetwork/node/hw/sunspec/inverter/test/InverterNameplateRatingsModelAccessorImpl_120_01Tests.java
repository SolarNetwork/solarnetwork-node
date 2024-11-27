/* ==================================================================
 * InverterNameplateRatingsModelAccessorImpl_120_01Tests.java - 8/10/2018 7:06:15 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterDerType;
import net.solarnetwork.node.hw.sunspec.inverter.InverterNameplateRatingsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterNameplateRatingsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link InverterNameplateRatingsModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.1
 */
public class InverterNameplateRatingsModelAccessorImpl_120_01Tests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-101-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InverterNameplateRatingsModelAccessor meterAccessor = data
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat(meterAccessor, instanceOf(InverterNameplateRatingsModelAccessorImpl.class));
	}

	@Test
	public void block() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(121));
		assertThat("Model block address", model.getBlockAddress(), equalTo(123));
		assertThat("Model ID", model.getModelId(), equalTo(InverterControlModelId.NameplateRatings));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(26));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(26));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void derType() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("DER type", model.getDerType(), equalTo(InverterDerType.PV));
	}

	@Test
	public void activePowerRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Active power rating", model.getActivePowerRating(), equalTo(11400));
	}

	@Test
	public void apparentPowerRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Apparent power rating", model.getApparentPowerRating(), equalTo(11400));
	}

	@Test
	public void reactivePowerQ1Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Reactive power Q1 rating", model.getReactivePowerQ1Rating(), equalTo(6000));
	}

	@Test
	public void reactivePowerQ2Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Reactive power Q2 rating", model.getReactivePowerQ2Rating(), nullValue());
	}

	@Test
	public void reactivePowerQ3Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Reactive power Q3 rating", model.getReactivePowerQ3Rating(), nullValue());
	}

	@Test
	public void reactivePowerQ4Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Reactive power Q4 rating", model.getReactivePowerQ4Rating(), equalTo(-6000));
	}

	@Test
	public void currentRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Current rating", model.getCurrentRating(), equalTo(47.50f));
	}

	@Test
	public void powerFactorQ1Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Power factor Q1 rating", model.getPowerFactorQ1Rating(), equalTo(-0.850f));
	}

	@Test
	public void powerFactorQ2Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Power factor Q1 rating", model.getPowerFactorQ2Rating(), nullValue());
	}

	@Test
	public void powerFactorQ3Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Power factor Q3 rating", model.getPowerFactorQ3Rating(), nullValue());
	}

	@Test
	public void powerFactorQ4Rating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Power factor Q4 rating", model.getPowerFactorQ4Rating(), equalTo(0.850f));
	}

	@Test
	public void storedEnergyRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Stored energy rating", model.getStoredEnergyRating(), nullValue());
	}

	@Test
	public void storedChargeCapcity() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Stored charge capcity", model.getStoredChargeCapacity(), nullValue());
	}

	@Test
	public void storedEnergyImportPowerRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Stored energy import power rating", model.getStoredEnergyImportPowerRating(),
				nullValue());
	}

	@Test
	public void storedEnergyExportPowerRating() {
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		assertThat("Stored energy export power rating", model.getStoredEnergyExportPowerRating(),
				nullValue());
	}

	@Test
	public void infoMap() {
		// GIVEN
		InverterNameplateRatingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);

		// WHEN
		Map<String, Object> result = model.nameplateRatingsInfo();

		// THEN
		// @formatter:off
		assertThat("Info map keys populated", result.keySet(), contains(
				InverterNameplateRatingsModelAccessor.INFO_KEY_DER_TYPE,
				InverterNameplateRatingsModelAccessor.INFO_KEY_DER_TYPE_CODE,
				InverterNameplateRatingsModelAccessor.INFO_KEY_ACTIVE_POWER_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_APPARENT_POWER_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_REACTIVE_POWER_Q1_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_REACTIVE_POWER_Q4_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_CURRENT_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_POWER_FACTOR_Q1_RATING,
				InverterNameplateRatingsModelAccessor.INFO_KEY_POWER_FACTOR_Q4_RATING
		));
		assertThat("Info map values populated", result.values(), contains(
				InverterDerType.PV.toString(),
				InverterDerType.PV.getCode(),
				11400,
				11400,
				6000,
				-6000,
				47.50f,
				-0.850f,
				0.850f
		));
		// @formatter:on

	}
}
