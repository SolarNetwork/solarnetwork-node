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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Jamod implementation of {@link ModbusNetwork} using a serial connection.
 * 
 * @author matt
 * @version 2.0
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
	public String getUid() {
		String uid = super.getUid();
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
		return createLockingConnection(mbconn);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
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

	/**
	 * Get the serial parameters.
	 * 
	 * @return the parameters
	 */
	public SerialParametersBean getSerialParams() {
		return serialParams;
	}

	/**
	 * Set the serial parameters.
	 * 
	 * @param serialParams
	 *        the serial parameters to set
	 */
	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

}
