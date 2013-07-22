/* ==================================================================
 * JamodModbusSerialConnectionFactory.java - Jul 10, 2013 7:35:26 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Default implementation of {@link ModbusSerialConnectionFactory}.
 * 
 * @author matt
 * @version 1.0
 */
public class JamodModbusSerialConnectionFactory implements ModbusSerialConnectionFactory,
		SettingSpecifierProvider {

	private static MessageSource MESSAGE_SOURCE;

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

	private final Logger log = LoggerFactory.getLogger(getClass());

	private SerialParametersBean serialParams = getDefaultSerialParametersInstance();

	@Override
	public String getUID() {
		return serialParams.getPortName();
	}

	@Override
	public SerialConnection getSerialConnection() {
		return openConnection(2);
	}

	private SerialConnection openConnection(int tries) {
		if ( log.isDebugEnabled() ) {
			log.debug("Opening serial connection to [" + serialParams.getPortName() + "], " + tries
					+ " tries remaining");
		}
		try {
			SerialConnection conn = new SerialConnection(serialParams);
			if ( log.isTraceEnabled() ) {
				log.trace("just before opening connection status:[" + conn.isOpen() + "]");
			}
			conn.open();
			if ( log.isTraceEnabled() ) {
				log.trace("just after opening connection status:[" + conn.isOpen() + "]");
			}
			return conn;
		} catch ( Exception e ) {
			if ( tries > 1 ) {
				return openConnection(tries - 1);
			}
			throw new RuntimeException("Unable to open serial connection to ["
					+ serialParams.getPortName() + "]", e);
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

	@Override
	public MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		JamodModbusSerialConnectionFactory defaults = new JamodModbusSerialConnectionFactory();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName", defaults.serialParams
				.getPortName()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate", String
				.valueOf(defaults.serialParams.getBaudRate())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits", String
				.valueOf(defaults.serialParams.getDatabits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits", String
				.valueOf(defaults.serialParams.getStopbits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				defaults.serialParams.getParityString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.encoding", defaults.serialParams
				.getEncoding()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout", String
				.valueOf(defaults.serialParams.getReceiveTimeout())));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.echo", String
				.valueOf(defaults.serialParams.isEcho())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString",
				defaults.serialParams.getFlowControlInString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString",
				defaults.serialParams.getFlowControlInString()));

		return results;
	}

	public SerialParametersBean getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

}
