/* ==================================================================
 * KTLCTDataTests.java - 23/11/2017 5:10:24 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Unit tests for the {@link KTLCTData} class.
 * 
 * @author Max Duncan
 * @version 1.0
 */
public class KTLCTDataTests {

	// TODO replace this with actual data read from an active device
	private static final int[] TEST_DATA = new int[] { 0x4031, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0001, 0x0002, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000 };

	private final KTLCTData data = new KTLCTData();

	@Before
	public void setup() {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA, 0);
				return true;
			}
		});
	}

	@Test
	public void getDeviceModel() {
		assertThat("Inveter type", data.getInverterType(), equalTo(KTLCTInverterType.CSI_50KTL_CT));
	}

	@Test
	public void getActivePower() {
		assertThat("Active power", data.getActivePower(), equalTo(100));
	}

	@Test
	public void getApparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(200));
	}

	// TODO test other values when populated

}
