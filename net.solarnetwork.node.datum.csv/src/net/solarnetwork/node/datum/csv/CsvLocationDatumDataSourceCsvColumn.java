/* ==================================================================
 * CsvDatumDataSourceCsvColumn.java - 10/03/2022 9:30:49 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csv;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for CSV location datum configuration.
 * 
 * @author matt
 * @version 1.0
 */
public enum CsvLocationDatumDataSourceCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The service name. */
	SERVICE_NAME(1, "Service Name"),

	/** The service group. */
	SERVICE_GROUP(2, "Service Group"),

	/** The location key. */
	LOCATION_KEY(3, "Location Key"),

	/** The location type. */
	LOCATION_TYPE(4, "Location Type"),

	/** The schedule. */
	SCHEDULE(5, "Schedule"),

	/** The URL template. */
	URL(6, "URL"),

	/** The character encoding. */
	ENCODING(7, "Encoding"),

	/** The network timeout. */
	TIMEOUT(8, "Timeout"),

	/** The number of rows to skip. */
	SKIP_ROWS(9, "Skip Rows"),

	/** The number of rows to keep. */
	KEEP_ROWS(10, "Keep Rows"),

	/** The URL date format. */
	URL_DATE_FORMAT(11, "URL Date Format"),

	/** The datum timestamp column. */
	DATE_COLUMN(12, "Date Column"),

	/** The CSV date format. */
	DATE_FORMAT(13, "Date Format"),

	/** The time zone to use when parsing/formatting dates. */
	TIME_ZONE(14, "Time Zone"),

	/** The sample cache time. */
	SAMPLE_CACHE(15, "Sample Cache"),

	/** The CSV column. */
	COLUMN(16, "Column"),

	/** The datum property name. */
	PROP_NAME(17, "Property"),

	/** The datum property type. */
	PROP_TYPE(18, "Property Type"),

	/** A property value multiplier. */
	MULTIPLIER(19, "Multiplier"),

	/** A property value offset. */
	OFFSET(20, "Offset"),

	/** A property value decimal scale. */
	DECIMAL_SCALE(21, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private CsvLocationDatumDataSourceCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify component-wide settings.
	 */
	public static final Set<CsvLocationDatumDataSourceCsvColumn> COMPONENT_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, SERVICE_NAME, SERVICE_GROUP, LOCATION_KEY,
					LOCATION_TYPE, SCHEDULE, URL, ENCODING, TIMEOUT, SKIP_ROWS, KEEP_ROWS,
					URL_DATE_FORMAT, DATE_COLUMN, DATE_FORMAT, TIME_ZONE, SAMPLE_CACHE));

	@Override
	public int getCode() {
		return idx;
	}

	/**
	 * Get a friendly name for the column.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
