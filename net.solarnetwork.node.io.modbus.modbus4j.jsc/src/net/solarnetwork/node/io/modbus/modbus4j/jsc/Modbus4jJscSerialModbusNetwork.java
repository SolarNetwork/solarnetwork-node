/* ==================================================================
 * Modbus4jJscSerialModbusNetwork.java - 22/11/2022 8:53:23 am
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

package net.solarnetwork.node.io.modbus.modbus4j.jsc;

import java.util.ArrayList;
import java.util.List;
import com.serotonin.modbus4j.serial.rtu.RtuMaster;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.modbus4j.AbstractModbus4jModbusNetwork;
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
public class Modbus4jJscSerialModbusNetwork extends AbstractModbus4jModbusNetwork<RtuMaster>
		implements SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	private static final SerialParameters DEFAULT_SERIAL_PARAMS;
	static {
		DEFAULT_SERIAL_PARAMS = new SerialParameters();
	}

	private final SerialParameters serialParams;

	/**
	 * Constructor.
	 */
	public Modbus4jJscSerialModbusNetwork() {
		super();
		setDisplayName("Modbus port");
		this.serialParams = DEFAULT_SERIAL_PARAMS.clone();
	}

	@Override
	protected boolean isConfigured() {
		String portName = serialParams.getSerialPort();
		return (portName != null && !portName.isEmpty());
	}

	// ModbusNetwork

	@Override
	protected RtuMaster createController() {
		return new RtuMaster(new JscModbusSerialPort(serialParams));
	}

	@Override
	protected void configureController(RtuMaster controller) {
		super.configureController(controller);
		if ( serialParams.getReceiveTimeout() > 0 ) {
			controller.setTimeout(serialParams.getReceiveTimeout());
		}
	}

	@Override
	protected String getNetworkDescription() {
		return serialParams.getSerialPort();
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

		results.addAll(baseModbus4jModbusNetworkSettings(DEFAULT_RETRIES, DEFAULT_KEEP_OPEN_SECONDS));

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
