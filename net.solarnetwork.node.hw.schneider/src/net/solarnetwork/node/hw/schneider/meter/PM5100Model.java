/* ==================================================================
 * PM5100Model.java - 17/05/2018 3:13:41 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.meter;

/**
 * Model information from a PM5100 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public enum PM5100Model {

	EM7230(15255),
	EM7280(15256),
	EM7630(15257),
	EM7680(15258),

	PM5000(15240),
	PM5010(15241),

	PM5100(15278),
	PM5110(15270),
	PM5111(15271),

	PM5200(15242),
	PM5250(15243),

	PM5310(15272),
	PM5320(15275),
	PM5330(15273),
	PM5331(15274),
	PM5340(15276),
	PM5341(15277),
	PM5350(15244);

	private final int code;

	private PM5100Model(int code) {
		this.code = code;
	}

	/**
	 * Get the model code.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get an enumeration for a code.
	 * 
	 * @param code
	 *        the code to get the enumeration for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static PM5100Model forCode(int code) {
		for ( PM5100Model e : PM5100Model.values() ) {
			if ( e.code == code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unsupported code: " + code);
	}

}
