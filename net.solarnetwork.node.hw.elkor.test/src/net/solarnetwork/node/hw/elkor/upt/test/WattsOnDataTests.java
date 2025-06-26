/* ==================================================================
 * WattsOnDataTests.java - 14/08/2020 10:42:28 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.elkor.upt.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.hw.elkor.upt.Ratio;
import net.solarnetwork.node.hw.elkor.upt.WattsOnData;
import net.solarnetwork.node.hw.elkor.upt.WattsOnDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link WattsOnData} class.
 * 
 * @author matt
 * @version 2.0
 */
public class WattsOnDataTests {

	private static final Logger log = LoggerFactory.getLogger(WattsOnDataTests.class);

	private static Map<Integer, Integer> parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
					new InputStreamReader(WattsOnDataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return Collections.emptyMap();
		}
	}

	private WattsOnData getDataInstance(String resource) {
		Map<Integer, Integer> registers = parseTestData(resource);
		WattsOnData data = new WattsOnData();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataMap(registers);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	private WattsOnData getTestDataInstance() {
		return getDataInstance("test-data-01.txt");
	}

	@Test
	public void dataDebugString() {
		WattsOnData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfoAndConfig() {
		WattsOnData data = getTestDataInstance();
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo(new BigDecimal("4.7")));
		assertThat("Serial number", data.getSerialNumber(), equalTo(18034));
		assertThat("PT ratio", data.getPowerTransformerRatio(), equalTo(new Ratio(1, 1)));
		assertThat("CT Ratio", data.getCurrentTransformerRatio(), equalTo(new Ratio(800, 5)));
	}

	@Test
	public void interpretBasic() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(60.0f));
		assertThat("Voltage", data.getVoltage(), equalTo(285.8f));
		assertThat("Current", data.getCurrent(), equalTo(26.08f));
		assertThat("Power factor", data.getPowerFactor(), equalTo(0.9285f));
	}

	@Test
	public void interpretPower() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Active power", data.getActivePower(), equalTo(20800));
		assertThat("Apparent power", data.getApparentPower(), equalTo(22400));
		assertThat("Reactive power", data.getReactivePower(), equalTo(8320));
	}

	@Test
	public void interpretEnergy() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(2085388200L));
		assertThat("Active energy received", data.getActiveEnergyReceived(), equalTo(2604800L));
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), equalTo(226496810L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(9440L));
	}

	@Test
	public void activePower() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Power", data.getActivePower(), equalTo(20800));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase power a", phaseData.getActivePower(), equalTo(6944));

		phaseData = data.accessorForPhase(PhaseB);
		assertThat("Phase power b", phaseData.getActivePower(), equalTo(7040));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase power c", phaseData.getActivePower(), equalTo(6816));
	}

	@Test
	public void activePowerReversed() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Power reversed", data.reversed().getActivePower(), equalTo(-20800));

		assertThat("Reversed phase power a", data.reversed().accessorForPhase(PhaseA).getActivePower(),
				equalTo(-6944));
		assertThat("Phase power a reversed", data.accessorForPhase(PhaseA).reversed().getActivePower(),
				equalTo(-6944));

		assertThat("Reversed phase power b", data.reversed().accessorForPhase(PhaseB).getActivePower(),
				equalTo(-7040));
		assertThat("Phase power b reversed", data.accessorForPhase(PhaseB).reversed().getActivePower(),
				equalTo(-7040));

		assertThat("Reversed phase power c", data.reversed().accessorForPhase(PhaseC).getActivePower(),
				equalTo(-6816));
		assertThat("Phase power c reversed", data.accessorForPhase(PhaseC).reversed().getActivePower(),
				equalTo(-6816));
	}

	@Test
	public void energyDelivered() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Active energy delievered", data.getActiveEnergyDelivered(), equalTo(2085388200L));

		assertThat("Active energy delivered reversed", data.reversed().getActiveEnergyDelivered(),
				equalTo(2604800L));
	}

	@Test
	public void energyReceived() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Active energy receieved", data.getActiveEnergyReceived(), equalTo(2604800L));

		assertThat("Active energy received reversed", data.reversed().getActiveEnergyReceived(),
				equalTo(2085388200L));
	}

	@Test
	public void current() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Current", data.getCurrent(), equalTo(26.08f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase current a", phaseData.getCurrent(), equalTo(25.92f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase current b", phaseData.getCurrent(), equalTo(26.24f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase current c", phaseData.getCurrent(), equalTo(25.76f));
	}

	@Test
	public void voltage() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Voltage", data.getVoltage(), equalTo(285.8f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase voltage a", phaseData.getVoltage(), equalTo(284.8f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase voltage b", phaseData.getVoltage(), equalTo(287.6f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase voltage c", phaseData.getVoltage(), equalTo(285.1f));
	}

	@Test
	public void lineVoltage() {
		WattsOnDataAccessor data = getTestDataInstance();
		assertThat("Line voltage", data.getLineVoltage(), equalTo(495.f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Line voltage ab", phaseData.getLineVoltage(), equalTo(495.7f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Line voltage bc", phaseData.getLineVoltage(), equalTo(495.9f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Line voltage ca", phaseData.getLineVoltage(), equalTo(493.5f));
	}

}
