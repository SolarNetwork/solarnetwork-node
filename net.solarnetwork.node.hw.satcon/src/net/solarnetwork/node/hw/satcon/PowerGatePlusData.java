/* ==================================================================
 * PowerGatePlusData.java - 8/11/2019 11:54:46 am
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

package net.solarnetwork.node.hw.satcon;

import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Implementation for accessing Power Gate Plus data.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerGatePlusData extends ModbusData implements PowerGateInverterDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public PowerGatePlusData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public PowerGatePlusData(ModbusData other) {
		super(other);
	}

	public PowerGatePlusData getSnapshot() {
		return new PowerGatePlusData(this);
	}

	@Override
	public ModbusData copy() {
		return getSnapshot();
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getRegisterAddressSet(), MAX_RESULTS);
		readControlData(conn);
	}

	/**
	 * Read the inverter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readInverterData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the control registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @since 1.4
	 */
	public final void readControlData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getControlRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public String getSerialNumber() {
		final int baseAddress = PowerGatePlusRegister.InfoSerialNumber.getAddress();
		int x = getInt16(baseAddress);
		int y = getInt16(baseAddress + 1);
		int a = getInt16(baseAddress + 2);
		int z = getInt16(baseAddress + 3);
		return String.format("%d%03d%c-%03d", x, y, 'A' + a - 1, z);
	}

	@Override
	public String getFirmwareVersion() {
		Number n = getNumber(PowerGatePlusRegister.InfoDpcbFirmwareVersion);
		String s = (n != null ? n.toString() : null);
		if ( s != null && s.length() > 2 ) {
			s = s.substring(0, s.length() - 2) + "." + s.substring(s.length() - 2);
		}
		return s;
	}

	@Override
	public Set<? extends Fault> getFaults() {
		Set<Fault> all = new LinkedHashSet<>();
		for ( int i = 0; i < 7; i++ ) {
			all.addAll(getFaults(i));
		}
		return all;
	}

	@Override
	public Set<? extends Fault> getFaults(int group) {
		PowerGatePlusRegister reg = null;
		switch (group) {
			case 0:
				reg = PowerGatePlusRegister.StatusFault0Bitmask;
				break;

			case 1:
				reg = PowerGatePlusRegister.StatusFault1Bitmask;
				break;

			case 2:
				reg = PowerGatePlusRegister.StatusFault2Bitmask;
				break;

			case 3:
				reg = PowerGatePlusRegister.StatusFault3Bitmask;
				break;

			case 4:
				reg = PowerGatePlusRegister.StatusFault4Bitmask;
				break;

			case 5:
				reg = PowerGatePlusRegister.StatusFault5Bitmask;
				break;

			case 6:
				reg = PowerGatePlusRegister.StatusFault6Bitmask;
				break;

			default:
				throw new IllegalArgumentException("Fault group " + group + " is not valid.");
		}

		Number n = getNumber(reg);
		if ( n == null ) {
			return null;
		}

		switch (group) {
			case 0:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault0.class);

			case 1:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault1.class);

			case 2:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault2.class);

			case 3:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault3.class);

			case 4:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault4.class);

			case 5:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault5.class);

			case 6:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault6.class);

			default:
				throw new IllegalArgumentException("Fault group " + group + " is not valid.");
		}
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getFrequency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getCurrent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getNeutralCurrent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getVoltage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getLineVoltage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getPowerFactor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getActivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getActiveEnergyDelivered() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getActiveEnergyReceived() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getApparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getApparentEnergyDelivered() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getReactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getDCVoltage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getDCPower() {
		// TODO Auto-generated method stub
		return null;
	}

}
