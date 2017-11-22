/* ==================================================================
 * SI60KTLCTData.java - 22 Nov 2017 12:28:46
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * SI-60KTL-CT implementation
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class SI60KTLCTData extends BaseCSIData {
	@Override
	public void populateMeasurements(GeneralNodeACEnergyDatum datum) {
		// TODO Auto-generated method stub
	}

	@Override
	public CSIData getSnapshot() {
		return new SI60KTLCTData();// TODO copy values
	}

	public static final int MODEL_1 = 16433;
	public static final int MODEL_2 = 16434;
	public static final int MODEL_3 = 16435;
	
	public static final int ADDR_START = 0;
	public static final int ADDR_LENGTH = 59;
	

	public static final int ADDR_DEVICE_MODEL = 0;
	public static final int ADDR_ACTIVE_POWER = 29;
	public static final int ADDR_APPARENT_POWER = 30;

	@Override
	protected boolean readInverterDataInternal(ModbusConnection conn) {
		readInputData(conn, ADDR_START, ADDR_LENGTH);
		return true;
	}
	
	public Integer getDeviceModel() {
		return getInteger(ADDR_DEVICE_MODEL);
	}
	
	public Integer getActivePower() {
		return getInteger(ADDR_ACTIVE_POWER);
	}
	
	public Integer getApparentPower() {
		return getInteger(ADDR_APPARENT_POWER);
	}

	@Override
	public String dataDebugString() {
		// TODO Auto-generated method stub
		return "TODO";
	}

}
