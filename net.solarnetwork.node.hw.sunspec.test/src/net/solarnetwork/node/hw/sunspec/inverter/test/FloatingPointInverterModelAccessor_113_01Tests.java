/* ==================================================================
 * FloatingPointInverterModelAccessor_113_01Tests.java - 11/10/2019 6:01:49 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.inverter.FloatingPointInverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterBasicSettingsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link InverterBasicSettingsModelAccessor} class on a
 * {@link FloatingPointInverterModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 */
public class FloatingPointInverterModelAccessor_113_01Tests {

	private static final Logger log = LoggerFactory
			.getLogger(FloatingPointInverterModelAccessorTests.class);

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-113-01.txt");
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void block() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(159));
		assertThat("Model block address", model.getBlockAddress(), equalTo(161));
		assertThat("Model ID", model.getModelId(), equalTo(InverterControlModelId.BasicSettings));
		assertThat("Model fixed length", model.getFixedBlockLength(), equalTo(30));
		assertThat("Model repeating instance length", model.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", model.getModelLength(), equalTo(30));
		assertThat("Model length", model.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void activePowerMaximum() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Active power max", model.getActivePowerMaximum(), equalTo(3000));
	}

	@Test
	public void pccVoltage() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("PCC voltage", model.getPccVoltage(), equalTo(240.0f));
	}

	@Test
	public void pccVoltageOffset() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("PCC voltage offset", model.getPccVoltageOffset(), equalTo(0.0f));
	}

	@Test
	public void voltageMax() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Voltage max", model.getVoltageMaximum(), nullValue());
	}

	@Test
	public void voltageMin() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Voltage min", model.getVoltageMinimum(), nullValue());
	}

	@Test
	public void apparentPowerMax() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VA max", model.getApparentPowerMaximum(), equalTo(3000));
	}

	@Test
	public void reactivePowerQ1Max() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VAR Q1 max", model.getReactivePowerQ1Maximum(), equalTo(2140));
	}

	@Test
	public void reactivePowerQ2Max() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VAR Q2 max", model.getReactivePowerQ2Maximum(), nullValue());
	}

	@Test
	public void reactivePowerQ3Max() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VAR Q3 max", model.getReactivePowerQ3Maximum(), nullValue());
	}

	@Test
	public void reactivePowerQ4Max() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VAR Q4 max", model.getReactivePowerQ4Maximum(), equalTo(-2140));
	}

	@Test
	public void activePowerRampRate() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Active power ramp rate", model.getActivePowerRampRate(), nullValue());
	}

	@Test
	public void powerFactorQ1Minimum() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Power factor Q1 minimum", model.getPowerFactorQ1Minimum(), equalTo(-0.850f));
	}

	@Test
	public void powerFactorQ2Minimum() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Power factor Q2 minimum", model.getPowerFactorQ2Minimum(), nullValue());
	}

	@Test
	public void powerFactorQ3Minimum() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Power factor Q3 minimum", model.getPowerFactorQ3Minimum(), nullValue());
	}

	@Test
	public void powerFactorQ4Minimum() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Power factor Q4 minimum", model.getPowerFactorQ4Minimum(), equalTo(0.850f));
	}

	@Test
	public void importExportChangeReactivePowerAction() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Import export reactive power action",
				model.getImportExportChangeReactivePowerAction(), nullValue());
	}

	@Test
	public void apparentPowerCalculationMethod() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Apparent power calculation method", model.getApparentPowerCalculationMethod(),
				nullValue());
	}

	@Test
	public void ecpFrequency() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Frequency min", model.getEcpFrequency(), nullValue());
	}

	@Test
	public void connectedPhase() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Connected phase", model.getConnectedPhase(), nullValue());
	}

}
