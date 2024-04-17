/* ==================================================================
 * JMBusSerialWMBusParameters.java - 06/07/2020 09:31:19 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.jmbus;

import java.util.ArrayList;
import java.util.List;
import org.openmuc.jmbus.transportlayer.SerialBuilder;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.StopBits;
import net.solarnetwork.node.service.support.SerialPortBean;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 *
 * Java bean for JMBus serial WMBus parameters
 *
 * @author alex
 * @version 1.2
 */
public class JMBusSerialParameters extends SerialPortBeanParameters {

	/** The default port name. */
	public static final String DEFAULT_PORT_NAME = "/dev/ttyS0";

	/** The default baud rate. */
	public static final int DEFAULT_BAUD_RATE = 9600;

	private static final JMBusSerialParameters DEFAULTS = new JMBusSerialParameters();

	/**
	 * Get a list of setting specifiers for this bean.
	 *
	 * @param prefix
	 *        the bean prefix to use
	 * @return setting specifiers
	 * @since 1.2
	 */
	public static List<SettingSpecifier> settingSpecifiers(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "portName",
				String.valueOf(DEFAULTS.getPortName())));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "baud", String.valueOf(DEFAULTS.getBaud())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "dataBits",
				String.valueOf(DEFAULTS.getDataBits())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "stopBits",
				String.valueOf(DEFAULTS.getStopBits())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "parity",
				String.valueOf(DEFAULTS.getParity())));
		return results;
	}

	/**
	 * Populate serial settings.
	 *
	 * @param builder
	 *        the builder to populate
	 * @since 1.2
	 */
	public void populateSerialSettings(SerialBuilder<?, ?> builder) {
		builder.setSerialPortName(getPortName());
		builder.setBaudrate(getBaud());

		DataBits data;
		switch (getDataBits()) {
			case 5:
				data = DataBits.DATABITS_5;
				break;
			case 6:
				data = DataBits.DATABITS_6;
				break;
			case 7:
				data = DataBits.DATABITS_7;
				break;
			default:
				data = DataBits.DATABITS_8;
				break;
		}
		builder.setDataBits(data);

		StopBits stop;
		switch (getStopBits()) {
			case 2:
				stop = StopBits.STOPBITS_2;
				break;
			case 3:
				stop = StopBits.STOPBITS_1_5;
				break;
			default:
				stop = StopBits.STOPBITS_1;
				break;
		}
		builder.setStopBits(stop);

		Parity parity;
		switch (getParity()) {
			case 0:
				parity = Parity.NONE;
				break;
			case 1:
				parity = Parity.ODD;
				break;
			case 3:
				parity = Parity.MARK;
				break;
			case 4:
				parity = Parity.SPACE;
				break;
			default:
				parity = Parity.EVEN;
				break;
		}
		builder.setParity(parity);
	}

	/**
	 * Constructor.
	 */
	public JMBusSerialParameters() {
		super();
		setSerialPort(DEFAULT_PORT_NAME);
		setBaud(DEFAULT_BAUD_RATE);
	}

	/**
	 * Set port name
	 *
	 * @param portName
	 *        the port name to set
	 * @see SerialPortBeanParameters#setSerialPort(String)
	 */
	public void setPortName(String portName) {
		setSerialPort(portName);
	}

	/**
	 * Get the port name
	 *
	 * @return port name
	 * @see SerialPortBeanParameters#getSerialPort()
	 */
	public String getPortName() {
		return getSerialPort();
	}

	/**
	 * Set the baud rate
	 *
	 * @param baudRate
	 *        the baud rate to set
	 * @see SerialPortBean#setBaud(int)
	 */
	public void setBaudRate(int baudRate) {
		setBaud(baudRate);
	}

	/**
	 * Get the baud rate
	 *
	 * @return baud rate
	 * @see SerialPortBean#getBaud()
	 */
	public int getBaudRate() {
		return getBaud();
	}

}
