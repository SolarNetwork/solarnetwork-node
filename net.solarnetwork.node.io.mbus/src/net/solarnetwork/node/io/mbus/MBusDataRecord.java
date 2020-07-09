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

import java.util.Date;

/**
 * Object to hold data record information extracted from an M-Bus device.
 * 
 * @author alex
 * @version 1.0
 */
public class MBusDataRecord {

	private MBusDataDescription description;
	private MBusDataType type = MBusDataType.None;
	private Object value = null;

	public MBusDataRecord() {
	}

	public MBusDataRecord(MBusDataDescription description, Double value) {
		this.description = description;
		this.type = MBusDataType.Double;
		this.value = value;
	}

	public MBusDataRecord(MBusDataDescription description, MBusDataType type, Long value) {
		this.description = description;
		this.type = type;
		this.value = value;
	}

	public MBusDataRecord(MBusDataDescription description, Date date) {
		this.description = description;
		this.type = MBusDataType.Date;
		this.value = new Date(date.getTime());
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
				this.value = new Date((record.getDateValue().getTime() / 1000) * 1000);
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
	}

	public MBusDataDescription getDescription() {
		return description;
	}

	public MBusDataType getType() {
		return type;
	}

	public double getDoubleValue() {
		return (Double) value;
	}

	public Date getDateValue() {
		return new Date(((Date) value).getTime());
	}

	public Long getLongValue() {
		return (Long) value;
	}

	public String getStringValue() {
		return new String((String) value);
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
				&& (dr.type == MBusDataType.None || dr.value.equals(this.value));
	}
}
