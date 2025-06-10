/* ==================================================================
 * Shark100DataTests.java - 26/07/2018 4:15:33 PM
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

package net.solarnetwork.node.hw.eig.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.hw.eig.meter.Shark100Data;
import net.solarnetwork.node.hw.eig.meter.Shark100DataAccessor;
import net.solarnetwork.node.hw.eig.meter.SharkPowerEnergyFormat;
import net.solarnetwork.node.hw.eig.meter.SharkScale;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link Shark100Data} class.
 * 
 * @author matt
 * @version 2.0
 */
public class Shark100DataTests {

	// @formatter:off
	private static final int[] TEST_DATA_REG_1 = new int[] {
			0x4531,
			0x3431,
			0x2053,
			0x6861,
			0x726B,
			0x2031,
			0x3030,
			0x2020,
			0x3030,
			0x3532,
			0x3638,
			0x3438,
			0x3333,
			0x2020,
			0x2020,
			0x2020,
			0x0003,
			0x3030,
			0x3437,
			0x0010,
			0x003C,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x4550,
			0x4D36,
			0x3030,
			0x3020,
			0x2020,
			0x2020,
			0x2020,
			0x2020,
	};
	
	private static final int[] TEST_DATA_REG_900 = new int[] {
			0xC972, 0x1400, // W total
			0xC884, 0x894C, // VAR total
			0x497A, 0xFC0B, // VA total
	};
	
	private static final int[] TEST_DATA_REG_1000 = new int[] {
			0x438B, 0xD7F3, // Volts A-N
			0x4391, 0x8AC7,
			0x4390, 0x43B3,
			0x43F7, 0x1F60, // Volts A-B
			0x43FA, 0xF4C4,
			0x43F6, 0x0475,
			0x4494, 0xD80F, // Amps A
			0x4494, 0x4ECA, // Amps B
			0x4494, 0xEFCA, // Amps C
			0xC970, 0x9B74, // Active power total
			0xC883, 0x81A0, // Reactive power total
			0x4979, 0x6E21, // Apparent power total
			0xBF76,
			0xEBF3,
			0x426F,
			0xEDE2,
			0x425E,
			0x2970,
	};
	
	private static final int[] TEST_DATA_REG_1100 = new int[] {
			0xFFE9, 0xA638, // Wh rec
			0x0000, 0x02BC, // Wh del
			0xFFE9, 0xA8F5, // Wh net
			0x0016, 0x5C85, // Wh tot
			0x0000, 0xDA5E, // VARh + (rec)
			0xFFFD, 0xE86F, // VARh - (del)
			0xFFFE, 0xC2CD, // VARh net
			0x0002, 0xF1EF, // VARh tot
			0x0016, 0xB49F, // VAh tot
	};
	
	private static final int[] TEST_DATA_REG_2000 = new int[] {
			0x449E,
			0x020F,
			0x449D,
			0x676B,
			0x449D,
			0xF050,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0xC97E,
			0x4105,
			0xC891,
			0x644B,
			0x4984,
			0x39F8,
			0x0000,
			0x0000,
			0xBF76,
			0x2089,
	};
	
	private static final int[] TEST_DATA_REG_30000 = new int[] {
			0x0501,
			0x0BB8,
			0x0115,
			0x0115,
			0x0100,
			0x0F81,
			0x3362,
			0x00FF,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0384,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0026,
			0x0011,
			0x0020,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
	};
	
	private static final int[] TEST_DATA_REG_40000 = new int[] {
			0x0000,
			0x0000,
			0x1710,
			0x17B7,
			0x179F,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x07FF,
			0x0BE7,
			0x07FB,
			0x1554,
			0x1591,
			0x1549,
			0x0BB8,
			0x0001,
			0x0005,
			0x0115,
			0x0001,
			0x0115,
			0x0016,
			0x59C8,
			0x0000,
			0x02BC,
			0x0000,
			0xDA5E,
			0x0002,
			0x1791,
			0x0016,
			0xB49F,
	};
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(Shark100Data.class);

	private Shark100Data getTestDataInstance() {
		Shark100Data data = new Shark100Data();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(TEST_DATA_REG_1, 0);
					m.saveDataArray(TEST_DATA_REG_900, 899);
					m.saveDataArray(TEST_DATA_REG_1000, 999);
					m.saveDataArray(TEST_DATA_REG_1100, 1099);
					m.saveDataArray(TEST_DATA_REG_2000, 1999);
					m.saveDataArray(TEST_DATA_REG_30000, 29999);
					m.saveDataArray(TEST_DATA_REG_40000, 39999);
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
		Shark100Data data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void interpretInfo() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Name", data.getName(), equalTo("E141 Shark 100"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("0052684833"));
		assertThat("Firmware version", data.getFirmwareRevision(), equalTo("0047"));
	}

	@Test
	public void powerEnergyFormat() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Power/energy format", data.getPowerEnergyFormat(),
				equalTo(new SharkPowerEnergyFormat(SharkScale.Kilo, 3, SharkScale.Mega, 2)));
	}

	@Test
	public void interpretBasic() {
		Shark100DataAccessor data = getTestDataInstance();
		assertThat("Frequency", data.getFrequency(), equalTo(59.982307f));
		assertThat("Voltage", data.getVoltage(), equalTo(286.43338f));
		assertThat("Line voltage", data.getLineVoltage(), equalTo(496.0640566667f));
		assertThat("Current", data.getCurrent(), equalTo(3568.7073f));
		assertThat("Active power", data.getActivePower(), equalTo(-991552));
		assertThat("Reactive power", data.getReactivePower(), equalTo(-271434));
		assertThat("Apparent power", data.getApparentPower(), equalTo(1028032));
		assertThat("Power factor", data.getPowerFactor(), equalTo(-0.9645378f));
		assertThat("Energy received", data.getActiveEnergyReceived(), equalTo(14647760000L));
		assertThat("Energy delivered", data.getActiveEnergyDelivered(), equalTo(7000000L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(559020000L));
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), equalTo(1371050000L));
	}

	@Test
	public void interpretBasicReversed() {
		Shark100DataAccessor data = getTestDataInstance().reversedDataAccessor();
		assertThat("Frequency", data.getFrequency(), equalTo(59.982307f));
		assertThat("Voltage", data.getVoltage(), equalTo(286.43338f));
		assertThat("Line voltage", data.getLineVoltage(), equalTo(496.0640566667f));
		assertThat("Current", data.getCurrent(), equalTo(3568.7073f));
		assertThat("Active power", data.getActivePower(), equalTo(991552));
		assertThat("Reactive power", data.getReactivePower(), equalTo(271434));
		assertThat("Apparent power", data.getApparentPower(), equalTo(1028032));
		assertThat("Power factor", data.getPowerFactor(), equalTo(-0.9645378f));
		assertThat("Energy received", data.getActiveEnergyReceived(), equalTo(7000000L));
		assertThat("Energy delivered", data.getActiveEnergyDelivered(), equalTo(14647760000L));
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), equalTo(1371050000L));
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), equalTo(559020000L));
	}

	@Test
	public void phaseVoltage() {
		Shark100DataAccessor data = getTestDataInstance();
		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase a", phaseData.getVoltage(), equalTo(279.6871f));
		assertThat("Phase a line", phaseData.getLineVoltage(), equalTo(494.24512f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase b", phaseData.getVoltage(), equalTo(291.0842f));
		assertThat("Phase b line", phaseData.getLineVoltage(), equalTo(501.91223f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase c", phaseData.getVoltage(), equalTo(288.5289f));
		assertThat("Phase c line", phaseData.getLineVoltage(), equalTo(492.03482f));
	}

	@Test
	public void phaseCurrent() {
		Shark100DataAccessor data = getTestDataInstance();
		AcEnergyDataAccessor phaseData = data.accessorForPhase(AcPhase.PhaseA);
		assertThat("Phase a", phaseData.getCurrent(), equalTo(1190.7518f));

		phaseData = data.accessorForPhase(AcPhase.PhaseB);
		assertThat("Phase b", phaseData.getCurrent(), equalTo(1186.4622f));

		phaseData = data.accessorForPhase(AcPhase.PhaseC);
		assertThat("Phase c", phaseData.getCurrent(), equalTo(1191.4934f));
	}
}
