/* ==================================================================
 * ModbusDataType.java - 20/12/2017 1:59:32 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

/**
 * An enumeration of supported Modbus data types.
 * 
 * @author matt
 * @version 1.0
 */
public enum ModbusDataType {

	Boolean(1),

	Int16(1),

	SignedInt16(1),

	Int32(2),

	Int64(4),

	Float32(2),

	Float64(4),

	Bytes(-1),

	StringUtf8(-1),

	StringAscii(-1);

	final private int wordLength;

	private ModbusDataType(int wordLength) {
		this.wordLength = wordLength;
	}

	/**
	 * Get the number of Modbus words (16-bit register values) this data type
	 * requires.
	 * 
	 * @return the number of words, or {@literal -1} for an unknown length (for
	 *         example for strings)
	 */
	public int getWordLength() {
		return wordLength;
	}

}
