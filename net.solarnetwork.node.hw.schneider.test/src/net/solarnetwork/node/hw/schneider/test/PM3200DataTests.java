/* ==================================================================
 * PM3200DataTests.java - Apr 1, 2014 11:08:32 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterActivePowerPhaseA;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterActivePowerTotal;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterApparentPowerPhaseA;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterApparentPowerTotal;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterCurrentAverage;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterCurrentPhaseA;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterPowerFactorPhaseA;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterVoltageLineNeutralAverage;
import static net.solarnetwork.node.hw.schneider.meter.PM3200Register.MeterVoltageLineNeutralPhaseA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.hw.schneider.meter.PM3200DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.PM3200Register;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.test.DataUtils;

/**
 * Unit tests for the {@Link PM3200Data} class.
 * 
 * @author matt
 * @version 1.1
 */
public class PM3200DataTests {

	private static final Logger log = LoggerFactory.getLogger(PM3200RegisterTests.class);

	private static final int[] TEST_DATA_2999 = new int[] { 16443, 38526, 65472, 0, 65472, 0, 65472, 0,
			65472, 0, 16443, 38526 };
	private static final int[] TEST_DATA_3019 = new int[] { 65472, 0, 65472, 0, 65472, 0, 65472, 0,
			17260, 31128, 65472, 0, 65472, 0, 65472, 0, 17260, 31128 };
	private static final int[] TEST_DATA_3053 = new int[] { 16138, 19556, 65472, 0, 65472, 0, 16138,
			19556, 48862, 21990, 65472, 0, 65472, 0, 48862, 21990, 16177, 28793, 65472, 0, 65472, 0,
			16177, 28793, 16284, 15418, 65472, 0, 65472, 0, 16284, 15418 };
	private static final int[] TEST_DATA_3107 = new int[] { 48973, 51073, 16968, 12166, 65472, 0, 65472,
			0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 65472, 0, 16835,
			55456 };
	private static final int[] TEST_DATA_3203 = new int[] { 0, 0, 7, 31493, 0, 0, 0, 0, 65472, 0, 65472,
			0, 65472, 0, 65472, 0, 0, 0, 0, 6526, 0, 0, 4, 369, 65472, 0, 65472, 0, 65472, 0, 65472, 0,
			0, 0, 8, 55045, 0, 0 };

	private static Map<Integer, Integer> parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
					new InputStreamReader(PM3200DataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return Collections.emptyMap();
		}
	}

	private PM3200Data getTestDataInstance(String resource) {
		PM3200Data data = new PM3200Data();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataMap(parseTestData(resource));
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	private PM3200Data getTestDataInstance() {
		PM3200Data data = new PM3200Data();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(TEST_DATA_2999, 2999);
					m.saveDataArray(TEST_DATA_3019, 3019);
					m.saveDataArray(TEST_DATA_3053, 3053);
					m.saveDataArray(TEST_DATA_3107, 3107);
					m.saveDataArray(TEST_DATA_3203, 3203);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void interpretCurrent() {
		PM3200Data data = getTestDataInstance();
		assertThat("Current phase A", data.getCurrent(MeterCurrentPhaseA.getAddress()),
				equalTo(2.9310603f));
		assertThat("Current average", data.getCurrent(MeterCurrentAverage.getAddress()),
				equalTo(2.9310603f));
	}

	@Test
	public void interpretVoltage() {
		PM3200Data data = getTestDataInstance();
		assertThat("Voltage L1-N", data.getVoltage(MeterVoltageLineNeutralPhaseA.getAddress()),
				equalTo(236.47498f));
		assertThat("Voltage L-N average", data.getVoltage(MeterVoltageLineNeutralAverage.getAddress()),
				equalTo(236.47498f));
	}

	@Test
	public void interpretPower() {
		PM3200Data data = getTestDataInstance();
		assertThat("Power phase A", data.getPower(MeterActivePowerPhaseA.getAddress()), equalTo(541));
		assertThat("Power total", data.getPower(MeterActivePowerTotal.getAddress()), equalTo(541));
		assertThat("Apparent power phase A", data.getPower(MeterApparentPowerPhaseA.getAddress()),
				equalTo(694));
		assertThat("Apparent power total", data.getPower(MeterApparentPowerTotal.getAddress()),
				equalTo(694));
	}

	@Test
	public void interpretPowerFactor() {
		PM3200Data data = getTestDataInstance();
		assertThat("Power factor phase A", data.getPowerFactor(MeterPowerFactorPhaseA.getAddress()),
				equalTo(1.220588f));
		assertThat("Power factor total",
				data.getPowerFactor(PM3200Register.MeterPowerFactorTotal.getAddress()),
				equalTo(1.220588f));
		assertThat("Reactive power factor total",
				data.getPowerFactor(PM3200Register.MeterReactivePowerFactorTotal.getAddress()),
				equalTo(-0.80382544f));
		assertThat("Effective power factor", data.getEffectiveTotalPowerFactor(), equalTo(-0.77941227f));
	}

	@Test
	public void dataDebugString() {
		PM3200Data data = getTestDataInstance("test-pm3200-data-01.txt");
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfo() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Model", data.getModel(), equalTo("iEM3255"));
		assertThat("Firmware version", data.getFirmwareRevision(), equalTo("1.3.007"));
		assertThat("Name", data.getName(), equalTo("Energy Meter"));
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Schneider Electric"));
		assertThat("Manufacture date", data.getManufactureDate(),
				equalTo(LocalDateTime.of(2016, 6, 1, 0, 0)));
	}

	@Test
	public void deviceInfo() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		Map<String, Object> info = data.getDeviceInfo();
		assertThat("Info size", info.keySet(), hasSize(4));
		assertThat("Model", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_MODEL, "iEM3255 (version 1.3.007)"));
		assertThat("Manufacturer", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_MANUFACTURER, "Schneider Electric"));
		assertThat("Serial number", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, 16233039L));
		assertThat("Manufacture date", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_MANUFACTURE_DATE, LocalDate.of(2016, 6, 1)));
	}

	@Test
	public void interpretBasic() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Frequency", data.getFrequency(), equalTo(50.065586f));
		assertThat("Voltage", data.getVoltage(), equalTo(241.9366f));
		assertThat("Current", data.getCurrent(), equalTo(4.4700837f));
		assertThat("Power", data.getActivePower(), equalTo(-607));
		assertThat("Power factor", data.getPowerFactor(), equalTo(-0.514518f));
	}

	@Test
	public void activePower() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Power", data.getActivePower(), equalTo(-607));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase power a", phaseData.getActivePower(), equalTo(602));

		phaseData = data.accessorForPhase(PhaseB);
		assertThat("Phase power b", phaseData.getActivePower(), equalTo(506));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase power c", phaseData.getActivePower(), equalTo(-1715));
	}

	@Test
	public void activePowerReversed() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Power reversed", data.reversed().getActivePower(), equalTo(607));

		assertThat("Reversed phase power a", data.reversed().accessorForPhase(PhaseA).getActivePower(),
				equalTo(-602));
		assertThat("Phase power a reversed", data.accessorForPhase(PhaseA).reversed().getActivePower(),
				equalTo(-602));

		assertThat("Reversed phase power b", data.reversed().accessorForPhase(PhaseB).getActivePower(),
				equalTo(-506));
		assertThat("Phase power b reversed", data.accessorForPhase(PhaseB).reversed().getActivePower(),
				equalTo(-506));

		assertThat("Reversed phase power c", data.reversed().accessorForPhase(PhaseC).getActivePower(),
				equalTo(1715));
		assertThat("Phase power c reversed", data.accessorForPhase(PhaseC).reversed().getActivePower(),
				equalTo(1715));
	}

	@Test
	public void energyDelivered() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Active energy delievered", data.getActiveEnergyDelivered(), equalTo(82872323L));

		assertThat("Active energy delivered reversed", data.reversed().getActiveEnergyDelivered(),
				equalTo(1043100L));
	}

	@Test
	public void energyReceived() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Active energy receieved", data.getActiveEnergyReceived(), equalTo(1043100L));

		assertThat("Active energy received reversed", data.reversed().getActiveEnergyReceived(),
				equalTo(82872323L));
	}

	@Test
	public void current() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Current", data.getCurrent(), equalTo(4.4700837f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase current a", phaseData.getCurrent(), equalTo(2.601986408f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase current b", phaseData.getCurrent(), equalTo(3.6124866f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase current c", phaseData.getCurrent(), equalTo(7.1957783699f));
	}

	@Test
	public void voltage() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Voltage", data.getVoltage(), equalTo(241.9366f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase voltage a", phaseData.getVoltage(), equalTo(239.2910919f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase voltage b", phaseData.getVoltage(), equalTo(240.9434509f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase voltage c", phaseData.getVoltage(), equalTo(245.575286865f));
	}

	@Test
	public void lineVoltage() {
		PM3200DataAccessor data = getTestDataInstance("test-pm3200-data-01.txt");
		assertThat("Line voltage", data.getLineVoltage(), equalTo(419.00527954f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Line voltage ab", phaseData.getLineVoltage(), equalTo(416.4808654785f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Line voltage bc", phaseData.getLineVoltage(), equalTo(418.658172607f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Line voltage ca", phaseData.getLineVoltage(), equalTo(421.876831f));
	}
}
