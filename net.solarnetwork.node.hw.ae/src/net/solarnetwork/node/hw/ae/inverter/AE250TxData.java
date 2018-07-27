/* ==================================================================
 * AE250TxData.java - 27/07/2018 2:13:03 PM
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

package net.solarnetwork.node.hw.ae.inverter;

import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.combineToReduceSize;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Data object for the AE 250TX series inverter.
 * 
 * @author matt
 * @version 1.0
 */
public class AE250TxData extends ModbusData implements AE250TxDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public AE250TxData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public AE250TxData(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new AE250TxData(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see #copy()
	 */
	public AE250TxData getSnapshot() {
		return (AE250TxData) copy();
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// we actually read ALL registers here, so our snapshot timestamp includes everything
				updateData(conn, m,
						combineToReduceSize(AE250TxRegister.getRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	/**
	 * Read the inverter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readInverterData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				updateData(conn, m, combineToReduceSize(AE250TxRegister.getInverterRegisterAddressSet(),
						MAX_RESULTS));
				return true;
			}
		});
	}

	/**
	 * Read the status registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readStatusData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				updateData(conn, m,
						combineToReduceSize(AE250TxRegister.getStatusRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	private void updateData(ModbusConnection conn, MutableModbusData m, IntRangeSet rangeSet) {
		IntRange[] ranges = rangeSet.ranges();
		for ( IntRange r : ranges ) {
			int[] data = conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, r.first(),
					r.length());
			m.saveDataArray(data, r.first());
		}
	}

	@Override
	public String getIdNumber() {
		return getAsciiString(AE250TxRegister.InfoInverterIdNumber, true);
	}

	@Override
	public AEInverterType getInverterType() {
		return AEInverterType.forInverterId(getIdNumber());
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(AE250TxRegister.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareRevision() {
		return getAsciiString(AE250TxRegister.InfoFirmwareVersion, true);
	}

	@Override
	public Integer getMapVersion() {
		Number n = getNumber(AE250TxRegister.InfoMapVersion);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public AEInverterConfiguration getInverterConfiguration() {
		Number n = getNumber(AE250TxRegister.InfoInverterConfiguration);
		return (n != null ? AEInverterConfiguration.forRegisterValue(n.intValue()) : null);
	}

	@Override
	public Integer getInverterRatedPower() {
		Number n = getNumber(AE250TxRegister.InfoRatedPower);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Float getFrequency() {
		Number n = getNumber(AE250TxRegister.InverterFrequency);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getCurrent() {
		Number a = getNumber(AE250TxRegister.InverterCurrentPhaseA);
		Number b = getNumber(AE250TxRegister.InverterCurrentPhaseB);
		Number c = getNumber(AE250TxRegister.InverterCurrentPhaseC);
		return (a != null && b != null && c != null ? a.floatValue() + b.floatValue() + c.floatValue()
				: null);
	}

	@Override
	public Float getVoltage() {
		Number a = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseA);
		Number b = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseB);
		Number c = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 3.0f
				: null);
	}

	@Override
	public Integer getActivePower() {
		Number kw = getNumber(AE250TxRegister.InverterActivePowerTotal);
		return (kw != null ? (int) (kw.doubleValue() * 1000) : null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		Number n = getNumber(AE250TxRegister.InverterActiveEnergyDelivered);
		return (n != null ? n.longValue() : null);
	}

}
