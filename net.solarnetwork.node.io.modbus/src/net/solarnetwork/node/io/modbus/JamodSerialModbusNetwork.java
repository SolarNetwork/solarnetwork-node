/* ==================================================================
 * JamodSerialModbusNetwork.java - Jul 29, 2014 12:54:53 PM
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

package net.solarnetwork.node.io.modbus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

/**
 * Jamod implementation of {@link ModbusNetwork} using a serial connection.
 * 
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public class JamodSerialModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	private SerialParametersBean serialParams = getDefaultSerialParametersInstance();

	/**
	 * Constructor.
	 */
	public JamodSerialModbusNetwork() {
		super();
		setUid("Serial Port");
	}

	private static SerialParametersBean getDefaultSerialParametersInstance() {
		SerialParametersBean params = new SerialParametersBean();
		params.setPortName("/dev/ttyS0");
		params.setBaudRate(9600);
		params.setDatabits(8);
		params.setParityString("None");
		params.setStopbits(1);
		params.setEncoding("rtu");
		params.setEcho(false);
		params.setReceiveTimeout(1600);
		return params;
	}

	@Override
	protected String getNetworkDescription() {
		return serialParams.getPortName();
	}

	@Override
	public String getUID() {
		String uid = super.getUID();
		if ( uid != null ) {
			return uid;
		}
		return serialParams.getPortName();
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		return new JamodModbusConnection(new LockingSerialConnection(serialParams), unitId,
				isHeadless());
	}

	/**
	 * Internal extension of {@link SerialConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingSerialConnection extends SerialConnection {

		/**
		 * Construct with {@link SerialParameters}.
		 * 
		 * @param parameters
		 *        the parameters
		 */
		private LockingSerialConnection(SerialParameters parameters) {
			super(parameters);
		}

		@Override
		public void open() throws Exception {
			if ( !isOpen() ) {
				acquireLock();
				super.open();
			}
		}

		@Override
		public void close() {
			try {
				if ( isOpen() ) {
					super.close();
				}
			} finally {
				releaseLock();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			releaseLock(); // as a catch-all
			super.finalize();
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Modbus port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		JamodSerialModbusNetwork defaults = new JamodSerialModbusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.getUid())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		results.add(new BasicToggleSettingSpecifier("headless", defaults.isHeadless()));
		results.add(
				new BasicTextFieldSettingSpecifier("timeout", String.valueOf(defaults.getTimeout())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate",
				String.valueOf(defaults.serialParams.getBaudRate())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(defaults.serialParams.getDatabits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(defaults.serialParams.getStopbits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				defaults.serialParams.getParityString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.encoding",
				defaults.serialParams.getEncoding()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout",
				String.valueOf(defaults.serialParams.getReceiveTimeout())));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.echo",
				String.valueOf(defaults.serialParams.isEcho())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString",
				defaults.serialParams.getFlowControlInString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString",
				defaults.serialParams.getFlowControlInString()));

		return results;
	}

	// Accessors

	public SerialParametersBean getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

	/**
	 * Get the timeout unit.
	 * 
	 * @return the unit
	 * @deprecated use {@link #getTimeoutUnit()}
	 */
	@Deprecated
	public TimeUnit getUnit() {
		return getTimeoutUnit();
	}

	/**
	 * SEt the timeout unit.
	 * 
	 * @param unit
	 *        the unit to set
	 * @deprecated use {@link #setTimeoutUnit(TimeUnit)}
	 */
	@Deprecated
	public void setUnit(TimeUnit unit) {
		setTimeoutUnit(unit);
	}

}
