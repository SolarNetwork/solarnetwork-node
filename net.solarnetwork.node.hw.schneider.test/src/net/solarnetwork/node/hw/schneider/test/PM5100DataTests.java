/* ==================================================================
 * PM5100DataTests.java - 18/05/2018 7:13:52 AM
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

package net.solarnetwork.node.hw.schneider.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.hw.schneider.meter.PM5100Data;
import net.solarnetwork.node.hw.schneider.meter.PM5100DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.PM5100Model;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Tests for the {@link PM5100Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM5100DataTests {

	// @formatter:off
	private static final int[] TEST_DATA_REG_28 = {
			0x0000, 0x506F,
			0x7765, 0x7220,
			0x4D65, 0x7465,
			0x7220, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x504D,
			0x3533, 0x3330,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x5363,
			0x686E, 0x6569,
			0x6465, 0x7220,
			0x456C, 0x6563,
			0x7472, 0x6963,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x0000,
			0x0000, 0x3BA9,
	};
	
	private static final int[] TEST_DATA_REG_128 = {
			0x0022, 0x055D,
			0xB7C3, 0x0010,
			0x081E, 0x060A,
			0x000D, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_1636 = {
			0x0000, 0x0002,
			0x0001, 0x0000,
			0x0004, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_2012 = {
			0x0000, 0x0003,
			0x0004, 0x000B,
	};

	private static final int[] TEST_DATA_REG_2998 = {
			0x3E11, 0x42db, // , I a
			0x6b85, 0x42d8, // , I b
			0x570a, 0x42dc, // , I c 
			0x6b85, 0x8000, // , I n
			0x8000, 0x0000, // , I g
			0x0000, 0x42DB, // , I avg
			0x6A20, 0x3FDA,
			0x90FE, 0x3EAB,
			0x06E0, 0x3FAF,
			0xCF0B, 0x3FDA,
			0x90FE, 0x43F8, // , Vll ab
			0x0A73, 0x43F6, // , Vll bc
			0xC5B1, 0x43F6, // , Vll ca
			0xEA0F, 0x43F7, // , Vll avg
			0x3E11, 0x438E, // , Vln a
			0x86AF, 0x438E, // , Vln b
			0xF30F, 0x438E, // , Vln c 
			0xCED0, 0x8000,
			0x8000, 0x438E, // , Vln avg
			0xC2DA, 0x3EA5,
			0x5473, 0x3E42,
			0xBF84, 0x3E07,
			0xE961, 0x3EA5,
			0x5473, 0x3E28,
			0x9567, 0x3E07,
			0x1209, 0x3D06,
			0x0D76, 0x3E28,
			0x9567, 0x41F5, // , Active power A
			0x8370, 0x41FB, // , Active power B
			0x2E41, 0x41FD, // , Active power C
			0xAEF4, 0x42BB, // , Active power total
			0x9829, 0xBFDE,
			0x601E, 0x4008,
			0x8510, 0xBFED,
			0x2324, 0xBFBA,
			0x7921, 0x0000,
	};

	private static final int[] TEST_DATA_REG_3074 = {
			0x0000, 0x42BB,
			0xF20F, 0x3F80,
			0x3461, 0x3F7F,
			0x6947, 0x3F80,
			0x37C8, 0x3F80,
			0x3D3A, 0x3F80,
			0x0110, 0x3F7F,
			0xDEF3, 0x3F80,
			0x0165, 0x3F80,
			0x0654, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x8000,
			0x8000, 0x4270,
			0x070C, 0x0000,
	};
	
	private static final int[] TEST_DATA_REG_3202 = {
			0x0000, 0x0000, // , Wh del
			0x0000, 0x046E, // 
			0x73EA, 0x0000, // , Wh rec
			0x0000, 0x0001, 
			0x6481, 0x0000,
			0x0000, 0x046F,
			0xD86B, 0x0000,
			0x0000, 0x046D,
			0x0F69, 0x0000,
			0x0000, 0x0027,
			0x90C1, 0x0000,
			0x0000, 0x000F,
			0xF38A, 0x0000,
			0x0000, 0x0037,
			0x844B, 0x0000,
			0x0000, 0x0017,
			0x9D37, 0x0000,
			0x0000, 0x0481,
			0xCE9A, 0x0000,
			0x0000, 0x0011,
			0xC261, 0x0000,			
	};
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(PM5100DataTests.class);

	private PM5100Data getTestDataInstance() {
		PM5100Data data = new PM5100Data();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(TEST_DATA_REG_28, 28);
					m.saveDataArray(TEST_DATA_REG_128, 128);
					m.saveDataArray(TEST_DATA_REG_1636, 1636);
					m.saveDataArray(TEST_DATA_REG_2012, 2012);
					m.saveDataArray(TEST_DATA_REG_2998, 2998);
					m.saveDataArray(TEST_DATA_REG_3074, 3074);
					m.saveDataArray(TEST_DATA_REG_3202, 3202);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void dataDebugString() {
		PM5100Data data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfo() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Model", data.getModel(), equalTo(PM5100Model.PM5330));
		assertThat("Firmware version", data.getFirmwareRevision(), equalTo("2.1.4"));
	}

	@Test
	public void interpretBasic() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(60.00688f));
		assertThat("Voltage", data.getVoltage(), equalTo(285.52228f));
		assertThat("Current", data.getCurrent(), equalTo(109.707275f));
		assertThat("Power", data.getActivePower(), equalTo(93797));
		assertThat("Power factor", data.getPowerFactor(), equalTo(1.0018685f));
	}

	@Test
	public void activePower() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Power", data.getActivePower(), equalTo(93797));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase power a", phaseData.getActivePower(), equalTo(30689));

		phaseData = data.accessorForPhase(PhaseB);
		assertThat("Phase power b", phaseData.getActivePower(), equalTo(31398));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase power c", phaseData.getActivePower(), equalTo(31710));
	}

	@Test
	public void activePowerReversed() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Power reversed", data.reversed().getActivePower(), equalTo(-93797));

		assertThat("Reversed phase power a", data.reversed().accessorForPhase(PhaseA).getActivePower(),
				equalTo(-30689));
		assertThat("Phase power a reversed", data.accessorForPhase(PhaseA).reversed().getActivePower(),
				equalTo(-30689));

		assertThat("Reversed phase power b", data.reversed().accessorForPhase(PhaseB).getActivePower(),
				equalTo(-31398));
		assertThat("Phase power b reversed", data.accessorForPhase(PhaseB).reversed().getActivePower(),
				equalTo(-31398));

		assertThat("Reversed phase power c", data.reversed().accessorForPhase(PhaseC).getActivePower(),
				equalTo(-31710));
		assertThat("Phase power c reversed", data.accessorForPhase(PhaseC).reversed().getActivePower(),
				equalTo(-31710));
	}

	@Test
	public void energyDelivered() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Active energy delievered", data.getActiveEnergyDelivered(), equalTo(74347498L));

		assertThat("Active energy delivered reversed", data.reversed().getActiveEnergyDelivered(),
				equalTo(91265L));
	}

	@Test
	public void energyReceived() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Active energy receieved", data.getActiveEnergyReceived(), equalTo(91265L));

		assertThat("Active energy received reversed", data.reversed().getActiveEnergyReceived(),
				equalTo(74347498L));
	}

	@Test
	public void current() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Current", data.getCurrent(), equalTo(109.707275f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase current a", phaseData.getCurrent(), equalTo(109.71f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase current b", phaseData.getCurrent(), equalTo(108.17f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase current c", phaseData.getCurrent(), equalTo(110.21f));
	}

	@Test
	public void voltage() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Voltage", data.getVoltage(), equalTo(285.52228f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase voltage a", phaseData.getVoltage(), equalTo(285.05222f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase voltage b", phaseData.getVoltage(), equalTo(285.8989f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase voltage c", phaseData.getVoltage(), equalTo(285.61572f));
	}

	@Test
	public void lineVoltage() {
		PM5100DataAccessor data = getTestDataInstance();
		assertThat("Line voltage", data.getLineVoltage(), equalTo(494.4849f));

		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Line voltage ab", phaseData.getLineVoltage(), equalTo(496.08163f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Line voltage bc", phaseData.getLineVoltage(), equalTo(493.54446f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Line voltage ca", phaseData.getLineVoltage(), equalTo(493.82858f));
	}

}
