/* ==================================================================
 * SmaFirmwareVersion.java - 11/09/2020 9:11:13 AM
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

package net.solarnetwork.node.hw.sma.domain;

/**
 * SMA firmware version data structure.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaFirmwareVersion {

	private final byte[] data;

	public static SmaFirmwareVersion forRegisterValue(int dword) {
		byte[] data = new byte[] { 0, 0, 0, 0 };
		for ( int i = 0; i < 4; i++ ) {
			data[i] = (byte) ((dword >>> (8 * (3 - i))) & 0xFF);
		}
		return new SmaFirmwareVersion(data);
	}

	/**
	 * Constructor.
	 */
	public SmaFirmwareVersion(byte[] data) {
		super();
		this.data = data;
	}

	/**
	 * Get the major version.
	 * 
	 * @return the major version
	 */
	public int getMajorVersion() {
		return (data[0] & 0xFF);
	}

	/**
	 * Get the minor version.
	 * 
	 * @return the minor version
	 */
	public int getMinorVersion() {
		return (data[1] & 0xFF);
	}

	/**
	 * Get the build version.
	 * 
	 * @return the build version
	 */
	public int getBuildVersion() {
		return (data[2] & 0xFF);
	}

	/**
	 * Get the release type.
	 * 
	 * @return the release type, never {@literal null}
	 */
	public SmaReleaseType getReleaseType() {
		return SmaReleaseType.forCode(data[3] & 0xFF);
	}

	@Override
	public String toString() {
		SmaReleaseType t = getReleaseType();
		String r = (t == SmaReleaseType.None ? String.valueOf(data[3] & 0xFF) : t.getKey());
		return String.format("%d.%d.%d.%s", getMajorVersion(), getMinorVersion(), getBuildVersion(), r);
	}

}
