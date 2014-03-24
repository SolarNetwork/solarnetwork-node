/* ==================================================================
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.consumption.hc;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * DatumDataSource for ConsumptionDatum with the Hsiang Cheng EM 5610 kWh meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class EM5610ConsumptionDatumDataSource implements DatumDataSource<ConsumptionDatum>,
		SettingSpecifierProvider {

	private static final int DEFAULT_CONNECTION_TRIES = 2;
	private static final int DEFAULT_WATT_HOUR_REGISTER = 0x160;
	private static final int DEFAULT_VOLTAGE_REGISTER = 0x136;
	private static final int DEFAULT_CURRENT_REGISTER = 0x130;
	private static final String DEFAULT_SERIAL_PORT = "/dev/ttyS0";
	private static final float CURRENT_UNIT_VALUE = 60000F;
	private static final float VOLTAGE_UNIT_VALUE = 100F;
	private static final int DEFAULT_UNIT_ID = 1;

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private int unitId = DEFAULT_UNIT_ID;
	private int currentRegister = DEFAULT_CURRENT_REGISTER;
	private int voltageRegister = DEFAULT_VOLTAGE_REGISTER;
	private int wattHourRegister = DEFAULT_WATT_HOUR_REGISTER;
	private int connectionTries = DEFAULT_CONNECTION_TRIES;
	private int transactionRepeat = 1;
	private String sourceId = DEFAULT_SOURCE_ID;
	private String groupUID = null;
	private SerialPortBeanParameters serialParams = getDefaultSerialParametersInstance();

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	private static SerialPortBeanParameters getDefaultSerialParametersInstance() {
		SerialPortBeanParameters params = new SerialPortBeanParameters();
		params.setSerialPort(DEFAULT_SERIAL_PORT);
		params.setBaud(9600);
		params.setDataBits(8);
		params.setParity(0);
		params.setStopBits(2);
		params.setReceiveTimeout(1600);
		return params;
	}

	private static SerialParameters modbusParams(SerialPortBeanParameters bean) {
		SerialParameters params = new SerialParameters();
		params.setPortName(bean.getSerialPort());
		params.setBaudRate(bean.getBaud());
		params.setDatabits(bean.getDataBits());
		params.setParity(bean.getParity());
		params.setStopbits(bean.getStopBits());
		params.setEncoding("rtu");
		params.setEcho(false);
		params.setReceiveTimeout(bean.getReceiveTimeout());
		return params;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		SerialConnection conn = null;
		try {
			conn = openConnection(this.connectionTries);
			return getSample(conn);
		} finally {
			if ( conn != null ) {
				conn.close();
			}
		}
	}

	private ConsumptionDatum getSample(SerialConnection conn) {
		final ConsumptionDatum datum = new ConsumptionDatum();

		readInputRegister(conn, new ReadMultipleRegistersRequest(this.currentRegister, 1),
				this.transactionRepeat, new ReadMultipleRegistersTransactionCallback() {

					@Override
					public void doInTransaction(ReadMultipleRegistersResponse res, int transactionIdx) {
						int regVal = res.getRegisterValue(0);
						log.trace("current regVal: {}", regVal);

						Float current = Float.valueOf(regVal / CURRENT_UNIT_VALUE);

						log.debug("current: {}", current);
						datum.setAmps(current);
					}
				});

		readInputRegister(conn, new ReadMultipleRegistersRequest(this.voltageRegister, 1),
				this.transactionRepeat, new ReadMultipleRegistersTransactionCallback() {

					@Override
					public void doInTransaction(ReadMultipleRegistersResponse res, int transactionIdx) {
						int regVal = res.getRegisterValue(0);
						log.trace("voltage regVal: {}", regVal);

						Float voltage = Float.valueOf(regVal / VOLTAGE_UNIT_VALUE);

						log.debug("voltage: {}", voltage);
						datum.setVolts(voltage);
					}
				});

		readInputRegister(conn, new ReadMultipleRegistersRequest(this.wattHourRegister, 2),
				this.transactionRepeat, new ReadMultipleRegistersTransactionCallback() {

					@Override
					public void doInTransaction(ReadMultipleRegistersResponse res, int transactionIdx) {
						int regVal = (res.getRegisterValue(0) << 16) | res.getRegisterValue(1);
						log.trace("WH regVal: {}", regVal);

						if ( regVal > 0 ) {
							Long wh = Long.valueOf(regVal);

							log.debug("WH: {}", wh);
							datum.setWattHourReading(wh);
						}
					}
				});
		datum.setSourceId(this.sourceId);
		return datum;
	}

	private void readInputRegister(SerialConnection conn, ReadMultipleRegistersRequest req,
			int txRepeat, ReadMultipleRegistersTransactionCallback callback) {
		log.debug("Starting modbus transaction for request [{}]", req);

		ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
		req.setUnitID(this.unitId);
		req.setHeadless();
		trans.setRequest(req);
		for ( int i = 0; i < txRepeat; i++ ) {
			try {
				trans.execute();
			} catch ( ModbusException e ) {
				throw new RuntimeException(e);
			}
			ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
			log.debug("Got response [{}]", res);
			callback.doInTransaction(res, i);
		}
	}

	private SerialConnection openConnection(int tries) {
		SerialParameters params = modbusParams(this.serialParams);
		log.debug("Opening serial connection to [{}], {} tries remaining", params.getPortName(), tries);
		try {
			SerialConnection conn = new SerialConnection(params);
			log.trace("just before opening connection status: {}", conn.isOpen());
			conn.open();
			log.trace("just after opening connection status: {}", conn.isOpen());
			return conn;
		} catch ( Exception e ) {
			if ( tries > 1 ) {
				return openConnection(tries - 1);
			}
			throw new RuntimeException("Unable to open serial connection to [" + params.getPortName()
					+ "]", e);
		}
	}

	@Override
	public String getUID() {
		return getSourceId();
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.hc";
	}

	@Override
	public String getDisplayName() {
		return "Hsiang Cheng EM 5610 kWh meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource serial = new ResourceBundleMessageSource();
				serial.setBundleClassLoader(SerialPortBeanParameters.class.getClassLoader());
				serial.setBasename(SerialPortBeanParameters.class.getName());

				PrefixedMessageSource serialSource = new PrefixedMessageSource();
				serialSource.setDelegate(serial);
				serialSource.setPrefix("serialParams.");

				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				source.setParentMessageSource(serialSource);
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("serialParams.serialPort", DEFAULT_SERIAL_PORT));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", null));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(DEFAULT_UNIT_ID)));
		results.add(new BasicTextFieldSettingSpecifier("currentRegister", String
				.valueOf(DEFAULT_CURRENT_REGISTER)));
		results.add(new BasicTextFieldSettingSpecifier("voltageRegister", String
				.valueOf(DEFAULT_VOLTAGE_REGISTER)));
		results.add(new BasicTextFieldSettingSpecifier("wattHourRegister", String
				.valueOf(DEFAULT_WATT_HOUR_REGISTER)));
		results.add(new BasicTextFieldSettingSpecifier("connectionTries", String
				.valueOf(DEFAULT_CONNECTION_TRIES)));
		results.addAll(SerialPortBeanParameters.getDefaultSettingSpecifiers("serialParams."));
		return results;
	}

	private static interface ReadMultipleRegistersTransactionCallback {

		/**
		 * Execute within a ReadInputRegister transaction.
		 * 
		 * @param res
		 *        the response
		 * @param transactionIdx
		 *        the transaction index
		 */
		void doInTransaction(ReadMultipleRegistersResponse res, int transactionIdx);

	}

	public int getUnitId() {
		return unitId;
	}

	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

	public int getCurrentRegister() {
		return currentRegister;
	}

	public void setCurrentRegister(int currentRegister) {
		this.currentRegister = currentRegister;
	}

	public int getVoltageRegister() {
		return voltageRegister;
	}

	public void setVoltageRegister(int voltageRegister) {
		this.voltageRegister = voltageRegister;
	}

	public int getWattHourRegister() {
		return wattHourRegister;
	}

	public void setWattHourRegister(int wattHourRegister) {
		this.wattHourRegister = wattHourRegister;
	}

	public int getConnectionTries() {
		return connectionTries;
	}

	public void setConnectionTries(int connectionTries) {
		this.connectionTries = connectionTries;
	}

	public int getTransactionRepeat() {
		return transactionRepeat;
	}

	public void setTransactionRepeat(int transactionRepeat) {
		this.transactionRepeat = transactionRepeat;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public SerialPortBeanParameters getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

}
