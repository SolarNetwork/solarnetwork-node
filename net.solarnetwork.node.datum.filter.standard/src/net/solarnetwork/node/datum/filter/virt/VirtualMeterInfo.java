/* ==================================================================
 * VirtualMeterInfo.java - 28/04/2025 12:53:45 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.virt;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Information about a virtual meter state.
 *
 * @author matt
 * @version 1.0
 * @since 3.13
 */
public final class VirtualMeterInfo {

	private Instant date;
	private BigDecimal value;
	private BigDecimal reading;

	/**
	 * Constructor.
	 */
	public VirtualMeterInfo() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param date
	 *        the date
	 * @param value
	 *        the value
	 * @param reading
	 *        the reading
	 */
	public VirtualMeterInfo(Instant date, BigDecimal value, BigDecimal reading) {
		super();
		this.date = date;
		this.value = value;
		this.reading = reading;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VirtualMeterInfo{");
		if ( date != null ) {
			builder.append("date=");
			builder.append(date);
			builder.append(", ");
		}
		if ( value != null ) {
			builder.append("value=");
			builder.append(value);
			builder.append(", ");
		}
		if ( reading != null ) {
			builder.append("reading=");
			builder.append(reading);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * The information date.
	 *
	 * @return the date
	 */
	public Instant getDate() {
		return date;
	}

	/**
	 * Set the information date.
	 *
	 * @param date
	 *        the date to set
	 */
	public void setDate(Instant date) {
		this.date = date;
	}

	/**
	 * Get the meter input value.
	 *
	 * @return the input value
	 */
	public BigDecimal getValue() {
		return value;
	}

	/**
	 * Set the meter input value.
	 *
	 * @param value
	 *        the input value to set
	 */
	public void setValue(BigDecimal value) {
		this.value = value;
	}

	/**
	 * Set the meter output value (the reading).
	 *
	 * @return the reading
	 */
	public BigDecimal getReading() {
		return reading;
	}

	/**
	 * Get the meter output value (the reading).
	 *
	 * @param reading
	 *        the reading to set
	 */
	public void setReading(BigDecimal reading) {
		this.reading = reading;
	}

}
