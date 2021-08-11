/* ==================================================================
 * AE250TxMainFault.java - 12/08/2021 9:28:24 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.tx;

/**
 * AE250TX system warnings.
 * 
 * <p>
 * These are not documented in the manual provided by AE.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxSystemWarning implements AE250TxWarning {

	W1(0, "W1"),

	W2(1, "W2"),

	W3(2, "W3"),

	W4(3, "W4"),

	W5(4, "W5"),

	W6(5, "W6"),

	W7(6, "W7"),

	W8(7, "W8"),

	W9(8, "W9"),

	W10(9, "W10"),

	W11(10, "W11"),

	W12(11, "W12"),

	W13(12, "W13"),

	W14(13, "W14"),

	W15(14, "W15"),

	W16(15, "W16"),

	;

	private final int bit;
	private final String description;

	private AE250TxSystemWarning(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
