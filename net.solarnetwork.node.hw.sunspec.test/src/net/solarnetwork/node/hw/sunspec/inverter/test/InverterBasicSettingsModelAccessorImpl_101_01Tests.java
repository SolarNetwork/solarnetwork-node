/* ==================================================================
 * IntegerInverterModelAccessor_103_02Tests.java - 8/10/2018 7:06:15 AM
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.inverter.InverterApparentPowerCalculationMethod;
import net.solarnetwork.node.hw.sunspec.inverter.InverterBasicSettingsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterBasicSettingsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterReactivePowerAction;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link InverterBasicSettingsModelAccessor} class.
 * 
 * @author matt
 * @version 1.1
 */
public class InverterBasicSettingsModelAccessorImpl_101_01Tests {

	private static final Logger log = LoggerFactory.getLogger(IntegerMeterModelAccessorTests.class);

	private static final String TEST_DATA = "test-data-101-01.txt";

	/** The model block address in the test data. */
	private static final int BLOCK_ADDRESS = 151;

	/** Synthetic values for the whole model block. */
	// @formatter:off
	private static final int[] SYNTHETIC_BLOCK = new int[] {
			0x0474, // WMax
			0x0960, // VRef
			0xFFE7, // VRefOfs
			0x0A50, // VMax
			0x0840, // VMin
			0x0474, // VAMax
			0x0258, // VArMaxQ1
			0x8000, // VArMaxQ2
			0xFED4, // VArMaxQ3
			0xFDA8, // VArMaxQ4
			0x03E8, // WGra
			0xFCAE, // PFMinQ1
			0x8000, // PFMinQ2
			0xFC7C, // PFMinQ3
			0x0352, // PFMinQ4
			0x0002, // VArAct
			0x0001, // ClcTotVA
			0x01F4, // MaxRmpRte
			0x1770, // ECPNomHz
			0x0002, // ConnPh
			0x0001, // WMax_SF
			0xFFFF, // VRef_SF
			0xFFFF, // VRefOfs_SF
			0xFFFF, // VMinMax_SF
			0x0001, // VAMax_SF
			0x0001, // VArMax_SF
			0xFFFE, // WGra_SF
			0xFFFD, // PFMin_SF
			0xFFFF, // MaxRmpRte_SF
			0xFFFE, // ECPNomHz_SF
	};
	// @formatter:on

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA);
	}

	private InverterBasicSettingsModelAccessor getTestModel(int address, int... words) {
		return ModelDataUtils.getModelDataInstanceWithRegisters(getClass(), TEST_DATA, address, words)
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
	}

	private static InverterBasicSettingsModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Fronius"));
		assertThat("Model name", data.getModelName(), equalTo("IG+V11.4"));
		assertThat("Options", data.getOptions(), equalTo("2.1.18"));
		assertThat("Version", data.getVersion(), equalTo("5.10.0"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("50.213262"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(5));
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		InverterBasicSettingsModelAccessor accessor = data
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat(accessor, instanceOf(InverterBasicSettingsModelAccessorImpl.class));
	}

	@Test
	public void block() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Model base address", model.getBaseAddress(), equalTo(149));
		assertThat("Model block address", model.getBlockAddress(), equalTo(151));
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
		assertThat("Active power max", model.getActivePowerMaximum(), equalTo(11400));
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
		assertThat("Voltage max", model.getVoltageMaximum(), equalTo(269.0f));
	}

	@Test
	public void voltageMin() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Voltage min", model.getVoltageMinimum(), equalTo(206.0f));
	}

	@Test
	public void apparentPowerMax() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VA max", model.getApparentPowerMaximum(), equalTo(11400));
	}

	@Test
	public void reactivePowerQ1Max() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("VAR Q1 max", model.getReactivePowerQ1Maximum(), equalTo(6000));
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
		assertThat("VAR Q4 max", model.getReactivePowerQ4Maximum(), equalTo(-6000));
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
		assertThat("ECP frequency", model.getEcpFrequency(), nullValue());
	}

	@Test
	public void connectedPhase() {
		InverterBasicSettingsModelAccessor model = getTestDataInstance()
				.findTypedModel(InverterBasicSettingsModelAccessor.class);
		assertThat("Connected phase", model.getConnectedPhase(), nullValue());
	}

	@Test
	public void syntheticValues() {
		// GIVEN
		InverterBasicSettingsModelAccessor model = getTestModel(BLOCK_ADDRESS, SYNTHETIC_BLOCK);

		// THEN
		assertThat("Active power max", model.getActivePowerMaximum(), is(equalTo(11400)));
		assertThat("PCC voltage", model.getPccVoltage(), is(equalTo(240.0f)));
		assertThat("Negative PCC voltage offset", model.getPccVoltageOffset(), is(equalTo(-2.5f)));
		assertThat("Voltage max", model.getVoltageMaximum(), is(equalTo(264.0f)));
		assertThat("Voltage min", model.getVoltageMinimum(), is(equalTo(211.2f)));
		assertThat("VA max", model.getApparentPowerMaximum(), is(equalTo(11400)));
		assertThat("VAR Q1 max", model.getReactivePowerQ1Maximum(), is(equalTo(6000)));
		assertThat("VAR Q2 max not implemented", model.getReactivePowerQ2Maximum(), is(nullValue()));
		assertThat("VAR Q3 max", model.getReactivePowerQ3Maximum(), is(equalTo(-3000)));
		assertThat("VAR Q4 max", model.getReactivePowerQ4Maximum(), is(equalTo(-6000)));
		assertThat("Active power ramp rate", model.getActivePowerRampRate(), is(equalTo(10.0f)));
		assertThat("Power factor Q1 minimum", model.getPowerFactorQ1Minimum(), is(equalTo(-0.85f)));
		assertThat("Power factor Q2 minimum not implemented", model.getPowerFactorQ2Minimum(),
				is(nullValue()));
		assertThat("Power factor Q3 minimum", model.getPowerFactorQ3Minimum(), is(equalTo(-0.9f)));
		assertThat("Power factor Q4 minimum", model.getPowerFactorQ4Minimum(), is(equalTo(0.85f)));
		assertThat("Import export reactive power action",
				model.getImportExportChangeReactivePowerAction(),
				is(equalTo(InverterReactivePowerAction.Maintain)));
		assertThat("Apparent power calculation method", model.getApparentPowerCalculationMethod(),
				is(equalTo(InverterApparentPowerCalculationMethod.Vector)));
		assertThat("Apparent power calculation method description",
				model.getApparentPowerCalculationMethod().getDescription(), is(equalTo("Vector")));
		assertThat("Active power ramp rate max with negative scale factor",
				model.getActivePowerRampRateMaximum(), is(equalTo(50.0f)));
		assertThat("ECP frequency with negative scale factor", model.getEcpFrequency(),
				is(equalTo(60.0f)));
		assertThat("Connected phase read from the model block", model.getConnectedPhase(),
				is(equalTo(AcPhase.PhaseB)));
	}

	@Test
	public void pccVoltageOffset_notImplemented() {
		assertThat("PCC voltage offset not implemented",
				getTestModel(BLOCK_ADDRESS + 2, 0x8000).getPccVoltageOffset(), is(nullValue()));
	}

	@Test
	public void enumValues_undefinedCodes() {
		// GIVEN
		// VArAct and ClcTotVA codes SunSpec does not define
		InverterBasicSettingsModelAccessor model = getTestModel(BLOCK_ADDRESS + 15, 0x0000, 0x0003);

		// THEN
		assertThat("Undefined reactive power action not available",
				model.getImportExportChangeReactivePowerAction(), is(nullValue()));
		assertThat("Undefined apparent power calculation method not available",
				model.getApparentPowerCalculationMethod(), is(nullValue()));
	}

	@Test
	public void writeValues() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnectionWithRegisters(
				getClass(), TEST_DATA, BLOCK_ADDRESS, SYNTHETIC_BLOCK);
		InverterBasicSettingsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setActivePowerMaximum(conn, 9000);
		model.setPccVoltage(conn, 230.5f);
		model.setPccVoltageOffset(conn, -1.5f);
		model.setVoltageMaximum(conn, 253.0f);
		model.setVoltageMinimum(conn, 207.0f);
		model.setApparentPowerMaximum(conn, 10000);
		model.setReactivePowerQ1Maximum(conn, 5000);
		model.setReactivePowerQ2Maximum(conn, 4000);
		model.setReactivePowerQ3Maximum(conn, -4000);
		model.setReactivePowerQ4Maximum(conn, -5000);
		model.setActivePowerRampRate(conn, 12.5f);
		model.setPowerFactorQ1Minimum(conn, -0.9f);
		model.setPowerFactorQ2Minimum(conn, 0.95f);
		model.setPowerFactorQ3Minimum(conn, -0.95f);
		model.setPowerFactorQ4Minimum(conn, 0.9f);
		model.setImportExportChangeReactivePowerAction(conn, InverterReactivePowerAction.Switch);
		model.setApparentPowerCalculationMethod(conn, InverterApparentPowerCalculationMethod.Arithmetic);
		model.setActivePowerRampRateMaximum(conn, 75.0f);
		model.setEcpFrequency(conn, 50.0f);
		model.setConnectedPhase(conn, AcPhase.PhaseC);

		// THEN
		assertThat("Each point written to its own register, in order", conn.getWrites(),
				is(equalTo(IntStream.rangeClosed(BLOCK_ADDRESS, BLOCK_ADDRESS + 19)
						.mapToObj(a -> List.of(a, 1)).toList())));

		InverterBasicSettingsModelAccessor device = discoverModel(conn);
		assertThat("Active power max", device.getActivePowerMaximum(), is(equalTo(9000)));
		assertThat("PCC voltage", device.getPccVoltage(), is(equalTo(230.5f)));
		assertThat("PCC voltage offset", device.getPccVoltageOffset(), is(equalTo(-1.5f)));
		assertThat("Voltage max", device.getVoltageMaximum(), is(equalTo(253.0f)));
		assertThat("Voltage min", device.getVoltageMinimum(), is(equalTo(207.0f)));
		assertThat("VA max", device.getApparentPowerMaximum(), is(equalTo(10000)));
		assertThat("VAR Q1 max", device.getReactivePowerQ1Maximum(), is(equalTo(5000)));
		assertThat("VAR Q2 max", device.getReactivePowerQ2Maximum(), is(equalTo(4000)));
		assertThat("VAR Q3 max", device.getReactivePowerQ3Maximum(), is(equalTo(-4000)));
		assertThat("VAR Q4 max", device.getReactivePowerQ4Maximum(), is(equalTo(-5000)));
		assertThat("Active power ramp rate", device.getActivePowerRampRate(), is(equalTo(12.5f)));
		assertThat("Power factor Q1 minimum", device.getPowerFactorQ1Minimum(), is(equalTo(-0.9f)));
		assertThat("Power factor Q2 minimum", device.getPowerFactorQ2Minimum(), is(equalTo(0.95f)));
		assertThat("Power factor Q3 minimum", device.getPowerFactorQ3Minimum(), is(equalTo(-0.95f)));
		assertThat("Power factor Q4 minimum", device.getPowerFactorQ4Minimum(), is(equalTo(0.9f)));
		assertThat("Import export reactive power action",
				device.getImportExportChangeReactivePowerAction(),
				is(equalTo(InverterReactivePowerAction.Switch)));
		assertThat("Apparent power calculation method", device.getApparentPowerCalculationMethod(),
				is(equalTo(InverterApparentPowerCalculationMethod.Arithmetic)));
		assertThat("Active power ramp rate max", device.getActivePowerRampRateMaximum(),
				is(equalTo(75.0f)));
		assertThat("ECP frequency", device.getEcpFrequency(), is(equalTo(50.0f)));
		assertThat("Connected phase", device.getConnectedPhase(), is(equalTo(AcPhase.PhaseC)));
	}

	@Test
	public void writeValue_scaleFactorNotImplemented() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		InverterBasicSettingsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setEcpFrequency(conn, 60.0f);
			fail("Scaled value without an implemented scale factor should be rejected.");
		} catch ( IllegalStateException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void writeConnectedPhase_total() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				TEST_DATA);
		InverterBasicSettingsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setConnectedPhase(conn, AcPhase.Total);
			fail("Total is not a connected phase.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

}
