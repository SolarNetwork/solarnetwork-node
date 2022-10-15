/* ==================================================================
 * DataRow.java - 5/02/2019 4:32:40 pm
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

package net.solarnetwork.node.datum.egauge.ws.client;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A single EGauge data register value.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class DataRegister {

	private final String name;
	private final String type;
	private final String runtimeType;
	private final BigInteger value;
	private final BigDecimal instant;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the name
	 * @param type
	 *        the type
	 * @param runtimeType
	 *        the runtime type, e.g. {@literal total}, or {@literal null}
	 * @param value
	 *        the value
	 * @param instant
	 *        the rate of change value
	 */
	public DataRegister(String name, String type, String runtimeType, BigInteger value,
			BigDecimal instant) {
		super();
		this.name = name;
		this.type = type;
		this.runtimeType = runtimeType;
		this.value = value;
		this.instant = instant;
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the runtime type.
	 * 
	 * @return the runtime type
	 */
	public String getRuntimeType() {
		return runtimeType;
	}

	/**
	 * Get the value.
	 * 
	 * @return the value
	 */
	public BigInteger getValue() {
		return value;
	}

	/**
	 * Get the instant value.
	 * 
	 * @return the instant value
	 */
	public BigDecimal getInstant() {
		return instant;
	}

}
