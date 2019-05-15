/* ==================================================================
 * KTLCTFirmwareVersion.java - 26/03/2019 11:26:00 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

/**
 * Firmware version for KTL CT devices.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class KTLCTFirmwareVersion {

	private final int dspVersion;
	private final int mcuVersion;

	public KTLCTFirmwareVersion(int dspVersion, int mcuVersion) {
		super();
		this.dspVersion = dspVersion;
		this.mcuVersion = mcuVersion;
	}

	/**
	 * Get the DSP version.
	 * 
	 * @return the DSP version
	 */
	public int getDspVersion() {
		return dspVersion;
	}

	/**
	 * Get the MCU version.
	 * 
	 * @return the MCU version
	 */
	public int getMcuVersion() {
		return mcuVersion;
	}

	/**
	 * Get an instance for a raw version code value.
	 * 
	 * @param code
	 *        the version code
	 * @return the version instance
	 */
	public static KTLCTFirmwareVersion forCode(int code) {
		int dsp = (code >> 16) & 0xFF;
		int mcu = (code & 0xFF);
		return new KTLCTFirmwareVersion(dsp, mcu);
	}

}
