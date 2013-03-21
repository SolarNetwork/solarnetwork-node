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
 */

package net.solarnetwork.node.support;

import java.util.List;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Configuration bean for {@link DataCollector}.
 * 
 * @author matt
 * @version 1.0
 */
public class DataCollectorSerialPortBeanParameters extends SerialPortBeanParameters {

	private static final DataCollectorSerialPortBeanParameters DEFAULTS = new DataCollectorSerialPortBeanParameters();

	private int bufferSize = 4096;
	private byte[] magic = new byte[] { 0x13 };
	private int readSize = 8;
	private boolean toggleDtr = true;
	private boolean toggleRts = true;

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param prefix
	 *        the bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(String prefix) {
		return getDefaultSettingSpecifiers(DEFAULTS, prefix);
	}

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param defaults
	 *        the default values to use
	 * @param prefix
	 *        the bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(
			DataCollectorSerialPortBeanParameters defaults, String prefix) {
		List<SettingSpecifier> results = SerialPortBeanParameters.getDefaultSettingSpecifiers(defaults,
				prefix);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "bufferSize", String.valueOf(defaults
				.getBufferSize())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "magicHex", new String(defaults
				.getMagicHex())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "readSize", String.valueOf(defaults
				.getReadSize())));
		results.add(new BasicToggleSettingSpecifier(prefix + "toggleDtr", defaults.isToggleDtr()));
		results.add(new BasicToggleSettingSpecifier(prefix + "toggleRts", defaults.isToggleRts()));
		return results;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public String getMagicHex() {
		if ( getMagic() == null ) {
			return null;
		}
		return Hex.encodeHexString(getMagic());
	}

	public void setMagicHex(String hex) {
		if ( hex == null || (hex.length() % 2) == 1 ) {
			setMagic(null);
		}
		try {
			setMagic(Hex.decodeHex(hex.toCharArray()));
		} catch ( DecoderException e ) {
			// fail silently, sorry
		}
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
