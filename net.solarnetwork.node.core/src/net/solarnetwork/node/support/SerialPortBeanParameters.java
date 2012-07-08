/* ==================================================================
 * SerialPortBeanParameters.java - Mar 24, 2012 9:11:59 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import java.util.List;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Parameters to configure a serial port with.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>serialPort</dt>
 * <dd>The name of the serial port to use. This is OS-specific, for example
 * <code>/dev/ttyUSB0</code>.</dd>
 * 
 * <dt>commPortAppName</dt>
 * <dd>A user-defined name to associate with the serial port. The serial port
 * can only be used by one application at a time.</dd>
 * 
 * <dt>maxWait</dt>
 * <dd>A maximum number of milliseconds to wait for the serial port to return
 * data, before giving up. This differs from the {@code receiveTimeout} 
 * setting in that this timeout is not set on the port itself, but is managed
 * by the application.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class SerialPortBeanParameters extends SerialPortBean {

	private static final SerialPortBeanParameters DEFAULTS = new SerialPortBeanParameters();
	
	private String serialPort = "/dev/ttyUSB0";
	private String commPortAppName = "SolarNode";
	private long maxWait = 0;

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param prefix the bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(String prefix) {
		return getDefaultSettingSpecifiers(DEFAULTS, prefix);
	}

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param defaults the default values to use
	 * @param prefix the bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(
			SerialPortBeanParameters defaults, String prefix) {
		List<SettingSpecifier> results = SerialPortBean.getDefaultSettingSpecifiers(
				defaults, prefix);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "commPortAppName", 
				defaults.getCommPortAppName()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxWait", 
				String.valueOf(defaults.getMaxWait())));
		return results;
	}

	public long getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	public String getSerialPort() {
		return serialPort;
	}

	public void setSerialPort(String serialPort) {
		this.serialPort = serialPort;
	}

	public String getCommPortAppName() {
		return commPortAppName;
	}

	public void setCommPortAppName(String commPortAppName) {
		this.commPortAppName = commPortAppName;
	}

}
