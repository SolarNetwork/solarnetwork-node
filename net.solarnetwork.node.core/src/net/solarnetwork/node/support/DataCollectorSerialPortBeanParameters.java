/* ==================================================================
 * DataCollectorSerialPortBeanParameters.java - Mar 24, 2012 9:07:09 PM
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

import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * Configuration bean for {@link DataCollector}.
 * 
 * @author matt
 * @version $Revision$
 */
public class DataCollectorSerialPortBeanParameters extends SerialPortBeanParameters {

	private int bufferSize;
	private byte[] magic;
	private int readSize;
	private boolean toggleDtr = true;
	private boolean toggleRts = true;

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param prefix
	 *            the bean prefix
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(String prefix) {
		List<SettingSpecifier> results = SerialPortBeanParameters
				.getDefaultSettingSpecifiers(prefix);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "bufferSize", "4096"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "magic", "0"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "readSize", "8"));
		results.add(new BasicToggleSettingSpecifier(prefix + "toggleDtr", Boolean.TRUE));
		results.add(new BasicToggleSettingSpecifier(prefix + "toggleRts", Boolean.TRUE));
		return results;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public byte[] getMagic() {
		return magic;
	}

	public void setMagic(byte[] magic) {
		this.magic = magic;
	}

	public int getReadSize() {
		return readSize;
	}

	public void setReadSize(int readSize) {
		this.readSize = readSize;
	}

	public boolean isToggleDtr() {
		return toggleDtr;
	}

	public void setToggleDtr(boolean toggleDtr) {
		this.toggleDtr = toggleDtr;
	}

	public boolean isToggleRts() {
		return toggleRts;
	}

	public void setToggleRts(boolean toggleRts) {
		this.toggleRts = toggleRts;
	}

}
