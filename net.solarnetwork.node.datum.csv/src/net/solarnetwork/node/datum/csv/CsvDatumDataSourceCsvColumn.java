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
 * The defined column order for CSV datum configuration.
 * 
 * @author matt
 * @version 1.0
 */
public enum CsvDatumDataSourceCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The service name. */
	SERVICE_NAME(1, "Service Name"),

	/** The service group. */
	SERVICE_GROUP(2, "Service Group"),

	/** The source ID. */
	SOURCE_ID(3, "Source ID"),

	/** The source ID column. */
	SOURCE_ID_COLUMN(4, "Source ID Column"),

	/** The schedule. */
	SCHEDULE(5, "Schedule"),

	/** The URL template. */
	URL(6, "URL"),

	/** The HTTP customizer service UID. */
	HTTP_CUSTOMIZER(7, "Customizer"),

	/** The character encoding. */
	ENCODING(8, "Encoding"),

	/** The network timeout. */
	TIMEOUT(9, "Timeout"),

	/** The number of rows to skip. */
	SKIP_ROWS(10, "Skip Rows"),

	/** The number of rows to keep. */
	KEEP_ROWS(11, "Keep Rows"),

	/** The URL date format. */
	URL_DATE_FORMAT(12, "URL Date Format"),

	/** The datum timestamp column. */
	DATE_COLUMN(13, "Date Column"),

	/** The CSV date format. */
	DATE_FORMAT(14, "Date Format"),

	/** The time zone to use when parsing/formatting dates. */
	TIME_ZONE(15, "Time Zone"),

	/** The sample cache time. */
	SAMPLE_CACHE(16, "Sample Cache"),

	/** The CSV column. */
	COLUMN(17, "Column"),

	/** The datum property name. */
	PROP_NAME(18, "Property"),

	/** The datum property type. */
	PROP_TYPE(19, "Property Type"),

	/** A property value multiplier. */
	MULTIPLIER(20, "Multiplier"),

	/** A property value offset. */
	OFFSET(21, "Offset"),

	/** A property value decimal scale. */
	DECIMAL_SCALE(22, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private CsvDatumDataSourceCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify component-wide settings.
	 */
	public static final Set<CsvDatumDataSourceCsvColumn> COMPONENT_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, SERVICE_NAME, SERVICE_GROUP, SOURCE_ID,
					SOURCE_ID_COLUMN, SCHEDULE, URL, HTTP_CUSTOMIZER, ENCODING, TIMEOUT, SKIP_ROWS,
					KEEP_ROWS, URL_DATE_FORMAT, DATE_COLUMN, DATE_FORMAT, TIME_ZONE, SAMPLE_CACHE));

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
