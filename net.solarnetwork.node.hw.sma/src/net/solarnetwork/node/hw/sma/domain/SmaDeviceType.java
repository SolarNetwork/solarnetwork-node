/* ==================================================================
 * SmaDeviceType.java - 11/09/2020 10:33:56 AM
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
 * Enumeration of WebBox supported device types.
 * 
 * <p>
 * These device types are for Modbus profile versions <b>prior</b> to 1.30.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaDeviceType implements SmaDeviceKind {

	SunnyWebBox(47, "WebBox"),

	SunnyBoyN000US(268, "Sunny Boy n000US"),

	SunnyBoyNn000TLUS12(269, "Sunny Boy nn000TL-US-12"),

	SunnyCentral500CP(160, "Sunny Central  500CP"),

	SunnyCentral500CPJP(253, "Sunny Central  500CP-JP"),

	SunnyCentral500CPUS(262, "Sunny Central  500CP-US"),

	SunnyCentral500CPUS600V(271, "Sunny Central  500CP-US 600V"),

	SunnyCentral500HE20(202, "Sunny Central  520HE-20"),

	SunnyCentral630CP(159, "Sunny Central  630CP"),

	SunnyCentral630CPJP(122, "Sunny Central  630CP-JP"),

	SunnyCentral630CPUS(261, "Sunny Central  630CP-US"),

	SunnyCentral630HE20(201, "Sunny Central  630HE-20"),

	SunnyCentral720CP(165, "Sunny Central  720CP"),

	SunnyCentral720CPUS(263, "Sunny Central  720CP-US"),

	SunnyCentral720HE(203, "Sunny Central  720HE-20"),

	SunnyCentral760CPUS(264, "Sunny Central  760CP-US"),

	SunnyCentral760CP(164, "Sunny Central  760CP"),

	SunnyCentral800CP(158, "Sunny Central  800CP"),

	SunnyCentral800CPUS(260, "Sunny Central  800CP-US"),

	SunnyCentral800HE(200, "Sunny Central  800HE-20"),

	SunnyCentral850CP(254, "Sunny Central  850CP"),

	SunnyCentral850CPUS(256, "Sunny Central  850CP-US"),

	SunnyCentral900CP(255, "Sunny Central  900CP"),

	SunnyCentral900CPUS(257, "Sunny Central  900CP-US"),

	SunnyCentral250HE(230, "Sunny Central  250HE-11"),

	SunnyCentral400HE(228, "Sunny Central  400HE-11"),

	SunnyCentral500HE(227, "Sunny Central  500HE-10 / SC 500HE-11"),

	SunnyCentral630HE11(166, "Sunny Central  630HE-11"),

	SunnyCentral500HEUS(157, "Sunny Central  500HE-US"),

	SunnyCentral250US(155, "Sunny Central  250-US"),

	SunnyCentral500US(156, "Sunny Central  500-US"),

	SunnyTripower(128, "Sunny Tripower nn000TL-10"),

	SunnyIsland(67, "Sunny Island  2nnn"),

	SunnyIslandUS(69, "Sunny Island  5048 / SI nnnn-US-10"),

	SunnyIslandH10(137, "Sunny Island  n.0H-10"),

	Optiprotect(198, "Optiprotect"),

	SunnyCentralStringMonitor(187, "Sunny Central String-Monitor Controller"),

	SunnyCentralStringMonitorUS(190, "Sunny Central String-Monitor-US"),

	SunnyStringMonitor(171, "Sunny String-Monitor"),

	MeteoStation(232, "SMA Meteo Station"),

	SunnySensorbox(81, "Sunny Sensorbox"),

	;

	private final int code;
	private final String description;

	private SmaDeviceType(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the enumeration, never {@literal null} and set to {@link #None}
	 *         if not any other valid code
	 * @throws IllegalArgumentException
	 *         if {@literal code} is not a valid value
	 */
	public static SmaDeviceType forCode(int code) {
		for ( SmaDeviceType v : values() ) {
			if ( v.code == code ) {
				return v;
			}
		}
		throw new IllegalArgumentException("The device ID " + code + " is not a supported type.");
	}

}
