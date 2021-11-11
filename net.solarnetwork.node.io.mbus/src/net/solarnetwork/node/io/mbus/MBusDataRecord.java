/* ==================================================================
 * MBusDataRecord.java - 29/06/2020 09:55:29 AM
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

package net.solarnetwork.node.io.mbus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Object to hold data record information extracted from an M-Bus device.
 * 
 * @author alex
 * @version 2.0
 */
public class MBusDataRecord {

	private MBusDataDescription description;
	private MBusDataType type = MBusDataType.None;
	private Object value = null;
	private int multiplierExponent = 0;

	public MBusDataRecord() {
	}

	public MBusDataRecord(MBusDataDescription description, Double value, int multiplierExponent) {
		this.description = description;
		this.type = MBusDataType.Double;
		this.value = value;
		this.multiplierExponent = multiplierExponent;
	}

	public MBusDataRecord(MBusDataDescription description, MBusDataType type, Long value,
			int multiplierExponent) {
		this.description = description;
		this.type = type;
		this.value = value;
		this.multiplierExponent = multiplierExponent;
	}

	public MBusDataRecord(MBusDataDescription description, Instant date) {
		this.description = description;
		this.type = MBusDataType.Date;
		this.value = date;
	}

	public MBusDataRecord(MBusDataDescription description, String value) {
		this.description = description;
		this.type = MBusDataType.String;
		this.value = new String(value);
	}

	public MBusDataRecord(MBusDataRecord record) {
		this.description = record.description;
		this.type = record.type;
		switch (type) {
			case Date:
				this.value = record.getDateValue().truncatedTo(ChronoUnit.SECONDS);
				break;
			case Double:
				this.value = record.getDoubleValue();
				break;
			case BCD:
			case Long:
				this.value = record.getLongValue();
				break;
			case String:
				this.value = new String(record.getStringValue());
				break;
			case None:
			default:
				break;

		}
		this.multiplierExponent = record.multiplierExponent;
	}

	public MBusDataDescription getDescription() {
		return description;
	}

	public MBusDataType getType() {
		return type;
	}

	public Double getDoubleValue() {
		return (Double) value;
	}

	public Instant getDateValue() {
		return (Instant) value;
	}

	public Long getLongValue() {
		return (Long) value;
	}

	public String getStringValue() {
		return new String((String) value);
	}

	public int getMultiplierExponent() {
		return multiplierExponent;
	}

	public Double getScaledValue() {
		switch (type) {
			case Double:
				return getDoubleValue() * Math.pow(10, multiplierExponent);
			case BCD:
			case Long:
				return getLongValue().doubleValue() * Math.pow(10, multiplierExponent);
			case Date:
			case None:
			case String:
			default:
				return null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( o == this ) {
			return true;
		}

		if ( !(o instanceof MBusDataRecord) ) {
			return false;
		}

		MBusDataRecord dr = (MBusDataRecord) o;

		return dr.description == this.description && dr.type == this.type
				&& (dr.type == MBusDataType.None || dr.value.equals(this.value))
				&& dr.multiplierExponent == this.multiplierExponent;
	}
}
