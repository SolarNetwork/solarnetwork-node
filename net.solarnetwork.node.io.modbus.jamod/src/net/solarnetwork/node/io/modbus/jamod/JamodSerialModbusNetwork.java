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

package net.solarnetwork.node.io.modbus.jamod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Jamod implementation of {@link ModbusNetwork} using a serial connection.
 * 
 * @author matt
 * @version 1.0
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
		JamodModbusConnection mbconn = new JamodModbusConnection(new SerialConnection(serialParams),
				unitId, isHeadless(), serialParams.getPortName());
		mbconn.setRetries(getRetries());
		return new LockingSerialConnection(mbconn);
	}

	/**
	 * Internal extension of {@link SerialConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingSerialConnection implements ModbusConnection {

		private final AtomicBoolean open = new AtomicBoolean(false);
		private final ModbusConnection delegate;

		/**
		 * Construct with delegate.
		 * 
		 * @param parameters
		 *        the parameters
		 */
		private LockingSerialConnection(ModbusConnection delegate) {
			this.delegate = delegate;
		}

		@Override
		public void open() throws IOException, LockTimeoutException {
			if ( open.compareAndSet(false, true) ) {
				acquireLock();
				try {
					delegate.open();
				} catch ( Exception e ) {
					releaseLock();
				}
			}
		}

		@Override
		public void close() {
			if ( open.compareAndSet(true, false) ) {
				try {
					delegate.close();
				} finally {
					releaseLock();
				}
			}
		}

		@Override
		protected void finalize() throws Throwable {
			releaseLock(); // as a catch-all
			super.finalize();
		}

		@Override
		public int getUnitId() {
			return delegate.getUnitId();
		}

		@Override
		public BitSet readDiscreetValues(int address, int count) {
			return delegate.readDiscreetValues(address, count);
		}

		@Override
		public BitSet readDiscreetValues(int[] addresses, int count) {
			return delegate.readDiscreetValues(addresses, count);
		}

		@Override
		public void writeDiscreetValues(int[] addresses, BitSet bits) {
			delegate.writeDiscreetValues(addresses, bits);
		}

		@Override
		public BitSet readInputDiscreteValues(int address, int count) {
			return delegate.readInputDiscreteValues(address, count);
		}

		@Override
		public short[] readWords(ModbusReadFunction function, int address, int count) {
			return delegate.readWords(function, address, count);
		}

		@Override
		public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count) {
			return delegate.readWordsUnsigned(function, address, count);
		}

		@Override
		public void writeWords(ModbusWriteFunction function, int address, short[] values) {
			delegate.writeWords(function, address, values);
		}

		@Override
		public void writeWords(ModbusWriteFunction function, int address, int[] values) {
			delegate.writeWords(function, address, values);
		}

		@Override
		public byte[] readBytes(ModbusReadFunction function, int address, int count) {
			return delegate.readBytes(function, address, count);
		}

		@Override
		public void writeBytes(ModbusWriteFunction function, int address, byte[] values) {
			delegate.writeBytes(function, address, values);
		}

		@Override
		public String readString(ModbusReadFunction function, int address, int count, boolean trim,
				Charset charset) {
			return delegate.readString(function, address, count, trim, charset);
		}

		@Override
		public void writeString(ModbusWriteFunction function, int address, String value,
				Charset charset) {
			delegate.writeString(function, address, value, charset);
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
		JamodSerialModbusNetwork defaults = new JamodSerialModbusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.getUid())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		results.addAll(getBaseSettingSpecifiers());
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
