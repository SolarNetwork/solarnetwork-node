/* ==================================================================
 * ColumnCsvMetaData.java - 6/10/2016 7:26:43 PM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.util.function.Function;

/**
 * Metadata about a JDBC table column for use in CSV processing.
 *
 * @author matt
 * @version 2.0
 * @since 1.17
 */
public class ColumnCsvMetaData {

	private final String columnName;
	private final Function<Object, Object> cellProcessor;
	private final int sqlType;
	private final boolean primaryKey;

	/**
	 * Construct as a non-primary key column.
	 *
	 * @param columnName
	 *        The column name.
	 * @param cellProcessor
	 *        The cell processor to use when parsing CSV data for this column.
	 * @param sqlType
	 *        The JDBC {@link java.sql.Types} value to use for this column.
	 */
	public ColumnCsvMetaData(String columnName, Function<Object, Object> cellProcessor, int sqlType) {
		this(columnName, cellProcessor, sqlType, false);
	}

	/**
	 * Constructor.
	 *
	 * @param columnName
	 *        The column name.
	 * @param cellProcessor
	 *        The cell processor to use when parsing CSV data for this column.
	 * @param sqlType
	 *        The JDBC {@link java.sql.Types} value to use for this column.
	 * @param primaryKey
	 *        {@code true} if this is a primary key column.
	 */
	public ColumnCsvMetaData(String columnName, Function<Object, Object> cellProcessor, int sqlType,
			boolean primaryKey) {
		super();
		this.columnName = columnName;
		this.cellProcessor = cellProcessor;
		this.sqlType = sqlType;
		this.primaryKey = primaryKey;
	}

	/**
	 * Get the JDBC column name.
	 *
	 * @return The column name.
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Get a {@code CellProcessor} to use with the associated CSV conversion.
	 *
	 * @return A cell processor.
	 */
	public Function<Object, Object> getCellProcessor() {
		return cellProcessor;
	}

	/**
	 * Get the JDBC column type, from the {@link java.sql.Types} class.
	 *
	 * @return The JDBC column type.
	 */
	public int getSqlType() {
		return sqlType;
	}

	/**
	 * Flag if this column is part of the table's primary key.
	 *
	 * @return {@code true} if part of the table's primary key.
	 */
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	/**
	 * Get a copy of this object with the primary key flag set to {@code true}.
	 *
	 * @return A new column metadata instance.
	 */
	public ColumnCsvMetaData asPrimaryKeyColumn() {
		return new ColumnCsvMetaData(columnName, cellProcessor, sqlType, true);
	}

}
