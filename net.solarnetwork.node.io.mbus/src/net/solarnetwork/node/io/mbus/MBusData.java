/* ==================================================================
 * MBusMessage.java - 09/07/2020 11:50:01 am
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
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * A class representing an MBus Message
 * 
 * @author alex
 * @version 2.0
 */
public class MBusData {

	public Instant receivedTime = Instant.now();
	public int status = 0;
	public List<MBusDataRecord> dataRecords = new ArrayList<MBusDataRecord>();

	public MBusData(Instant receivedTime) {
		this.receivedTime = receivedTime;
	}

	public MBusData(MBusData data) {
		this.receivedTime = data.receivedTime;
		this.status = data.status;
		addRecordsFrom(data);
	}

	public void addRecordsFrom(MBusData data) {
		for ( MBusDataRecord record : data.dataRecords ) {
			dataRecords.add(new MBusDataRecord(record));
		}
	}

	/**
	 * Get timestamp of data. If a time cannot be found in the data records,
	 * message reception time will be used
	 * 
	 * @return data timestamp
	 */
	public Instant getDataTimestamp() {
		final MBusDataRecord record = getRecord(MBusDataDescription.DateTime);

		if ( record == null )
			return receivedTime;

		if ( record.getType() != MBusDataType.Date )
			return receivedTime;

		final Instant date = record.getDateValue();
		if ( date == null )
			return receivedTime;

		return date;
	}

	/**
	 * Find a data record given a description
	 * 
	 * @param description
	 *        description type to find record for
	 * @return data record
	 */
	private MBusDataRecord getRecord(MBusDataDescription description) {
		for ( MBusDataRecord record : dataRecords ) {
			if ( record.getDescription() == description ) {
				return record;
			}
		}
		return null;
	}

	/**
	 * Get a scaled numeric value (long, double or BCD), given a description
	 * 
	 * @param description
	 *        description type to find record for
	 * @return scaled value
	 */
	public Double getScaledValue(MBusDataDescription description) {
		final MBusDataRecord record = getRecord(description);
		if ( record == null )
			return null;

		return record.getScaledValue();
	}

	/**
	 * Get a string value, given a description
	 * 
	 * @param description
	 *        description type to find record for
	 * @return string value
	 */
	public String getStringValue(MBusDataDescription description) {
		final MBusDataRecord record = getRecord(description);
		if ( record == null )
			return null;

		return record.getStringValue();
	}

	/**
	 * Get a date value, given a description
	 * 
	 * @param description
	 *        description type to find record for
	 * @return date value
	 */
	public Instant getDateValue(MBusDataDescription description) {
		final MBusDataRecord record = getRecord(description);
		if ( record == null )
			return null;

		return record.getDateValue();
	}

	@Override
	public boolean equals(Object o) {
		if ( o == this ) {
			return true;
		}

		if ( !(o instanceof MBusData) ) {
			return false;
		}

		MBusData d = (MBusData) o;

		return d.dataRecords.equals(this.dataRecords);
	}
}
