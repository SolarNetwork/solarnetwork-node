/* ==================================================================
 * Modbus4jPjcSerialModbusNetwork.java - 22/11/2022 8:53:23 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.modbus4j.pjc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.serotonin.modbus4j.serial.rtu.RtuMaster;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.modbus4j.Modbus4jModbusConnection;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Modbus4j implementation of {@link ModbusNetwork} using a JSSC serial
 * connection.
 * 
 * @author matt
 * @version 1.0
 */
public class Modbus4jPjcSerialModbusNetwork extends AbstractModbusNetwork
		implements SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	private static final SerialParameters DEFAULT_SERIAL_PARAMS;
	static {
		DEFAULT_SERIAL_PARAMS = new SerialParameters();
	}

	private final SerialParameters serialParams;

	private RtuMaster controller;

	/**
	 * Constructor.
	 */
	public Modbus4jPjcSerialModbusNetwork() {
		super();
		setDisplayName("Modbus port");
		setUid(null);
		this.serialParams = DEFAULT_SERIAL_PARAMS.clone();
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( controller != null ) {
			controller.destroy();
			controller = null;
		}
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( controller != null ) {
			controller.destroy();
		}
		if ( isConfigured() ) {
			controller = new RtuMaster(new PjcModbusSerialPort(serialParams));
		}
	}

	private boolean isConfigured() {
		String portName = serialParams.getSerialPort();
		return (portName != null && !portName.isEmpty());
	}

	// ModbusNetwork

	@Override
	protected String getNetworkDescription() {
		return serialParams.getSerialPort();
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		return createLockingConnection(new Modbus4jModbusConnection(unitId, isHeadless(), controller,
				this::getNetworkDescription));
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				DEFAULT_SERIAL_PARAMS.getPortName()));
		results.addAll(getBaseSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate",
				String.valueOf(DEFAULT_SERIAL_PARAMS.getBaudRate())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(DEFAULT_SERIAL_PARAMS.getDatabits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(DEFAULT_SERIAL_PARAMS.getStopbits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				DEFAULT_SERIAL_PARAMS.getParityString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout",
				String.valueOf(DEFAULT_SERIAL_PARAMS.getReceiveTimeout())));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString",
				DEFAULT_SERIAL_PARAMS.getFlowControlInString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString",
				DEFAULT_SERIAL_PARAMS.getFlowControlInString()));

		return results;
	}

	// Accessors

	/**
	 * Get the serial parameters.
	 * 
	 * @return the parameters
	 */
	public SerialParameters getSerialParams() {
		return serialParams;
	}

}
