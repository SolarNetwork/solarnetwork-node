/* ==================================================================
 * J2ModSerialModbusNetwork.java - 8/07/2022 2:46:00 pm
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

package net.solarnetwork.node.io.modbus.j2mod;

import java.util.ArrayList;
import java.util.List;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * j2mod implementation of {@link ModbusNetwork} using a serial connection.
 * 
 * @author matt
 * @version 1.0
 */
public class J2ModSerialModbusNetwork extends AbstractModbusNetwork implements SettingSpecifierProvider {

	private SerialParametersBean serialParams = getDefaultSerialParametersInstance();

	/**
	 * Constructor.
	 */
	public J2ModSerialModbusNetwork() {
		super();
		setDisplayName("Modbus port");
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
		return params;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName", null));
		results.addAll(getBaseSettingSpecifiers());

		final SerialParametersBean defaults = getDefaultSerialParametersInstance();

		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate", null));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(defaults.getDatabits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(defaults.getStopbits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				defaults.getParityString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.encoding", defaults.getEncoding()));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.echo",
				String.valueOf(defaults.isEcho())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString",
				defaults.getFlowControlInString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString",
				defaults.getFlowControlInString()));

		return results;
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
		J2ModSerialModbusConnection mbconn = new J2ModSerialModbusConnection(
				new SerialConnection(serialParams), unitId, isHeadless(), serialParams.getPortName());
		mbconn.setRetries(getRetries());
		return createLockingConnection(mbconn);
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
	 *        the parameters to set
	 */
	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

}
