/* ==================================================================
 * GenericSmaCodedValue.java - 17/09/2020 10:32:51 AM
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
 * Generic implementation of {@link SmaCodedValue} for non-enumerated constants.
 * 
 * @author matt
 * @version 1.0
 */
public class GenericSmaCodedValue implements SmaCodedValue {

	private final int code;
	private final String description;

	/**
	 * Constructor.
	 * 
	 * @param code
	 *        the code
	 */
	public GenericSmaCodedValue(int code) {
		this(code, String.valueOf(code));
	}

	/**
	 * Constructor.
	 * 
	 * @param code
	 *        the code
	 * @param description
	 *        the description
	 */
	public GenericSmaCodedValue(int code, String description) {
		super();
		this.code = code;
		this.description = (description != null ? description : String.valueOf(code));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GenericSmaCodedValue{");
		builder.append(code);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
