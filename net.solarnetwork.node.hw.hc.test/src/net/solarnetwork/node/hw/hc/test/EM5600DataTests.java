/* ==================================================================
 * EM5600DataTests.java - 23/01/2020 11:55:37 am
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

package net.solarnetwork.node.hw.hc.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.Test;
import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.hw.hc.EM5600Register;
import net.solarnetwork.node.hw.hc.UnitFactor;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link EM5600Data} class.
 * 
 * @author matt
 * @version 1.0
 */
public class EM5600DataTests {

	@Test
	public void testParseDate() throws IOException {
		EM5600Data data = new EM5600Data();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { 0x1200, 0xD03 },
						EM5600Register.InfoManufactureDate.getAddress());
				return true;
			}
		});

		assertThat("Manufacture date", data.getManufactureDate(),
				equalTo(LocalDateTime.of(2013, 3, 18, 0, 0, 0)));
	}

	@Test
	public void testParseBasic() throws IOException {
		EM5600Data data = new EM5600Data();
		data.setUnitFactor(UnitFactor.EM5630_30A);
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(
						new int[] { 195, 175, 137, 168, 0, 0, 23929, 23929, 0, 15952, 0, 0, 0, 0, 0, 0,
								3, 0, 44, 750, 28061, 1, 1, 23, 752, 1, 0, 20, -747, 0, 0, 0 },
						EM5600Register.MeterCurrentPhaseA.getAddress());
				m.saveDataArray(
						new int[] { 0, 10926, 0, 0, 0, 20, 0, 131, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
								0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
						EM5600Register.MeterActiveEnergyDelivered.getAddress());
				return true;
			}
		});

		assertThat("Current", data.getCurrent(), equalTo(0.504f));
		assertThat("Active power", data.getActivePower(), equalTo(18));
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(109260L));
		assertThat("Power factor", data.getPowerFactor(), equalTo(0.075f));
	}

}
